/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2197",
  name = "Modulus results should not be checked for direct equality",
  tags = {"bug"},
  priority = Priority.CRITICAL)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.DATA_RELIABILITY)
@SqaleConstantRemediation("5min")
public class ModulusEqualityCheck extends SubscriptionBaseVisitor {

  private List<Symbol> methodParams = Lists.newArrayList();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);
    methodParams.clear();
  }

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.EQUAL_TO, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.EQUAL_TO)) {
      BinaryExpressionTree equality = (BinaryExpressionTree) tree;
      checkModulusAndIntLiteral(equality.leftOperand(), equality.rightOperand());
      checkModulusAndIntLiteral(equality.rightOperand(), equality.leftOperand());
    } else {
      MethodTree methodTree = (MethodTree) tree;
      for (VariableTree variableTree : methodTree.parameters()) {
        methodParams.add(variableTree.symbol());
      }
    }
  }

  private void checkModulusAndIntLiteral(ExpressionTree operand1, ExpressionTree operand2) {
    if (operand1.is(Tree.Kind.REMAINDER)) {
      BinaryExpressionTree modulusExp = (BinaryExpressionTree) operand1;
      Integer intValue = LiteralUtils.intLiteralValue(operand2);
      ExpressionTree leftOperand = modulusExp.leftOperand();
      ExpressionTree rightOperand = modulusExp.rightOperand();
      boolean usesMethodParam = isMethodParameter(leftOperand) || isMethodParameter(rightOperand);
      boolean usesSize = isSizeAccessor(leftOperand) || isSizeAccessor(rightOperand);
      if (intValue != null && intValue != 0 && usesMethodParam && !usesSize) {
        String sign = intValue > 0 ? "positive" : "negative";
        addIssue(operand1, "The results of this modulus operation may not be " + sign + ".");
      }
    }
  }

  private boolean isMethodParameter(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) expressionTree;
      Symbol symbol = identifier.symbol();
      return methodParams.contains(symbol);
    } else if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) expressionTree;
      return isMethodParameter(memberSelectExpressionTree.expression());
    } else if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) expressionTree;
      return isMethodParameter(methodInvocationTree.methodSelect());
    }
    return false;
  }

  private static boolean isSizeAccessor(ExpressionTree expressionTree) {
    if (expressionTree.is(Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) expressionTree;
      Type type = memberSelectExpressionTree.expression().symbolType();
      String memberName = memberSelectExpressionTree.identifier().name();
      return isCollectionSize(type, memberName) || isStringLength(type, memberName) || isArrayLength(type, memberName);
    } else if (expressionTree.is(Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) expressionTree;
      return isSizeAccessor(methodInvocationTree.methodSelect());
    }
    return false;
  }

  private static boolean isArrayLength(Type type, String memberName) {
    return type.isArray() && "length".equals(memberName);
  }

  private static boolean isStringLength(Type type, String memberName) {
    return type.is("java.lang.String") && "length".equals(memberName);
  }

  private static boolean isCollectionSize(Type type, String memberName) {
    return type.isSubtypeOf("java.util.Collection") && "size".equals(memberName);
  }
}
