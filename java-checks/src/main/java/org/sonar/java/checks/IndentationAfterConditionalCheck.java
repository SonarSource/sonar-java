/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

@Rule(key = "S3973")
public class IndentationAfterConditionalCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    if (thereIsNoIssue(tree)) {
      return;
    }
    if (checkElseStatementReports(tree)) {
      context.reportIssue(this, tree.elseKeyword(),
        "Use curly braces or indentation to denote the code conditionally executed by this \"" + tree.elseKeyword().text() + "\".");
    }
    report(tree.ifKeyword(), tree.closeParenToken(), tree.ifKeyword().text(), findSecondariesToReport(tree));
    super.visitIfStatement(tree);
  }

  @Override
  public void visitWhileStatement(WhileStatementTree tree) {
    if (thereIsNoIssue(tree)) {
      return;
    }
    report(tree.whileKeyword(), tree.closeParenToken(), tree.whileKeyword().text(), findSecondariesToReport(tree));
    super.visitWhileStatement(tree);
  }

  @Override
  public void visitForStatement(ForStatementTree tree) {
    if (thereIsNoIssue(tree)) {
      return;
    }
    report(tree.forKeyword(), tree.closeParenToken(), tree.forKeyword().text(), findSecondariesToReport(tree));
    super.visitForStatement(tree);
  }

  @Override
  public void visitForEachStatement(ForEachStatement tree) {
    if (thereIsNoIssue(tree)) {
      return;
    }
    report(tree.forKeyword(), tree.closeParenToken(), tree.forKeyword().text(), findSecondariesToReport(tree));
    super.visitForEachStatement(tree);
  }

  private static boolean thereIsNoIssue(Tree tree) {
    return (blockExists(tree) || checkIdentation(tree));
  }

  public void report(Tree firstTree, Tree secondTree, String operationName, List<JavaFileScannerContext.Location> flow) {
    context.reportIssue(this, firstTree, secondTree,
      "Use curly braces or indentation to denote the code conditionally executed by this \"" + operationName + "\".", flow, null);
  }

  private static boolean checkElseStatementReports(IfStatementTree tree) {
    if (tree.elseKeyword() != null && !tree.elseStatement().is(Tree.Kind.IF_STATEMENT)) {
      return tree.elseKeyword().column() >= tree.elseStatement().firstToken().column();
    }
    return false;
  }

  private static List<JavaFileScannerContext.Location> findSecondariesToReport(Tree tree) {
    Tree parentTree = tree.parent();
    List<Tree> secondaryLinesToReport = new ArrayList<>();
    StatementTree statement = returnStatement(tree);
    secondaryLinesToReport.add(statement);
    if (parentTree.is(Tree.Kind.BLOCK)) {
      List<StatementTree> blockStmtList = ((BlockTree) parentTree).body();
      secondaryLinesToReport.addAll(loopUntilNextEmptyLine(statement.firstToken().line(), blockStmtList, blockStmtList.indexOf(tree)));
    }
    return secondaryLinesToReport.stream().map(lineToReport -> new JavaFileScannerContext.Location("", lineToReport))
      .collect(Collectors.toList());
  }

  private static List<Tree> loopUntilNextEmptyLine(int previousLine, List<StatementTree> statementsList, int currentLine) {
    List<Tree> secondaryLinesToReport = new ArrayList<>();
    for (int i = currentLine + 1; i < statementsList.size(); i++) {
      StatementTree toBeReportedLine = statementsList.get(i);
      int currLine = toBeReportedLine.firstToken().line();
      if (previousLine < currLine - 1
        && linesBeforeCurrentAreNotCommented(toBeReportedLine.firstToken().trivias().stream().map(SyntaxTrivia::startLine).collect(Collectors.toList()), currLine - 1)) {
        break;
      } else {
        secondaryLinesToReport.add(toBeReportedLine);
        previousLine = currLine;
      }
    }
    return secondaryLinesToReport;

  }

  private static boolean linesBeforeCurrentAreNotCommented(List<Integer> syntaxTrivia, int prevLine) {
    return !syntaxTrivia.contains(prevLine);
  }

  private static int columnStatement(Tree tree) {
    return tree.firstToken().column();
  }

  private static boolean checkIdentation(Tree tree) {
    return returnStatement(tree).firstToken().column() > columnStatement(tree);
  }

  private static boolean blockExists(Tree tree) {
    StatementTree statementTree = returnStatement(tree);
    return statementTree.is(Tree.Kind.BLOCK);
  }

  private static StatementTree returnStatement(Tree tree) {
    StatementTree statementTree = null;
    switch (tree.kind()) {
      case IF_STATEMENT:
        statementTree = ((IfStatementTree) tree).thenStatement();
        break;
      case WHILE_STATEMENT:
        statementTree = ((WhileStatementTree) tree).statement();
        break;
      case FOR_STATEMENT:
        statementTree = ((ForStatementTree) tree).statement();
        break;
      case FOR_EACH_STATEMENT:
        statementTree = ((ForEachStatement) tree).statement();
        break;
      default:
        throw new IllegalStateException("If there is no statement, then exception.");
    }
    return statementTree;
  }
}
