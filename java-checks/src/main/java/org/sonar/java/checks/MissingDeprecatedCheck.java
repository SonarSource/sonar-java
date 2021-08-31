/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

import static org.sonar.java.checks.helpers.DeprecatedCheckerHelper.deprecatedAnnotation;
import static org.sonar.java.checks.helpers.DeprecatedCheckerHelper.reportTreeForDeprecatedTree;
import static org.sonar.java.checks.helpers.DeprecatedCheckerHelper.hasJavadocDeprecatedTag;
import static org.sonar.java.model.JUtils.isLocalVariable;

@DeprecatedRuleKey(ruleKey = "MissingDeprecatedCheck", repositoryKey = "squid")
@Rule(key = "S1123")
public class MissingDeprecatedCheck extends IssuableSubscriptionVisitor {

  private static final Kind[] CLASS_KINDS = PublicApiChecker.classKinds();

  private final Deque<Boolean> classOrInterfaceIsDeprecated = new LinkedList<>();
  private boolean isJava9 = false;

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(PublicApiChecker.apiKinds());
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    isJava9 = context.getJavaVersion().asInt() >= 9;
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    boolean isLocalVar = tree.is(Tree.Kind.VARIABLE) && isLocalVariable(((VariableTree) tree).symbol());
    AnnotationTree deprecatedAnnotation = deprecatedAnnotation(tree);
    boolean hasDeprecatedAnnotation = deprecatedAnnotation != null;
    boolean hasJavadocDeprecatedTag = hasJavadocDeprecatedTag(tree);
    if (currentClassNotDeprecated() && !isLocalVar) {
      if (hasDeprecatedAnnotation) {
        if (!hasJavadocDeprecatedTag) {
          reportIssue(reportTreeForDeprecatedTree(tree), "Add the missing @deprecated Javadoc tag.");
        } else if (isJava9 && deprecatedAnnotation.arguments().isEmpty()) {
          reportIssue(reportTreeForDeprecatedTree(deprecatedAnnotation), "Add 'since' and/or 'forRemoval' arguments to the @Deprecated annotation.");
        }
      } else if (hasJavadocDeprecatedTag) {
        reportIssue(reportTreeForDeprecatedTree(tree), "Add the missing @Deprecated annotation.");
      }
    }
    if (tree.is(CLASS_KINDS)) {
      classOrInterfaceIsDeprecated.push(hasDeprecatedAnnotation || hasJavadocDeprecatedTag);
    }
  }

  private boolean currentClassNotDeprecated() {
    return classOrInterfaceIsDeprecated.isEmpty() || !classOrInterfaceIsDeprecated.peek();
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(CLASS_KINDS)) {
      classOrInterfaceIsDeprecated.pop();
    }
  }
}
