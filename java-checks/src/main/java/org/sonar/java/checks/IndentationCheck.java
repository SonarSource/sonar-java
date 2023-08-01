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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.LineUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.location.Position;
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
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;
import org.sonarsource.analyzer.commons.collections.ListUtils;

@DeprecatedRuleKey(ruleKey = "IndentationCheck", repositoryKey = "squid")
@Rule(key = "S1120")
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
      excludeIssueAtLine = LineUtils.startLine(tree.openBraceToken());
      expectedLevel = Position.startOf(tree.closeBraceToken()).columnOffset();
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
    int oldExpectedLevel = expectedLevel;
    adjustBlockForExceptionalParents(tree);
    checkIndentation(tree.body());
    super.visitBlock(tree);
    expectedLevel = oldExpectedLevel;
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
      excludeIssueAtLine = LineUtils.startLine(block.openBraceToken());
      int previousLevel = expectedLevel;
      expectedLevel = Position.startOf(block.closeBraceToken()).columnOffset();
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
    excludeIssueAtLine = LineUtils.startLine(tree.lastToken());
  }

  private void checkCaseGroup(CaseGroupTree tree) {
    List<CaseLabelTree> labels = tree.labels();
    if (labels.size() >= 2) {
      CaseLabelTree previousCaseLabelTree = labels.get(labels.size() - 2);
      excludeIssueAtLine = LineUtils.startLine(previousCaseLabelTree.lastToken());
    }
    List<StatementTree> body = tree.body();

    if (body.size() == 1 && body.get(0).is(Kind.BLOCK)) {
      checkCaseGroupSingleBlock(tree);
    } else {
      checkCaseGroupMixedStatements(tree);
    }
  }

  private void checkCaseGroupSingleBlock(CaseGroupTree tree) {
    List<StatementTree> body = tree.body();
    SyntaxToken separatorToken = ListUtils.getLast(tree.labels()).colonOrArrowToken();
    int nextOffsetInLine = Position.endOf(separatorToken).columnOffset() + 1;

    BlockTree block = (BlockTree) body.get(0);
    Position openBracePosition = Position.startOf(block.openBraceToken());
    Position separatorPosition = Position.endOf(separatorToken);
    if (openBracePosition.line() == separatorPosition.line()) {
      // `{` is on same line -> one additional indentation for inner block and `}` is optional
      checkIndentation(block.openBraceToken(), nextOffsetInLine);
      if (block.body().isEmpty()) {
        checkIndentationWithOptionalAllowed(block.closeBraceToken(), expectedLevel - indentationLevel);
      } else {
        boolean isAdditionalIndentation = checkIndentationWithOptionalAllowed(block.body().get(0), expectedLevel);
        checkIndentation(block.closeBraceToken(), isAdditionalIndentation ? expectedLevel : expectedLevel - indentationLevel);
      }
    } else {
      // `{` is on next line -> one additional indentation is optional, inner block and `}` must have same indentation
      boolean isAdditionalIndentation = checkIndentationWithOptionalAllowed(block.openBraceToken(), expectedLevel - indentationLevel);
      int x = isAdditionalIndentation ? expectedLevel : expectedLevel - indentationLevel;
      if (!block.body().isEmpty()) {
        checkIndentation(block.body().get(0), x + indentationLevel);
      }
      checkIndentation(block.closeBraceToken(), x);
    }
  }

  private void checkCaseGroupMixedStatements(CaseGroupTree tree) {
    List<StatementTree> body = tree.body();
    SyntaxToken separatorToken = ListUtils.getLast(tree.labels()).colonOrArrowToken();
    int nextOffsetInLine = Position.endOf(separatorToken).columnOffset() + 1;
    List<StatementTree> newBody = body;
    int bodySize = body.size();

    int oldExpectedLevel = expectedLevel;
    if (bodySize > 0 && body.get(0).is(Kind.BLOCK)) {
      expectedLevel -= indentationLevel;
      checkIndentation(body.get(0), nextOffsetInLine);
      newBody = body.subList(1, bodySize);
    }

    if (bodySize == 1 && "->".equals(separatorToken.text())) {
      checkSameOrNextLineIndentation(Position.startOf(separatorToken).line(), nextOffsetInLine, body.get(0));
    } else {
      checkIndentation(newBody);
    }

    expectedLevel = oldExpectedLevel;
  }

  private void checkSameOrNextLineIndentation(int curLine, int nextOffsetInLine, StatementTree statement) {
    checkIndentation(statement, Position.startOf(statement).line() == curLine ? nextOffsetInLine : expectedLevel);
  }

  private void adjustBlockForExceptionalParents(BlockTree tree) {
    if (Objects.requireNonNull(tree.parent()).is(Kind.CASE_GROUP)) {
      expectedLevel = tree.body().isEmpty() ?
        getIndentation(Position.startOf(tree.closeBraceToken())) + indentationLevel :
        getIndentation(Position.startOf(tree.body().get(0)));
    }
  }

  private void checkIndentation(List<? extends Tree> trees) {
    for (Tree tree : trees) {
      checkIndentation(tree, expectedLevel);
    }
  }

  private void checkIndentation(Tree tree, int expectedLevel) {
    Position treeStart = Position.startOf(tree);
    if (getIndentation(treeStart) != expectedLevel) {
      addIssue(tree, expectedLevel);
    }
    excludeIssueAtLine = LineUtils.startLine(tree.lastToken());
  }

  private boolean checkIndentationWithOptionalAllowed(Tree tree, int expectedLevel) {
    Position treeStart = Position.startOf(tree);
    int level = getIndentation(treeStart);
    boolean isAdditinalIndentation = level == expectedLevel + indentationLevel;

    if (level != expectedLevel && !isAdditinalIndentation) {
      addIssue(tree, expectedLevel, expectedLevel + indentationLevel);
    }
    excludeIssueAtLine = LineUtils.startLine(tree.lastToken());
    return isAdditinalIndentation;
  }

  void addIssue(Tree tree, Integer... expectedLevels) {
    Position treeStart = Position.startOf(tree);
    String messageAfter = Arrays.stream(expectedLevels).map(Object::toString).collect(Collectors.joining(" or "));
    if (!isExcluded(tree, treeStart.line())) {
      int level = getIndentation(treeStart);
      String message = "Make this line start after " + messageAfter + " spaces instead of " +
        level + " in order to indent the code consistently. (Indentation level is at " + indentationLevel + ".)";
      context.addIssue(((JavaTree) tree).getLine(), this, message);
      isBlockAlreadyReported = true;
    }
  }

  private int getIndentation(Position treeStart) {
    String line = fileLines.get(treeStart.lineOffset());
    int level = treeStart.columnOffset();
    int indentLength = Math.min(treeStart.columnOffset(), /* defensive programming */ line.length());
    for (int i = 0; i < indentLength; i++) {
      if (line.charAt(i) == '\t') {
        level += indentationLevel - 1;
      }
    }
    return level;
  }

  private boolean isExcluded(Tree node, int nodeLine) {
    return excludeIssueAtLine == nodeLine || isBlockAlreadyReported || node.is(Kind.ENUM_CONSTANT);
  }

}
