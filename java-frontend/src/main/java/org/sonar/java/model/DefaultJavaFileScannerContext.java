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
package org.sonar.java.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.EndOfAnalysisCheck;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.visitors.ComplexityVisitor;
import org.sonar.java.regex.RegexCache;
import org.sonar.java.regex.RegexCheck;
import org.sonar.java.regex.RegexScannerContext;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.FluentReporting;
import org.sonar.java.reporting.InternalJavaIssueBuilder;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.SourceMap;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.analyzer.commons.regex.ast.RegexSyntaxElement;

public class DefaultJavaFileScannerContext implements JavaFileScannerContext, RegexScannerContext, FluentReporting {
  private final JavaTree.CompilationUnitTreeImpl tree;
  private final boolean semanticEnabled;
  private final SonarComponents sonarComponents;
  private final ComplexityVisitor complexityVisitor;
  private final RegexCache regexCache;
  private final InputFile inputFile;
  private final JavaVersion javaVersion;
  private final boolean fileParsed;

  public DefaultJavaFileScannerContext(CompilationUnitTree tree, InputFile inputFile, Sema semanticModel,
                                       @Nullable SonarComponents sonarComponents, JavaVersion javaVersion, boolean fileParsed) {
    this.tree = (JavaTree.CompilationUnitTreeImpl) tree;
    this.inputFile = inputFile;
    this.semanticEnabled = semanticModel != null;
    this.sonarComponents = sonarComponents;
    this.complexityVisitor = new ComplexityVisitor();
    this.regexCache = new RegexCache();
    this.javaVersion = javaVersion;
    this.fileParsed = fileParsed;
  }

  @Override
  public CompilationUnitTree getTree() {
    return tree;
  }

  @Override
  public void addIssueOnFile(JavaCheck javaCheck, String message) {
    addIssue(-1, javaCheck, message);
  }

  @Override
  public void addIssueOnProject(JavaCheck check, String message) {
    sonarComponents.addIssue(getProject(), check, -1, message, 0);
  }

  @Override
  public void addIssue(int line, JavaCheck javaCheck, String message) {
    addIssue(line, javaCheck, message, null);
  }

  @Override
  public void addIssue(int line, JavaCheck javaCheck, String message, @Nullable Integer cost) {
    sonarComponents.addIssue(inputFile, javaCheck, line, message, cost);
  }

  @Override
  @Nullable
  public Object getSemanticModel() {
    if (!semanticEnabled) {
      return null;
    }
    return tree.sema;
  }

  @Override
  public JavaVersion getJavaVersion() {
    return this.javaVersion;
  }

  @Override
  public boolean fileParsed() {
    return fileParsed;
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree tree, String message) {
    reportIssue(javaCheck, tree, message, Collections.emptyList(), null);
  }

  @Override
  public void reportIssue(RegexCheck regexCheck, RegexSyntaxElement regexTree, String message, @Nullable Integer cost, List<RegexCheck.RegexIssueLocation> secondaries) {
    List<RegexCheck.RegexIssueLocation> completedSecondaries = new ArrayList<>();

    List<RegexCheck.RegexIssueLocation> mainLocations = new RegexCheck.RegexIssueLocation(regexTree, message).toSingleLocationItems();
    if (mainLocations.size() > 1) {
      // handle other main locations as secondaries with same message
      mainLocations.subList(1, mainLocations.size())
        .stream()
        .forEach(completedSecondaries::add);
    }
    completedSecondaries.addAll(secondaries);

    reportIssue(regexCheck, mainLocations.get(0).locations().get(0), message, cost, completedSecondaries);
  }

  @Override
  public void reportIssue(RegexCheck regexCheck, Tree javaSyntaxElement, String message, @Nullable Integer cost, List<RegexCheck.RegexIssueLocation> secondaries) {
    reportIssue(regexCheck, AnalyzerMessage.textSpanFor(javaSyntaxElement), message, cost, secondaries);
  }

  private void reportIssue(RegexCheck regexCheck, AnalyzerMessage.TextSpan mainLocation, String message, @Nullable Integer cost, List<RegexCheck.RegexIssueLocation> secondaries) {
    List<List<RegexCheck.RegexIssueLocation>> secondariesAsFlows = new ArrayList<>();

    secondaries.stream()
      .flatMap(regexIssueLocation -> regexIssueLocation.toSingleLocationItems().stream())
      .map(Collections::singletonList)
      .forEach(secondariesAsFlows::add);

    AnalyzerMessage analyzerMessage = new AnalyzerMessage(regexCheck, inputFile, mainLocation, message, cost != null ? cost : 0);
    completeAnalyzerMessageWithFlows(analyzerMessage, secondariesAsFlows, ril -> ril.locations().get(0), RegexCheck.RegexIssueLocation::message);
    reportIssue(analyzerMessage);
  }

  @Override
  public RegexParseResult regexForLiterals(FlagSet initialFlags, LiteralTree... stringLiterals) {
    return regexCache.getRegexForLiterals(initialFlags, stringLiterals);
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree syntaxNode, String message, List<Location> secondary, @Nullable Integer cost) {
    List<List<Location>> flows = secondary.stream().map(Collections::singletonList).collect(Collectors.toList());
    reportIssueWithFlow(javaCheck, syntaxNode, message, flows, cost);
  }

  @Override
  public void reportIssueWithFlow(JavaCheck javaCheck, Tree syntaxNode, String message, Iterable<List<Location>> flows, @Nullable Integer cost) {
    throwIfEndOfAnalysisCheck(javaCheck);

    reportIssue(createAnalyzerMessage(inputFile, javaCheck, syntaxNode, null, message, flows, cost));
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message) {
    reportIssue(javaCheck, startTree, endTree, message, Collections.emptyList(), null);
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message, List<Location> secondary, @Nullable Integer cost) {
    throwIfEndOfAnalysisCheck(javaCheck);

    List<List<Location>> flows = secondary.stream().map(Collections::singletonList).collect(Collectors.toList());
    reportIssue(createAnalyzerMessage(inputFile, javaCheck, startTree, endTree, message, flows, cost));
  }

  @Override
  public List<String> getFileLines() {
    return sonarComponents.fileLines(inputFile);
  }

  @Override
  public String getFileContent() {
    return sonarComponents.inputFileContents(inputFile);
  }

  public void reportIssue(AnalyzerMessage message) {
    sonarComponents.reportIssue(message);
  }

  public AnalyzerMessage createAnalyzerMessage(JavaCheck javaCheck, Tree startTree, String message) {
    return createAnalyzerMessage(inputFile, javaCheck, startTree, null, message, Collections.emptyList(), null);
  }

  protected static AnalyzerMessage createAnalyzerMessage(InputFile inputFile, JavaCheck javaCheck, Tree startTree, @Nullable Tree endTree, String message,
    Iterable<List<Location>> flows, @Nullable Integer cost) {

    AnalyzerMessage.TextSpan location = endTree != null ? AnalyzerMessage.textSpanBetween(startTree, endTree) : AnalyzerMessage.textSpanFor(startTree);
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, inputFile, location, message, cost != null ? cost : 0);
    completeAnalyzerMessageWithFlows(analyzerMessage, flows, loc -> AnalyzerMessage.textSpanFor(loc.syntaxNode), loc -> loc.msg);
    return analyzerMessage;
  }

  private static <L> void completeAnalyzerMessageWithFlows(
    AnalyzerMessage analyzerMessage,
    Iterable<List<L>> flows,
    Function<L, AnalyzerMessage.TextSpan> flowItemLocationProdivder,
    Function<L, String> flowItemMessageProvider) {

    JavaCheck check = analyzerMessage.getCheck();
    InputComponent component = analyzerMessage.getInputComponent();

    for (List<L> flow : flows) {
      List<AnalyzerMessage> sonarqubeFlow = flow.stream()
        .map(l -> new AnalyzerMessage(check, component, flowItemLocationProdivder.apply(l), flowItemMessageProvider.apply(l),0))
        .collect(Collectors.toList());
      analyzerMessage.flows.add(sonarqubeFlow);
    }
  }

  @Override
  public InputFile getInputFile() {
    return inputFile;
  }

  @Override
  public InputComponent getProject() {
    return sonarComponents.project();
  }

  @Override
  public File getWorkingDirectory() {
    return sonarComponents.workDir();
  }

  @Override
  public List<Tree> getComplexityNodes(Tree tree) {
    return complexityVisitor.getNodes(tree);
  }

  protected static void throwIfEndOfAnalysisCheck(JavaCheck javaCheck) {
    if (javaCheck instanceof EndOfAnalysisCheck) {
      throw new UnsupportedOperationException("EndOfAnalysisCheck must only call reportIssue with AnalyzerMessage and must never pass a Tree reference.");
    }
  }

  @Override
  public Optional<SourceMap> sourceMap() {
    if (inputFile instanceof GeneratedFile) {
      return Optional.of(((GeneratedFile) inputFile).sourceMap());
    }
    return Optional.empty();
  }

  @Override
  public JavaIssueBuilder newIssue() {
    return new InternalJavaIssueBuilder(inputFile, sonarComponents);
  }
}
