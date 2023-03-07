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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Rule(key = "S6213")
public class RestrictedIdentifiersUsageCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> RESTRICTED_IDENTIFIERS = SetUtils.immutableSetOf("var", "record", "yield");
  private static final String MESSAGE = "Rename this %s to not match a restricted identifier.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.VARIABLE, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) tree;
      IdentifierTree identifierTree = variableTree.simpleName();
      reportRestrictedIdentifier(identifierTree, "variable");
    } else {
      MethodTree methodTree = (MethodTree) tree;
      IdentifierTree identifierTree = methodTree.simpleName();
      reportRestrictedIdentifier(identifierTree, "method");
    }
  }

  private void reportRestrictedIdentifier(IdentifierTree identifierTree, String method) {
    if (RESTRICTED_IDENTIFIERS.contains(identifierTree.name())) {
      reportIssue(identifierTree, String.format(MESSAGE, method));
    }
  }
}
