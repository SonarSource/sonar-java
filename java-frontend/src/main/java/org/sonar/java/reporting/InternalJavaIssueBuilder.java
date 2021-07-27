/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.reporting;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.Preconditions;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

public class InternalJavaIssueBuilder implements FluentReporting.JavaIssueBuilder {

  private static final String MESSAGE_NAME = "message";
  private static final String TEXT_SPAN_NAME = "position";
  private static final String RULE_NAME = "rule";

  private static final Logger LOG = Loggers.get(InternalJavaIssueBuilder.class);

  private final InputFile inputFile;
  @Nullable
  private final SonarComponents sonarComponents;

  private JavaCheck rule;
  private AnalyzerMessage.TextSpan textSpan;
  private String message;
  @Nullable
  private List<JavaFileScannerContext.Location> secondaries;
  @Nullable
  private List<List<JavaFileScannerContext.Location>> flows;
  @Nullable
  private Integer cost;

  public InternalJavaIssueBuilder(InputFile inputFile, @Nullable SonarComponents sonarComponents) {
    this.inputFile = inputFile;
    this.sonarComponents = sonarComponents;
  }

  private static void requiresExistence(Object field, String name) {
    Preconditions.checkState(field != null, String.format("A %s must be set first.", name));
  }

  private static void requiresUniquess(Object field, String name) {
    Preconditions.checkState(field == null, String.format("Cannot set %s multiple times.", name));
  }

  @Override
  public InternalJavaIssueBuilder forRule(JavaCheck rule) {
    requiresUniquess(this.rule, RULE_NAME);

    this.rule = rule;
    return this;
  }

  @Override
  public InternalJavaIssueBuilder onTree(Tree tree) {
    return onRange(AnalyzerMessage.textSpanFor(tree));
  }

  @Override
  public InternalJavaIssueBuilder onRange(Tree from, Tree to) {
    return onRange(AnalyzerMessage.textSpanBetween(from, to));
  }

  private InternalJavaIssueBuilder onRange(AnalyzerMessage.TextSpan range) {
    requiresExistence(this.rule, RULE_NAME);
    requiresUniquess(this.textSpan, TEXT_SPAN_NAME);

    this.textSpan = range;
    return this;
  }

  @Override
  public InternalJavaIssueBuilder withMessage(String message) {
    requiresExistence(this.textSpan, TEXT_SPAN_NAME);
    requiresUniquess(this.message, MESSAGE_NAME);

    this.message = message;
    return this;
  }

  @Override
  public InternalJavaIssueBuilder withMessage(String message, Object... args) {
    requiresExistence(this.textSpan, TEXT_SPAN_NAME);
    requiresUniquess(this.message, MESSAGE_NAME);

    this.message = String.format(message, args);
    return this;
  }

  @Override
  public InternalJavaIssueBuilder withSecondaries(JavaFileScannerContext.Location... secondaries) {
    return withSecondaries(Arrays.asList(secondaries));
  }

  @Override
  public InternalJavaIssueBuilder withSecondaries(List<JavaFileScannerContext.Location> secondaries) {
    requiresExistence(this.message, MESSAGE_NAME);
    requiresUniquess(this.secondaries, "secondaries");
    Preconditions.checkState(this.flows == null, "Cannot set flows and secondaries at the same time.");

    this.secondaries = Collections.unmodifiableList(secondaries);
    return this;
  }

  @Override
  public InternalJavaIssueBuilder withFlows(List<List<JavaFileScannerContext.Location>> flows) {
    requiresExistence(this.message, MESSAGE_NAME);
    requiresUniquess(this.flows, "flows");
    Preconditions.checkState(this.secondaries == null, "Cannot set flows and secondaries at the same time.");

    this.flows = Collections.unmodifiableList(flows);
    return this;
  }

  @Override
  public InternalJavaIssueBuilder withCost(int cost) {
    requiresExistence(this.message, MESSAGE_NAME);
    requiresUniquess(this.cost, "cost");

    this.cost = cost;
    return this;
  }

  @Override
  public void build() {
    requiresExistence(this.rule, RULE_NAME);
    requiresExistence(this.textSpan, TEXT_SPAN_NAME);
    requiresExistence(this.message, MESSAGE_NAME);

    if (sonarComponents == null) {
      // can be noisy , so using trace only.
      LOG.trace("SonarComponents is not set - discarding issue");
      return;
    }
    Optional<RuleKey> ruleKey = sonarComponents.getRuleKey(rule);
    if (!ruleKey.isPresent()) {
      // can be noisy , so using trace only.
      LOG.trace("Rule not enabled - discarding issue");
      return;
    }

    NewIssue newIssue = sonarComponents.context().newIssue()
      .forRule(ruleKey.get())
      .gap(cost == null ? 0 : cost.doubleValue());

    newIssue.at(
      newIssue.newLocation()
        .on(inputFile)
        .at(inputFile.newRange(textSpan.startLine, textSpan.startCharacter, textSpan.endLine, textSpan.endCharacter))
        .message(message));

    if (secondaries != null) {
      flows = secondaries.stream().map(Collections::singletonList).collect(Collectors.toList());
      // Keep secondaries and flows mutually exclusive.
      secondaries = null;
    }

    if (flows != null) {
      for (List<JavaFileScannerContext.Location> flow : flows) {
        newIssue.addFlow(flow.stream()
          .map(location -> newIssue.newLocation()
            .on(inputFile)
            .at(range(inputFile, location))
            .message(location.msg))
          .collect(Collectors.toList()));
      }
    }

    newIssue.save();
  }

  private static TextRange range(InputFile file, JavaFileScannerContext.Location location) {
    AnalyzerMessage.TextSpan textSpan = AnalyzerMessage.textSpanFor(location.syntaxNode);
    return file.newRange(textSpan.startLine, textSpan.startCharacter, textSpan.endLine, textSpan.endCharacter);
  }

  public JavaCheck rule() {
    return rule;
  }

  public InputFile inputFile() {
    return inputFile;
  }

  public String message() {
    return message;
  }

  public AnalyzerMessage.TextSpan textSpan() {
    return textSpan;
  }

  public Optional<Integer> cost() {
    return Optional.ofNullable(cost);
  }

  public Optional<List<JavaFileScannerContext.Location>> secondaries() {
    return Optional.ofNullable(secondaries);
  }

  public Optional<List<List<JavaFileScannerContext.Location>>> flows() {
    return Optional.ofNullable(flows);
  }
}
