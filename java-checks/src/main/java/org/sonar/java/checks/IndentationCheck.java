/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.RspecKey;
import org.sonar.java.model.JavaTree;
import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
import org.sonar.java.syntaxtoken.LastSyntaxTokenFinder;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.CaseLabelTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

@Rule(key = "IndentationCheck")
@RspecKey("S1120")
public class IndentationCheck extends IssuableSubscriptionVisitor {

  private static final List<Kind> BLOCK_TYPES = ImmutableList.of(
    Kind.CLASS,
    Kind.INTERFACE,
    Kind.ENUM,
    Kind.ANNOTATION_TYPE,
    Kind.CLASS,
    Kind.BLOCK,
    Kind.STATIC_INITIALIZER,
    Kind.INITIALIZER,
    Kind.SWITCH_STATEMENT,
    Kind.CASE_GROUP,
    Kind.METHOD_INVOCATION,
    Kind.LAMBDA_EXPRESSION
  );

  private static final int DEFAULT_INDENTATION_LEVEL = 2;

  @RuleProperty(
    key = "indentationLevel",
    description = "Number of white-spaces of an indent. If this property is not set, we just check that the code is indented.",
    defaultValue = "" + DEFAULT_INDENTATION_LEVEL)
  public int indentationLevel = DEFAULT_INDENTATION_LEVEL;

  private int expectedLevel;
  private boolean isBlockAlreadyReported;
  private int lastCheckedLine;
  private Deque<Boolean> isInAnonymousClass = new LinkedList<>();
  private Deque<SyntaxToken> methodInvocationFirstToken = new LinkedList<>();
  private Deque<SyntaxToken> lambdaPreviousToken = new LinkedList<>();

  @Override
  public List<Kind> nodesToVisit() {
    return BLOCK_TYPES;
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    expectedLevel = 0;
    isBlockAlreadyReported = false;
    lastCheckedLine = 0;
    isInAnonymousClass.clear();
    methodInvocationFirstToken.clear();
    lambdaPreviousToken.clear();
    super.scanFile(context);
  }

  @Override
  public void visitNode(Tree tree) {
    if (isClassTree(tree)) {
      ClassTree classTree = (ClassTree) tree;
      // Exclude anonymous classes
      isInAnonymousClass.push(classTree.simpleName() == null);
      if (!isInAnonymousClass.peek()) {
        checkIndentation(Collections.singletonList(classTree));
      }
    } else if (tree.is(Kind.METHOD_INVOCATION)) {
      adjustMethodInvocation((MethodInvocationTree) tree);
      return;
    } else if (tree.is(Kind.LAMBDA_EXPRESSION)) {
      adjustLambdaExpression((LambdaExpressionTree) tree);
      return;
    }

    expectedLevel += indentationLevel;
    isBlockAlreadyReported = false;

    switch (tree.kind()) {
      case CLASS:
      case ENUM:
      case INTERFACE:
      case ANNOTATION_TYPE:
        checkClass((ClassTree) tree);
        break;
      case CASE_GROUP:
        checkCaseGroup((CaseGroupTree) tree);
        break;
      case BLOCK:
        checkBlock((BlockTree) tree);
        break;
      default:
        break;
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Kind.METHOD_INVOCATION)) {
      restoreMethodInvocation((MethodInvocationTree) tree);
      return;
    } else if (tree.is(Kind.BLOCK)) {
      restoreBlockForExceptionalParents(tree.parent());
    } else if (tree.is(Kind.LAMBDA_EXPRESSION)) {
      restoreLambdaExpression((LambdaExpressionTree) tree);
      return;
    }
    expectedLevel -= indentationLevel;
    isBlockAlreadyReported = false;
    lastCheckedLine = LastSyntaxTokenFinder.lastSyntaxToken(tree).line();
    if (isClassTree(tree)) {
      isInAnonymousClass.pop();
    }
  }

  private void adjustMethodInvocation(MethodInvocationTree tree) {
    SyntaxToken firstToken = FirstSyntaxTokenFinder.firstSyntaxToken(tree);
    methodInvocationFirstToken.push(firstToken);
    int parenthesisLine = tree.arguments().openParenToken().line();
    if (firstToken.line() != parenthesisLine) {
      expectedLevel += indentationLevel;
    }
  }

  private void restoreMethodInvocation(MethodInvocationTree tree) {
    int startLine = methodInvocationFirstToken.pop().line();
    int parenthesisLine = tree.arguments().openParenToken().line();
    if (startLine != parenthesisLine) {
      expectedLevel -= indentationLevel;
    }
  }

  private void adjustLambdaExpression(LambdaExpressionTree lambda) {
    SyntaxToken previousToken = getPreviousToken(lambda);
    lambdaPreviousToken.push(previousToken);
    int lambdaFirstTokenLine = FirstSyntaxTokenFinder.firstSyntaxToken(lambda).line();
    if (previousToken.line() != lambdaFirstTokenLine) {
      expectedLevel += indentationLevel;
    }
    Tree body = lambda.body();
    if (partOfChainedMethodInvocations(lambda) && body.is(Kind.BLOCK)) {
      // compensates block and method invocation
      expectedLevel -= indentationLevel;
    }
  }

  private static boolean partOfChainedMethodInvocations(Tree tree) {
    Tree parent = tree.parent();
    if (parent.is(Kind.ARGUMENTS, Kind.METHOD_INVOCATION)) {
      return partOfChainedMethodInvocations(parent);
    }
    return parent.is(Kind.MEMBER_SELECT);
  }

  private static SyntaxToken getPreviousToken(Tree tree) {
    Tree previous = null;
    for (Tree children : ((JavaTree) tree.parent()).getChildren()) {
      if (children.equals(tree)) {
        break;
      }
      previous = children;
    }
    if(previous == null) {
      return getPreviousToken(tree.parent());
    }
    return LastSyntaxTokenFinder.lastSyntaxToken(previous);
  }

  private void restoreLambdaExpression(LambdaExpressionTree lambda) {
    int previousTokenLine = lambdaPreviousToken.pop().line();
    int lambdaFirstTokenLine = FirstSyntaxTokenFinder.firstSyntaxToken(lambda).line();
    if (previousTokenLine != lambdaFirstTokenLine) {
      expectedLevel -= indentationLevel;
    }
    if (partOfChainedMethodInvocations(lambda) && lambda.body().is(Kind.BLOCK)) {
      // compensates block and method invocation
      expectedLevel += indentationLevel;
    }
  }

  private void checkClass(ClassTree classTree) {
    // Exclude anonymous classes
    if (classTree.simpleName() != null) {
      checkIndentation(classTree.members());
    }
  }

  private void checkBlock(BlockTree blockTree) {
    adjustBlockForExceptionalParents(blockTree.parent());
    checkIndentation(blockTree.body());
  }

  private void checkCaseGroup(CaseGroupTree tree) {
    List<CaseLabelTree> labels = tree.labels();
    if (labels.size() >= 2) {
      CaseLabelTree previousCaseLabelTree = labels.get(labels.size() - 2);
      lastCheckedLine = LastSyntaxTokenFinder.lastSyntaxToken(previousCaseLabelTree).line();
    }
    List<StatementTree> body = tree.body();
    List<StatementTree> newBody = body;
    int bodySize = body.size();
    if (bodySize > 0 && body.get(0).is(Kind.BLOCK)) {
      expectedLevel -= indentationLevel;
      checkIndentation(body.get(0), Iterables.getLast(labels).colonToken().column() + 2);
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
    SyntaxToken firstSyntaxToken = FirstSyntaxTokenFinder.firstSyntaxToken(tree);
    if (firstSyntaxToken.column() != expectedLevel && !isExcluded(tree, firstSyntaxToken.line())) {
      addIssue(((JavaTree) tree).getLine(), "Make this line start at column " + (expectedLevel + 1) + ".");
      isBlockAlreadyReported = true;
    }
    lastCheckedLine = LastSyntaxTokenFinder.lastSyntaxToken(tree).line();
  }

  private boolean isExcluded(Tree node, int nodeLine) {
    return node.is(Kind.ENUM_CONSTANT) || isBlockAlreadyReported || lastCheckedLine == nodeLine || isInAnonymousClass.peek();
  }

  private static boolean isClassTree(Tree tree) {
    return tree.is(Kind.CLASS, Kind.ENUM, Kind.INTERFACE, Kind.ANNOTATION_TYPE);
  }

}
