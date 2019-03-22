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
package org.sonar.java.checks;

import com.google.common.collect.Iterables;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.RspecKey;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.List;

@Rule(key = "IndentationCheck")
@RspecKey("S1120")
public class IndentationCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final int DEFAULT_INDENTATION_LEVEL = 2;

  @RuleProperty(
    key = "indentationLevel",
    description = "Number of white-spaces of an indent.",
    defaultValue = "" + DEFAULT_INDENTATION_LEVEL)
  public int indentationLevel = DEFAULT_INDENTATION_LEVEL;

  private int expectedLevel;
  private boolean isBlockAlreadyReported;
  private int excludeIssueAtLine;
  private JavaFileScannerContext context;
  private List<String> fileLines;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    expectedLevel = 0;
    isBlockAlreadyReported = false;
    excludeIssueAtLine = 0;
    this.context = context;
    fileLines = context.getFileLines();
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    // Exclude anonymous classes
    boolean isAnonymous = tree.simpleName() == null;
    if (!isAnonymous) {
      checkIndentation(Collections.singletonList(tree));
    }
    int previousLevel = expectedLevel;
    if (isAnonymous) {
      excludeIssueAtLine = tree.openBraceToken().line();
      expectedLevel = tree.closeBraceToken().column();
    }
    newBlock();
    checkIndentation(tree.members());
    super.visitClass(tree);
    leaveNode(tree);
    expectedLevel = previousLevel;
  }

  @Override
  public void visitBlock(BlockTree tree) {
    newBlock();
    adjustBlockForExceptionalParents(tree.parent());
    checkIndentation(tree.body());
    super.visitBlock(tree);
    restoreBlockForExceptionalParents(tree.parent());
    leaveNode(tree);
  }

  @Override
  public void visitSwitchStatement(SwitchStatementTree tree) {
    newBlock();
    scan(tree.expression());
    for (CaseGroupTree caseGroupTree : tree.cases()) {
      newBlock();
      checkCaseGroup(caseGroupTree);
      scan(caseGroupTree);
      leaveNode(caseGroupTree);
    }
    leaveNode(tree);
  }

  @Override
  public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
    // doesn't scan lambda parameters because there's no indentation check on types and identifiers
    Tree body = lambdaExpressionTree.body();
    if (body.is(Kind.BLOCK)) {
      BlockTree block = (BlockTree) body;
      excludeIssueAtLine = block.openBraceToken().line();
      int previousLevel = expectedLevel;
      expectedLevel = block.closeBraceToken().column();
      scan(block);
      expectedLevel = previousLevel;
    } else {
      scan(body);
    }
  }

  private void newBlock() {
    expectedLevel += indentationLevel;
    isBlockAlreadyReported = false;
  }

  private void leaveNode(Tree tree) {
    expectedLevel -= indentationLevel;
    isBlockAlreadyReported = false;
    excludeIssueAtLine = tree.lastToken().line();
  }

  private void checkCaseGroup(CaseGroupTree tree) {
    List<CaseLabelTree> labels = tree.labels();
    if (labels.size() >= 2) {
      CaseLabelTree previousCaseLabelTree = labels.get(labels.size() - 2);
      excludeIssueAtLine = previousCaseLabelTree.lastToken().line();
    }
    List<StatementTree> body = tree.body();
    List<StatementTree> newBody = body;
    int bodySize = body.size();
    if (bodySize > 0 && body.get(0).is(Kind.BLOCK)) {
      expectedLevel -= indentationLevel;
      checkIndentation(body.get(0), Iterables.getLast(labels).colonOrArrowToken().column() + 2);
      newBody = body.subList(1, bodySize);
    }
    checkIndentation(newBody);
    if (bodySize > 0 && body.get(0).is(Kind.BLOCK)) {
      expectedLevel += indentationLevel;
    }
  }

  private void adjustBlockForExceptionalParents(Tree parent) {
    if (parent.is(Kind.CASE_GROUP)) {
      expectedLevel -= indentationLevel;
    }
  }

  private void restoreBlockForExceptionalParents(Tree parent) {
    if (parent.is(Kind.CASE_GROUP)) {
      expectedLevel += indentationLevel;
    }
  }

  private void checkIndentation(List<? extends Tree> trees) {
    for (Tree tree : trees) {
      checkIndentation(tree, expectedLevel);
    }
  }

  private void checkIndentation(Tree tree, int expectedLevel) {
    SyntaxToken firstSyntaxToken = tree.firstToken();
    String line = fileLines.get(firstSyntaxToken.line() - 1);
    int level = firstSyntaxToken.column();
    for (int i = 0; i < firstSyntaxToken.column(); i++) {
      if (line.charAt(i) == '\t') {
        level += indentationLevel - 1;
      }
    }
    if (level != expectedLevel && !isExcluded(tree, firstSyntaxToken.line())) {
      context.addIssue(((JavaTree) tree).getLine(), this, "Make this line start after "+expectedLevel+" spaces to indent the code consistently.");
      isBlockAlreadyReported = true;
    }
    excludeIssueAtLine = tree.lastToken().line();
  }

  private boolean isExcluded(Tree node, int nodeLine) {
    return excludeIssueAtLine == nodeLine || isBlockAlreadyReported || node.is(Kind.ENUM_CONSTANT);
  }

}
