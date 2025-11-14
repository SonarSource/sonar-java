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
package org.sonar.java.checks.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.UnitTestUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.reporting.AnalyzerMessage;
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

  public enum ModifierScope {
    // annotation like @Nested that applies to class
    CLASS,
    // annotation like @BeforeAll that applies to static class method
    CLASS_METHOD,
    // annotation like @BeforeEach that applies to non-static method
    INSTANCE_METHOD
  }

  protected static final String WRONG_MODIFIER_ISSUE_MESSAGE = "Remove this '%s' modifier.";

  protected abstract boolean isNonCompliantModifier(Modifier modifier, ModifierScope modifierScope);

  protected abstract void raiseIssueOnNonCompliantReturnType(MethodTree methodTree);

  protected void raiseIssueOnNonCompliantModifier(ModifierKeywordTree modifier) {
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(modifier)
      .withMessage(WRONG_MODIFIER_ISSUE_MESSAGE, modifier.keyword().text())
      .withQuickFix(() ->
        JavaQuickFix.newQuickFix("Remove \"%s\" modifier", modifier.keyword().text())
          .addTextEdit(JavaTextEdit.removeTextSpan(AnalyzerMessage.textSpanBetween(modifier, true, QuickFixHelper.nextToken(modifier), false)))
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

    List<MethodTree> junit5ClassMethods = new ArrayList<>();
    List<MethodTree> junit5InstanceMethods = new ArrayList<>();
    List<MethodTree> nonJunit5Methods = new ArrayList<>();
    classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .forEach(method -> {
        if (UnitTestUtils.hasJUnit5TestAnnotation(method) || UnitTestUtils.hasJUnit5InstanceLifecycleAnnotation(method)) {
          if (isNotOverriding(method)) {
            junit5InstanceMethods.add(method);
          }
        } else if (UnitTestUtils.hasJUnit5ClassLifecycleAnnotation(method)) {
          if (isNotOverriding(method)) {
            junit5ClassMethods.add(method);
          }
        } else {
          nonJunit5Methods.add(method);
        }
      });

    raiseIssueOnMethods(junit5ClassMethods, ModifierScope.CLASS_METHOD);
    raiseIssueOnMethods(junit5InstanceMethods, ModifierScope.INSTANCE_METHOD);
    boolean classHasJunit5InstanceMethods = !junit5InstanceMethods.isEmpty();
    if (classHasJunit5InstanceMethods) {
      raiseIssueOnClass(nonJunit5Methods, classTree);
    }
  }

  private void raiseIssueOnMethods(List<MethodTree> junit5ClassMethods, ModifierScope classMethod) {
    for (MethodTree junit5ClassMethod : junit5ClassMethods) {
      raiseIssueOnNotCompliantModifiers(junit5ClassMethod.modifiers(), classMethod);
      raiseIssueOnNonCompliantReturnType(junit5ClassMethod);
    }
  }

  private void raiseIssueOnClass(List<MethodTree> nonJunit5Methods, ClassTree classTree) {
    boolean hasPublicStaticMethods = nonJunit5Methods.stream()
      .map(MethodTree::modifiers)
      .anyMatch(AbstractJUnit5NotCompliantModifierChecker::isPublicStatic);

    boolean hasPublicStaticFields = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .map(VariableTree::modifiers)
      .anyMatch(AbstractJUnit5NotCompliantModifierChecker::isPublicStatic);

    // Can we change the visibility of the class?
    if (hasPublicStaticMethods || hasPublicStaticFields) {
      return;
    }
    raiseIssueOnNotCompliantModifiers(classTree.modifiers(), ModifierScope.CLASS);
  }

  private static boolean isPublicStatic(ModifiersTree modifiers) {
    return ModifiersUtils.hasAll(modifiers, Modifier.PUBLIC, Modifier.STATIC);
  }

  private void raiseIssueOnNotCompliantModifiers(ModifiersTree modifierTree, ModifierScope modifierScope) {
    modifierTree.modifiers().stream()
      .filter(modifier -> isNonCompliantModifier(modifier.modifier(), modifierScope))
      .findFirst()
      .ifPresent(this::raiseIssueOnNonCompliantModifier);
  }

  private static boolean isNotOverriding(MethodTree tree) {
    // When it cannot be decided, isOverriding will return null, we consider that it as an override to avoid FP.
    return Boolean.FALSE.equals(tree.isOverriding());
  }

}
