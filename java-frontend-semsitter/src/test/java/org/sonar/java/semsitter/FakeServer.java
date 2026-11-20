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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.sonarsource.semsitter.wire.Document;
import org.sonarsource.semsitter.wire.Node;

/**
 * Stand-in for the SemSitter server, run as a separate process by the tests. The first argument is the protocol
 * version it announces. {@code --exit-after-hello} stops after the handshake and {@code --wrong-id} answers
 * {@code get-file} with another id. A requested path ending with {@code Deep.java} gets a tree nested
 * {@link #DEEP_TREE_DEPTH} levels deep.
 */
public final class FakeServer {

  static final int DEEP_TREE_DEPTH = 1_000;

  private FakeServer() {
  }

  public static void main(String[] args) {
    try (InputStream in = new FileInputStream(FileDescriptor.in); OutputStream out = new FileOutputStream(FileDescriptor.out)) {
      serve(List.of(args), in, out);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void serve(List<String> args, InputStream in, OutputStream out) throws IOException {
    String line;
    while ((line = readLine(in)) != null) {
      JsonObject request = JsonParser.parseString(line).getAsJsonObject();
      JsonObject header = new JsonObject();
      header.addProperty("id", args.contains("--wrong-id") && "get-file".equals(request.get("cmd").getAsString()) ? 0 : request.get("id").getAsInt());
      header.addProperty("ok", true);
      byte[] payload = answer(request, header, args.get(0));
      if (payload.length > 0) {
        header.addProperty("payload_bytes", payload.length);
      }
      out.write(header.toString().getBytes(StandardCharsets.UTF_8));
      out.write('\n');
      out.write(payload);
      out.flush();
      if ("quit".equals(request.get("cmd").getAsString()) || ("hello".equals(request.get("cmd").getAsString()) && args.contains("--exit-after-hello"))) {
        return;
      }
    }
  }

  private static byte[] answer(JsonObject request, JsonObject header, String protocol) {
    switch (request.get("cmd").getAsString()) {
      case "hello" -> header.addProperty("protocol", protocol);
      case "set-settings" -> header.add("settings", request.get("settings"));
      case "full-gen" -> header.addProperty("files", request.has("files") ? request.getAsJsonArray("files").size() : 0);
      case "get-file" -> {
        String path = request.get("path").getAsString();
        if (path.endsWith("Nope.java")) {
          header.addProperty("ok", false);
          header.addProperty("error", "no such file " + path);
          return new byte[0];
        }
        header.addProperty("format", "protobuf");
        Node.Builder tree = Node.newBuilder().setKind("program");
        if (path.endsWith("Deep.java")) {
          for (int depth = 0; depth < DEEP_TREE_DEPTH; depth++) {
            tree = Node.newBuilder().setKind("parenthesized_expression").addChildren(tree);
          }
        }
        return Document.newBuilder().setPath(path).setTree(tree).build().toByteArray();
      }
      case "get-types" -> {
        header.addProperty("format", "json");
        return "{\"java.lang.String\":{\"kind\":\"class\"}}".getBytes(StandardCharsets.UTF_8);
      }
      case "has-package" -> {
        JsonObject packages = new JsonObject();
        request.getAsJsonArray("names").forEach(name -> packages.addProperty(name.getAsString(), name.getAsString().startsWith("java")));
        header.add("packages", packages);
      }
      default -> {
        // a quit request has no result beyond ok
      }
    }
    return new byte[0];
  }

  private static String readLine(InputStream in) throws IOException {
    ByteArrayOutputStream line = new ByteArrayOutputStream();
    for (int b = in.read(); b != '\n'; b = in.read()) {
      if (b < 0) {
        return line.size() == 0 ? null : line.toString(StandardCharsets.UTF_8);
      }
      line.write(b);
    }
    return line.toString(StandardCharsets.UTF_8);
  }
}
