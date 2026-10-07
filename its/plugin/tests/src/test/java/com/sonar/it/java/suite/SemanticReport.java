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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

record SemanticReport(Counts totals, Map<String, Counts> files, Map<String, List<UnknownIdentifier>> unknownIdentifiers,
                      GraphCounts graphTotals, Map<String, GraphCounts> moduleGraphCounts, List<String> unknownSymbols, List<String> unknownTypes,
                      GraphTraversal graphTraversal, Map<String, Counts> moduleIdentifierCounts, boolean fileObservationsAvailable) {

  SemanticReport {
    files = Collections.unmodifiableMap(new TreeMap<>(files));
    Map<String, List<UnknownIdentifier>> details = new TreeMap<>();
    unknownIdentifiers.forEach((path, identifiers) -> details.put(path, identifiers.stream()
      .sorted(Comparator.comparing(UnknownIdentifier::name).thenComparing(UnknownIdentifier::range).thenComparing(UnknownIdentifier::parentKind))
      .toList()));
    unknownIdentifiers = Collections.unmodifiableMap(details);
    moduleGraphCounts = Collections.unmodifiableMap(new TreeMap<>(moduleGraphCounts));
    unknownSymbols = unknownSymbols.stream().sorted().toList();
    unknownTypes = unknownTypes.stream().sorted().toList();
    moduleIdentifierCounts = Collections.unmodifiableMap(new TreeMap<>(moduleIdentifierCounts));
  }

  SemanticReport(Counts totals, Map<String, Counts> files, Map<String, List<UnknownIdentifier>> unknownIdentifiers,
                 GraphCounts graphTotals, Map<String, GraphCounts> moduleGraphCounts, List<String> unknownSymbols, List<String> unknownTypes,
                 GraphTraversal graphTraversal, Map<String, Counts> moduleIdentifierCounts) {
    this(totals, files, unknownIdentifiers, graphTotals, moduleGraphCounts, unknownSymbols, unknownTypes, graphTraversal, moduleIdentifierCounts,
      !files.isEmpty() || moduleIdentifierCounts.isEmpty());
  }

  SemanticReport(Counts totals, Map<String, Counts> files, Map<String, List<UnknownIdentifier>> unknownIdentifiers,
                 GraphCounts graphTotals, Map<String, GraphCounts> moduleGraphCounts, List<String> unknownSymbols, List<String> unknownTypes,
                 GraphTraversal graphTraversal) {
    this(totals, files, unknownIdentifiers, graphTotals, moduleGraphCounts, unknownSymbols, unknownTypes, graphTraversal, Map.of(), true);
  }

  SemanticReport(Counts totals, Map<String, Counts> files, Map<String, List<UnknownIdentifier>> unknownIdentifiers,
                 GraphCounts graphTotals, Map<String, GraphCounts> moduleGraphCounts, List<String> unknownSymbols, List<String> unknownTypes) {
    this(totals, files, unknownIdentifiers, graphTotals, moduleGraphCounts, unknownSymbols, unknownTypes, null);
  }

  SemanticReport(Counts totals, Map<String, Counts> files, Map<String, List<UnknownIdentifier>> unknownIdentifiers) {
    this(totals, files, unknownIdentifiers, null, Map.of(), List.of(), List.of());
  }

  SemanticReport(Counts totals, Map<String, Counts> files) {
    this(totals, files, Map.of());
  }

  record UnknownIdentifier(String name, String range, String parentKind) {
  }

  record GraphCounts(int resolvedSymbols, int unknownSymbols, int resolvedTypes, int unknownTypes) {
    GraphCounts {
      if (resolvedSymbols < 0 || unknownSymbols < 0 || resolvedTypes < 0 || unknownTypes < 0) {
        throw new IllegalArgumentException("Invalid semantic graph counts: resolvedSymbols=" + resolvedSymbols + ", unknownSymbols=" + unknownSymbols
          + ", resolvedTypes=" + resolvedTypes + ", unknownTypes=" + unknownTypes);
      }
    }
  }

  record GraphTraversal(boolean complete, int limit, int expansions) {
    GraphTraversal {
      if (limit < 0 || expansions < 0) {
        throw new IllegalArgumentException("Invalid graph traversal counts: limit=" + limit + ", expansions=" + expansions);
      }
    }
  }

  private record Occurrence(String name, String range) {
  }

  boolean hasUnknownDetails() {
    return fileObservationsAvailable && files.keySet().equals(unknownIdentifiers.keySet());
  }

  boolean hasFileObservations() {
    return fileObservationsAvailable;
  }

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
      SemanticReport result = report.has("modules") ? readModules(report) : readLegacy(report);
      if (!result.unknownIdentifiers().isEmpty() && !result.unknownIdentifiers().keySet().equals(result.files().keySet())) {
        throw new IllegalArgumentException("Unknown identifier details must be provided for every file or none");
      }
      var expected = new TreeSet<>(expectedFiles.stream().map(SemanticReport::normalize).toList());
      if (result.hasFileObservations() && !result.files().keySet().equals(expected)) {
        var missing = new TreeSet<>(expected);
        missing.removeAll(result.files().keySet());
        var extra = new TreeSet<>(result.files().keySet());
        extra.removeAll(expected);
        throw new IllegalArgumentException("File coverage mismatch: missing=" + missing + ", extra=" + extra);
      }
      if (result.hasFileObservations()) {
        validateFileSums(result.totals(), result.files().values(), "Aggregate");
      }
      return result;
    } catch (IOException | RuntimeException e) {
      throw new IOException("Failed to read semantic report " + json + ": " + e.getMessage(), e);
    }
  }

  private static SemanticReport readLegacy(JsonObject report) {
    Counts totals = counts(report, "totalNumberOfIdentifier", "totalNumberOfUnknownIdentifier");
    Map<String, Counts> files = new TreeMap<>();
    Map<String, List<UnknownIdentifier>> details = new TreeMap<>();
    readFiles(report, null, files, details);
    return new SemanticReport(totals, files, details);
  }

  private static SemanticReport readModules(JsonObject report) {
    Counts totals = resolvedCounts(report);
    GraphCounts graphTotals = graphCounts(report);
    Map<String, Counts> files = new TreeMap<>();
    Map<String, List<UnknownIdentifier>> details = new TreeMap<>();
    Map<String, Counts> moduleIdentifiers = new TreeMap<>();
    Map<String, GraphCounts> moduleGraphs = new TreeMap<>();
    GraphTraversal traversal = graphTraversal(report);
    Map<String, GraphTraversal> moduleTraversals = new TreeMap<>();
    List<String> unknownSymbols = new ArrayList<>();
    List<String> unknownTypes = new ArrayList<>();
    boolean observedModules = false;
    boolean observedPopulatedModules = false;
    boolean unobservedPopulatedModules = false;
    for (var element : report.getAsJsonArray("modules")) {
      JsonObject module = element.getAsJsonObject();
      String path = normalize(string(module, "path"));
      Counts identifiers = resolvedCounts(module);
      if (moduleIdentifiers.putIfAbsent(path, identifiers) != null) {
        throw new IllegalArgumentException("Duplicate module: " + path);
      }
      GraphCounts graph = graphCounts(module);
      moduleGraphs.put(path, graph);
      GraphTraversal moduleTraversal = graphTraversal(module);
      if ((traversal == null) != (moduleTraversal == null)) {
        throw new IllegalArgumentException("Graph traversal metadata must be present for the report and every module or none");
      }
      if (moduleTraversal != null) {
        if (moduleTraversal.limit() != traversal.limit() || moduleTraversal.expansions() > moduleTraversal.limit()) {
          throw new IllegalArgumentException("Invalid module graph expansion budget for " + path);
        }
        moduleTraversals.put(path, moduleTraversal);
      }
      strings(module, "unknownIdentifiers", identifiers.unknown());
      strings(module, "unknownSymbols", graph.unknownSymbols()).forEach(location -> unknownSymbols.add(path + ":" + location));
      strings(module, "unknownTypes", graph.unknownTypes()).forEach(location -> unknownTypes.add(path + ":" + location));
      Map<String, Counts> moduleFiles = new TreeMap<>();
      if (module.has("files")) {
        readFiles(module, path, moduleFiles, details);
        validateFileSums(identifiers, moduleFiles.values(), "Module " + path);
        observedModules = true;
        observedPopulatedModules |= identifiers.total() != 0;
      } else {
        unobservedPopulatedModules |= identifiers.total() != 0;
      }
      moduleFiles.forEach((file, counts) -> {
        if (files.putIfAbsent(file, counts) != null) {
          throw new IllegalArgumentException("Duplicate file: " + file);
        }
      });
    }
    if (observedPopulatedModules && unobservedPopulatedModules) {
      throw new IllegalArgumentException("Per-file observations must cover every populated module or none");
    }
    validateFileSums(totals, moduleIdentifiers.values(), "Aggregate module");
    if (graphTotals.resolvedSymbols() != moduleGraphs.values().stream().mapToLong(GraphCounts::resolvedSymbols).sum()
      || graphTotals.unknownSymbols() != moduleGraphs.values().stream().mapToLong(GraphCounts::unknownSymbols).sum()
      || graphTotals.resolvedTypes() != moduleGraphs.values().stream().mapToLong(GraphCounts::resolvedTypes).sum()
      || graphTotals.unknownTypes() != moduleGraphs.values().stream().mapToLong(GraphCounts::unknownTypes).sum()) {
      throw new IllegalArgumentException("Aggregate semantic graph counts do not match module sums");
    }
    if (traversal != null && (traversal.expansions() != moduleTraversals.values().stream().mapToLong(GraphTraversal::expansions).sum()
      || traversal.complete() != moduleTraversals.values().stream().allMatch(GraphTraversal::complete))) {
      throw new IllegalArgumentException("Aggregate graph traversal metadata does not match module observations");
    }
    return new SemanticReport(totals, files, details, graphTotals, moduleGraphs, unknownSymbols, unknownTypes, traversal, moduleIdentifiers,
      observedModules && !unobservedPopulatedModules);
  }

  private static void readFiles(JsonObject object, String module, Map<String, Counts> files, Map<String, List<UnknownIdentifier>> details) {
    for (var element : object.getAsJsonArray("files")) {
      JsonObject file = element.getAsJsonObject();
      var pathElement = file.get("path");
      if (pathElement == null || !pathElement.isJsonPrimitive() || !pathElement.getAsJsonPrimitive().isString() || pathElement.getAsString().isBlank()) {
        throw new IllegalArgumentException("Missing or invalid file path");
      }
      String relative = pathElement.getAsString();
      String path = normalize(module == null ? relative : module + "/" + relative);
      Counts counts = counts(file, "numberOfIdentifier", "numberOfUnknownIdentifier");
      if (files.putIfAbsent(path, counts) != null) {
        throw new IllegalArgumentException("Duplicate file: " + path);
      }
      if (file.has("unknownIdentifiers")) {
        if (details.putIfAbsent(path, unknownIdentifiers(file, path, counts.unknown())) != null) {
          throw new IllegalArgumentException("Duplicate file: " + path);
        }
      }
    }
  }

  private static void validateFileSums(Counts expected, Collection<Counts> files, String context) {
    long total = files.stream().mapToLong(Counts::total).sum();
    long unknown = files.stream().mapToLong(Counts::unknown).sum();
    if (expected.total() != total || expected.unknown() != unknown) {
      throw new IllegalArgumentException(context + " identifier counts do not match per-file sums");
    }
  }

  private static List<String> strings(JsonObject object, String key, int expectedCount) {
    var values = object.get(key);
    if (values == null && expectedCount == 0) {
      return List.of();
    }
    if (values == null || !values.isJsonArray()) {
      throw new IllegalArgumentException("Missing or invalid semantic graph array: " + key);
    }
    var strings = new TreeSet<String>();
    for (var element : values.getAsJsonArray()) {
      if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString() || element.getAsString().isBlank()) {
        throw new IllegalArgumentException("Missing or invalid semantic graph location: " + key);
      }
      if (!strings.add(element.getAsString())) {
        throw new IllegalArgumentException("Duplicate semantic graph location in " + key + ": " + element.getAsString());
      }
    }
    if (strings.size() != expectedCount) {
      throw new IllegalArgumentException(key + " detail count does not match canonical count");
    }
    return List.copyOf(strings);
  }

  private static List<UnknownIdentifier> unknownIdentifiers(JsonObject file, String path, int expectedCount) {
    var values = file.get("unknownIdentifiers");
    if (!values.isJsonArray()) {
      throw new IllegalArgumentException("Invalid unknown identifier details for " + path);
    }
    List<UnknownIdentifier> identifiers = new ArrayList<>();
    var occurrences = new HashSet<Occurrence>();
    for (var element : values.getAsJsonArray()) {
      JsonObject identifier = element.getAsJsonObject();
      String name = string(identifier, "name");
      String range = string(identifier, "range");
      String parentKind = string(identifier, "parentKind");
      if (!occurrences.add(new Occurrence(name, range))) {
        throw new IllegalArgumentException("Duplicate unknown identifier occurrence in " + path + ": " + name + " at " + range);
      }
      identifiers.add(new UnknownIdentifier(name, range, parentKind));
    }
    if (identifiers.size() != expectedCount) {
      throw new IllegalArgumentException("Unknown identifier detail count does not match numberOfUnknownIdentifier for " + path);
    }
    return identifiers;
  }

  private static String string(JsonObject object, String key) {
    var value = object.get(key);
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || value.getAsString().isBlank()) {
      throw new IllegalArgumentException("Missing or invalid unknown identifier string: " + key);
    }
    return value.getAsString();
  }

  private static Counts counts(JsonObject object, String total, String unknown) {
    return new Counts(integer(object, total), integer(object, unknown));
  }

  private static Counts resolvedCounts(JsonObject object) {
    int resolved = integer(object, "resolvedIdentifierCount");
    int unknown = integer(object, "unknownIdentifierCount");
    if (resolved < 0 || unknown < 0) {
      throw new IllegalArgumentException("Invalid identifier counts: resolved=" + resolved + ", unknown=" + unknown);
    }
    return new Counts(Math.addExact(resolved, unknown), unknown);
  }

  private static GraphCounts graphCounts(JsonObject object) {
    return new GraphCounts(integer(object, "resolvedSymbolCount"), integer(object, "unknownSymbolCount"),
      integer(object, "resolvedTypeCount"), integer(object, "unknownTypeCount"));
  }

  private static GraphTraversal graphTraversal(JsonObject object) {
    if (!object.has("graphTraversalComplete") && !object.has("graphExpansionLimit") && !object.has("graphExpansions")) {
      return null;
    }
    var complete = object.get("graphTraversalComplete");
    if (complete == null || !complete.isJsonPrimitive() || !complete.getAsJsonPrimitive().isBoolean()) {
      throw new IllegalArgumentException("Missing or invalid graphTraversalComplete boolean");
    }
    return new GraphTraversal(complete.getAsBoolean(), integer(object, "graphExpansionLimit"), integer(object, "graphExpansions"));
  }

  private static int integer(JsonObject object, String key) {
    var value = object.get(key);
    if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
      throw new IllegalArgumentException("Missing or invalid integer: " + key);
    }
    return value.getAsBigDecimal().intValueExact();
  }

  private static String normalize(String path) {
    String normalized = Path.of(path.replace('\\', '/')).normalize().toString().replace('\\', '/');
    return normalized.isEmpty() ? "." : normalized;
  }
}
