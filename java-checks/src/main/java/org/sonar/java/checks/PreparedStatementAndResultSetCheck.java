/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.NameCriteria;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;

@Rule(
  key = "S2695",
  name = "\"PreparedStatement\" and \"ResultSet\" methods should be called with valid indices",
  tags = {"bug", "sql"},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("2min")
public class PreparedStatementAndResultSetCheck extends AbstractMethodDetection {

  private static final String INT = "int";
  private static final String JAVA_SQL_RESULTSET = "java.sql.ResultSet";

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodInvocationMatcher.create().typeDefinition("java.sql.PreparedStatement").name(NameCriteria.startsWith("set")).addParameter(INT).addParameter(TypeCriteria.anyType()),
      MethodInvocationMatcher.create().typeDefinition(JAVA_SQL_RESULTSET).name(NameCriteria.startsWith("get")).addParameter(INT),
      MethodInvocationMatcher.create().typeDefinition(JAVA_SQL_RESULTSET).name(NameCriteria.startsWith("get")).addParameter(INT).addParameter(TypeCriteria.anyType()));
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    Integer methodFirstArgumentAsInteger = LiteralUtils.intLiteralValue(mit.arguments().get(0));
    if (methodFirstArgumentAsInteger == null) {
      // nothing to say if first argument can not be evaluated
      return;
    }

    boolean isMethodFromJavaSqlResultSet = mit.symbol().owner().type().is(JAVA_SQL_RESULTSET);
    int methodFirstArgumentValue = methodFirstArgumentAsInteger.intValue();

    if (isMethodFromJavaSqlResultSet && methodFirstArgumentValue == 0) {
      addIssue(mit, "ResultSet indices start at 1.");
    } else if (!isMethodFromJavaSqlResultSet) {
      if (methodFirstArgumentValue == 0) {
        addIssue(mit, "PreparedStatement indices start at 1.");
      } else {
        Tree preparedStatementReference = getPreparedStatementReference(mit);
        Integer numberParameters = getNumberParametersFromPreparedStatement(preparedStatementReference);
        if (numberParameters != null && methodFirstArgumentValue > numberParameters.intValue()) {
          addIssue(mit, "This \"PreparedStatement\" " + (numberParameters == 0 ? "has no" : "only has " + numberParameters) + " parameters.");
        }
      }
    }
  }

  @CheckForNull
  private Tree getPreparedStatementReference(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
      if (expression.is(Kind.IDENTIFIER)) {
        Symbol referenceSymbol = ((IdentifierTree) expression).symbol();
        return referenceSymbol.declaration();
      }
    }
    return null;
  }

  @CheckForNull
  private Integer getNumberParametersFromPreparedStatement(@Nullable Tree tree) {
    if (tree != null && tree.is(Kind.VARIABLE)) {
      ExpressionTree initializer = ((VariableTree) tree).initializer();
      if (initializer != null && initializer.is(Kind.METHOD_INVOCATION)) {
        List<ExpressionTree> arguments = ((MethodInvocationTree) initializer).arguments();
        if (!arguments.isEmpty() && arguments.get(0).is(Tree.Kind.STRING_LITERAL)) {
          return StringUtils.countMatches(((LiteralTree) arguments.get(0)).value(), "?");
        }
      }
    }
    return null;
  }
}
