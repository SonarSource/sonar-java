/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S4144")
public class MethodIdenticalImplementationsCheck extends IssuableSubscriptionVisitor {

  private static final String ISSUE_MSG = "Update this method so that its implementation is not identical to \"%s\" on line %d.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    List<MethodTree> methods = ((ClassTree) tree).members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(MethodIdenticalImplementationsCheck::isDuplicateCandidate)
      .collect(Collectors.toList());
    if (methods.size() <= 1) {
      return;
    }
    Set<MethodTree> reported = new HashSet<>();
    for (int i = 0; i < methods.size(); i++) {
      MethodTree method = methods.get(i);
      SyntaxToken methodIdentifier = method.simpleName().identifierToken();
      List<StatementTree> methodBody = method.block().body();
      methods.stream()
        .skip(i + 1L)
        // avoid reporting multiple times
        .filter(otherMethod -> !reported.contains(otherMethod))
        // skip overloads
        .filter(otherMethod -> !methodIdentifier.text().equals(otherMethod.simpleName().name()))
        .filter(otherMethod -> SyntacticEquivalence.areEquivalent(methodBody, otherMethod.block().body()))
        .forEach(otherMethod -> {
          reported.add(otherMethod);
          reportIssue(
            otherMethod.simpleName(),
            String.format(ISSUE_MSG, methodIdentifier.text(), methodIdentifier.line()),
            Collections.singletonList(new JavaFileScannerContext.Location("original implementation", methodIdentifier)),
            null);
        });
    }
  }

  private static boolean isDuplicateCandidate(MethodTree method) {
    BlockTree block = method.block();
    if (block == null) {
      return false;
    }
    List<StatementTree> statements = block.body();
    if (statements.isEmpty()) {
      return false;
    }
    if (statements.size() == 1) {
      StatementTree singleStatement = statements.get(0);
      return !singleStatement.is(Tree.Kind.THROW_STATEMENT)
        && !isTrivialReturn(singleStatement)
        && !isMethodInvocationWithoutParameter(singleStatement);
    }
    return true;
  }

  private static boolean isTrivialReturn(Tree tree) {
    if (!tree.is(Tree.Kind.RETURN_STATEMENT)) {
      return false;
    }
    ExpressionTree returnExpression = ((ReturnStatementTree) tree).expression();
    return returnExpression == null
      || returnExpression.is(Tree.Kind.BOOLEAN_LITERAL, Tree.Kind.NULL_LITERAL)
      || LiteralUtils.isEmptyString(returnExpression)
      || isThis(returnExpression)
      || isZeroOrOne(returnExpression)
      || isMethodInvocationWithoutParameter(returnExpression);
  }

  private static boolean isThis(Tree tree) {
    return tree.is(Tree.Kind.IDENTIFIER) && "this".equals(((IdentifierTree) tree).name());
  }

  private static boolean isZeroOrOne(ExpressionTree tree) {
    Long longValue = LiteralUtils.longLiteralValue(tree);
    return longValue != null && (longValue == -1L || longValue == 0L || longValue == 1L);
  }

  private static boolean isMethodInvocationWithoutParameter(Tree tree) {
    Tree expr = tree;
    if (expr.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      expr = ((ExpressionStatementTree) tree).expression();
    }
    if (!expr.is(Tree.Kind.METHOD_INVOCATION)) {
      return false;
    }
    MethodInvocationTree mit = (MethodInvocationTree) expr;
    return mit.methodSelect().is(Tree.Kind.IDENTIFIER) && mit.arguments().isEmpty();
  }
}
