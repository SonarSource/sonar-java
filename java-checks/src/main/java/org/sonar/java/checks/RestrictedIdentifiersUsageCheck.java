/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
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
