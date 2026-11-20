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

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.TreeMap;
import org.sonar.java.semsitter.protocol.Document;
import org.sonar.java.semsitter.protocol.FullGenResponse;
import org.sonar.java.semsitter.protocol.HasPackageResponse;
import org.sonar.java.semsitter.protocol.HelloResponse;
import org.sonar.java.semsitter.protocol.Node;
import org.sonar.java.semsitter.protocol.Request;
import org.sonar.java.semsitter.protocol.Response;
import org.sonar.java.semsitter.protocol.SetSettingsResponse;
import org.sonar.java.semsitter.protocol.TypeDeclaration;
import org.sonar.java.semsitter.protocol.TypesResponse;

/**
 * Stand-in for the SemSitter server, run as a separate process by the tests. The first argument is the protocol
 * version it announces; {@code --exit-after-hello} and {@code --wrong-id} make it misbehave.
 */
public final class FakeServer {

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
    Request request;
    while ((request = Request.parseDelimitedFrom(in)) != null) {
      Response.Builder response = Response.newBuilder().setId(args.contains("--wrong-id") && request.hasGetFile() ? 0 : request.getId());
      answer(request, response, args.get(0));
      response.build().writeDelimitedTo(out);
      out.flush();
      if (request.hasQuit() || (request.hasHello() && args.contains("--exit-after-hello"))) {
        return;
      }
    }
  }

  private static void answer(Request request, Response.Builder response, String protocol) {
    switch (request.getCommandCase()) {
      case HELLO -> response.setHello(HelloResponse.newBuilder().setProtocol(protocol));
      case SET_SETTINGS -> response.setSetSettings(SetSettingsResponse.newBuilder()
        .addAllDiagnostics(new TreeMap<>(request.getSetSettings().getSettingsMap()).entrySet().stream()
          .map(e -> e.getKey() + "=" + String.join(",", e.getValue().getValuesList()))
          .toList()));
      case FULL_GEN -> response.setFullGen(FullGenResponse.newBuilder().setFiles(request.getFullGen().getFilesCount()));
      case GET_FILE -> {
        String path = request.getGetFile().getPath();
        if (path.endsWith("Nope.java")) {
          response.setError("no such file " + path);
        } else {
          response.setDocument(Document.newBuilder().setPath(path).setTree(Node.newBuilder().setKind("program")));
        }
      }
      case GET_TYPES -> {
        TypesResponse.Builder types = TypesResponse.newBuilder();
        for (String name : request.getGetTypes().getNamesList()) {
          if (name.startsWith("x.")) {
            types.addMissing(name);
          } else {
            types.putDeclarations(name, TypeDeclaration.newBuilder().setOrigin("jdk").setKind("class").build());
          }
        }
        response.setTypes(types);
      }
      case HAS_PACKAGE -> {
        HasPackageResponse.Builder packages = HasPackageResponse.newBuilder();
        request.getHasPackage().getNamesList().forEach(name -> packages.putPackages(name, name.startsWith("java")));
        response.setHasPackage(packages);
      }
      default -> response.clearResult();
    }
  }
}
