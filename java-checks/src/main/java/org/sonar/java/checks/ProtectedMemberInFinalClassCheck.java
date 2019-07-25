/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2156")
public class ProtectedMemberInFinalClassCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove this \"protected\" modifier.";

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    if (ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.FINAL)) {
      classTree.members().forEach(this::checkMember);
    }
  }

  private void checkMember(Tree member) {
    if (member.is(Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) member;
      checkVariableCompliance(variableTree);
    } else if (member.is(Kind.METHOD)) {
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
