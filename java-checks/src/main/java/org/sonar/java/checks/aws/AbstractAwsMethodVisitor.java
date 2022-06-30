/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.aws;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sonar.java.checks.helpers.TreeHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class AbstractAwsMethodVisitor extends IssuableSubscriptionVisitor {
  private static final List<Tree.Kind> NODES_TO_VISIT = List.of(Tree.Kind.METHOD);

  protected static final MethodMatchers HANDLE_REQUEST_MATCHER = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("com.amazonaws.services.lambda.runtime.RequestHandler")
      .names("handleRequest")
      .addParametersMatcher("java.lang.Object", "com.amazonaws.services.lambda.runtime.Context")
      .build(),
    MethodMatchers.create()
      .ofSubTypes("com.amazonaws.services.lambda.runtime.RequestStreamHandler")
      .names("handleRequest")
      .addParametersMatcher("java.io.InputStream", "java.io.OutputStream", "com.amazonaws.services.lambda.runtime.Context")
      .build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return NODES_TO_VISIT;
  }

  @Override
  public void visitNode(Tree handleRequestMethodTree) {
    var methodTree = (MethodTree) handleRequestMethodTree;
    if (!HANDLE_REQUEST_MATCHER.matches(methodTree)) {
      return;
    }

    Set<MethodTree> methodTrees = new HashSet<>();
    methodTrees.addAll(TreeHelper.findReachableMethodsInSameFile(methodTree));
    methodTrees.add(methodTree);
    visitReachableMethodsFromHandleRequest(methodTrees);
  }

  abstract void visitReachableMethodsFromHandleRequest(Set<MethodTree> methodTrees);
}
