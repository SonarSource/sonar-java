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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client for the {@code semsitter-bridge} executable.
 *
 * <p>A current bridge is spoken to over shared memory: protobuf payloads, one ping byte on the
 * pipe. {@code -Dsonar.java.semsitter.dump=<dir>} also asks it to write {@code udg.json}, the
 * semantic-dump JSON and the annotated CST JSON under that directory.
 *
 * <p>{@link #analyze} scans a batch of files with the classpath, then {@link #cst} returns the
 * CST of one of them with its SemSitter semantics: {@code tree} (nodes with kind {@code k},
 * field {@code f}, position {@code p} as 1-based lines and 0-based UTF-16 columns, text {@code t},
 * children {@code c}, declared symbol ids {@code decls}, resolved occurrences {@code refs}),
 * {@code symbols} (the semantic dump symbols) and {@code facts} (graph facts by id).
 */
public final class SemsitterBridge implements AutoCloseable {

  private static final int STDERR_LIMIT = 8192;

  private final Session session;
  private int nextId = 1;
  private boolean closed;

  private SemsitterBridge(Session session) {
    this.session = session;
  }

  public static SemsitterBridge start(Path executable) throws IOException {
    boolean shm = speaksShm(executable);
    List<String> command = new ArrayList<>();
    command.add(executable.toAbsolutePath().toString());
    command.add("serve");
    if (shm) {
      command.add("--shm");
    }
    String dump = System.getProperty("sonar.java.semsitter.dump");
    if (shm && dump != null && !dump.isBlank()) {
      command.add("--dump-json");
      command.add(dump);
    }
    Process process = new ProcessBuilder(command).start();
    return new SemsitterBridge(shm ? new ShmSession(process) : new JsonlSession(process));
  }

  /**
   * Scans {@code files} as one project rooted at {@code root}.
   * @return the response, whose {@code files} are the project-relative paths accepted by {@link #cst}
   */
  public synchronized JsonObject analyze(Path root, Collection<Path> files, Collection<String> classpath) throws IOException {
    return session.exchange(BridgeFrames.analyze(nextId++, root.toAbsolutePath().toString(), absolute(files), classpath), jsonAnalyze(root, files, classpath));
  }

  public synchronized JsonObject cst(String path) throws IOException {
    JsonObject request = new JsonObject();
    request.addProperty("op", "cst");
    request.addProperty("path", path);
    return session.exchange(BridgeFrames.cst(nextId++, path), request).getAsJsonObject("cst");
  }

  /**
   * @return declarations (kind, supertypes, members) of the types named by erased binary names, and of their supertypes
   */
  public synchronized JsonObject types(Collection<String> names) throws IOException {
    return session.exchange(BridgeFrames.types(nextId++, names), jsonTypes(names)).getAsJsonObject("types");
  }

  public synchronized JsonObject parse(String source) throws IOException {
    JsonObject request = new JsonObject();
    request.addProperty("op", "parse");
    request.addProperty("source", source);
    return session.exchange(BridgeFrames.parse(nextId++, source), request).getAsJsonObject("cst");
  }

  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    session.close(BridgeFrames.quit(nextId++));
  }

  private static boolean speaksShm(Path executable) throws IOException {
    Process help = new ProcessBuilder(executable.toAbsolutePath().toString(), "-h").start();
    String text = new String(help.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    try {
      help.waitFor(2, TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
    help.destroyForcibly();
    return text.contains("--shm");
  }

  private static JsonObject jsonAnalyze(Path root, Collection<Path> files, Collection<String> classpath) {
    JsonObject request = new JsonObject();
    request.addProperty("op", "analyze");
    request.addProperty("root", root.toAbsolutePath().toString());
    JsonArray fileArray = new JsonArray();
    absolute(files).forEach(fileArray::add);
    request.add("files", fileArray);
    JsonArray classpathArray = new JsonArray();
    classpath.forEach(classpathArray::add);
    request.add("classpath", classpathArray);
    return request;
  }

  private static JsonObject jsonTypes(Collection<String> names) {
    JsonObject request = new JsonObject();
    request.addProperty("op", "types");
    JsonArray nameArray = new JsonArray();
    names.forEach(nameArray::add);
    request.add("names", nameArray);
    return request;
  }

  private static List<String> absolute(Collection<Path> files) {
    List<String> paths = new ArrayList<>();
    files.forEach(file -> paths.add(file.toAbsolutePath().toString()));
    return paths;
  }

  private interface Session {
    JsonObject exchange(byte[] protobuf, JsonObject jsonl) throws IOException;

    void close(byte[] quit);
  }

  private static final class ShmSession implements Session {
    private final Process process;
    private final OutputStream toBridge;
    private final InputStream fromBridge;
    private final SharedSlots slots;
    private final StringBuilder stderr = new StringBuilder();

    private ShmSession(Process process) throws IOException {
      this.process = process;
      this.toBridge = process.getOutputStream();
      this.fromBridge = process.getInputStream();
      Thread drain = new Thread(() -> drain(process.getErrorStream(), stderr), "semsitter-bridge-stderr");
      drain.setDaemon(true);
      drain.start();
      int ready = fromBridge.read();
      if (ready != SharedSlots.READY) {
        throw new IOException("semsitter bridge did not open shared memory" + suffix(stderr));
      }
      this.slots = SharedSlots.open(process.pid());
      toBridge.write(SharedSlots.ATTACHED);
      toBridge.flush();
    }

    @Override
    public JsonObject exchange(byte[] protobuf, JsonObject jsonl) throws IOException {
      slots.writeRequest(protobuf, toBridge, fromBridge);
      JsonObject response = BridgeFrames.response(slots.readResponse(fromBridge, toBridge));
      if (!response.has("ok") || !response.get("ok").getAsBoolean()) {
        String error = response.has("error") ? response.get("error").getAsString() : "bridge request failed";
        throw new IOException("semsitter bridge: " + error + suffix(stderr));
      }
      return response;
    }

    @Override
    public void close(byte[] quit) {
      try {
        exchange(quit, new JsonObject());
        slots.close();
        toBridge.close();
      } catch (IOException ignored) {
        process.destroy();
      }
      waitFor(process);
    }
  }

  private static final class JsonlSession implements Session {
    private final Process process;
    private final BufferedWriter toBridge;
    private final BufferedReader fromBridge;
    private final StringBuilder stderr = new StringBuilder();
    private int nextId = 1;

    private JsonlSession(Process process) {
      this.process = process;
      this.toBridge = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
      this.fromBridge = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
      Thread drain = new Thread(() -> drain(process.getErrorStream(), stderr), "semsitter-bridge-stderr");
      drain.setDaemon(true);
      drain.start();
    }

    @Override
    public JsonObject exchange(byte[] protobuf, JsonObject jsonl) throws IOException {
      int id = nextId++;
      jsonl.addProperty("id", id);
      toBridge.write(jsonl.toString());
      toBridge.write('\n');
      toBridge.flush();
      String responseLine = fromBridge.readLine();
      if (responseLine == null) {
        throw new IOException("semsitter bridge closed stdout" + suffix(stderr));
      }
      JsonElement parsed;
      try {
        JsonReader reader = new JsonReader(new StringReader(responseLine));
        reader.setNestingLimit(Integer.MAX_VALUE);
        parsed = JsonParser.parseReader(reader);
      } catch (RuntimeException invalid) {
        throw new IOException("semsitter bridge returned invalid JSON" + suffix(stderr), invalid);
      }
      if (!parsed.isJsonObject()) {
        throw new IOException("semsitter bridge returned a non-object" + suffix(stderr));
      }
      JsonObject response = parsed.getAsJsonObject();
      if (!response.has("id") || response.get("id").getAsInt() != id) {
        throw new IOException("semsitter bridge response id mismatch, expected " + id);
      }
      if (!response.has("ok") || !response.get("ok").getAsBoolean()) {
        String error = response.has("error") ? response.get("error").getAsString() : responseLine;
        throw new IOException("semsitter bridge: " + error + suffix(stderr));
      }
      return response;
    }

    @Override
    public void close(byte[] quit) {
      try {
        JsonObject request = new JsonObject();
        request.addProperty("op", "quit");
        exchange(quit, request);
        toBridge.close();
      } catch (IOException ignored) {
        process.destroy();
      }
      waitFor(process);
    }
  }

  private static void waitFor(Process process) {
    try {
      if (!process.waitFor(2, TimeUnit.SECONDS)) {
        process.destroyForcibly();
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
    }
  }

  private static String suffix(StringBuilder stderr) {
    synchronized (stderr) {
      return stderr.isEmpty() ? "" : (" stderr: " + stderr);
    }
  }

  private static void drain(InputStream stream, StringBuilder buffer) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        synchronized (buffer) {
          buffer.append(line).append('\n');
          if (buffer.length() > STDERR_LIMIT) {
            buffer.delete(0, buffer.length() - STDERR_LIMIT);
          }
        }
      }
    } catch (IOException ignored) {
      // stderr closed with the process
    }
  }
}
