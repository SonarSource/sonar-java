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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3366")
public class ThisExposedFromConstructorCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    methodTree.block().accept(new ConstructorBodyVisitor(methodTree.symbol().owner()));
  }

  private class ConstructorBodyVisitor extends BaseTreeVisitor {
    private Symbol owner;

    public ConstructorBodyVisitor(Symbol owner) {
      this.owner = owner;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (this.owner == tree.symbol().owner()) {
        return;
      }
      tree.arguments().stream().filter(ExpressionUtils::isThis).forEach(this::report);
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (!ExpressionUtils.isThis(tree.expression())) {
        return;
      }
      ExpressionTree variable = tree.variable();
      if (variable.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) variable;
        // not an issue because "this" is assigned to an object of the same type which is also defined as static
        if (memberSelect.expression().symbolType().symbol().equals(this.owner) && memberSelect.identifier().symbol().isStatic()) {
          return;
        }
      } else if (variable.is(Tree.Kind.IDENTIFIER) && (((IdentifierTree) variable).symbol().isStatic())) {
        return;
      }
      report(tree);
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip nested and anonymous classes
    }

    private void report(ExpressionTree tree) {
      reportIssue(tree, "Make sure the use of \"this\" doesn't" +
        " expose partially-constructed instances of this class in multi-threaded environments.");
    }
  }
}
