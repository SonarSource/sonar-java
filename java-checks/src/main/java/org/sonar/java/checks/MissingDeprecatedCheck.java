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

import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

import static org.sonar.java.checks.helpers.DeprecatedCheckerHelper.reportTreeForDeprecatedTree;

@DeprecatedRuleKey(ruleKey = "MissingDeprecatedCheck", repositoryKey = "squid")
@Rule(key = "S1123")
public class MissingDeprecatedCheck extends AbstractMissingDeprecatedChecker {

  void handleDeprecatedElement(Tree tree, @CheckForNull AnnotationTree deprecatedAnnotation, boolean hasJavadocDeprecatedTag) {
    // Record fields cannot have JavaDocs, so skip the check.
    if (isRecordComponent(tree)) {
      return;
    }

    boolean hasDeprecatedAnnotation = deprecatedAnnotation != null;
    if (hasDeprecatedAnnotation) {
      if (!hasJavadocDeprecatedTag) {
        reportIssue(reportTreeForDeprecatedTree(tree), "Add the missing @deprecated Javadoc tag.");
      }
    } else if (hasJavadocDeprecatedTag) {
      reportIssue(reportTreeForDeprecatedTree(tree), "Add the missing @Deprecated annotation.");
    }
  }

  /**
   * Checks whether the argument is a component of a record (a non-static field).
   */
  private static boolean isRecordComponent(Tree tree) {
    if (tree instanceof VariableTree variableTree && !variableTree.symbol().isStatic()) {
      return Optional.ofNullable(tree.parent())
        .filter(parent -> parent.is(Tree.Kind.RECORD))
        .isPresent();
    }
    return false;
  }
}
