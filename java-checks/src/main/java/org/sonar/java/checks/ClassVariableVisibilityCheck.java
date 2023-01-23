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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "ClassVariableVisibilityCheck", repositoryKey = "squid")
@Rule(key = "S1104")
public class ClassVariableVisibilityCheck extends BaseTreeVisitor implements JavaFileScanner {

  private Deque<Boolean> isClassStack = new ArrayDeque<>();

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    isClassStack.push(tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.ENUM));
    super.visitClass(tree);
    isClassStack.pop();
  }

  @Override
  public void visitVariable(VariableTree tree) {
    ModifiersTree modifiers = tree.modifiers();
    List<AnnotationTree> annotations = modifiers.annotations();
    Optional<ModifierKeywordTree> publicMod = getPublicModifier(modifiers);
    if (isClass() && publicMod.isPresent() && !(isFinal(modifiers) || !annotations.isEmpty())) {
      reportWithQuickFix(tree, publicMod.get());
    }
    super.visitVariable(tree);
  }

  private boolean isClass() {
    return !isClassStack.isEmpty() && isClassStack.peek();
  }

  private static boolean isFinal(ModifiersTree modifiers) {
    return ModifiersUtils.hasModifier(modifiers, Modifier.FINAL);
  }
  
  private void reportWithQuickFix(VariableTree tree, ModifierKeywordTree publicMod) {
    QuickFixHelper.newIssue(context)
    .forRule(this)
    .onTree(tree.simpleName())
    .withMessage( "Make " + tree.simpleName() + " a static final constant or non-public and provide accessors if needed.")
    .withQuickFix(()->computeQuickFix(tree, publicMod))
    .report();
  }
  
  static JavaQuickFix computeQuickFix(VariableTree tree, ModifierKeywordTree publicModifier){
    var quickFixBuilder = JavaQuickFix.newQuickFix("Replace public modifier with private");
    quickFixBuilder.addTextEdit(JavaTextEdit.replaceTree(publicModifier, "private"));
    return quickFixBuilder.build();
  }

  static Optional<ModifierKeywordTree> getPublicModifier(ModifiersTree modifiersTree) {
    for (ModifierKeywordTree modifierKeywordTree : modifiersTree.modifiers()) {
      if (modifierKeywordTree.modifier() == Modifier.PUBLIC) {
        return Optional.of(modifierKeywordTree);
      }
    }
    return Optional.empty();
  }
  
}
