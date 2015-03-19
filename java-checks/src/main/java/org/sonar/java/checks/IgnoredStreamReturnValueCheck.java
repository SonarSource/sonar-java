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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.resolve.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2674",
  name = "The value returned from a stream read should be checked",
  tags = {"bug"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("15min")
public class IgnoredStreamReturnValueCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.EXPRESSION_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    ExpressionTree statement = ((ExpressionStatementTree) tree).expression();
    if (statement.is(Kind.METHOD_INVOCATION)) {
      Symbol symbol = ((MethodInvocationTree) statement).symbol();
      if (symbol.isMethodSymbol()) {
        checkMethod(statement, (MethodSymbol) symbol);
      }
    }
  }

  private void checkMethod(ExpressionTree statement, MethodSymbol method) {
    if (method.owner().type().isSubtypeOf("java.io.InputStream") && (isRead(method) || isSkip(method))) {
      addIssue(statement, "Check the return value of the \"" + method.name() + "\" call to see how many bytes were read.");
    }
  }

  private boolean isSkip(MethodSymbol method) {
    return isMethod(method, "skip", "long", "long");
  }

  private boolean isRead(MethodSymbol method) {
    return isMethod(method, "read", "int", "byte[]");
  }

  private boolean isMethod(MethodSymbol method, String name, String returnType, String parameterType) {
    List<Type> parameters = method.parameterTypes();
    return name.equals(method.name())
      && method.getReturnType().type().is(returnType)
      && parameters.size() == 1
      && parameters.get(0).is(parameterType);
  }
}
