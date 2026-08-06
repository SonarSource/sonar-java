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
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9142")
public class CompilationOrPreparationInLoopCheck extends IssuableSubscriptionVisitor {

  private static final Set<Tree.Kind> LOOP_KINDS = EnumSet.of(
    Tree.Kind.FOR_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT,
    Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT
  );

  private static final MethodMatchers MATCHERS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.util.regex.Pattern")
      .names("compile")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("java.lang.String")
      .names("matches", "replaceAll", "replaceFirst", "split")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofSubTypes("java.sql.Connection")
      .names("prepareStatement")
      .withAnyParameters()
      .build()
  );

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
    ExpressionTree patternArg = mit.arguments().get(0);
    if (isLoopInvariant(patternArg, loop)) {
      reportIssue(mit, String.format(
        "Move this \"%s\" call outside the loop.", ExpressionUtils.methodName(mit).name()));
    }
  }

  private static boolean isLoopInvariant(ExpressionTree arg, Tree loop) {
    if (arg.is(Tree.Kind.IDENTIFIER)) {
      var collector = new DeclaredOrAssignedLocalsCollector();
      loop.accept(collector);
      return !collector.names.contains(((IdentifierTree) arg).name());
    }
    return ExpressionUtils.resolveAsConstant(arg) != null;
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
      switch (tree.kind()) {
        case POSTFIX_INCREMENT, POSTFIX_DECREMENT, PREFIX_INCREMENT, PREFIX_DECREMENT -> {
          if (tree.expression().is(Tree.Kind.IDENTIFIER)) {
            names.add(((IdentifierTree) tree.expression()).name());
          }
        }
        default -> {
          // not a mutation
        }
      }
    }

    @Override
    public void visitForEachStatement(ForEachStatement tree) {
      super.visitForEachStatement(tree);
      names.add(tree.variable().simpleName().name());
    }
  }
}
