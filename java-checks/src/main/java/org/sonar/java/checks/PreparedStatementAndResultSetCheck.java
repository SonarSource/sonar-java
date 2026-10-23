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

import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ReassignmentFinder;
import org.sonar.java.utils.StringUtils;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.semantic.Type.Primitives;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.semantic.MethodMatchers.ANY;

@Rule(key = "S2695")
public class PreparedStatementAndResultSetCheck extends AbstractMethodDetection {

  private static final String INT = "int";
  private static final String JAVA_SQL_RESULTSET = "java.sql.ResultSet";
  private static final MethodMatchers PREPARE_STATEMENT = MethodMatchers.create()
    .ofTypes("java.sql.Connection").name(name -> name.startsWith("prepareStatement")).withAnyParameters().build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.or(
      MethodMatchers.create().ofTypes("java.sql.PreparedStatement").name(name -> name.startsWith("set")).addParametersMatcher(INT, ANY).build(),
      MethodMatchers.create().ofTypes(JAVA_SQL_RESULTSET).name(name -> name.startsWith("get")).addParametersMatcher(INT).addParametersMatcher(INT, ANY).build()
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree firstArgument = mit.arguments().get(0);
    Optional<Integer> firstArgumentAsConstant = firstArgument.asConstant(Integer.class);
    if (!firstArgumentAsConstant.isPresent()) {
      // nothing to say if first argument can not be evaluated
      return;
    }

    boolean isMethodFromJavaSqlResultSet = mit.methodSymbol().owner().type().is(JAVA_SQL_RESULTSET);
    int methodFirstArgumentValue = firstArgumentAsConstant.get();

    if (isMethodFromJavaSqlResultSet && methodFirstArgumentValue == 0) {
      reportIssue(firstArgument, "ResultSet indices start at 1.");
    } else if (!isMethodFromJavaSqlResultSet) {
      if (methodFirstArgumentValue == 0) {
        reportIssue(firstArgument, "PreparedStatement indices start at 1.");
      } else {
        ExpressionTree preparedStatementReference = getPreparedStatementReference(mit);
        Integer numberParameters = getPreparedStatementNumberOfParameters(preparedStatementReference);
        if (numberParameters != null && methodFirstArgumentValue > numberParameters) {
          reportIssue(firstArgument, "This \"PreparedStatement\" " + (numberParameters == 0 ? "has no" : ("only has " + numberParameters)) + " parameters.");
        }
      }
    }
  }

  @CheckForNull
  private static ExpressionTree getPreparedStatementReference(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        Symbol referenceSymbol = ((IdentifierTree) expression).symbol();
        return ReassignmentFinder.getClosestReassignmentOrDeclarationExpression(mit, referenceSymbol);
      }
    }
    return null;
  }

  @CheckForNull
  private static Integer getPreparedStatementNumberOfParameters(@Nullable ExpressionTree tree) {
    if (tree != null && tree.is(Tree.Kind.METHOD_INVOCATION)) {
      Arguments arguments = ((MethodInvocationTree) tree).arguments();
      if (!arguments.isEmpty() && PREPARE_STATEMENT.matches((MethodInvocationTree) tree)) {
        ExpressionTree firstArgument = arguments.get(0);
        return getNumberQuery(firstArgument, firstArgument);
      }
    }
    return null;
  }

  /**
   * Count the number of query parameters in the given expression. The method will reconstruct the string, if created in
   * multiples steps. During the process, we are looking for closest reassignment of a variable. We will look for any
   * reassignment before the first token of the argument "startingPointForReassignment". It is used to avoid infinite
   * recursion in case the variable is reassigned and read in the same statement.
   * Returns {@code null} as soon as any part of the query can not be resolved: an unresolved fragment may itself hold
   * parameter placeholders, so the total count is unknown rather than zero.
   */
  @CheckForNull
  private static Integer getNumberQuery(ExpressionTree expression, Tree startingPointForReassignment) {
    ExpressionTree expr = ExpressionUtils.skipParentheses(expression);
    Integer unresolvedBound = unresolvedPlaceholderBound(expr);
    if (unresolvedBound != null) {
      return unresolvedBound;
    }
    Integer constantCount = countQuery(expr);
    if (constantCount != null) {
      return constantCount;
    }
    if (expr.is(Tree.Kind.IDENTIFIER)) {
      return handleVariableUsedAsQuery((IdentifierTree) expr, startingPointForReassignment);
    }
    if (expr.is(Tree.Kind.PLUS)) {
      return handleStringConcatenation((BinaryExpressionTree) expr, startingPointForReassignment);
    }
    if (expr.is(Tree.Kind.CONDITIONAL_EXPRESSION)) {
      return handleConditionalExpression((ConditionalExpressionTree) expr, startingPointForReassignment);
    }
    return null;
  }

  @CheckForNull
  private static Integer handleVariableUsedAsQuery(IdentifierTree identifier, Tree startingPointForReassignment) {
    ExpressionTree lastAssignmentExpr = ReassignmentFinder.getClosestReassignmentOrDeclarationExpression(startingPointForReassignment, identifier.symbol());
    if (lastAssignmentExpr != null) {
      Tree lastAssignment = lastAssignmentExpr.parent();
      if (lastAssignment.is(Tree.Kind.PLUS_ASSIGNMENT)) {
        return sumOrNull(
          getNumberQuery(((AssignmentExpressionTree) lastAssignment).variable(), lastAssignment),
          getNumberQuery(lastAssignmentExpr, lastAssignment));
      }
      // If the current assignment contains the identifier, we have to update the starting point for reassignment to avoid infinite recursion.
      return getNumberQuery(lastAssignmentExpr, lastAssignment);
    }
    return null;
  }

  @CheckForNull
  private static Integer handleStringConcatenation(BinaryExpressionTree expr, Tree startingPointForReassignment) {
    return sumOrNull(
      getNumberQuery(expr.leftOperand(), startingPointForReassignment),
      getNumberQuery(expr.rightOperand(), startingPointForReassignment));
  }

  /**
   * A conditional expression contributes as many placeholders as its most parameterized branch: reporting against the
   * smaller branch would flag indices which are valid whenever the other branch is taken.
   */
  @CheckForNull
  private static Integer handleConditionalExpression(ConditionalExpressionTree expr, Tree startingPointForReassignment) {
    Integer trueBranch = getNumberQuery(expr.trueExpression(), startingPointForReassignment);
    Integer falseBranch = getNumberQuery(expr.falseExpression(), startingPointForReassignment);
    if (trueBranch == null || falseBranch == null) {
      return null;
    }
    return Math.max(trueBranch, falseBranch);
  }

  /**
   * Upper bound on the placeholders an operand can contribute when its value is unknown: none for a number or a
   * boolean, whatever their runtime value, and at most one for a character, since '?' is a valid one. Returns
   * {@code null} for anything that can hold an arbitrary number of them.
   */
  @CheckForNull
  private static Integer unresolvedPlaceholderBound(ExpressionTree expression) {
    Type type = expression.symbolType();
    Type primitiveType = type.isPrimitiveWrapper() ? type.primitiveType() : type;
    if (primitiveType == null || !primitiveType.isPrimitive()) {
      return null;
    }
    return primitiveType.isPrimitive(Primitives.CHAR) ? 1 : 0;
  }

  @CheckForNull
  private static Integer sumOrNull(@Nullable Integer left, @Nullable Integer right) {
    if (left == null || right == null) {
      return null;
    }
    return left + right;
  }

  @CheckForNull
  private static Integer countQuery(ExpressionTree expression) {
    return expression.asConstant(String.class)
      .map(query -> StringUtils.countMatches(query, "?"))
      .orElse(null);
  }
}
