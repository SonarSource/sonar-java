/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.testing;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.Preconditions;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;

public class JavaIssueBuilderForTests extends InternalJavaIssueBuilder {

  private final Set<AnalyzerMessage> issues;
  private final Map<AnalyzerMessage.TextSpan, List<JavaQuickFix>> quickFixes;
  private boolean reported;

  public JavaIssueBuilderForTests(InputFile inputFile, Set<AnalyzerMessage> issues, Map<AnalyzerMessage.TextSpan, List<JavaQuickFix>> quickFixes) {
    super(inputFile, null);
    this.issues = issues;
    this.reported = false;
    this.quickFixes = quickFixes;
  }

  @Override
  public void report() {
    Preconditions.checkState(!reported, "Can only be reported once.");
    JavaCheck rule = rule();
    InputFile inputFile = inputFile();
    AnalyzerMessage.TextSpan textSpan = textSpan();
    AnalyzerMessage issue = new AnalyzerMessage(rule, inputFile, textSpan, message(), cost().orElse(0));

    secondaries()
      .map(JavaIssueBuilderForTests::toSingletonList)
      .map(secondaries -> listOfLocationsToListOfAnalyzerMessages(secondaries, rule, inputFile))
      .ifPresent(issue.flows::addAll);

    flows()
      .map(flows -> listOfLocationsToListOfAnalyzerMessages(flows, rule, inputFile))
      .ifPresent(issue.flows::addAll);

    quickFixes.put(textSpan, quickFixes().stream().map(Supplier::get).flatMap(Collection::stream).toList());

    issues.add(issue);
    reported = true;
  }

  private static List<List<JavaFileScannerContext.Location>> toSingletonList(List<JavaFileScannerContext.Location> secondaries) {
    return secondaries.stream()
      .map(Collections::singletonList)
      .toList();
  }

  private static List<List<AnalyzerMessage>> listOfLocationsToListOfAnalyzerMessages(List<List<JavaFileScannerContext.Location>> locations, JavaCheck rule, InputFile inputFile) {
    return locations.stream()
      .map(listOfLocations -> locationsToAnalyzerMessages(listOfLocations, rule, inputFile))
      .toList();
  }

  private static List<AnalyzerMessage> locationsToAnalyzerMessages(List<JavaFileScannerContext.Location> locations, JavaCheck rule, InputFile inputFile) {
    return locations.stream()
      .map(location -> locationToAnalyzerMessage(location, rule, inputFile))
      .toList();
  }

  private static AnalyzerMessage locationToAnalyzerMessage(JavaFileScannerContext.Location location, JavaCheck rule, InputFile inputFile) {
    return new AnalyzerMessage(rule, inputFile, AnalyzerMessage.textSpanFor(location.syntaxNode), location.msg, 0);
  }
}
