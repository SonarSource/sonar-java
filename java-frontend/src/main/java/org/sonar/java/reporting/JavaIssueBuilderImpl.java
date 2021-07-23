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
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.Preconditions;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

public class JavaIssueBuilderImpl implements FluentReporting.JavaIssueBuilder {

  private static final Logger LOG = Loggers.get(JavaIssueBuilderImpl.class);

  protected final InputFile inputFile;
  @Nullable
  private final SonarComponents sonarComponents;

  protected JavaCheck rule;
  protected AnalyzerMessage.TextSpan textSpan;
  protected String message;
  private List<JavaFileScannerContext.Location> secondaries;
  private List<List<JavaFileScannerContext.Location>> flows;
  protected Integer cost;

  public JavaIssueBuilderImpl(InputFile inputFile, @Nullable SonarComponents sonarComponents) {
    this.inputFile = inputFile;
    this.sonarComponents = sonarComponents;
  }

  @Override
  public JavaIssueBuilderImpl forRule(JavaCheck rule) {
    Preconditions.checkState(this.rule == null, "Cannot set rule multiple times.");
    this.rule = rule;
    return this;
  }

  @Override
  public JavaIssueBuilderImpl onTree(Tree tree) {
    Preconditions.checkState(this.rule != null, "A rule must be set first.");
    Preconditions.checkState(this.textSpan == null, "Cannot set position multiple times.");
    textSpan = AnalyzerMessage.textSpanFor(tree);
    return this;
  }

  @Override
  public JavaIssueBuilderImpl onRange(Tree from, Tree to) {
    Preconditions.checkState(this.rule != null, "A rule must be set first.");
    Preconditions.checkState(this.textSpan == null, "Cannot set position multiple times.");
    textSpan = AnalyzerMessage.textSpanBetween(from, to);
    return this;
  }

  @Override
  public JavaIssueBuilderImpl withMessage(String message) {
    return withMessage(message, new Object[0]);
  }

  @Override
  public JavaIssueBuilderImpl withMessage(String message, Object... args) {
    Preconditions.checkState(this.textSpan != null, "A position must be set first.");
    Preconditions.checkState(this.message == null, "Cannot set message multiple times.");
    this.message = String.format(message, args);
    return this;
  }

  @Override
  public JavaIssueBuilderImpl withSecondaries(JavaFileScannerContext.Location... secondaries) {
    return withSecondaries(Arrays.asList(secondaries));
  }

  @Override
  public JavaIssueBuilderImpl withSecondaries(List<JavaFileScannerContext.Location> secondaries) {
    Preconditions.checkState(this.message != null, "A message must be set first.");
    Preconditions.checkState(this.secondaries == null, "Cannot set secondaries multiple times.");
    Preconditions.checkState(this.flows == null, "Cannot set flows and secondaries at the same time.");
    this.secondaries = Collections.unmodifiableList(secondaries);
    return this;
  }

  @Override
  public JavaIssueBuilderImpl withFlows(List<List<JavaFileScannerContext.Location>> flows) {
    Preconditions.checkState(this.message != null, "A message must be set first.");
    Preconditions.checkState(this.flows == null, "Cannot set flows multiple times.");
    Preconditions.checkState(this.secondaries == null, "Cannot set flows and secondaries at the same time.");
    this.flows = Collections.unmodifiableList(flows);
    return this;
  }

  @Override
  public JavaIssueBuilderImpl withCost(int cost) {
    Preconditions.checkState(this.message != null, "A message must be set first.");
    Preconditions.checkState(this.cost == null, "Cannot set cost multiple times.");
    this.cost = cost;
    return this;
  }

  @Override
  public JavaIssueBuilderImpl withQuickFix() {
    // TODO
    return this;
  }

  @Override
  public void build() {
    Preconditions.checkState(this.rule != null, "A rule must be set first.");
    Preconditions.checkState(this.textSpan != null, "A position must be set first.");
    Preconditions.checkState(this.message != null, "A message must be set first.");
    if (sonarComponents == null) {
      LOG.debug("SonarComponents is not set");
      return;
    }
    Optional<RuleKey> ruleKey = sonarComponents.getRuleKey(rule);
    if (!ruleKey.isPresent()) {
      LOG.debug("Rule not enabled");
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
    if (textSpan.onLine()) {
      return file.selectLine(textSpan.startLine);
    }
    return file.newRange(textSpan.startLine, textSpan.startCharacter, textSpan.endLine, textSpan.endCharacter);
  }

}
