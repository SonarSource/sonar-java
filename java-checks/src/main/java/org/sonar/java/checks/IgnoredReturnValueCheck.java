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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.MethodsHelper;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2201",
  name = "Return values should not be ignored when function calls don't have any side effects",
  priority = Priority.CRITICAL,
  tags = {Tag.BUG, Tag.CERT, Tag.MISRA})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("10min")
public class IgnoredReturnValueCheck extends SubscriptionBaseVisitor {

  private static final List<String> CHECKED_TYPES = ImmutableList.<String>builder()
      .add("java.lang.String")
      .add("java.lang.Boolean")
      .add("java.lang.Integer")
      .add("java.lang.Double")
      .add("java.lang.Float")
      .add("java.lang.Byte")
      .add("java.lang.Character")
      .add("java.lang.Short")
      .add("java.lang.StackTraceElement")
      .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.EXPRESSION_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionStatementTree est = (ExpressionStatementTree) tree;
    if (est.expression().is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) est.expression();
      Type methodType = mit.symbolType();
      if (!returnsVoid(methodType) && isCheckedType(mit)) {
        addIssue(tree, "The return value of \"" + methodName(mit) + "\" must be used.");
      }
    }
  }

  private static boolean isCheckedType(MethodInvocationTree mit) {
    Symbol owner = mit.symbol().owner();
    for (String type : CHECKED_TYPES) {
      if (owner.type().is(type)) {
        return true;
      }
    }
    return false;
  }

  private static boolean returnsVoid(Type methodType) {
    return methodType.isVoid() || methodType.isUnknown();
  }

  private static String methodName(MethodInvocationTree mit) {
    return MethodsHelper.methodName(mit).name();
  }
}
