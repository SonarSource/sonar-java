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
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S1849")
public class HasNextCallingNextCheck extends IssuableSubscriptionVisitor {

  private HasNextBodyVisitor hasNextBodyVisitor = new HasNextBodyVisitor();

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (!hasSemantic()) {
      return;
    }
    if (methodTree.block() != null && isHasNextMethod(methodTree)) {
      hasNextBodyVisitor.setHasNextOwner(methodTree.symbol().owner());
      methodTree.block().accept(hasNextBodyVisitor);
    }
  }

  private static boolean isHasNextMethod(MethodTree methodTree) {
    return "hasNext".equals(methodTree.simpleName().name()) && methodTree.parameters().isEmpty() && isIteratorMethod(methodTree.symbol());
  }

  private static boolean isIteratorMethod(Symbol method) {
    Type type = method.owner().enclosingClass().type();
    return !type.is("java.util.Iterator") && type.isSubtypeOf("java.util.Iterator");
  }

  private class HasNextBodyVisitor extends BaseTreeVisitor {

    private Symbol hasNextOwner;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      Symbol method = tree.symbol();
      if ("next".equals(method.name())
        && tree.arguments().isEmpty()
        && isIteratorMethod(method)
        && (hasNextOwner == method.owner() || hasNextOwner.type().isSubtypeOf(method.owner().type()))) {
        reportIssue(ExpressionUtils.methodName(tree), "Refactor the implementation of this \"Iterator.hasNext()\" method to not call \"Iterator.next()\".");
      }
      super.visitMethodInvocation(tree);
    }

    public void setHasNextOwner(Symbol hasNextOwner) {
      this.hasNextOwner = hasNextOwner;
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Don't visit nested classes
    }

  }

}
