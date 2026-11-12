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
package org.sonar.java.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** EXPERIMENT: type declarations of library types read from the SemSitter framework API database, used when the bridge cannot resolve them. */
final class FrameworkDb {

  private static final Map<String, String> PRIMITIVES = Map.of("int", "I", "long", "J", "boolean", "Z", "byte", "B", "char", "C", "short", "S", "float", "F", "double", "D", "void", "V");
  private static FrameworkDb instance;
  private static boolean loaded;

  private final Map<String, String[]> typeRows = new HashMap<>();
  private final Map<String, List<String[]>> memberRows = new HashMap<>();
  private final Map<String, JsonObject> cache = new HashMap<>();

  static synchronized FrameworkDb instance() {
    if (!loaded) {
      loaded = true;
      String files = System.getProperty("sonar.java.semsitter.frameworkDb");
      if (files != null && !files.isBlank()) {
        instance = new FrameworkDb();
        for (String file : files.split(",")) {
          instance.read(Path.of(file.trim()));
        }
      }
    }
    return instance;
  }

  private void read(Path file) {
    try {
      for (String line : Files.readAllLines(file)) {
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        String[] fields = line.split("\\|", -1);
        if ("TYPE".equals(fields[0]) && fields.length > 8) {
          typeRows.putIfAbsent(fields[1], fields);
        } else if (fields[0].contains("::") && fields.length > 4) {
          int cut = fields[0].indexOf("::");
          memberRows.computeIfAbsent(fields[0].substring(0, cut), k -> new ArrayList<>()).add(fields);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /** Adds, for each requested name the bridge did not answer, the declaration the database has. Returns the number added. */
  static int complete(JsonObject answered, Set<String> requested) {
    FrameworkDb db = instance();
    if (db == null) {
      return 0;
    }
    int added = 0;
    for (String name : requested) {
      if (!answered.has(name)) {
        JsonObject info = db.info(name);
        if (info != null) {
          answered.add(name, info);
          added++;
        }
      }
    }
    return added;
  }

  private JsonObject info(String fqn) {
    if (cache.containsKey(fqn)) {
      return cache.get(fqn);
    }
    String[] type = typeRows.get(fqn);
    List<String[]> members = memberRows.get(fqn);
    JsonObject info = null;
    if (type != null || members != null) {
      info = new JsonObject();
      String kind = type == null ? "class" : type[2];
      info.addProperty("kind", "exception".equals(kind) ? "class" : kind);
      if (type != null) {
        String superName = erase(type[7]);
        if (!superName.isEmpty()) {
          info.addProperty("super", superName);
        } else if (!"interface".equals(kind) && !"annotation".equals(kind) && !"java.lang.Object".equals(fqn)) {
          info.addProperty("super", "java.lang.Object");
        }
        info.add("interfaces", erasedList(type[8], ";"));
        List<String> modifiers = new ArrayList<>();
        addAll(modifiers, type[3]);
        addAll(modifiers, type[4]);
        info.add("modifiers", array(modifiers));
        if (type.length > 9 && !type[9].isEmpty()) {
          info.add("annotations", erasedList(type[9], ","));
        }
        if (!type[6].isEmpty()) {
          info.addProperty("typeParameters", type[6].split(";").length);
        }
      } else {
        info.addProperty("super", "java.lang.Object");
      }
      JsonArray memberArray = new JsonArray();
      if (members != null) {
        for (String[] row : members) {
          memberArray.add(member(row));
        }
      }
      info.add("members", memberArray);
    }
    cache.put(fqn, info);
    return info;
  }

  private static JsonObject member(String[] row) {
    String name = row[0].substring(row[0].indexOf("::") + 2);
    JsonObject member = new JsonObject();
    member.addProperty("k", "m");
    member.addProperty("name", name);
    StringBuilder desc = new StringBuilder("(");
    if (!row[4].isEmpty()) {
      for (String param : row[4].split(",")) {
        desc.append(descriptor(param));
      }
    }
    desc.append(')').append(descriptor(row[1].isEmpty() ? "void" : row[1]));
    member.addProperty("desc", desc.toString());
    List<String> modifiers = new ArrayList<>();
    addAll(modifiers, row[2]);
    for (String modifier : modifiers) {
      switch (modifier) {
        case "static" -> member.addProperty("static", true);
        case "abstract" -> member.addProperty("abstract", true);
        case "final" -> member.addProperty("final", true);
        case "varargs" -> member.addProperty("varargs", true);
        default -> {
        }
      }
    }
    member.addProperty("vis", row[3].isEmpty() ? "public" : row[3]);
    if (row.length > 8 && !row[8].isEmpty()) {
      member.add("annotations", erasedList(row[8], ","));
    }
    return member;
  }

  private static String descriptor(String name) {
    if (name.endsWith("[]")) {
      return "[" + descriptor(name.substring(0, name.length() - 2));
    }
    String primitive = PRIMITIVES.get(name);
    return primitive != null ? primitive : "L" + name.replace('.', '/') + ";";
  }

  private static void addAll(List<String> target, String csv) {
    for (String part : csv.split("[,;]")) {
      if (!part.isBlank()) {
        target.add(part.trim());
      }
    }
  }

  private static JsonArray array(List<String> values) {
    JsonArray array = new JsonArray();
    values.forEach(array::add);
    return array;
  }

  private static JsonArray erasedList(String joined, String separator) {
    JsonArray array = new JsonArray();
    for (String part : joined.split(separator)) {
      String erased = erase(part);
      if (!erased.isEmpty()) {
        array.add(erased);
      }
    }
    return array;
  }

  private static String erase(String name) {
    int generic = name.indexOf('<');
    return (generic < 0 ? name : name.substring(0, generic)).trim();
  }
}
