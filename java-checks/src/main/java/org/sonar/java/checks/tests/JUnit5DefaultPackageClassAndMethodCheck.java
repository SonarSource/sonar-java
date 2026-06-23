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
package org.sonar.java.checks.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ClassPatternsUtils;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.UnitTestUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5786")
public class JUnit5DefaultPackageClassAndMethodCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Remove redundant visibility modifiers from this test class and its methods.";
  private static final String MODIFIER_MESSAGE = "Remove this '%s' modifier.";
  private static final String QUICK_FIX_ALL = "Remove all redundant visibility modifiers";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.symbol().isAbstract() || (classTree.simpleName() == null)) {
      return;
    }

    UnitTestUtils.JUnit5MethodGroups groups = UnitTestUtils.groupJUnit5Methods(classTree);
    List<MethodTree> junit5ClassMethods = groups.classMethods();
    List<MethodTree> junit5InstanceMethods = groups.instanceMethods();
    List<MethodTree> nonJunit5Methods = groups.otherMethods();

    List<ModifierKeywordTree> noncompliantModifiers = new ArrayList<>();
    collectNonCompliantModifiers(junit5ClassMethods, noncompliantModifiers);
    collectNonCompliantModifiers(junit5InstanceMethods, noncompliantModifiers);

    if (!junit5InstanceMethods.isEmpty() && !ClassPatternsUtils.shouldBePublicClass(classTree, nonJunit5Methods)) {
      if (noncompliantModifiers.isEmpty()) {
        // Only the class modifier is noncompliant (no method violations): report on the modifier directly.
        classTree.modifiers().modifiers().stream()
          .filter(m -> isNonCompliantModifier(m.modifier()))
          .findFirst()
          .ifPresent(m -> QuickFixHelper.newIssue(context)
            .forRule(this)
            .onTree(m)
            .withMessage(MODIFIER_MESSAGE, m.keyword().text())
            .withQuickFixes(() -> List.of(buildQuickFix(List.of(m))))
            .report());
        return;
      }
      addNonCompliantModifier(classTree.modifiers(), noncompliantModifiers);
    }

    if (!noncompliantModifiers.isEmpty()) {
      List<JavaFileScannerContext.Location> secondaries = noncompliantModifiers.stream()
        .map(m -> new JavaFileScannerContext.Location(String.format(MODIFIER_MESSAGE, m.keyword().text()), m))
        .toList();
      List<ModifierKeywordTree> modifiersForFix = new ArrayList<>(noncompliantModifiers);
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(classTree.simpleName())
        .withMessage(MESSAGE)
        .withSecondaries(secondaries)
        .withQuickFixes(() -> List.of(buildQuickFix(modifiersForFix)))
        .report();
    }
  }

  private static void collectNonCompliantModifiers(List<MethodTree> methods, List<ModifierKeywordTree> modifiers) {
    for (MethodTree method : methods) {
      addNonCompliantModifier(method.modifiers(), modifiers);
    }
  }

  private static void addNonCompliantModifier(ModifiersTree modifiers, List<ModifierKeywordTree> result) {
    modifiers.modifiers().stream()
      .filter(m -> isNonCompliantModifier(m.modifier()))
      .findFirst()
      .ifPresent(result::add);
  }

  private static JavaQuickFix buildQuickFix(List<ModifierKeywordTree> modifiers) {
    List<JavaTextEdit> edits = modifiers.stream()
      .map(m -> JavaTextEdit.removeTextSpan(
        AnalyzerMessage.textSpanBetween(m, true, QuickFixHelper.nextToken(m), false)))
      .toList();
    String description = modifiers.size() == 1
      ? String.format(MODIFIER_MESSAGE, modifiers.get(0).keyword().text())
      : QUICK_FIX_ALL;
    return JavaQuickFix.newQuickFix(description)
      .addTextEdits(edits)
      .build();
  }

  private static boolean isNonCompliantModifier(Modifier modifier) {
    // All visibility modifiers except 'private' handled by S5810
    return modifier == Modifier.PUBLIC || modifier == Modifier.PROTECTED;
  }
}
