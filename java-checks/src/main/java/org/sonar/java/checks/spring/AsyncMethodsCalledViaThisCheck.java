/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.spring;

import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6809")
public class AsyncMethodsCalledViaThisCheck extends IssuableSubscriptionVisitor {

  private static final Map<String, String> DISALLOWED_METHOD_ANNOTATIONS = Map.of(
    "org.springframework.scheduling.annotation.Async", "async",
    "org.springframework.transaction.annotation.Transactional", "transactional");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    var mit = (MethodInvocationTree) tree;

    if (
      // If the call is not a member select, it must be an identifier, so it's a call to a local method, implicitly via 'this'
      !mit.methodSelect().is(Tree.Kind.MEMBER_SELECT) ||
        // On the other hand, if calls do have a qualifier, an explicit 'this' means we also want to raise an issue.
        ExpressionUtils.isThis(((MemberSelectExpressionTree) mit.methodSelect()).expression())
    ) {
      DISALLOWED_METHOD_ANNOTATIONS.entrySet().stream()
        .filter(entry -> mit.methodSymbol().metadata().isAnnotatedWith(entry.getKey()))
        .findFirst()
        .map(Map.Entry::getValue)
        .ifPresent(friendlyName -> reportIssue(mit, "Call " + friendlyName + " methods via an injected dependency instead of directly via 'this'."));
    }
  }
}
