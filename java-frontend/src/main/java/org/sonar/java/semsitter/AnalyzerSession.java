/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Client of {@code udg-gen --server} analyzer queries: one process for the whole JVM, one scan per module and classpath,
 * {@code get-file} and {@code get-types} per request, and a type cache so each declaration crosses once.
 */
final class AnalyzerSession {

  static final AtomicLong BYTES_IN = new AtomicLong();
  static final AtomicLong BYTES_OUT = new AtomicLong();
  static final AtomicLong REQUESTS = new AtomicLong();
  static final AtomicLong TYPE_REQUESTS = new AtomicLong();

  private static AnalyzerSession shared;

  private final Path executable;
  private Process process;
  private OutputStream toServer;
  private DataInputStream fromServer;
  private String startedRoot;
  private String scannedKey;
  private static volatile Path moduleRoot;
  private static volatile Map<String, List<String>> moduleSettings = Map.of();
  private final Map<String, JsonObject> stableTypes = new HashMap<>();
  private final Map<String, JsonObject> batchTypes = new HashMap<>();
  private final Set<String> absent = new HashSet<>();
  private int nextId = 1;

  AnalyzerSession(Path executable) {
    this.executable = executable;
  }

  /** The module base directory the server scans; without it, the common ancestor of the files of a request. */
  static void moduleRoot(Path root) {
    moduleRoot = root;
  }

  /** Server settings of the module (the {@code jvm.*} classpath keys), as the scanner properties give them. */
  static void moduleSettings(Map<String, List<String>> settings) {
    moduleSettings = settings;
  }

  static synchronized AnalyzerSession shared(Path executable) {
    if (shared == null) {
      shared = new AnalyzerSession(executable);
      Runtime.getRuntime().addShutdownHook(new Thread(shared::quit));
    }
    return shared;
  }

  /**
   * A module is scanned once per classpath: later batches with the same root and classpath only iterate its files. The
   * server takes its root on the command line, so a new root restarts it.
   */
  synchronized JsonObject analyze(JsonObject request) throws IOException {
    String root = projectRoot(request);
    if (process != null && !root.equals(startedRoot)) {
      stop();
    }
    if (process == null) {
      start(root);
    }
    JsonObject settings = settings(request);
    String key = root + "\n" + settings;
    if (!key.equals(scannedKey)) {
      scan(settings);
      scannedKey = key;
    }
    JsonObject result = new JsonObject();
    result.addProperty("ok", true);
    result.addProperty("root", root);
    result.add("files", request.getAsJsonArray("files"));
    return result;
  }

  /**
   * The module root when set, else the request's root when every file is under it, else the closest directory above all
   * of them.
   */
  private static String projectRoot(JsonObject request) {
    Path module = moduleRoot;
    if (module != null) {
      return module.toAbsolutePath().toString();
    }
    Path root = Path.of(request.get("root").getAsString());
    List<Path> files = new ArrayList<>();
    request.getAsJsonArray("files").forEach(f -> files.add(Path.of(f.getAsString())));
    if (files.isEmpty() || files.stream().allMatch(f -> f.startsWith(root))) {
      return root.toString();
    }
    Path common = files.get(0).getParent();
    for (Path file : files) {
      while (common != null && !file.startsWith(common)) {
        common = common.getParent();
      }
    }
    return (common == null ? root : common).toString();
  }

  private void start(String root) throws IOException {
    List<String> command = new ArrayList<>();
    command.add(executable.toAbsolutePath().toString());
    command.addAll(List.of("--server", "--lang", "java", "--modifiers", "--structural", "--decl-location", "--field-items",
      "--field-reference-edges", "--override-edges", "--src", root, "--project-root", root));
    String extra = System.getProperty("sonar.java.semsitter.udg.args");
    if (extra != null && !extra.isBlank()) {
      command.addAll(List.of(extra.split(" ")));
    }
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.redirectError(ProcessBuilder.Redirect.INHERIT);
    process = builder.start();
    startedRoot = root;
    toServer = new BufferedOutputStream(process.getOutputStream());
    fromServer = new DataInputStream(process.getInputStream());
    try {
      JsonObject hello = new JsonObject();
      hello.addProperty("cmd", "hello");
      String protocol = send(hello, false).header.get("protocol").getAsString();
      if (!protocol.startsWith("1.")) {
        throw new IOException("unsupported analyzer protocol " + protocol);
      }
    } catch (IOException | RuntimeException e) {
      stop();
      throw e;
    }
  }

  /** Ends the process and forgets everything tied to it, so the next request starts a fresh one. */
  private void stop() {
    if (process != null) {
      process.destroy();
    }
    process = null;
    toServer = null;
    fromServer = null;
    startedRoot = null;
    scannedKey = null;
    stableTypes.clear();
    batchTypes.clear();
    absent.clear();
  }

  /**
   * The module {@code jvm.*} settings win when they name libraries or binaries; otherwise the request classpath is used,
   * minus the JDK runtime image, which the server reads from {@code jvm.jdk_home}.
   */
  private static JsonObject settings(JsonObject request) {
    JsonObject settings = new JsonObject();
    Map<String, List<String>> configured = moduleSettings;
    for (Map.Entry<String, List<String>> entry : configured.entrySet()) {
      JsonArray values = new JsonArray();
      entry.getValue().forEach(values::add);
      settings.add(entry.getKey(), values);
    }
    if (!settings.has("jvm.libraries") && !settings.has("jvm.binaries")) {
      JsonArray libraries = new JsonArray();
      for (JsonElement entry : request.getAsJsonArray("classpath")) {
        if (!isJdkImage(entry.getAsString())) {
          libraries.add(entry.getAsString());
        }
      }
      settings.add("jvm.libraries", libraries);
    }
    if (!settings.has("jvm.jdk_home")) {
      JsonArray home = new JsonArray();
      home.add(System.getProperty("java.home"));
      settings.add("jvm.jdk_home", home);
    }
    return settings;
  }

  /** The JDK runtime image ({@code lib/modules}), whichever separator the entry uses. */
  static boolean isJdkImage(String entry) {
    return entry.replace('\\', '/').endsWith("/lib/modules");
  }

  private void scan(JsonObject settings) throws IOException {
    batchTypes.clear();
    absent.clear();
    if (System.getenv("SEMSITTER_COUNTERS_FILE") != null) {
      System.err.println("SEMSITTER-SETTINGS " + settings);
    }
    JsonObject set = new JsonObject();
    set.addProperty("cmd", "set-settings");
    set.add("settings", settings);
    send(set, false);
    JsonObject full = new JsonObject();
    full.addProperty("cmd", "full-gen");
    send(full, false);
  }

  synchronized JsonObject cst(JsonObject request) throws IOException {
    JsonObject get = new JsonObject();
    get.addProperty("cmd", "get-file");
    get.addProperty("path", request.get("path").getAsString());
    String ahead = System.getProperty("sonar.java.semsitter.udg.prefetch");
    if (ahead != null) {
      get.addProperty("ahead", Integer.parseInt(ahead));
    }
    Answer answer = send(get, true);
    // The bridge's response message around the document: ok = true, document = field 5.
    ByteArrayOutputStream wrapped = new ByteArrayOutputStream(answer.payload.length + 16);
    wrapped.write(0x10);
    wrapped.write(0x01);
    wrapped.write(0x2a);
    varint(wrapped, answer.payload.length);
    wrapped.write(answer.payload);
    return BridgeFrames.response(wrapped.toByteArray());
  }

  /** The declarations of {@code names} and their supertypes, asking the server only for names never seen. */
  synchronized JsonObject types(Set<String> names) throws IOException {
    Set<String> missing = new HashSet<>();
    for (String name : names) {
      String erased = erase(name);
      if (!stableTypes.containsKey(erased) && !batchTypes.containsKey(erased) && !absent.contains(erased)) {
        missing.add(erased);
      }
    }
    if (!missing.isEmpty()) {
      JsonObject request = new JsonObject();
      request.addProperty("cmd", "get-types");
      JsonArray array = new JsonArray();
      missing.forEach(array::add);
      request.add("names", array);
      TYPE_REQUESTS.incrementAndGet();
      Answer answer = send(request, true);
      Set<String> project = new HashSet<>();
      if (answer.header.has("project")) {
        answer.header.getAsJsonArray("project").forEach(e -> project.add(e.getAsString()));
      }
      JsonReader reader = new JsonReader(new StringReader(new String(answer.payload, StandardCharsets.UTF_8)));
      reader.setNestingLimit(Integer.MAX_VALUE);
      for (Map.Entry<String, JsonElement> entry : JsonParser.parseReader(reader).getAsJsonObject().entrySet()) {
        (project.contains(entry.getKey()) ? batchTypes : stableTypes).put(entry.getKey(), entry.getValue().getAsJsonObject());
      }
      for (String name : missing) {
        if (!stableTypes.containsKey(name) && !batchTypes.containsKey(name)) {
          absent.add(name);
        }
      }
    }
    JsonObject found = new JsonObject();
    Deque<String> queue = new ArrayDeque<>();
    names.forEach(name -> queue.add(erase(name)));
    Set<String> seen = new HashSet<>();
    while (!queue.isEmpty()) {
      String name = queue.poll();
      if (!seen.add(name)) {
        continue;
      }
      JsonObject info = batchTypes.containsKey(name) ? batchTypes.get(name) : stableTypes.get(name);
      if (info == null) {
        continue;
      }
      found.add(name, info);
      for (String key : new String[] {"super", "interfaces"}) {
        JsonElement value = info.get(key);
        if (value == null || value.isJsonNull()) {
          continue;
        }
        if (value.isJsonArray()) {
          value.getAsJsonArray().forEach(e -> {
            if (e.isJsonPrimitive()) {
              queue.add(erase(e.getAsString()));
            }
          });
        } else if (value.isJsonPrimitive()) {
          queue.add(erase(value.getAsString()));
        }
      }
    }
    JsonObject response = new JsonObject();
    response.addProperty("ok", true);
    response.add("types", found);
    return response;
  }

  private record Answer(JsonObject header, byte[] payload) {}

  private Answer send(JsonObject request, boolean payload) throws IOException {
    try {
      return exchange(request);
    } catch (IOException | RuntimeException e) {
      System.err.println("SERVER-ERROR " + request.get("cmd") + " " + e);
      if (process != null && !process.isAlive()) {
        stop();
      }
      throw e;
    }
  }

  private Answer exchange(JsonObject request) throws IOException {
    request.addProperty("id", nextId++);
    byte[] line = (request + "\n").getBytes(StandardCharsets.UTF_8);
    toServer.write(line);
    toServer.flush();
    BYTES_OUT.addAndGet(line.length);
    String text = readLine(fromServer);
    BYTES_IN.addAndGet(text.length() + 1L);
    REQUESTS.incrementAndGet();
    JsonReader reader = new JsonReader(new StringReader(text));
    reader.setNestingLimit(Integer.MAX_VALUE);
    JsonObject header = JsonParser.parseReader(reader).getAsJsonObject();
    if (!header.has("ok") || !header.get("ok").getAsBoolean()) {
      throw new IOException("semsitter server: " + header.get("error"));
    }
    byte[] bytes = new byte[0];
    if (header.has("payload_bytes")) {
      bytes = fromServer.readNBytes(header.get("payload_bytes").getAsInt());
      BYTES_IN.addAndGet(bytes.length);
    }
    return new Answer(header, bytes);
  }

  private static String readLine(InputStream in) throws IOException {
    ByteArrayOutputStream line = new ByteArrayOutputStream(256);
    for (int b = in.read(); b != '\n'; b = in.read()) {
      if (b < 0) {
        throw new IOException("semsitter server closed its output");
      }
      line.write(b);
    }
    return line.toString(StandardCharsets.UTF_8);
  }

  private static void varint(ByteArrayOutputStream out, long value) {
    while ((value & ~0x7FL) != 0) {
      out.write((int) ((value & 0x7F) | 0x80));
      value >>>= 7;
    }
    out.write((int) value);
  }

  private synchronized void quit() {
    if (process == null) {
      return;
    }
    try {
      String counters = System.getenv("SEMSITTER_COUNTERS_FILE");
      if (counters != null && !counters.isBlank()) {
        JsonObject release = new JsonObject();
        release.addProperty("cmd", "release");
        String line = "SERVER-COUNTERS " + send(release, false).header.get("counters") + "\n"
          + String.format("SERVER-CLIENT requests=%d type_requests=%d bytes_in=%d bytes_out=%d%n", REQUESTS.get(), TYPE_REQUESTS.get(),
            BYTES_IN.get(), BYTES_OUT.get());
        Files.writeString(Path.of(counters), line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      }
      JsonObject quit = new JsonObject();
      quit.addProperty("cmd", "quit");
      send(quit, false);
      toServer.close();
      process.waitFor();
    } catch (Exception ignored) {
      process.destroy();
    }
  }

  /** Strips the generic arity suffix of a SemSitter id segment. */
  static String erase(String id) {
    StringBuilder out = new StringBuilder(id.length());
    for (int i = 0; i < id.length(); i++) {
      char c = id.charAt(i);
      if (c == '`') {
        while (i + 1 < id.length() && Character.isDigit(id.charAt(i + 1))) {
          i++;
        }
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }
}
