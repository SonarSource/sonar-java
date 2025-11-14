/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import java.util.Locale;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2039")
public class FieldModifierCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    classTree.members().stream()
      .filter(FieldModifierCheck::isConsentWithCheck)
      .forEach(member -> {
        VariableTree variableTree = (VariableTree) member;
        if (!hasModifierComment(variableTree)) {
          IdentifierTree simpleName = variableTree.simpleName();
          reportIssue(simpleName, "Explicitly declare the visibility for \"" + simpleName.name() + "\".");
        }
      });
  }

  private static boolean hasModifierComment(VariableTree variableTree) {
    for (SyntaxTrivia syntaxTrivia : variableTree.type().lastToken().trivias()) {
      if (syntaxTrivia.comment().toLowerCase(Locale.ROOT).contains("modifier")) {
        return true;
      }
    }
    return false;
  }

  private static boolean isConsentWithCheck(Tree member) {
    return member.is(Tree.Kind.VARIABLE)
      && hasNoVisibilityModifier((VariableTree) member)
      && !isVisibleForTesting((VariableTree) member);
  }

  private static boolean hasNoVisibilityModifier(VariableTree variableTree) {
    return ModifiersUtils.hasNoneOf(variableTree.modifiers(), Modifier.PUBLIC, Modifier.PRIVATE, Modifier.PROTECTED);
  }

  private static boolean isVisibleForTesting(VariableTree variableTree) {
    return variableTree.modifiers().annotations().stream()
      .anyMatch(annotation -> "VisibleForTesting".equals(annotation.annotationType().lastToken().text()));
  }

}
