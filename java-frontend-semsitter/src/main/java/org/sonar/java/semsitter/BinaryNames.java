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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** The binary names Java gives the types SemSitter names, and back, for the types of one file. */
final class BinaryNames {

  private static final Pattern LAMBDA_SEGMENTS = Pattern.compile("^(?:\\$lambda\\$[^$()]*+\\(\\))++");

  private final Map<String, String> binaryById = new HashMap<>();
  private final Map<String, String> idByBinary = new HashMap<>();

  /**
   * The binary name Java gives the type SemSitter names {@code id}: a local class {@code p.C#f()$Local} becomes
   * {@code p.C$1Local}, numbered by the first index not yet taken in its enclosing class, in declaration order.
   */
  String binaryName(String id) {
    String known = binaryById.get(id);
    if (known != null) {
      return known;
    }
    int hash = id.lastIndexOf('#');
    int open = id.indexOf('(', hash);
    int close = open < 0 ? -1 : id.indexOf(')', open);
    String rest = close < 0 ? "" : LAMBDA_SEGMENTS.matcher(id.substring(close + 1)).replaceFirst("");
    if (hash < 0 || !rest.startsWith("$") || rest.length() == 1) {
      return id;
    }
    int nested = rest.indexOf('$', 1);
    String name = nested < 0 ? rest.substring(1) : rest.substring(1, nested);
    String localId = id.substring(0, close + 1) + "$" + name;
    String local = binaryById.get(localId);
    if (local == null) {
      String enclosing = binaryName(id.substring(0, hash));
      int index = 1;
      while (idByBinary.containsKey(enclosing + "$" + index + name)) {
        index++;
      }
      local = enclosing + "$" + index + name;
      binaryById.put(localId, local);
      idByBinary.put(local, localId);
    }
    String binary = nested < 0 ? local : (local + rest.substring(nested));
    binaryById.put(id, binary);
    idByBinary.put(binary, id);
    return binary;
  }

  /** The binary name already given to {@code name}, or {@code name} itself. */
  String binaryOf(String name) {
    return binaryById.getOrDefault(name, name);
  }

  /** The SemSitter name of the type Java names {@code fqn}, or {@code fqn} itself. */
  String sourceName(String fqn) {
    return idByBinary.getOrDefault(fqn, fqn);
  }
}
