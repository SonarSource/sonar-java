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
