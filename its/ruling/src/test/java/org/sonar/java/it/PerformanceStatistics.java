/*
 * SonarQube Java
 * Copyright (C) 2013-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.it;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PerformanceStatistics {

  private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(Locale.ROOT);
  private static final NumberFormat TIME_FORMAT = new DecimalFormat("0.000000000", SYMBOLS);
  private static final NumberFormat RANK_FORMAT = new DecimalFormat("000", SYMBOLS);

  private static final Map<String, String> CATEGORY_MAP = new HashMap<>();
  static {
    CATEGORY_MAP.put("Main", "1.main");
    CATEGORY_MAP.put("Test", "1.test");
    CATEGORY_MAP.put("Scanners", "2.scanners");
    CATEGORY_MAP.put("IssuableSubscriptionVisitors", "2.subscription");
    CATEGORY_MAP.put("SymbolicExecutionVisitor", "3.symbolic-execution");
  }

  public static void main(String[] args) throws IOException {
    PerformanceStatistics.generate(Paths.get("target","performance"));
  }

  public static void generate(Path performanceDirectory) throws IOException {
    Path performanceJsonFile = performanceDirectory.resolve("sonar.java.performance.measure.json");
    Measure sensorMeasure = fromJson(performanceJsonFile);
    MeasurementCost measurementCost = new MeasurementCost(sensorMeasure);
    measurementCost.subtractObservationCost(sensorMeasure);
    mergeOnUpperLevel(sensorMeasure, "RegexParser");
    Map<String, Set<String>> measureCategories = new HashMap<>();
    categorizeMeasures(sensorMeasure, measureCategories, Collections.emptySet());
    Map<String, Measure> allChecks = new HashMap<>();
    Map<Measure, Measure> replacementMeasureByParent = new HashMap<>();
    extractAllChecks(allChecks, sensorMeasure, measureCategories, replacementMeasureByParent);

    StringBuilder stat = new StringBuilder();
    stat.append("____________________________\n");
    stat.append("Sensor Performance (in seconds without observation cost)\n");
    simplifiedTreeOrderedByDuration(stat, sensorMeasure, "");
    stat.append("\n____________________________\n");
    stat.append("Checks Performance (in seconds without observation cost)\n");
    rankedCheckList(stat, allChecks, measureCategories);
    Path performanceStatFile = performanceDirectory.resolve("sonar.java.performance.statistics.txt");
    Files.write(performanceStatFile, stat.toString().getBytes(UTF_8));
  }

  private static void categorizeMeasures(Measure measure, Map<String, Set<String>> measureCategories, Set<String> parentCategories) {
    Set<String> categories = measureCategories.computeIfAbsent(measure.name, name -> new TreeSet<>());
    categories.addAll(parentCategories);
    String newCategory = CATEGORY_MAP.get(measure.name);
    Set<String> childCategories;
    if (newCategory != null) {
      categories.add(newCategory);
      childCategories = new TreeSet<>(parentCategories);
      childCategories.add(newCategory);
    } else {
      childCategories = parentCategories;
    }
    measure.children().forEach(child -> categorizeMeasures(child, measureCategories, childCategories));
  }

  private static void simplifiedTreeOrderedByDuration(StringBuilder stat, Measure measure, String indent) {
    stat.append(indent).append("• ").append(toSeconds(measure.durationNanos)).append(" ").append(measure.name).append("\n");
    measure.children().stream()
      .sorted(Comparator.comparing(x -> -x.durationNanos))
      .forEach(child -> simplifiedTreeOrderedByDuration(stat, child, indent + "    "));
  }

  private static void rankedCheckList(StringBuilder stat, Map<String, Measure> allChecks, Map<String, Set<String>> measureCategories) {
    long totalDuration = allChecks.values().stream().mapToLong(c -> c.durationNanos).sum();
    stat.append("Total   ").append(toSeconds(totalDuration)).append("\n");
    List<Measure> checkMeasures = allChecks.values().stream().sorted(Comparator.comparing(c -> -c.durationNanos)).collect(Collectors.toList());
    for (int i = 0; i < checkMeasures.size(); i++) {
      Measure measure = checkMeasures.get(i);
      stat.append(toRank(i, checkMeasures))
        .append(" ").append(toSeconds(measure.durationNanos))
        .append(" ").append(String.format("%-50s", measure.name))
        .append(" (").append(String.join(", ", measureCategories.get(measure.name))).append(")")
        .append("\n");
    }
  }

  private static String toSeconds(long durationNanos) {
    double seconds = durationNanos / 1_000_000_000.0d;
    return String.format("%13s" , TIME_FORMAT.format(seconds));
  }

  private static String toRank(long index, Collection<?> collection) {
    return RANK_FORMAT.format(index + 1) + "/" + RANK_FORMAT.format(collection.size());
  }

  private static void mergeOnUpperLevel(Measure measure, String name) {
    if (name.equals(measure.name)) {
      Measure parent = measure.parent;
      parent.durationNanos -= measure.durationNanos;
      Measure existing = parent.parent.get(measure.name);
      if (existing != null) {
        existing.calls += measure.calls;
        existing.durationNanos += measure.durationNanos;
        measure.detach();
      } else {
        parent.parent.add(measure);
      }
    } else {
      measure.children().forEach(child -> mergeOnUpperLevel(child, name));
    }
  }

  private static void extractAllChecks(Map<String, Measure> allChecks, Measure measure,
    Map<String, Set<String>> measureCategories, Map<Measure, Measure> replacementMeasureByParent) {
    if (measure.name.endsWith("Check")) {
      if (!measure.isEmpty()) {
        throw new IllegalStateException("Unexpected children under " + measure.name);
      }
      Measure replacement = replacementMeasureByParent.computeIfAbsent(measure.parent,
        parent -> createChecksReplacementMeasure(parent, measureCategories));
      replacement.calls += measure.calls;
      replacement.durationNanos += measure.durationNanos;
      measure.detach();
      Measure existing = allChecks.get(measure.name);
      if (existing != null) {
        existing.calls += measure.calls;
        existing.durationNanos += measure.durationNanos;
      } else {
        allChecks.put(measure.name, measure);
      }
    } else {
      measure.children().forEach(child -> extractAllChecks(allChecks, child, measureCategories, replacementMeasureByParent));
    }
  }

  private static Measure createChecksReplacementMeasure(Measure parent, Map<String, Set<String>> measureCategories) {
    long numberOfCheckToReplace = parent.children().stream().filter(child -> child.name.endsWith("Check")).count();
    String categories = parent.children().stream().flatMap(child -> measureCategories.get(child.name).stream())
      .distinct().sorted().collect(Collectors.joining(", ", "(", ")"));
    String name = "[ " + numberOfCheckToReplace + " checks " + categories + " ]";
    Measure replacement =  new Measure(name, 0, 0, Collections.emptyList());
    parent.add(replacement);
    return replacement;
  }

  private static Measure fromJson(Path performanceFile) throws IOException {
    JsonObject jsonObject = new Gson().fromJson(new String(Files.readAllBytes(performanceFile), UTF_8), JsonObject.class);
    return fromJson(jsonObject);
  }

  private static Measure fromJson(JsonObject jsonObject) {
    List<Measure> children = new ArrayList<>();
    JsonArray childrenArray = jsonObject.getAsJsonArray("children");
    if (childrenArray != null) {
      childrenArray.forEach(jsonChild -> children.add(fromJson(jsonChild.getAsJsonObject())));
    }
    return new Measure(
      jsonObject.getAsJsonPrimitive("name").getAsString(),
      jsonObject.getAsJsonPrimitive("calls").getAsLong(),
      jsonObject.getAsJsonPrimitive("durationNanos").getAsLong(),
      children);
  }

  static class MeasurementCost {
    long createChild;
    long incrementChild;
    long nanoTime;
    long observationCost;

    MeasurementCost(Measure sensorMeasure) {
      String measurementCostName = "#MeasurementCost_v1";
      Measure measurementCost = sensorMeasure.get(measurementCostName);
      if (measurementCost == null) {
        throw new IllegalStateException("Missing " + measurementCostName);
      }
      measurementCost.detach();
      createChild = averageDuration(measurementCost, "createChild");
      incrementChild = averageDuration(measurementCost, "incrementChild");
      nanoTime = averageDuration(measurementCost, "nanoTime");
      observationCost = averageDuration(measurementCost, "observationCost");
    }

    static long averageDuration(Measure measurementCost, String name) {
      Measure measure = measurementCost.get(name);
      if (measure == null) {
        throw new IllegalStateException("Missing " + name);
      }
      return measurementCost.calls == 0 ? 0 : (measure.durationNanos / measurementCost.calls);
    }

    void subtractObservationCost(Measure measure) {
      measure.durationNanos -= (measure.calls == 0 ? 0 : (observationCost * measure.calls)) + recursiveChildCallsObservationCost(measure);
      measure.children().forEach(this::subtractObservationCost);
    }

    long callsObservationCost(Measure measure) {
      return measure.calls == 0 ? 0 : (createChild + (incrementChild * (measure.calls - 1)));
    }

    long recursiveCallsObservationCost(Measure measure) {
      return callsObservationCost(measure) + recursiveChildCallsObservationCost(measure);
    }

    long recursiveChildCallsObservationCost(Measure measure) {
      return measure.children().stream().mapToLong(this::recursiveCallsObservationCost).sum();
    }
  }

  static class Measure {

    Measure parent;
    String name;
    long calls;
    long durationNanos;
    Map<String, Measure> childrenMap = new HashMap<>();

    Measure(String name, long calls, long durationNanos, List<Measure> children) {
      this.name = name;
      this.calls = calls;
      this.durationNanos = durationNanos;
      children.forEach(this::add);
    }

    void add(Measure child) {
      if (child.parent != this) {
        child.detach();
        childrenMap.put(child.name, child);
        child.parent = this;
      }
    }

    Measure get(String childName) {
      return childrenMap.get(childName);
    }

    List<Measure> children() {
      return new ArrayList<>(childrenMap.values());
    }

    boolean isEmpty() {
      return childrenMap.isEmpty();
    }

    void detach() {
      if (parent != null) {
        parent.childrenMap.remove(name);
        parent = null;
      }
    }

  }

}
