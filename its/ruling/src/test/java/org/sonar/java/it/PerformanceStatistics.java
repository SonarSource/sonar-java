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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.sonarsource.performance.measure.DurationMeasure;
import org.sonarsource.performance.measure.DurationMeasureFiles;

public class PerformanceStatistics {

  static final String MEASUREMENT_FILE = "sonar.java.performance.measure.json";
  private static final String STATISTICS_FILE = "sonar.java.performance.statistics.txt";

  public static void main(String[] args) throws IOException {
    PerformanceStatistics.generate(Paths.get("target", "performance"));
  }

  static void generateFromAnalyses(Path performanceDirectory, Path analysesDirectory) throws IOException {
    DurationMeasure total = null;
    try (var analyses = Files.list(analysesDirectory)) {
      var measurementFiles = analyses.map(directory -> directory.resolve(MEASUREMENT_FILE))
        .filter(Files::isRegularFile)
        .sorted()
        .toList();
      for (Path measurementFile : measurementFiles) {
        DurationMeasure measure = DurationMeasureFiles.fromJson(measurementFile);
        total = total == null ? measure : total.merge(measure);
      }
    }
    Path combinedMeasurement = performanceDirectory.resolve(MEASUREMENT_FILE);
    if (total == null) {
      Files.deleteIfExists(combinedMeasurement);
      Files.deleteIfExists(performanceDirectory.resolve(STATISTICS_FILE));
      return;
    }
    Files.createDirectories(performanceDirectory);
    DurationMeasureFiles.writeJson(combinedMeasurement, total);
    generate(performanceDirectory);
  }

  public static void generate(Path performanceDirectory) throws IOException {
    Map<String, String> categoryNames = new HashMap<>();
    categoryNames.put("Main", "1.main");
    categoryNames.put("Test", "1.test");
    categoryNames.put("Scanners", "2.scanners");
    categoryNames.put("IssuableSubscriptionVisitors", "2.subscription");
    categoryNames.put("SymbolicExecutionVisitor", "3.symbolic-execution");
    Predicate<String> groupedMeasurePredicate = name -> name.endsWith("Check");

    Path performanceJsonFile = performanceDirectory.resolve(MEASUREMENT_FILE);
    DurationMeasure measure = DurationMeasureFiles.fromJsonWithoutObservationCost(performanceJsonFile);
    measure.recursiveMergeOnUpperLevel("RegexParser");
    measure.recursiveMergeOnUpperLevel("JavaWriteCache.write");
    Path performanceStatFile = performanceDirectory.resolve(STATISTICS_FILE);
    DurationMeasureFiles.writeStatistics(performanceStatFile, measure, categoryNames, groupedMeasurePredicate);
  }

}
