/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;

import java.util.List;
import java.util.Set;

@Rule(key = "S2387")
public class ChildClassShadowFieldCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> IGNORED_FIELDS = ImmutableSet.of("serialVersionUID");

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    TypeTree superClass = ((ClassTree) tree).superClass();
    if (superClass != null) {
      Symbol.TypeSymbol superclassSymbol = superClass.symbolType().symbol();
      ((ClassTree) tree).members().stream()
        .filter(m -> m.is(Kind.VARIABLE))
        .map(m -> (VariableTree) m)
        .forEach(v -> {
          String fieldName = v.simpleName().name();
          if (!IGNORED_FIELDS.contains(fieldName)) {
            checkForIssue(superclassSymbol, v, fieldName);
          }
        });
    }
  }

  private void checkForIssue(Symbol.TypeSymbol classSymbol, VariableTree variableTree, String fieldName) {
    for (Symbol.TypeSymbol symbol = classSymbol; symbol != null; symbol = getSuperclass(symbol)) {
      if (checkMembers(variableTree, fieldName, symbol)) {
        return;
      }
    }
  }

  private boolean checkMembers(VariableTree variableTree, String fieldName, Symbol.TypeSymbol symbol) {
    for (Symbol member : symbol.memberSymbols()) {
      if (member.isVariableSymbol() && !member.isPrivate()) {
        if (member.name().equals(fieldName)) {
          reportIssue(variableTree.simpleName(), String.format("\"%s\" is the name of a field in \"%s\".", fieldName, symbol.name()));
          return true;
        }
        if (member.name().equalsIgnoreCase(fieldName)) {
          reportIssue(variableTree.simpleName(), String.format("\"%s\" differs only by case from \"%s\" in \"%s\".", fieldName, member.name(), symbol.name()));
          return true;
        }
      }
    }
    return false;
  }

  @CheckForNull
  private static Symbol.TypeSymbol getSuperclass(Symbol.TypeSymbol symbol) {
    Type superType = symbol.superClass();
    if (superType != null) {
      return superType.symbol();
    }
    return null;
  }

}
