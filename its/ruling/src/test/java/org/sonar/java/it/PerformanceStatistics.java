/*
 * SonarQube Java
 * Copyright (C) 2013-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.sonarsource.performance.measure.DurationMeasure;
import org.sonarsource.performance.measure.DurationMeasureFiles;

public class PerformanceStatistics {

  public static void main(String[] args) throws IOException {
    PerformanceStatistics.generate(Paths.get("target","performance"));
  }

  public static void generate(Path performanceDirectory) throws IOException {
    Map<String, String> categoryNames = new HashMap<>();
    categoryNames.put("Main", "1.main");
    categoryNames.put("Test", "1.test");
    categoryNames.put("Scanners", "2.scanners");
    categoryNames.put("IssuableSubscriptionVisitors", "2.subscription");
    categoryNames.put("SymbolicExecutionVisitor", "3.symbolic-execution");
    Predicate<String> groupedMeasurePredicate = name -> name.endsWith("Check");

    Path performanceJsonFile = performanceDirectory.resolve("sonar.java.performance.measure.json");
    DurationMeasure measure = DurationMeasureFiles.fromJsonWithoutObservationCost(performanceJsonFile);
    measure.recursiveMergeOnUpperLevel("RegexParser");
    measure.recursiveMergeOnUpperLevel("JavaWriteCache.write");
    Path performanceStatFile = performanceDirectory.resolve("sonar.java.performance.statistics.txt");
    DurationMeasureFiles.writeStatistics(performanceStatFile, measure, categoryNames, groupedMeasurePredicate);
  }

}
