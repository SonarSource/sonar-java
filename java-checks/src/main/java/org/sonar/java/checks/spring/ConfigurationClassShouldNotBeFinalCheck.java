/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.spring;

import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9021")
public class ConfigurationClassShouldNotBeFinalCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;

    Optional<AnnotationTree> configurationAnnotation = getConfigurationAnnotation(classTree);
    if (configurationAnnotation.isEmpty()) {
      return;
    }

    Optional<ModifierKeywordTree> finalModifier = getFinalModifier(classTree);
    if (finalModifier.isEmpty()) {
      return;
    }

    if (hasProxyBeanMethodsDisabled(configurationAnnotation.get())) {
      return;
    }

    reportIssue(finalModifier.get(), "Remove the \"final\" modifier from this \"@Configuration\" class.");
  }

  private static Optional<AnnotationTree> getConfigurationAnnotation(ClassTree tree) {
    return tree.modifiers().annotations().stream()
      .filter(annotation -> annotation.symbolType().is(SpringUtils.CONFIGURATION_ANNOTATION))
      .findFirst();
  }

  private static Optional<ModifierKeywordTree> getFinalModifier(ClassTree tree) {
    return tree.modifiers().stream()
      .filter(modifier -> modifier.is(Tree.Kind.MODIFIER))
      .map(ModifierKeywordTree.class::cast)
      .filter(modifier -> modifier.modifier() == Modifier.FINAL)
      .findFirst();
  }

  private static boolean hasProxyBeanMethodsDisabled(AnnotationTree annotation) {
    return annotation.arguments().stream()
      .filter(argument -> argument.is(Tree.Kind.ASSIGNMENT))
      .map(AssignmentExpressionTree.class::cast)
      .anyMatch(ConfigurationClassShouldNotBeFinalCheck::setsProxyBeanMethodsToFalse);
  }

  private static boolean setsProxyBeanMethodsToFalse(AssignmentExpressionTree assignment) {
    return "proxyBeanMethods".equals(((IdentifierTree) assignment.variable()).name()) &&
      Boolean.FALSE.equals(ExpressionsHelper.getConstantValueAsBoolean(assignment.expression()).value());
  }

}
