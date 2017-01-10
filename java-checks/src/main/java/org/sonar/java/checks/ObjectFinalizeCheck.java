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
import org.sonar.java.RspecKey;
import org.sonar.java.checks.helpers.MethodsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "ObjectFinalizeCheck")
@RspecKey("S1111")
public class ObjectFinalizeCheck extends IssuableSubscriptionVisitor {

  private boolean isInFinalizeMethod = false;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      isInFinalizeMethod = isFinalizeMethodMember((MethodTree) tree);
    } else {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
      IdentifierTree methodName = MethodsHelper.methodName(methodInvocationTree);
      if (!isInFinalizeMethod && "finalize".equals(methodName.name()) && methodInvocationTree.arguments().isEmpty()) {
        reportIssue(methodName, "Remove this call to finalize().");
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
