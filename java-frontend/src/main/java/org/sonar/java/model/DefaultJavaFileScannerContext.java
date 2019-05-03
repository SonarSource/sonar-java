/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.EndOfAnalysisCheck;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.visitors.ComplexityVisitor;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

public class DefaultJavaFileScannerContext implements JavaFileScannerContext {
  private final CompilationUnitTree tree;
  @VisibleForTesting
  private final SemanticModel semanticModel;
  private final SonarComponents sonarComponents;
  private final ComplexityVisitor complexityVisitor;
  private final InputFile inputFile;
  private final JavaVersion javaVersion;
  private final boolean fileParsed;

  public DefaultJavaFileScannerContext(CompilationUnitTree tree, InputFile inputFile, SemanticModel semanticModel,
                                       @Nullable SonarComponents sonarComponents, JavaVersion javaVersion, boolean fileParsed) {
    this.tree = tree;
    this.inputFile = inputFile;
    this.semanticModel = semanticModel;
    this.sonarComponents = sonarComponents;
    this.complexityVisitor = new ComplexityVisitor();
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
    return semanticModel;
  }

  @Override
  public JavaVersion getJavaVersion() {
    return this.javaVersion;
  }

  @Override
  public boolean fileParsed() {
    return fileParsed;
  }

  /**
   * @deprecated since SonarJava 5.12 - Use key of InputFile instead, using {@link #getInputFile()}.
   */
  @Deprecated
  @Override
  public String getFileKey() {
    return inputFile.file().getAbsolutePath();
  }

  /**
   * @deprecated since SonarJava 5.12 - Use 'reportIssue' instead, or dedicated method to report issue directly on file or project
   */
  @Deprecated
  @Override
  public void addIssue(File file, JavaCheck check, int line, String message) {
    if (sonarComponents != null) {
      sonarComponents.addIssue(file, check, line, message, null);
    }
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree tree, String message) {
    reportIssue(javaCheck, tree, message, Collections.emptyList(), null);
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
    return createAnalyzerMessage(inputFile, javaCheck, startTree, null, message, new ArrayList<>(), null);
  }

  protected static AnalyzerMessage createAnalyzerMessage(InputFile inputFile, JavaCheck javaCheck, Tree startTree, @Nullable Tree endTree, String message,
    Iterable<List<Location>> flows, @Nullable Integer cost) {
    AnalyzerMessage.TextSpan textSpan = endTree != null ? AnalyzerMessage.textSpanBetween(startTree, endTree) : AnalyzerMessage.textSpanFor(startTree);
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, inputFile, textSpan, message, cost != null ? cost : 0);
    for (List<Location> flow : flows) {
      List<AnalyzerMessage> sonarqubeFlow =
      flow.stream().map(l -> new AnalyzerMessage(javaCheck, inputFile, AnalyzerMessage.textSpanFor(l.syntaxNode), l.msg, 0)).collect(Collectors.toList());
      analyzerMessage.flows.add(sonarqubeFlow);
    }
    return analyzerMessage;
  }

  /**
   * @deprecated since SonarJava 5.12 - Rely on {@link #getInputFile()} instead
   */
  @Deprecated
  @Override
  public File getFile() {
    return inputFile.file();
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

  public File getBaseDirectory() {
    return sonarComponents.baseDir();
  }

  @Override
  public List<Tree> getComplexityNodes(Tree tree) {
    return complexityVisitor.getNodes(tree);
  }

  private static void throwIfEndOfAnalysisCheck(JavaCheck javaCheck) {
    if (javaCheck instanceof EndOfAnalysisCheck) {
      throw new UnsupportedOperationException("EndOfAnalysisCheck must only call reportIssue with AnalyzerMessage and must never pass a Tree reference.");
    }
  }
}
