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
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2160")
public class EqualsNotOverriddenInSubclassCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher EQUALS_MATCHER = MethodMatcher.create().name("equals").parameters("java.lang.Object");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    ClassTree classTree = (ClassTree) tree;
    if (shouldImplementEquals(classTree)) {
      reportIssue(classTree.simpleName(), "Override the \"equals\" method in this class.");
    }
  }

  private static boolean shouldImplementEquals(ClassTree classTree) {
    return hasAtLeastOneField(classTree) && !hasNotFinalEqualsMethod(classTree.symbol()) && parentClassImplementsEquals(classTree);
  }

  private static boolean hasAtLeastOneField(ClassTree classTree) {
    return classTree.members().stream().anyMatch(EqualsNotOverriddenInSubclassCheck::isField);
  }

  private static boolean isField(Tree tree) {
    return tree.is(Tree.Kind.VARIABLE) && !ModifiersUtils.hasModifier(((VariableTree) tree).modifiers(), Modifier.STATIC);
  }

  private static boolean parentClassImplementsEquals(ClassTree tree) {
    TypeTree superClass = tree.superClass();
    if (superClass != null) {
      Type superClassType = superClass.symbolType();
      while (superClassType.symbol().isTypeSymbol() && !superClassType.is("java.lang.Object")) {
        Symbol.TypeSymbol superClassSymbol = superClassType.symbol();
        Optional<Symbol> equalsMethod = equalsMethod(superClassSymbol);
        if (equalsMethod.isPresent()) {
          return !equalsMethod.get().isFinal();
        }
        superClassType = superClassSymbol.superClass();
      }
    }
    return false;
  }

  private static boolean hasNotFinalEqualsMethod(Symbol.TypeSymbol type) {
    return equalsMethod(type).filter(equalsMethod -> !equalsMethod.isFinal()).isPresent();
  }

  private static Optional<Symbol> equalsMethod(Symbol.TypeSymbol type) {
    return type.lookupSymbols("equals").stream().filter(EQUALS_MATCHER::matches).findFirst();
  }
}
