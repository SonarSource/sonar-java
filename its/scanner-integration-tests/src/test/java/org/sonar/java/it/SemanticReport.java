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
package org.sonar.java.it;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

record SemanticReport(Counts totals, Map<String, Counts> files) {

  record Counts(int total, int unknown) {
    Counts {
      if (total < 0 || unknown < 0 || unknown > total) {
        throw new IllegalArgumentException("Invalid identifier counts: total=" + total + ", unknown=" + unknown);
      }
    }

    int known() {
      return total - unknown;
    }

    Double unknownPercentage() {
      return total == 0 ? null : 100.0 * unknown / total;
    }
  }

  static SemanticReport read(Path json, List<String> expectedFiles) throws IOException {
    try {
      JsonObject report = JsonParser.parseString(Files.readString(json)).getAsJsonObject();
      Counts totals = counts(report, "totalNumberOfIdentifier", "totalNumberOfUnknownIdentifier");
      Map<String, Counts> files = new TreeMap<>();
      for (var element : report.getAsJsonArray("files")) {
        JsonObject file = element.getAsJsonObject();
        var pathElement = file.get("path");
        if (pathElement == null || !pathElement.isJsonPrimitive() || !pathElement.getAsJsonPrimitive().isString()) {
          throw new IllegalArgumentException("Missing or invalid file path");
        }
        String path = normalize(pathElement.getAsString());
        if (path.isBlank()) {
          throw new IllegalArgumentException("Empty file path");
        }
        if (files.putIfAbsent(path, counts(file, "numberOfIdentifier", "numberOfUnknownIdentifier")) != null) {
          throw new IllegalArgumentException("Duplicate file: " + path);
        }
      }
      var expected = new TreeSet<>(expectedFiles.stream().map(SemanticReport::normalize).toList());
      if (!files.keySet().equals(expected)) {
        var missing = new TreeSet<>(expected);
        missing.removeAll(files.keySet());
        var extra = new TreeSet<>(files.keySet());
        extra.removeAll(expected);
        throw new IllegalArgumentException("File coverage mismatch: missing=" + missing + ", extra=" + extra);
      }
      long total = files.values().stream().mapToLong(Counts::total).sum();
      long unknown = files.values().stream().mapToLong(Counts::unknown).sum();
      if (totals.total() != total || totals.unknown() != unknown) {
        throw new IllegalArgumentException("Aggregate identifier counts do not match per-file sums");
      }
      return new SemanticReport(totals, Collections.unmodifiableMap(files));
    } catch (IOException | RuntimeException e) {
      throw new IOException("Failed to read semantic report " + json + ": " + e.getMessage(), e);
    }
  }

  private static Counts counts(JsonObject object, String total, String unknown) {
    return new Counts(integer(object, total), integer(object, unknown));
  }

  private static int integer(JsonObject object, String key) {
    var value = object.get(key);
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
      throw new IllegalArgumentException("Missing or invalid integer: " + key);
    }
    return value.getAsBigDecimal().intValueExact();
  }

  private static String normalize(String path) {
    return path.replace('\\', '/');
  }
}
