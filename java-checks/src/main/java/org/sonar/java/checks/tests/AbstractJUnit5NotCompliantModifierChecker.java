/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks.tests;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.UnitTestUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

public abstract class AbstractJUnit5NotCompliantModifierChecker extends IssuableSubscriptionVisitor {

  protected static final String WRONG_MODIFIER_ISSUE_MESSAGE = "Remove this '%s' modifier.";

  protected abstract boolean isNonCompliantModifier(Modifier modifier, boolean isMethod);

  protected abstract void raiseIssueOnNonCompliantReturnType(MethodTree methodTree);

  protected void raiseIssueOnNonCompliantModifier(ModifierKeywordTree modifier) {
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(modifier)
      .withMessage(String.format(WRONG_MODIFIER_ISSUE_MESSAGE, modifier.keyword().text()))
      .withQuickFix(() ->
        JavaQuickFix.newQuickFix("Remove modifier")
          .addTextEdit(JavaTextEdit.removeTree(modifier))
          .build())
      .report();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.symbol().isAbstract()) {
      return;
    }
    List<MethodTree> methods = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .collect(Collectors.toList(/*mutable*/));

    List<MethodTree> testMethods = methods.stream()
      .filter(UnitTestUtils::hasJUnit5TestAnnotation)
      .filter(AbstractJUnit5NotCompliantModifierChecker::isNotOverriding)
      .toList();

    for (MethodTree testMethod : testMethods) {
      raiseIssueOnNotCompliantModifiers(testMethod.modifiers(), true);
      raiseIssueOnNonCompliantReturnType(testMethod);
    }

    methods.removeAll(testMethods);
    boolean hasPublicStaticMethods = methods.stream()
      .map(MethodTree::modifiers)
      .anyMatch(AbstractJUnit5NotCompliantModifierChecker::isPublicStatic);

    boolean hasPublicStaticFields = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .map(VariableTree::modifiers)
      .anyMatch(AbstractJUnit5NotCompliantModifierChecker::isPublicStatic);

    if (hasPublicStaticMethods || hasPublicStaticFields) {
      // we can not ask for a change of visibility of the class
      return;
    }

    if (!testMethods.isEmpty()) {
      raiseIssueOnNotCompliantModifiers(classTree.modifiers(), false);
    }
  }

  private static boolean isPublicStatic(ModifiersTree modifiers) {
    return ModifiersUtils.hasAll(modifiers, Modifier.PUBLIC, Modifier.STATIC);
  }

  private void raiseIssueOnNotCompliantModifiers(ModifiersTree modifierTree, boolean isMethod) {
    modifierTree.modifiers().stream()
      .filter(modifier -> isNonCompliantModifier(modifier.modifier(), isMethod))
      .findFirst()
      .ifPresent(this::raiseIssueOnNonCompliantModifier);
  }

  private static boolean isNotOverriding(MethodTree tree) {
    // When it cannot be decided, isOverriding will return null, we consider that it as an override to avoid FP.
    return Boolean.FALSE.equals(tree.isOverriding());
  }

}
