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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * The type declarations a server session answered so far. The server sends a declaration once per session, while each
 * file is translated against its own semantic model, so a declaration sent for one file is kept to be given again to
 * the next files that name it.
 */
final class SessionTypes {

  private static final String[] SUPERTYPE_KEYS = {"super", "interfaces"};

  private final Function<Set<String>, JsonObject> server;
  private final Map<String, JsonObject> declarations = new HashMap<>();
  private final Set<String> undeclared = new HashSet<>();

  SessionTypes(Function<Set<String>, JsonObject> server) {
    this.server = server;
  }

  /**
   * The declarations of {@code names} and of their supertypes. Only the names never asked before go to the server; a
   * name it does not declare is not asked again.
   */
  JsonObject get(Set<String> names) {
    Set<String> missing = new LinkedHashSet<>();
    for (String name : names) {
      String erased = erase(name);
      if (!declarations.containsKey(erased) && !undeclared.contains(erased)) {
        missing.add(erased);
      }
    }
    if (!missing.isEmpty()) {
      for (Map.Entry<String, JsonElement> entry : server.apply(missing).entrySet()) {
        declarations.put(entry.getKey(), entry.getValue().getAsJsonObject());
      }
      missing.stream().filter(name -> !declarations.containsKey(name)).forEach(undeclared::add);
    }
    JsonObject found = new JsonObject();
    Deque<String> queue = new ArrayDeque<>();
    names.forEach(name -> queue.add(erase(name)));
    while (!queue.isEmpty()) {
      String name = queue.poll();
      JsonObject declaration = declarations.get(name);
      if (declaration == null || found.has(name)) {
        continue;
      }
      found.add(name, declaration);
      for (String key : SUPERTYPE_KEYS) {
        supertypes(declaration.get(key)).forEach(supertype -> queue.add(erase(supertype)));
      }
    }
    return found;
  }

  private static Set<String> supertypes(JsonElement value) {
    Set<String> names = new LinkedHashSet<>();
    if (value == null || value.isJsonNull()) {
      return names;
    }
    if (value.isJsonArray()) {
      value.getAsJsonArray().forEach(element -> {
        if (element.isJsonPrimitive()) {
          names.add(element.getAsString());
        }
      });
    } else if (value.isJsonPrimitive()) {
      names.add(value.getAsString());
    }
    return names;
  }

  /** The name without the generic arity suffix ({@code `1}) SemSitter ids may carry. */
  static String erase(String name) {
    return name.replaceAll("`\\d+", "");
  }
}
