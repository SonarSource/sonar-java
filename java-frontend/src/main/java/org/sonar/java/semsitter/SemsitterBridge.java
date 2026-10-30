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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.sonar.java.semsitter.PlatformDetection.Platform;

/**
 * JSONL stdin/stdout client for the {@code semsitter-bridge} executable.
 *
 * <p>One JSON object per line. A parse request is
 * {@code {"id":1,"op":"parse","source":"hello"}} and the reply is one line
 * containing {@code id}, {@code ok}, {@code hasError} and {@code tree}.
 * Tree positions are tree-sitter's 0-based rows and columns.
 * {@link #start()} extracts {@code bridge/bridge-<os>-<arch>.xz} from the classpath,
 * the same layout SonarDRE uses, and runs the {@code bridge} binary inside it.
 */
public final class SemsitterBridge implements AutoCloseable {

  private static volatile Path extractedExecutable;

  private static final int STDERR_LIMIT = 8192;

  private final Process process;
  private final BufferedWriter toBridge;
  private final BufferedReader fromBridge;
  private final StringBuilder stderr = new StringBuilder();
  private int nextId = 1;
  private boolean closed;

  private SemsitterBridge(Process process) {
    this.process = process;
    this.toBridge = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
    this.fromBridge = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
    Thread drain = new Thread(() -> drain(process.getErrorStream(), stderr), "semsitter-bridge-stderr");
    drain.setDaemon(true);
    drain.start();
  }

  public static SemsitterBridge start() throws IOException {
    return start(extractedExecutable());
  }

  public static SemsitterBridge start(Path executable) throws IOException {
    Process process = new ProcessBuilder(executable.toAbsolutePath().toString()).start();
    return new SemsitterBridge(process);
  }

  private static Path extractedExecutable() throws IOException {
    Path current = extractedExecutable;
    if (current != null && Files.isExecutable(current)) {
      return current;
    }
    synchronized (SemsitterBridge.class) {
      if (extractedExecutable == null || !Files.isExecutable(extractedExecutable)) {
        Platform platform = new PlatformDetection().getPlatform();
        Path directory = Files.createTempDirectory("bridge");
        extractedExecutable = BridgeExtractor.extract(directory, platform);
      }
      return extractedExecutable;
    }
  }

  /**
   * @return the raw response line, so a caller can log the exact JSON the bridge produced
   */
  public synchronized String parse(String source) throws IOException {
    JsonObject request = new JsonObject();
    request.addProperty("id", nextId);
    request.addProperty("op", "parse");
    request.addProperty("source", source);
    nextId++;
    return exchange(request);
  }

  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      JsonObject request = new JsonObject();
      request.addProperty("id", nextId);
      request.addProperty("op", "quit");
      nextId++;
      exchange(request);
    } catch (IOException ignored) {
      process.destroy();
    }
    try {
      toBridge.close();
    } catch (IOException ignored) {
      process.destroy();
    }
    process.destroy();
    try {
      if (!process.waitFor(2, TimeUnit.SECONDS)) {
        process.destroyForcibly();
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
    }
  }

  private String exchange(JsonObject request) throws IOException {
    int id = request.get("id").getAsInt();
    toBridge.write(request.toString());
    toBridge.write('\n');
    toBridge.flush();
    String responseLine = fromBridge.readLine();
    if (responseLine == null) {
      throw new IOException("semsitter bridge closed stdout" + stderrSuffix());
    }
    JsonElement parsed;
    try {
      parsed = JsonParser.parseString(responseLine);
    } catch (RuntimeException invalid) {
      throw new IOException("semsitter bridge returned invalid JSON: " + responseLine + stderrSuffix(), invalid);
    }
    if (!parsed.isJsonObject()) {
      throw new IOException("semsitter bridge returned a non-object: " + responseLine + stderrSuffix());
    }
    JsonObject response = parsed.getAsJsonObject();
    if (response.has("id") && response.get("id").isJsonPrimitive() && response.get("id").getAsJsonPrimitive().isNumber()
      && response.get("id").getAsInt() != id) {
      throw new IOException("semsitter bridge response id mismatch: " + responseLine);
    }
    if (!response.has("ok") || !response.get("ok").getAsBoolean()) {
      String error = response.has("error") ? response.get("error").getAsString() : responseLine;
      throw new IOException("semsitter bridge: " + error + stderrSuffix());
    }
    return responseLine;
  }

  private String stderrSuffix() {
    synchronized (stderr) {
      if (stderr.isEmpty()) {
        return "";
      }
      return " stderr: " + stderr;
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
      // The process closed stderr.
    }
  }
}
