/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.ExtendedIssueBuilderSubscriptionVisitor;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "ModifiersOrderCheck", repositoryKey = "squid")
@Rule(key = "S1124")
public class ModifiersOrderCheck extends ExtendedIssueBuilderSubscriptionVisitor {
  private Set<Tree> alreadyReported = new HashSet<>();

  @Override
  public void setContext(JavaFileScannerContext context) {
    alreadyReported.clear();
    super.setContext(context);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.MODIFIERS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!alreadyReported.contains(tree)) {
      ModifiersTree modifiers = (ModifiersTree) tree;
      alreadyReported.add(modifiers);
      getFirstBadlyOrdered(modifiers)
        .ifPresent(wrongModifier -> newIssue()
          .onTree(wrongModifier)
          .withMessage("Reorder the modifiers to comply with the Java Language Specification.")
          .withQuickFix(() -> reorderedFix(modifiers))
          .report());
    }
  }

  private static Optional<ModifierTree> getFirstBadlyOrdered(ModifiersTree modifiersTree) {
    ListIterator<ModifierTree> modifiersIterator = modifiersTree.listIterator();
    skipAnnotations(modifiersIterator);
    Modifier[] modifiers = Modifier.values();
    int modifierIndex = 0;
    while (modifiersIterator.hasNext()){
      ModifierTree modifier = modifiersIterator.next();
      if (modifier.is(Tree.Kind.ANNOTATION)) {
        break;
      }
      ModifierKeywordTree mkt = (ModifierKeywordTree) modifier;
      for (; modifierIndex < modifiers.length && !mkt.modifier().equals(modifiers[modifierIndex]); modifierIndex++) {
        // We're just interested in the final value of modifierIndex
      }
      if (modifierIndex == modifiers.length) {
        return Optional.of(modifier);
      }
    }
    return testOnlyAnnotationsAreLeft(modifiersIterator);
  }

  /**
   * Move iterator on the first element which is not an annotation
   */
  private static void skipAnnotations(ListIterator<ModifierTree> modifiersIterator) {
    while (modifiersIterator.hasNext() && modifiersIterator.next().is(Tree.Kind.ANNOTATION)) {
      // skip modifiers which are annotations
    }
    if (modifiersIterator.hasNext()) {
      modifiersIterator.previous();
    }
  }

  private static Optional<ModifierTree> testOnlyAnnotationsAreLeft(ListIterator<ModifierTree> modifiersIterator) {
    while (modifiersIterator.hasNext()) {
      ModifierTree modifier = modifiersIterator.next();
      if (!modifier.is(Tree.Kind.ANNOTATION)) {
        modifiersIterator.previous();
        if (modifiersIterator.hasPrevious()) {
          return Optional.of(modifiersIterator.previous());
        }
      }
    }
    return Optional.empty();
  }

  private static JavaQuickFix reorderedFix(ModifiersTree modifiersTree) {
    JavaQuickFix.Builder builder = JavaQuickFix.newQuickFix("Reorder modifiers");
    List<AnnotationTree> annotations = new ArrayList<>(modifiersTree.annotations());

    if (annotations.isEmpty()) {
      // EASY: there is no annotations...
      // 1) remove all modifiers
      builder.addTextEdit(JavaTextEdit.removeTree(modifiersTree));
      // 2) add it at the beginning of modifiers
      builder.addTextEdit(reorderedModifiers(modifiersTree, false));
    } else {
      // HARD: there is annotations, and they might be anywhere, and spread on multiple lines
      // 1) Remove all modifiers individually
      removalOfAllModifiers(modifiersTree).stream()
        .map(JavaTextEdit::removeTextSpan)
        .forEach(builder::addTextEdit);

      // 2) reintroduce them right before the next token
      builder.addTextEdit(reorderedModifiers(modifiersTree, true));
    }

    return builder.build();
  }

  private static List<AnalyzerMessage.TextSpan> removalOfAllModifiers(ModifiersTree modifiersTree) {
    List<AnalyzerMessage.TextSpan> removals = new ArrayList<>();
    int numberModifiers = modifiersTree.size();
    for (int i = 0; i < numberModifiers; i++) {
      ModifierTree current = modifiersTree.get(i);
      if (current.is(Tree.Kind.ANNOTATION)) {
        continue;
      }
      if (i == (numberModifiers - 1)) {
        // Last: remove last token and potential space
        removals.add(AnalyzerMessage.textSpanBetween(current, true, QuickFixHelper.nextToken(modifiersTree), false));
      } else {
        // Take into account neighboring modifiers (can be on different lines)
        removals.add(AnalyzerMessage.textSpanBetween(current, true, modifiersTree.get(i + 1), false));
      }
    }
    return removals;
  }

  private static JavaTextEdit reorderedModifiers(ModifiersTree modifiersTree, boolean useParent) {
    String replacement = modifiersTree.modifiers()
      .stream()
      .sorted((m1, m2) -> m1.modifier().compareTo(m2.modifier()))
      .map(ModifierKeywordTree::keyword)
      .map(SyntaxToken::text)
      .collect(Collectors.joining(" "));
    if (!useParent) {
      return JavaTextEdit.insertBeforeTree(modifiersTree.get(0), replacement);
    }
    return JavaTextEdit.insertBeforeTree(QuickFixHelper.nextToken(modifiersTree), replacement + " ");
  }
}
