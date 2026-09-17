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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Rule(key = "S9396")
public class InnerClassShadowFieldCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Rename \"%s\" which hides the field declared in \"%s\".";
  private static final Set<String> IGNORED_FIELDS = Set.of("serialVersionUID");

  @Override
  public List<Kind> nodesToVisit() {
    return Kind.CLASS_KINDS;
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Symbol.TypeSymbol classSymbol = classTree.symbol();

    if (classSymbol.outermostClass().equals(classSymbol)) {
      return;
    }

    Symbol.TypeSymbol enclosingClass = classSymbol.enclosingClass();
    if (enclosingClass == null) {
      return;
    }

    for (Tree member : classTree.members()) {
      if (member.is(Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) member;
        Symbol fieldSymbol = variableTree.symbol();
        if (!fieldSymbol.isStatic() && !IGNORED_FIELDS.contains(fieldSymbol.name())) {
          checkEnclosingClasses(enclosingClass, fieldSymbol, variableTree.simpleName());
        }
      }
    }
  }

  private void checkEnclosingClasses(Symbol.TypeSymbol enclosingClass, Symbol field,
      IdentifierTree fieldSimpleName) {
    Set<Symbol.TypeSymbol> visitedClasses = new HashSet<>();
    Symbol.TypeSymbol current = enclosingClass;
    while (current != null && visitedClasses.add(current)) {
      for (Symbol symbol : current.memberSymbols()) {
        if (symbol.isVariableSymbol() && !symbol.isStatic() && symbol.name().equals(field.name())) {
          reportIssue(fieldSimpleName, String.format(MESSAGE, field.name(), current.name()));
          return;
        }
      }
      current = current.enclosingClass();
    }
  }
}
