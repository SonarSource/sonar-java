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
package org.sonar.java.testing;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.SonarComponents;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.Sema;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.FluentReporting;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

public class JavaFileScannerContextForTests extends DefaultJavaFileScannerContext {

  private final Set<AnalyzerMessage> issues = new LinkedHashSet<>();
  private final SonarComponents sonarComponents;

  public JavaFileScannerContextForTests(CompilationUnitTree tree, InputFile inputFile, Sema semanticModel,
                                        @Nullable SonarComponents sonarComponents, JavaVersion javaVersion, boolean failedParsing) {
    super(tree, inputFile, semanticModel, sonarComponents, javaVersion, failedParsing);
    this.sonarComponents = sonarComponents;
  }

  public Set<AnalyzerMessage> getIssues() {
    return issues;
  }

  @Override
  public void addIssueOnProject(JavaCheck javaCheck, String message) {
    issues.add(new AnalyzerMessage(javaCheck, sonarComponents.project(), null, message, 0));
  }

  @Override
  public void addIssue(int line, JavaCheck javaCheck, String message, @Nullable Integer cost) {
    issues.add(new AnalyzerMessage(javaCheck, getInputFile(), line, message, cost != null ? cost.intValue() : 0));
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree tree, String message) {
    throwIfEndOfAnalysisCheck(javaCheck);
    newIssue()
      .forRule(javaCheck)
      .onTree(tree)
      .withMessage(message)
      .report();
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree syntaxNode, String message, List<JavaFileScannerContext.Location> secondary, @Nullable Integer cost) {
    throwIfEndOfAnalysisCheck(javaCheck);
    newIssue()
      .forRule(javaCheck)
      .onTree(syntaxNode)
      .withMessage(message)
      .withSecondaries(secondary)
      .withCost(cost == null ? 0 : cost)
      .report();
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message) {
    throwIfEndOfAnalysisCheck(javaCheck);
    newIssue()
      .forRule(javaCheck)
      .onRange(startTree, endTree)
      .withMessage(message)
      .report();
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message, List<JavaFileScannerContext.Location> secondary, @Nullable Integer cost) {
    throwIfEndOfAnalysisCheck(javaCheck);
    newIssue()
      .forRule(javaCheck)
      .onRange(startTree, endTree)
      .withMessage(message)
      .withSecondaries(secondary)
      .withCost(cost == null ? 0 : cost)
      .report();
  }

  @Override
  public void reportIssueWithFlow(JavaCheck javaCheck, Tree syntaxNode, String message, Iterable<List<JavaFileScannerContext.Location>> flows, @Nullable Integer cost) {
    throwIfEndOfAnalysisCheck(javaCheck);
    newIssue()
      .forRule(javaCheck)
      .onTree(syntaxNode)
      .withMessage(message)
      .withFlows(StreamSupport.stream(flows.spliterator(), false).collect(Collectors.toList()))
      .withCost(cost == null ? 0 : cost)
      .report();
  }

  @Override
  public void reportIssue(AnalyzerMessage message) {
    issues.add(message);
  }

  @Override
  public AnalyzerMessage createAnalyzerMessage(JavaCheck javaCheck, Tree startTree, String message) {
    return createAnalyzerMessage(getInputFile(), javaCheck, startTree, null, message, Collections.emptyList(), null);
  }

  @Override
  public FluentReporting.JavaIssueBuilder newIssue() {
    return new JavaIssueBuilderForTests(getInputFile(), issues);
  }
}
