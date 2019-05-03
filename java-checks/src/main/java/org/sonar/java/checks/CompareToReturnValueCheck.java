/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2167")
public class CompareToReturnValueCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (hasSemantic() && isCompareToDeclaration(methodTree)) {
      methodTree.accept(new ReturnStatementVisitor());
    }
  }

  private static boolean isCompareToDeclaration(MethodTree tree) {
    return isNamedCompareTo(tree) && hasOneNonPrimitiveParameter(tree) && returnsInt(tree);
  }

  private static boolean isNamedCompareTo(MethodTree tree) {
    return "compareTo".equals(tree.simpleName().name());
  }

  private static boolean hasOneNonPrimitiveParameter(MethodTree methodTree) {
    List<VariableTree> parameters = methodTree.parameters();
    return parameters.size() == 1 && !parameters.get(0).type().symbolType().isPrimitive();
  }

  private static boolean returnsInt(MethodTree methodTree) {
    TypeTree typeTree = methodTree.returnType();
    return typeTree != null && typeTree.symbolType().isPrimitive(Type.Primitives.INT);
  }

  private class ReturnStatementVisitor extends BaseTreeVisitor {

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      if (returnsIntegerMinValue(tree.expression())) {
        reportIssue(tree.expression(), "Simply return -1");
      }
    }

    private boolean returnsIntegerMinValue(ExpressionTree expressionTree) {
      if (expressionTree.is(Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) expressionTree;
        boolean isInteger = memberSelect.expression().symbolType().is("java.lang.Integer");
        boolean isMinValue = "MIN_VALUE".equals(memberSelect.identifier().name());
        return isInteger && isMinValue;
      }
      return false;
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Do not visit inner classes as methods of inner classes will be visited by main visitor
    }
  }
}
