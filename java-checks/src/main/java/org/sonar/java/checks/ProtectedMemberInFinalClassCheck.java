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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2156")
public class ProtectedMemberInFinalClassCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove this \"protected\" modifier.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.FINAL)) {
      classTree.members().forEach(this::checkMember);
    }
  }

  private void checkMember(Tree member) {
    if (member.is(Tree.Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) member;
      checkVariableCompliance(variableTree);
    } else if (member.is(Tree.Kind.METHOD)) {
      MethodTree methodTree = (MethodTree) member;
      if (Boolean.FALSE.equals(methodTree.isOverriding())) {
        checkMethodCompliance(methodTree);
      }
    }
  }

  private void checkMethodCompliance(MethodTree methodTree) {
    checkComplianceOnModifiersAndSymbol(methodTree.modifiers());
  }

  private void checkVariableCompliance(VariableTree variableTree) {
    checkComplianceOnModifiersAndSymbol(variableTree.modifiers());
  }

  private void checkComplianceOnModifiersAndSymbol(ModifiersTree modifiers) {
    ModifierKeywordTree modifier = ModifiersUtils.getModifier(modifiers, Modifier.PROTECTED);
    if (modifier != null && !isVisibleForTesting(modifiers)) {
      reportIssue(modifier.keyword(), MESSAGE);
    }
  }

  private static boolean isVisibleForTesting(ModifiersTree modifiers) {
    return modifiers.annotations().stream()
      .anyMatch(annotation -> "VisibleForTesting".equals(annotation.annotationType().lastToken().text()));
  }

}
