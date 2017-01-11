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
package org.sonar.java.checks.naming;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

@Rule(key = "S1201")
public class MethodNamedEqualsCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if ("equals".equalsIgnoreCase(methodTree.simpleName().name()) && !hasSingleObjectParameter(methodTree) && !isOverriding(methodTree)) {
      reportIssue(methodTree.simpleName(), "Either override Object.equals(Object), or totally rename the method to prevent any confusion.");
    }
  }

  private static boolean hasSingleObjectParameter(MethodTree methodTree) {
    List<VariableTree> parameters = methodTree.parameters();
    if (parameters.size() != 1) {
      return false;
    }
    return isObjectType(parameters.get(0));
  }

  private static boolean isObjectType(VariableTree variableTree) {
    String type = ExpressionsHelper.concatenate((ExpressionTree) variableTree.type());
    return "Object".equals(type)|| "java.lang.Object".equals(type);
  }

  private static boolean isOverriding(MethodTree methodTree) {
    return BooleanUtils.isTrue(((MethodTreeImpl) methodTree).isOverriding());
  }
}
