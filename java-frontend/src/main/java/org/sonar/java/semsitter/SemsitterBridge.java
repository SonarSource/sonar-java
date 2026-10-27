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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * JSONL stdin/stdout client for the {@code semsitter-bridge} executable.
 *
 * <p>One JSON object per line. {@link #analyze} scans a batch of files with the classpath,
 * then {@link #cst} returns the CST of one of them with its SemSitter semantics:
 * {@code tree} (nodes with kind {@code k}, field {@code f}, position {@code p} as
 * 1-based lines and 0-based UTF-16 columns, text {@code t}, children {@code c},
 * declared symbol ids {@code decls}, resolved occurrences {@code refs}),
 * {@code symbols} (the semantic dump symbols) and {@code facts} (graph facts by id).
 */
public final class SemsitterBridge implements AutoCloseable {

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

  public static SemsitterBridge start(Path executable) throws IOException {
    Process process = new ProcessBuilder(executable.toAbsolutePath().toString(), "serve").start();
    return new SemsitterBridge(process);
  }

  /**
   * Scans {@code files} as one project rooted at {@code root}.
   * @return the response, whose {@code files} are the project-relative paths accepted by {@link #cst}
   */
  public synchronized JsonObject analyze(Path root, Collection<Path> files, Collection<String> classpath) throws IOException {
    JsonObject request = request("analyze");
    request.addProperty("root", root.toAbsolutePath().toString());
    JsonArray fileArray = new JsonArray();
    files.forEach(f -> fileArray.add(f.toAbsolutePath().toString()));
    request.add("files", fileArray);
    JsonArray classpathArray = new JsonArray();
    classpath.forEach(classpathArray::add);
    request.add("classpath", classpathArray);
    return exchange(request);
  }

  public synchronized JsonObject cst(String path) throws IOException {
    JsonObject request = request("cst");
    request.addProperty("path", path);
    return exchange(request).getAsJsonObject("cst");
  }

  /**
   * @return declarations (kind, supertypes, members) of the types named by erased binary names, and of their supertypes
   */
  public synchronized JsonObject types(Collection<String> names) throws IOException {
    JsonObject request = request("types");
    JsonArray nameArray = new JsonArray();
    names.forEach(nameArray::add);
    request.add("names", nameArray);
    return exchange(request).getAsJsonObject("types");
  }

  public synchronized JsonObject parse(String source) throws IOException {
    JsonObject request = request("parse");
    request.addProperty("source", source);
    return exchange(request).getAsJsonObject("cst");
  }

  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    try {
      exchange(request("quit"));
      toBridge.close();
    } catch (IOException ignored) {
      process.destroy();
    }
    try {
      if (!process.waitFor(2, TimeUnit.SECONDS)) {
        process.destroyForcibly();
      }
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
    }
  }

  private JsonObject request(String op) {
    JsonObject request = new JsonObject();
    request.addProperty("id", nextId++);
    request.addProperty("op", op);
    return request;
  }

  private JsonObject exchange(JsonObject request) throws IOException {
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
      throw new IOException("semsitter bridge returned invalid JSON" + stderrSuffix(), invalid);
    }
    if (!parsed.isJsonObject()) {
      throw new IOException("semsitter bridge returned a non-object" + stderrSuffix());
    }
    JsonObject response = parsed.getAsJsonObject();
    if (!response.has("id") || response.get("id").getAsInt() != id) {
      throw new IOException("semsitter bridge response id mismatch, expected " + id);
    }
    if (!response.has("ok") || !response.get("ok").getAsBoolean()) {
      String error = response.has("error") ? response.get("error").getAsString() : responseLine;
      throw new IOException("semsitter bridge: " + error + stderrSuffix());
    }
    return response;
  }

  private String stderrSuffix() {
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
