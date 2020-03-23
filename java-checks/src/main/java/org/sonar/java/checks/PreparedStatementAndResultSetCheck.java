/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ReassignmentFinder;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
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
    Integer methodFirstArgumentAsInteger = LiteralUtils.intLiteralValue(firstArgument);
    if (methodFirstArgumentAsInteger == null) {
      // nothing to say if first argument can not be evaluated
      return;
    }

    boolean isMethodFromJavaSqlResultSet = mit.symbol().owner().type().is(JAVA_SQL_RESULTSET);
    int methodFirstArgumentValue = methodFirstArgumentAsInteger.intValue();

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
        return getNumberQuery(arguments.get(0));
      }
    }
    return null;
  }

  @CheckForNull
  private static Integer getNumberQuery(ExpressionTree expression) {
    ExpressionTree expr = ExpressionUtils.skipParentheses(expression);
    if (expr.is(Tree.Kind.IDENTIFIER)) {
      return handleVariableUsedAsQuery((IdentifierTree) expr);
    } else if (expr.is(Tree.Kind.PLUS)) {
      return handleStringConcatenation((BinaryExpressionTree) expr);
    }
    return countQuery(expr);
  }

  private static Integer handleVariableUsedAsQuery(IdentifierTree identifier) {
    ExpressionTree lastAssignmentExpr = ReassignmentFinder.getClosestReassignmentOrDeclarationExpression(identifier, identifier.symbol());
    if (lastAssignmentExpr != null) {
      Tree lastAssignment = lastAssignmentExpr.parent();
      if (lastAssignment.is(Tree.Kind.PLUS_ASSIGNMENT)) {
        return zeroIfNull(getNumberQuery(lastAssignmentExpr)) + zeroIfNull(getNumberQuery(((AssignmentExpressionTree) lastAssignment).variable()));
      }
      if (!isPartOfExpression(identifier, lastAssignmentExpr)) {
        return getNumberQuery(lastAssignmentExpr);
      }
    }
    return null;
  }

  private static boolean isPartOfExpression(IdentifierTree identifier, ExpressionTree lastAssignment) {
    Tree parent = identifier;
    do {
      parent = parent.parent();
    } while (parent != null && !parent.equals(lastAssignment));
    return parent != null;
  }

  private static Integer handleStringConcatenation(BinaryExpressionTree expr) {
    Integer left = getNumberQuery(expr.leftOperand());
    Integer right = getNumberQuery(expr.rightOperand());
    return (left == null && right == null) ? null : (zeroIfNull(left) + zeroIfNull(right));
  }

  private static int zeroIfNull(@Nullable Integer intValue) {
    return intValue == null ? 0 : intValue;
  }

  @CheckForNull
  private static Integer countQuery(ExpressionTree expression) {
    return expression.is(Tree.Kind.STRING_LITERAL) ? StringUtils.countMatches(((LiteralTree) expression).value(), "?") : null;
  }
}
