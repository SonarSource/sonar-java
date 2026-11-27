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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonarsource.semsitter.wire.Document;
import org.sonarsource.semsitter.wire.Node;

/**
 * An in-memory {@link SemSitterApi}: it answers {@code get-file} from the documents it was given and records every request.
 * With {@code declarations}, {@code get-types} answers each declaration once, as the server does in a session; without,
 * it answers {@code types}.
 */
final class ScriptedServer implements SemSitterApi {

  final List<String> requests = new ArrayList<>();
  final List<String> scanned = new ArrayList<>();
  final Set<String> failing = new HashSet<>();
  final Map<String, Document> documents = new HashMap<>();
  final Map<String, Boolean> packages = new HashMap<>();
  String types = "{}";
  final Map<String, String> declarations = new HashMap<>();
  private final Set<String> sentTypes = new HashSet<>();
  Map<String, List<String>> settings;
  int ahead = -1;
  boolean closed;
  IOException settingsFailure;
  IOException loaderFailure;

  static Document emptyFile(String path) {
    return Document.newBuilder().setPath(path).setSemantic(true).setTree(Node.newBuilder().setKind("program")).build();
  }

  @Override
  public void setSettings(Map<String, List<String>> settings) throws IOException {
    requests.add("set-settings");
    if (settingsFailure != null) {
      throw settingsFailure;
    }
    this.settings = settings;
  }

  @Override
  public int fullGen(Collection<String> files) {
    requests.add("full-gen");
    scanned.addAll(files);
    return files.size();
  }

  @Override
  public Document getFile(String path, int ahead) throws IOException {
    requests.add("get-file " + path);
    this.ahead = ahead;
    if (failing.contains(path)) {
      throw new IOException("SemSitter server: no such file " + path);
    }
    return documents.getOrDefault(path, emptyFile(path));
  }

  @Override
  public byte[] getTypes(Collection<String> names) throws IOException {
    requests.add("get-types");
    if (loaderFailure != null) {
      throw loaderFailure;
    }
    if (declarations.isEmpty()) {
      return types.getBytes(StandardCharsets.UTF_8);
    }
    JsonObject answer = new JsonObject();
    names.stream().filter(declarations::containsKey).filter(sentTypes::add)
      .forEach(name -> answer.add(name, JsonParser.parseString(declarations.get(name))));
    return answer.toString().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public Map<String, Boolean> hasPackage(Collection<String> names) throws IOException {
    requests.add("has-package");
    if (loaderFailure != null) {
      throw loaderFailure;
    }
    Map<String, Boolean> answer = new HashMap<>();
    names.forEach(name -> answer.put(name, packages.getOrDefault(name, false)));
    return answer;
  }

  @Override
  public void close() {
    closed = true;
  }
}
