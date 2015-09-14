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
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "ObjectFinalizeCheck",
  name = "The Object.finalize() method should not be called",
  tags = {"cert", "cwe", "security"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("20min")
public class ObjectFinalizeCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.METHOD_INVOCATION);
  }

  private boolean isInFinalizeMethod = false;

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      isInFinalizeMethod = isFinalizeMethodMember((MethodTree) tree);
    } else {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
      String name = "";
      if (methodInvocationTree.methodSelect().is(Tree.Kind.IDENTIFIER)) {
        name = ((IdentifierTree) methodInvocationTree.methodSelect()).name();
      } else if (methodInvocationTree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        name = ((MemberSelectExpressionTree) methodInvocationTree.methodSelect()).identifier().name();
      }
      if (!isInFinalizeMethod && "finalize".equals(name) && methodInvocationTree.arguments().isEmpty()) {
        addIssue(tree, "Remove this call to finalize().");
      }
    }

  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD) && isFinalizeMethodMember((MethodTree) tree)) {
      isInFinalizeMethod = false;
    }
  }

  private static boolean isFinalizeMethodMember(MethodTree methodTree) {
    Tree returnType = methodTree.returnType();
    boolean returnVoid = returnType != null && returnType.is(Tree.Kind.PRIMITIVE_TYPE) && "void".equals(((PrimitiveTypeTree) returnType).keyword().text());
    return returnVoid && "finalize".equals(methodTree.simpleName().name());
  }

}
