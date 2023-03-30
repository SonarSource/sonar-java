/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.ast.visitors.CognitiveComplexityVisitor;
import org.sonar.java.ast.visitors.CommentLinesVisitor;
import org.sonar.java.ast.visitors.ComplexityVisitor;
import org.sonar.java.ast.visitors.LinesOfCodeVisitor;
import org.sonar.java.ast.visitors.MethodNestingLevelVisitor;
import org.sonar.java.ast.visitors.NumberOfAccessedVariablesVisitor;
import org.sonar.java.ast.visitors.StatementVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

public class MetricsComputer {

  private final Map<Integer, List<Tree>> methodComplexityNodes = new HashMap<>();
  private final Map<Integer, CognitiveComplexityVisitor.Result> methodComplexity = new HashMap<>();
  private final Map<Integer, Integer> compilationUnityComplexity = new HashMap<>();
  private final Map<Integer, Integer> methodNumberOfAccessedVariables = new HashMap<>();
  private final Map<Integer, Integer> treeLinesOfCode = new HashMap<>();
  private final Map<Integer, Integer> treeNumberOfStatements = new HashMap<>();
  private final Map<Integer, Integer> treeNumberOfCommentedLines = new HashMap<>();
  private final Map<Integer, Set<Integer>> treeNoSonarLines = new HashMap<>();
  private final Map<Integer, Integer> methodNestingLevel = new HashMap<>();

  ComplexityVisitor complexityVisitor = new ComplexityVisitor();

  public List<Tree> getComplexityNodes(Tree tree) {
    return methodComplexityNodes.computeIfAbsent(tree.hashCode(), k -> complexityVisitor.getNodes(tree));
  }

  public CognitiveComplexityVisitor.Result methodComplexity(MethodTree tree) {
    return methodComplexity.computeIfAbsent(tree.hashCode(), k -> CognitiveComplexityVisitor.methodComplexity(tree));
  }

  NumberOfAccessedVariablesVisitor methodBodyVisitor = new NumberOfAccessedVariablesVisitor();

  public int getNumberOfAccessedVariables(MethodTree tree) {
    return methodNumberOfAccessedVariables.computeIfAbsent(tree.hashCode(), k -> methodBodyVisitor.getNumberOfAccessedVariables(tree));
  }

  LinesOfCodeVisitor linesOfCodeVisitor = new LinesOfCodeVisitor();

  public int linesOfCode(Tree tree) {
    return treeLinesOfCode.computeIfAbsent(tree.hashCode(), k -> linesOfCodeVisitor.linesOfCode(tree));
  }

  StatementVisitor numberOfStatementsVisitor = new StatementVisitor();

  public int numberOfStatements(Tree tree) {
    return treeNumberOfStatements.computeIfAbsent(tree.hashCode(), k -> numberOfStatementsVisitor.numberOfStatements(tree));
  }

  CommentLinesVisitor commentedLineVisitor = new CommentLinesVisitor();

  public Integer numberOfCommentedLines(CompilationUnitTree tree) {
    return treeNumberOfCommentedLines.computeIfAbsent(tree.hashCode(), k -> {
      commentedLineVisitor.analyzeCommentLines(tree);
      return commentedLineVisitor.commentLinesMetric();
    });
  }

  public Set<Integer> noSonarLines(CompilationUnitTree tree) {
    return treeNoSonarLines.computeIfAbsent(tree.hashCode(), k -> {
      commentedLineVisitor.analyzeCommentLines(tree);
      return commentedLineVisitor.noSonarLines();
    });
  }

  public int compilationUnitComplexity(CompilationUnitTree tree) {
    return compilationUnityComplexity.computeIfAbsent(tree.hashCode(), k -> CognitiveComplexityVisitor.compilationUnitComplexity(tree));
  }

  MethodNestingLevelVisitor methodNestingVisitor = new MethodNestingLevelVisitor();

  public int methodNestingLevel(MethodTree tree) {
    return methodNestingLevel.computeIfAbsent(tree.hashCode(), k -> methodNestingVisitor.getMaxNestingLevel(tree));
  }

  @VisibleForTesting
  Map<Integer, List<Tree>> getMethodComplexityNodes() {
    return methodComplexityNodes;
  }

  @VisibleForTesting
  Map<Integer, CognitiveComplexityVisitor.Result> getMethodComplexity() {
    return methodComplexity;
  }

  @VisibleForTesting
  Map<Integer, Integer> getCompilationUnityComplexity() {
    return compilationUnityComplexity;
  }

  @VisibleForTesting
  Map<Integer, Integer> getMethodNumberOfAccessedVariables() {
    return methodNumberOfAccessedVariables;
  }

  @VisibleForTesting
  Map<Integer, Integer> getTreeLinesOfCode() {
    return treeLinesOfCode;
  }

  @VisibleForTesting
  Map<Integer, Integer> getTreeNumberOfStatements() {
    return treeNumberOfStatements;
  }

  @VisibleForTesting
  Map<Integer, Integer> getTreeNumberOfCommentedLines() {
    return treeNumberOfCommentedLines;
  }

  @VisibleForTesting
  Map<Integer, Set<Integer>> getTreeNoSonarLines() {
    return treeNoSonarLines;
  }

  @VisibleForTesting
  Map<Integer, Integer> getMethodNestingLevel() {
    return methodNestingLevel;
  }

}
