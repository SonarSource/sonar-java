/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.TreeHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9142")
public class PreparedStatementInsideLoopCheck extends IssuableSubscriptionVisitor {

  private static final Set<Tree.Kind> LOOP_KINDS = EnumSet.of(
    Tree.Kind.FOR_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT,
    Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT
  );

  private static final MethodMatchers MATCHERS = MethodMatchers.create()
    .ofSubTypes("java.sql.Connection")
    .names("prepareStatement")
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (!MATCHERS.matches(mit) || mit.arguments().isEmpty()) {
      return;
    }
    Tree loop = TreeHelper.findClosestParentOfKind(mit, LOOP_KINDS);
    if (loop == null) {
      return;
    }
    if (isExecutedOncePerLoop(mit, loop)) {
      loop = TreeHelper.findClosestParentOfKind(loop.parent(), LOOP_KINDS);
      if (loop == null) {
        return;
      }
    }
    if (isLoopInvariant(mit.arguments().get(0), loop)) {
      reportIssue(mit, message(mit));
    }
  }

  private static String message(MethodInvocationTree mit) {
    return String.format("Move this \"%s\" call outside the loop.", ExpressionUtils.methodName(mit).name());
  }

  private static boolean isExecutedOncePerLoop(Tree tree, Tree loop) {
    Tree anchor = null;
    if (loop instanceof ForStatementTree forStatementTree) {
      anchor = forStatementTree.initializer();
    } else if (loop instanceof ForEachStatement forEachStatement) {
      anchor = forEachStatement.expression();
    }
    if (anchor == null) {
      return false;
    }
    for (Tree current = tree; current != null && current != loop; current = current.parent()) {
      if (current == anchor) {
        return true;
      }
    }
    return false;
  }

  private static boolean isLoopInvariant(ExpressionTree arg, Tree loop) {
    ExpressionTree expression = ExpressionUtils.skipParentheses(arg);
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol();
      if (!symbol.isVariableSymbol()) {
        return false;
      }
      if (symbol.owner().isTypeSymbol()) {
        return symbol.isFinal();
      }
      var collector = new DeclaredOrAssignedLocalsCollector();
      loop.accept(collector);
      return !collector.names.contains(((IdentifierTree) expression).name());
    }
    return ExpressionUtils.resolveAsConstant(expression) != null;
  }

  private static class DeclaredOrAssignedLocalsCollector extends BaseTreeVisitor {

    final Set<String> names = new HashSet<>();

    @Override
    public void visitVariable(VariableTree tree) {
      super.visitVariable(tree);
      names.add(tree.simpleName().name());
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      super.visitAssignmentExpression(tree);
      if (tree.variable().is(Tree.Kind.IDENTIFIER)) {
        names.add(((IdentifierTree) tree.variable()).name());
      }
    }

    @Override
    public void visitUnaryExpression(UnaryExpressionTree tree) {
      super.visitUnaryExpression(tree);
      if (isIncrementOrDecrement(tree.kind()) && tree.expression() instanceof IdentifierTree identifierTree) {
        names.add(identifierTree.name());
      }
    }

    private static boolean isIncrementOrDecrement(Tree.Kind kind) {
      return switch (kind) {
        case POSTFIX_INCREMENT, POSTFIX_DECREMENT, PREFIX_INCREMENT, PREFIX_DECREMENT -> true;
        default -> false;
      };
    }

    @Override
    public void visitForEachStatement(ForEachStatement tree) {
      super.visitForEachStatement(tree);
      names.add(tree.variable().simpleName().name());
    }
  }
}
