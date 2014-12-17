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
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collection;
import java.util.List;

public abstract class AbstractInjectionChecker extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION);
  }

  protected class LocalVariableDynamicStringVisitor extends BaseTreeVisitor {

    private final Collection<IdentifierTree> usages;
    private final Tree methodInvocationTree;
    private final Symbol currentlyChecking;
    boolean dynamicString;
    private boolean stopInspection;

    public LocalVariableDynamicStringVisitor(Symbol currentlyChecking, Collection<IdentifierTree> usages, Tree methodInvocationTree) {
      this.currentlyChecking = currentlyChecking;
      stopInspection = false;
      this.usages = usages;
      this.methodInvocationTree = methodInvocationTree;
      dynamicString = false;
    }


    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (!stopInspection && tree.variable().is(Tree.Kind.IDENTIFIER) && usages.contains(tree.variable())) {
        dynamicString |= isDynamicString(methodInvocationTree, tree.expression(), currentlyChecking);
      }
      super.visitAssignmentExpression(tree);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (tree.equals(methodInvocationTree)) {
        //stop inspection, all concerned usages have been visited.
        stopInspection = true;
      } else {
        super.visitMethodInvocation(tree);
      }
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      if (tree.equals(methodInvocationTree)) {
        //stop inspection, all concerned usages have been visited.
        stopInspection = true;
      } else {
        super.visitNewClass(tree);
      }
    }
  }

  protected abstract boolean isDynamicString(Tree methodInvocationTree, ExpressionTree expression, Symbol currentlyChecking);

}
