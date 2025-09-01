/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.DeprecatedCheckerHelper.deprecatedAnnotation;
import static org.sonar.java.checks.helpers.DeprecatedCheckerHelper.hasJavadocDeprecatedTag;
import static org.sonar.java.checks.helpers.DeprecatedCheckerHelper.isMarkedForRemoval;
import static org.sonar.java.checks.helpers.DeprecatedCheckerHelper.reportTreeForDeprecatedTree;

@Rule(key = "S1133")
public class DeprecatedTagPresenceCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(PublicApiChecker.apiKinds());
  }

  @Override
  public void visitNode(Tree tree) {
    if (!isMarkedForRemoval(tree, false) && (hasDeprecatedAnnotation(tree) || hasJavadocDeprecatedTag(tree))) {
      reportIssue(reportTreeForDeprecatedTree(tree), "Do not forget to remove this deprecated code someday.");
    }
  }

  private static boolean hasDeprecatedAnnotation(Tree tree) {
    return deprecatedAnnotation(tree) != null;
  }

}
