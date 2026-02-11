/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.List;

@Rule(key = "S2208")
public class WildcardImportsShouldNotBeUsedCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return !version.isJava25Compatible();
  }

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.IMPORT);
  }

  @Override
  public void visitNode(Tree tree) {
    ImportTree importTree = (ImportTree) tree;

    // See RSPEC-2208 : exception with static imports.
    String qualifiedName = ExpressionsHelper.concatenate((ExpressionTree) importTree.qualifiedIdentifier());
    if (qualifiedName.endsWith(".*") && !importTree.isStatic()) {
      reportIssue(importTree.qualifiedIdentifier(), "Explicitly import the specific classes needed.");
    }
  }
}
