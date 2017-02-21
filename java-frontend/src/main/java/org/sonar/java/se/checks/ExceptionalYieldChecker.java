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
package org.sonar.java.se.checks;

import com.google.common.collect.ImmutableSet;

import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

public class ExceptionalYieldChecker {

  private final String message;

  ExceptionalYieldChecker(String message) {
    this.message = message;
  }

  void reportOnExceptionalYield(ExplodedGraph.Node node, SECheck check) {
    node.parents().stream().forEach(parent -> {
      MethodYield yield = node.selectedMethodYield(parent);
      if (yield != null && yield.generatedByCheck(check)) {
        reportIssue(parent, check);
      }
    });
  }

  private void reportIssue(ExplodedGraph.Node node, SECheck check) {
    MethodInvocationTree mit = (MethodInvocationTree) node.programPoint.syntaxTree();
    ExpressionTree methodSelect = mit.methodSelect();
    Tree reportTree = methodSelect;
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      reportTree = ((MemberSelectExpressionTree) methodSelect).identifier();
    }
    check.reportIssue(reportTree, String.format(message, mit.symbol().name()), ImmutableSet.of());
  }

}
