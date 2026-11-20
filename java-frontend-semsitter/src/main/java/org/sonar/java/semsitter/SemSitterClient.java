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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.sonar.java.semsitter.protocol.Document;
import org.sonar.java.semsitter.protocol.FullGenRequest;
import org.sonar.java.semsitter.protocol.FullGenResponse;
import org.sonar.java.semsitter.protocol.GetFileRequest;
import org.sonar.java.semsitter.protocol.GetTypesRequest;
import org.sonar.java.semsitter.protocol.HasPackageRequest;
import org.sonar.java.semsitter.protocol.HelloRequest;
import org.sonar.java.semsitter.protocol.QuitRequest;
import org.sonar.java.semsitter.protocol.Request;
import org.sonar.java.semsitter.protocol.Response;
import org.sonar.java.semsitter.protocol.SetSettingsRequest;
import org.sonar.java.semsitter.protocol.SetSettingsResponse;
import org.sonar.java.semsitter.protocol.StringList;
import org.sonar.java.semsitter.protocol.TypesResponse;

/**
 * Client of one SemSitter Java analyzer server process. The process starts with the client and ends with
 * {@link #close()}; requests are answered one at a time, in order.
 */
public final class SemSitterClient implements AutoCloseable {

  static final String PROTOCOL_MAJOR = "2";

  private static final List<String> SERVER_FLAGS = List.of("--server", "--lang", "java", "--modifiers", "--structural",
    "--decl-location", "--field-items", "--field-reference-edges", "--override-edges");

  private final Process process;
  private final OutputStream toServer;
  private final InputStream fromServer;
  private int nextId = 1;

  private SemSitterClient(Process process) {
    this.process = process;
    this.toServer = new BufferedOutputStream(process.getOutputStream());
    this.fromServer = new BufferedInputStream(process.getInputStream());
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
      String protocol = client.send(Request.newBuilder().setHello(HelloRequest.getDefaultInstance())).getHello().getProtocol();
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

  public SetSettingsResponse setSettings(Map<String, List<String>> settings) throws IOException {
    SetSettingsRequest.Builder request = SetSettingsRequest.newBuilder();
    settings.forEach((key, values) -> request.putSettings(key, StringList.newBuilder().addAllValues(values).build()));
    return send(Request.newBuilder().setSetSettings(request)).getSetSettings();
  }

  /**
   * Scans {@code files}, or the source roots when empty.
   */
  public FullGenResponse fullGen(Collection<String> files) throws IOException {
    return send(Request.newBuilder().setFullGen(FullGenRequest.newBuilder().addAllFiles(files))).getFullGen();
  }

  public Document getFile(String path, int ahead) throws IOException {
    return send(Request.newBuilder().setGetFile(GetFileRequest.newBuilder().setPath(path).setAhead(ahead))).getDocument();
  }

  public TypesResponse getTypes(Collection<String> names) throws IOException {
    return send(Request.newBuilder().setGetTypes(GetTypesRequest.newBuilder().addAllNames(names))).getTypes();
  }

  public Map<String, Boolean> hasPackage(Collection<String> names) throws IOException {
    return send(Request.newBuilder().setHasPackage(HasPackageRequest.newBuilder().addAllNames(names))).getHasPackage().getPackagesMap();
  }

  private synchronized Response send(Request.Builder request) throws IOException {
    int id = nextId;
    nextId++;
    request.setId(id).build().writeDelimitedTo(toServer);
    toServer.flush();
    Response response = Response.parseDelimitedFrom(fromServer);
    if (response == null) {
      throw new IOException("SemSitter server closed its output");
    }
    if (response.getId() != id) {
      throw new IOException("SemSitter server answered request " + response.getId() + " instead of " + id);
    }
    if (response.getResultCase() == Response.ResultCase.ERROR) {
      throw new IOException("SemSitter server: " + response.getError());
    }
    return response;
  }

  /**
   * Asks the server to quit and waits for it, forcing it to stop when it does not answer.
   */
  @Override
  public void close() {
    try {
      if (process.isAlive()) {
        send(Request.newBuilder().setQuit(QuitRequest.getDefaultInstance()));
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
}
