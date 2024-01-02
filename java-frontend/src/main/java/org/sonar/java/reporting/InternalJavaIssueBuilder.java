/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.fix.NewInputFileEdit;
import org.sonar.api.batch.sensor.issue.fix.NewQuickFix;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.Preconditions;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

public class InternalJavaIssueBuilder implements JavaIssueBuilderExtended {

  private static final String RULE_NAME = "rule";
  private static final String TEXT_SPAN_NAME = "position";
  private static final String MESSAGE_NAME = "message";
  private static final String FLOWS_NAME = "flows";
  private static final String SECONDARIES_NAME = "secondaries";

  private static final Logger LOG = LoggerFactory.getLogger(InternalJavaIssueBuilder.class);

  private final InputFile inputFile;
  @Nullable
  private final SonarComponents sonarComponents;
  private final boolean isQuickFixCompatible;
  private final boolean isSetQuickFixAvailableCompatible;

  private JavaCheck rule;
  private AnalyzerMessage.TextSpan textSpan;
  private String message;
  @Nullable
  private List<JavaFileScannerContext.Location> secondaries;
  @Nullable
  private List<List<JavaFileScannerContext.Location>> flows;
  @Nullable
  private Integer cost;
  private final List<Supplier<List<JavaQuickFix>>> quickFixes = new ArrayList<>();
  private boolean reported;

  public InternalJavaIssueBuilder(InputFile inputFile, @Nullable SonarComponents sonarComponents) {
    this.inputFile = inputFile;
    this.sonarComponents = sonarComponents;
    this.reported = false;
    isQuickFixCompatible = sonarComponents != null && sonarComponents.isQuickFixCompatible();
    isSetQuickFixAvailableCompatible = sonarComponents != null && sonarComponents.isSetQuickFixAvailableCompatible();
  }

  private static void requiresValueToBeSet(Object target, String targetName) {
    Preconditions.checkState(target != null, String.format("A %s must be set first.", targetName));
  }

  private static void requiresValueNotToBeSet(Object target, String targetName, String otherName) {
    Preconditions.checkState(target == null, String.format("Cannot set %s when %s is already set.", targetName, otherName));
  }

  private static void requiresSetOnlyOnce(Object target, String targetName) {
    Preconditions.checkState(target == null, String.format("Cannot set %s multiple times.", targetName));
  }

  @Override
  public final InternalJavaIssueBuilder forRule(JavaCheck rule) {
    requiresSetOnlyOnce(this.rule, RULE_NAME);

    this.rule = rule;
    return this;
  }

  @Override
  public final InternalJavaIssueBuilder onTree(Tree tree) {
    return onRange(AnalyzerMessage.textSpanFor(tree));
  }

  @Override
  public final InternalJavaIssueBuilder onRange(Tree from, Tree to) {
    return onRange(AnalyzerMessage.textSpanBetween(from, to));
  }

  private InternalJavaIssueBuilder onRange(AnalyzerMessage.TextSpan range) {
    requiresValueToBeSet(this.rule, RULE_NAME);
    requiresSetOnlyOnce(this.textSpan, TEXT_SPAN_NAME);

    this.textSpan = range;
    return this;
  }

  @Override
  public final InternalJavaIssueBuilder withMessage(String message) {
    requiresValueToBeSet(this.textSpan, TEXT_SPAN_NAME);
    requiresSetOnlyOnce(this.message, MESSAGE_NAME);

    this.message = message;
    return this;
  }

  @Override
  public final InternalJavaIssueBuilder withMessage(String message, Object... args) {
    requiresValueToBeSet(this.textSpan, TEXT_SPAN_NAME);
    requiresSetOnlyOnce(this.message, MESSAGE_NAME);

    this.message = String.format(message, args);
    return this;
  }

  @Override
  public final InternalJavaIssueBuilder withSecondaries(JavaFileScannerContext.Location... secondaries) {
    return withSecondaries(Arrays.asList(secondaries));
  }

  @Override
  public final InternalJavaIssueBuilder withSecondaries(List<JavaFileScannerContext.Location> secondaries) {
    requiresValueToBeSet(this.message, MESSAGE_NAME);
    requiresValueNotToBeSet(this.flows, FLOWS_NAME, SECONDARIES_NAME);
    requiresSetOnlyOnce(this.secondaries, SECONDARIES_NAME);

    this.secondaries = Collections.unmodifiableList(secondaries);
    return this;
  }

  @Override
  public final InternalJavaIssueBuilder withFlows(List<List<JavaFileScannerContext.Location>> flows) {
    requiresValueToBeSet(this.message, MESSAGE_NAME);
    requiresValueNotToBeSet(this.secondaries, SECONDARIES_NAME, FLOWS_NAME);
    requiresSetOnlyOnce(this.flows, FLOWS_NAME);

    this.flows = Collections.unmodifiableList(flows);
    return this;
  }

  @Override
  public final InternalJavaIssueBuilder withCost(int cost) {
    requiresValueToBeSet(this.message, MESSAGE_NAME);
    requiresSetOnlyOnce(this.cost, "cost");

    this.cost = cost;
    return this;
  }

  @Override
  public final InternalJavaIssueBuilder withQuickFix(Supplier<JavaQuickFix> quickFix) {
    requiresValueToBeSet(this.message, MESSAGE_NAME);

    this.quickFixes.add(() -> Collections.singletonList(quickFix.get()));
    return this;
  }

  @Override
  public final InternalJavaIssueBuilder withQuickFixes(Supplier<List<JavaQuickFix>> quickFixes) {
    requiresValueToBeSet(this.message, MESSAGE_NAME);

    this.quickFixes.add(quickFixes);
    return this;
  }

  @Override
  public void report() {
    Preconditions.checkState(!reported, "Can only be reported once.");
    requiresValueToBeSet(rule, RULE_NAME);
    requiresValueToBeSet(textSpan, TEXT_SPAN_NAME);
    requiresValueToBeSet(message, MESSAGE_NAME);

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

    final RuleKey ruleKeyVal = ruleKey.get();
    NewIssue newIssue = sonarComponents.context().newIssue()
      .forRule(ruleKeyVal)
      .gap(cost == null ? 0 : cost.doubleValue());

    newIssue.at(
      newIssue.newLocation()
        .on(inputFile)
        .at(inputFile.newRange(textSpan.startLine, textSpan.startCharacter, textSpan.endLine, textSpan.endCharacter))
        .message(message));

    if (secondaries != null) {
      // Transform secondaries into flows: List(size:N)<Location> -> List(size:N)<List(size:1)<Location>>"
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

    handleQuickFixes(ruleKeyVal, newIssue);

    newIssue.save();
    reported = true;
  }

  private void handleQuickFixes(RuleKey ruleKey, NewIssue newIssue) {
    if (quickFixes.isEmpty() || (!isQuickFixCompatible && !isSetQuickFixAvailableCompatible)) {
      return;
    }
    final List<JavaQuickFix> flatQuickFixes = quickFixes.stream()
      .flatMap(s -> s.get().stream())
      .collect(Collectors.toList());
    if (flatQuickFixes.isEmpty()) {
      return;
    }
    if (isQuickFixCompatible) {
      addQuickFixes(inputFile, ruleKey, flatQuickFixes, newIssue);
    } else {
      newIssue.setQuickFixAvailable(true);
    }
  }

  private static void addQuickFixes(InputFile inputFile, RuleKey ruleKey, Iterable<JavaQuickFix> quickFixes, NewIssue sonarLintIssue) {
    try {
      for (JavaQuickFix quickFix : quickFixes) {
        NewQuickFix newQuickFix = sonarLintIssue.newQuickFix()
          .message(quickFix.getDescription());

        NewInputFileEdit edit = newQuickFix.newInputFileEdit().on(inputFile);

        quickFix.getTextEdits().stream()
          .map(javaTextEdit ->
            edit.newTextEdit().at(rangeFromTextSpan(inputFile, javaTextEdit.getTextSpan()))
              .withNewText(javaTextEdit.getReplacement()))
          .forEach(edit::addTextEdit);
        newQuickFix.addInputFileEdit(edit);
        sonarLintIssue.addQuickFix(newQuickFix);
      }
    } catch (RuntimeException e) {
      // We still want to report the issue if we did not manage to create a quick fix.
      LOG.warn(String.format("Could not report quick fixes for rule: %s. %s: %s", ruleKey, e.getClass().getName(), e.getMessage()));
    }
  }

  private static TextRange range(InputFile file, JavaFileScannerContext.Location location) {
    return rangeFromTextSpan(file, AnalyzerMessage.textSpanFor(location.syntaxNode));
  }

  private static TextRange rangeFromTextSpan(InputFile file, AnalyzerMessage.TextSpan textSpan) {
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

  public List<Supplier<List<JavaQuickFix>>> quickFixes() {
    return quickFixes;
  }

}
