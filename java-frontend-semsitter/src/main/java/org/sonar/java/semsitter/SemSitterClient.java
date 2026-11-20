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
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.sonarsource.semsitter.wire.Document;

/**
 * Client of one SemSitter Java analyzer server process. Control messages are JSON lines. A {@code get-file} answer
 * carries a {@link Document} and a {@code get-types} answer carries a JSON object, each prefixed by the byte count
 * the header names. The process starts with the client and ends with {@link #close()}.
 */
public final class SemSitterClient implements AutoCloseable {

  static final String PROTOCOL_MAJOR = "2";

  private static final List<String> SERVER_FLAGS = List.of("--server", "--lang", "java", "--modifiers", "--structural",
    "--decl-location", "--field-items", "--field-reference-edges", "--override-edges");

  private final Process process;
  private final BufferedWriter toServer;
  private final InputStream fromServer;
  private int nextId = 1;

  private SemSitterClient(Process process) {
    this.process = process;
    this.toServer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
    this.fromServer = process.getInputStream();
  }

  /**
   * Starts the server on the sources of {@code projectRoot} and checks that it speaks this protocol.
   */
  public static SemSitterClient start(Path executable, Path projectRoot) throws IOException {
    return start(command(List.of(executable.toAbsolutePath().toString()), projectRoot));
  }

  static SemSitterClient start(List<String> command) throws IOException {
    Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
    SemSitterClient client = new SemSitterClient(process);
    try {
      String protocol = client.send(client.command("hello")).get("protocol").getAsString();
      if (!protocol.startsWith(PROTOCOL_MAJOR + ".")) {
        throw new IOException("Unsupported SemSitter protocol " + protocol);
      }
    } catch (IOException | RuntimeException e) {
      process.destroyForcibly();
      throw e;
    }
    return client;
  }

  static List<String> command(List<String> launcher, Path projectRoot) {
    String root = projectRoot.toAbsolutePath().toString();
    List<String> command = new ArrayList<>(launcher);
    command.addAll(SERVER_FLAGS);
    command.addAll(List.of("--src", root, "--project-root", root));
    return command;
  }

  public void setSettings(Map<String, List<String>> settings) throws IOException {
    JsonObject request = command("set-settings");
    JsonObject values = new JsonObject();
    settings.forEach((key, list) -> {
      JsonArray array = new JsonArray();
      list.forEach(array::add);
      values.add(key, array);
    });
    request.add("settings", values);
    send(request);
  }

  /**
   * Scans {@code files}, or the source roots when empty.
   * @return how many files the server scanned
   */
  public int fullGen(Collection<String> files) throws IOException {
    JsonObject request = command("full-gen");
    if (!files.isEmpty()) {
      JsonArray array = new JsonArray();
      files.forEach(array::add);
      request.add("files", array);
    }
    return send(request).get("files").getAsInt();
  }

  public Document getFile(String path, int ahead) throws IOException {
    JsonObject request = command("get-file");
    request.addProperty("path", path);
    request.addProperty("ahead", ahead);
    return Document.parseFrom(exchange(request).payload);
  }

  /**
   * @return the JSON object of declarations, as {@code types.schema.json} describes it
   */
  public byte[] getTypes(Collection<String> names) throws IOException {
    JsonObject request = command("get-types");
    JsonArray array = new JsonArray();
    names.forEach(array::add);
    request.add("names", array);
    return exchange(request).payload;
  }

  public Map<String, Boolean> hasPackage(Collection<String> names) throws IOException {
    JsonObject request = command("has-package");
    JsonArray array = new JsonArray();
    names.forEach(array::add);
    request.add("names", array);
    Map<String, Boolean> found = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> entry : send(request).getAsJsonObject("packages").entrySet()) {
      found.put(entry.getKey(), entry.getValue().getAsBoolean());
    }
    return found;
  }

  private JsonObject command(String name) {
    JsonObject request = new JsonObject();
    request.addProperty("cmd", name);
    return request;
  }

  private synchronized JsonObject send(JsonObject request) throws IOException {
    return exchange(request).header;
  }

  private Answer exchange(JsonObject request) throws IOException {
    int id = nextId;
    nextId++;
    request.addProperty("id", id);
    try {
      toServer.write(request.toString());
      toServer.write('\n');
      toServer.flush();
      String text = readLine(fromServer);
      JsonObject header = JsonParser.parseString(text).getAsJsonObject();
      byte[] payload = new byte[0];
      if (header.has("payload_bytes")) {
        payload = fromServer.readNBytes(header.get("payload_bytes").getAsInt());
      }
      if (header.get("id").getAsInt() != id) {
        throw new IOException("SemSitter server answered request " + header.get("id") + " instead of " + id);
      }
      if (!header.get("ok").getAsBoolean()) {
        throw new IOException("SemSitter server: " + header.get("error").getAsString());
      }
      return new Answer(header, payload);
    } catch (IOException e) {
      String message = e.getMessage().startsWith("SemSitter") ? e.getMessage() : ("SemSitter server: " + e.getMessage());
      throw new IOException(message, e);
    }
  }

  /**
   * Asks the server to quit and waits for it, forcing it to stop when it does not answer.
   */
  @Override
  public void close() {
    try {
      if (process.isAlive()) {
        send(command("quit"));
        toServer.close();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
          process.destroyForcibly();
        }
      }
    } catch (IOException e) {
      process.destroyForcibly();
    } catch (InterruptedException e) {
      process.destroyForcibly();
      Thread.currentThread().interrupt();
    }
  }

  private static String readLine(InputStream in) throws IOException {
    ByteArrayOutputStream line = new ByteArrayOutputStream();
    for (int b = in.read(); b != '\n'; b = in.read()) {
      if (b < 0) {
        throw new IOException("SemSitter server closed its output");
      }
      line.write(b);
    }
    return line.toString(StandardCharsets.UTF_8);
  }

  private static final class Answer {
    private final JsonObject header;
    private final byte[] payload;

    private Answer(JsonObject header, byte[] payload) {
      this.header = header;
      this.payload = payload;
    }
  }
}
