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
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.TypeSymbol;
import org.sonar.java.resolve.Type.ClassType;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Set;

@Rule(
  key = "S1849",
  name = "\"Iterator.hasNext()\" should not call \"Iterator.next()\"",
  tags = {"bug"},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("20min")
public class HasNextCallingNextCheck extends SubscriptionBaseVisitor {

  private HasNextBodyVisitor hasNextBodyVisitor = new HasNextBodyVisitor();

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (hasSemantic() && methodTree.block() != null && isHasNextMethod(methodTree)) {
      methodTree.block().accept(hasNextBodyVisitor);
    }
  }

  private boolean isHasNextMethod(MethodTree methodTree) {
    MethodTreeImpl method = (MethodTreeImpl) methodTree;
    return "hasNext".equals(methodTree.simpleName().name()) && methodTree.parameters().isEmpty() && isIteratorMethod(method.getSymbol());
  }

  private boolean isIteratorMethod(Symbol method) {
    TypeSymbol methodOwner = method.owner().enclosingClass();
    Set<ClassType> superTypes = methodOwner.superTypes();
    for (ClassType classType : superTypes) {
      if (classType.is("java.util.Iterator")) {
        return true;
      }
    }
    return false;
  }

  private class HasNextBodyVisitor extends BaseTreeVisitor {

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      MethodInvocationTreeImpl invocation = (MethodInvocationTreeImpl) tree;
      Symbol method = invocation.getSymbol();
      if ("next".equals(method.getName()) && invocation.arguments().isEmpty() && isIteratorMethod(method)) {
        addIssue(tree, "Refactor the implementation of this \"Iterator.hasNext()\" method to not call \"Iterator.next()\".");
      }
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Don't visit nested classes
    }

  }

}
