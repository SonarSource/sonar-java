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
import com.google.common.collect.Lists;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.VariableSymbol;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

@Rule(
  key = "S2197",
  priority = Priority.CRITICAL)
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
        VariableSymbol symbol = ((VariableTreeImpl) variableTree).getSymbol();
        methodParams.add(symbol);
      }
    }
  }

  private void checkModulusAndIntLiteral(ExpressionTree operand1, ExpressionTree operand2) {
    if (operand1.is(Tree.Kind.REMAINDER)) {
      BinaryExpressionTree modulusExp = (BinaryExpressionTree) operand1;
      Integer intValue = LiteralUtils.intLiteralValue(operand2);
      boolean usesMethodParam = isMethodParameter(modulusExp.leftOperand()) || isMethodParameter(modulusExp.rightOperand());
      if (intValue != null && intValue != 0 && usesMethodParam) {
        String sign = intValue > 0 ? "positive" : "negative";
        addIssue(operand1, "The results of this modulus operation may not be " + sign + ".");
      }
    }
  }

  private boolean isMethodParameter(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifier = (IdentifierTree) expressionTree;
      Symbol symbol = getSemanticModel().getReference(identifier);
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

}
