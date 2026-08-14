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
import org.apache.commons.lang3.StringEscapeUtils;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.TreeHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
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
public class CompilationOrPreparationInLoopCheck extends IssuableSubscriptionVisitor {

  private static final String STRING_REGEX_MESSAGE =
    "Extract this regular expression to a Pattern compiled outside the loop.";

  private static final Set<Tree.Kind> LOOP_KINDS = EnumSet.of(
    Tree.Kind.FOR_STATEMENT, Tree.Kind.FOR_EACH_STATEMENT,
    Tree.Kind.WHILE_STATEMENT, Tree.Kind.DO_STATEMENT
  );

  private static final MethodMatchers PATTERN_COMPILE = MethodMatchers.create()
    .ofTypes("java.util.regex.Pattern")
    .names("compile")
    .withAnyParameters()
    .build();

  private static final MethodMatchers STRING_REGEX_METHODS = MethodMatchers.create()
    .ofTypes("java.lang.String")
    .names("matches", "replaceAll", "replaceFirst", "split")
    .withAnyParameters()
    .build();

  private static final MethodMatchers SPLIT = MethodMatchers.create()
    .ofTypes("java.lang.String")
    .names("split")
    .withAnyParameters()
    .build();

  private static final MethodMatchers MATCHERS = MethodMatchers.or(
    PATTERN_COMPILE,
    STRING_REGEX_METHODS,
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
    if (loop == null || isInForInitializer(mit, loop)) {
      return;
    }
    if (SPLIT.matches(mit) && isSplitFastPath(mit.arguments().get(0))) {
      return;
    }
    List<ExpressionTree> argsToCheck = PATTERN_COMPILE.matches(mit) ? mit.arguments() : List.of(mit.arguments().get(0));
    if (argsToCheck.stream().allMatch(arg -> isLoopInvariant(arg, loop))) {
      reportIssue(mit, message(mit));
    }
  }

  private static String message(MethodInvocationTree mit) {
    if (STRING_REGEX_METHODS.matches(mit)) {
      return STRING_REGEX_MESSAGE;
    }
    return String.format("Move this \"%s\" call outside the loop.", ExpressionUtils.methodName(mit).name());
  }

  private static boolean isInForInitializer(Tree tree, Tree loop) {
    if (!loop.is(Tree.Kind.FOR_STATEMENT)) {
      return false;
    }
    Tree initializer = ((ForStatementTree) loop).initializer();
    for (Tree current = tree; current != null && current != loop; current = current.parent()) {
      if (current == initializer) {
        return true;
      }
    }
    return false;
  }

  private static boolean isSplitFastPath(ExpressionTree arg) {
    return ExpressionUtils.skipParentheses(arg).asConstant(String.class)
      .filter(CompilationOrPreparationInLoopCheck::exceptionSplitMethod)
      .isPresent();
  }

  /**
   * Copy of {@link java.lang.String#split(String, int)} fast-path, matching {@link RegexPatternsNeedlesslyCheck}.
   */
  private static boolean exceptionSplitMethod(String argValue) {
    String regex = StringEscapeUtils.unescapeJava(argValue);
    char ch;
    return ((regex.length() == 1 && ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1) ||
      (regex.length() == 2 &&
        regex.charAt(0) == '\\' &&
        (((ch = regex.charAt(1)) - '0') | ('9' - ch)) < 0 &&
        ((ch - 'a') | ('z' - ch)) < 0 &&
        ((ch - 'A') | ('Z' - ch)) < 0)) &&
      (ch < Character.MIN_HIGH_SURROGATE || ch > Character.MAX_LOW_SURROGATE);
  }

  private static boolean isLoopInvariant(ExpressionTree arg, Tree loop) {
    ExpressionTree expression = ExpressionUtils.skipParentheses(arg);
    if (expression.is(Tree.Kind.IDENTIFIER)) {
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
