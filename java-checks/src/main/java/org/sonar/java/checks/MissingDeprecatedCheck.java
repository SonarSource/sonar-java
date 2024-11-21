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

import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

import static org.sonar.java.checks.helpers.DeprecatedCheckerHelper.reportTreeForDeprecatedTree;

@DeprecatedRuleKey(ruleKey = "MissingDeprecatedCheck", repositoryKey = "squid")
@Rule(key = "S1123")
public class MissingDeprecatedCheck extends AbstractMissingDeprecatedChecker {

  void handleDeprecatedElement(Tree tree, @CheckForNull AnnotationTree deprecatedAnnotation, boolean hasJavadocDeprecatedTag) {
    boolean hasDeprecatedAnnotation = deprecatedAnnotation != null;
    if (hasDeprecatedAnnotation) {
      if (!hasJavadocDeprecatedTag) {
        reportIssue(reportTreeForDeprecatedTree(tree), "Add the missing @deprecated Javadoc tag.");
      }
    } else if (hasJavadocDeprecatedTag) {
      reportIssue(reportTreeForDeprecatedTree(tree), "Add the missing @Deprecated annotation.");
    }
  }

}
