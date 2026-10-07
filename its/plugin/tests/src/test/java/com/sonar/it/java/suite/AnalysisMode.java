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
package com.sonar.it.java.suite;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

enum AnalysisMode {
  BASELINE("baseline", "Baseline", false, false),
  SOURCE_PATHS("sourcepaths", "Source paths", true, false),
  BYTECODE("bytecode", "Bytecode", false, true),
  COMBINED("combined", "Combined", true, true);

  private final String id;
  private final String label;
  private final boolean sourcePaths;
  private final boolean compilation;

  AnalysisMode(String id, String label, boolean sourcePaths, boolean compilation) {
    this.id = id;
    this.label = label;
    this.sourcePaths = sourcePaths;
    this.compilation = compilation;
  }

  String id() {
    return id;
  }

  String label() {
    return label;
  }

  boolean sourcePaths() {
    return sourcePaths;
  }

  boolean compilation() {
    return compilation;
  }

  Map<String, String> properties(List<String> roots, Map<String, String> extraProperties) {
    var properties = new TreeMap<>(extraProperties);
    properties.put("sonar.java.compileToByteCode", Boolean.toString(compilation));
    properties.put("sonar.java.sourcepath", sourcePaths ? String.join(",", roots) : "");
    properties.put("sonar.java.test.sourcepath", "");
    return Map.copyOf(properties);
  }
}
