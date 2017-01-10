/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S2162")
public class SymmetricEqualsCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      MethodTreeImpl methodTree = (MethodTreeImpl) tree;
      if (methodTree.isEqualsMethod() && methodTree.block() != null) {
        methodTree.block().accept(new SymmetryBrokePatterns(methodTree.symbol()));
      }
    }
  }

  private class SymmetryBrokePatterns extends BaseTreeVisitor {

    private final Symbol.MethodSymbol methodSymbol;
    private final Symbol owner;

    public SymmetryBrokePatterns(Symbol.MethodSymbol methodSymbol) {
      this.methodSymbol = methodSymbol;
      this.owner = methodSymbol.owner();
    }

    private boolean isOwnerFinal() {
      return owner.isFinal();
    }

    @Override
    public void visitInstanceOf(InstanceOfTree tree) {
      if (tree.type().symbolType().equals(owner.type())) {
        if (!isOwnerFinal() && !methodSymbol.isFinal()) {
          reportIssue(tree, "Compare to \"this.getClass()\" instead.");
        }
      } else {
        reportIssue(tree, "Remove this comparison to an unrelated class.");
      }
      super.visitInstanceOf(tree);
    }

    @Override
    public void visitClass(ClassTree tree) {
      // inner classes will be visited by main visitor.
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
        checkOperand(tree.leftOperand());
        checkOperand(tree.rightOperand());
      }
      super.visitBinaryExpression(tree);
    }

    private void checkOperand(ExpressionTree expressionTree) {
      if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) expressionTree;
        if (isClassExpression(mset) && isClassOfOwner(mset) && !isOwnerFinal()) {
          reportIssue(expressionTree, "Compare to \"this.getClass()\" instead.");
        }
      }
    }

    private boolean isClassExpression(MemberSelectExpressionTree mset) {
      return JavaKeyword.CLASS.getValue().equals(mset.identifier().name());
    }

    private boolean isClassOfOwner(MemberSelectExpressionTree mset) {
      return mset.expression().symbolType().equals(owner.type());
    }

  }

}
