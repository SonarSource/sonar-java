/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6548")
public class SingletonUsageCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    final var classTree = (ClassTree) tree;
    if (tree.is(Tree.Kind.CLASS)) {
      visitClass(classTree);
    } else {
      visitEnum(classTree);
    }
  }

  private void visitEnum(ClassTree classTree) {
    var enumConstants = classTree.members().stream().filter(member -> member.is(Tree.Kind.ENUM_CONSTANT)).collect(Collectors.toList());
    if (enumConstants.size() == 1 && hasNonPrivateInstanceMethodsOrFields(classTree)) {
      reportIssue(classTree.simpleName(), "Enum singleton pattern detected",
        Collections.singletonList(new JavaFileScannerContext.Location("Single enum", enumConstants.get(0))), null);
    }
  }

  private void visitClass(ClassTree classTree) {
    ClassTree wrappingClass = null;
    final var parent = classTree.parent();
    if (parent != null && parent.is(Tree.Kind.CLASS)) {
      wrappingClass = (ClassTree) parent;
    }

    VariableTree singletonField = null;
    ClassTree singletonClass = null;
    for (var member : classTree.members()) {
      if (!(member.is(Tree.Kind.VARIABLE))) continue;

      final var varTree = (VariableTree) member;
      final var fieldSymbol = varTree.symbol();

      if (!fieldSymbol.isStatic()) continue;

      if (fieldSymbol.type().equals(classTree.symbol().type())) {
        singletonClass = classTree;
      } else if (wrappingClass != null && fieldSymbol.type().equals(wrappingClass.symbol().type())) {
        singletonClass = wrappingClass;
      } else {
        continue;
      }

      if (isEffectivelyFinal(fieldSymbol)) {
        if (singletonField != null) {
          return;
        } else {
          singletonField = varTree;
        }
      }
    }

    if (singletonField == null) return;

    var allConstructors = singletonClass.members().stream()
      .filter(member -> member.is(Tree.Kind.CONSTRUCTOR))
      .map(MethodTree.class::cast)
      .collect(Collectors.toList());

    if (allConstructors.size() <= 1 &&
      allConstructors.stream().allMatch(constructor -> constructor.symbol().isPrivate() && constructor.parameters().isEmpty()) &&
      hasNonPrivateInstanceMethodsOrFields(singletonClass)) {

      var flows = new ArrayList<JavaFileScannerContext.Location>();
      flows.add(new JavaFileScannerContext.Location("Singleton field", singletonField.simpleName()));
      if (singletonClass != classTree) {
        flows.add(new JavaFileScannerContext.Location("Singleton helper", classTree.simpleName()));
      }

      reportIssue(singletonClass.simpleName(), "This looks like a singleton class", flows, null);
    }
  }

  private static boolean isEffectivelyFinal(Symbol symbol) {
    return symbol.isFinal() ||
      (symbol.isPrivate() && ExpressionsHelper.getSingleWriteUsage(symbol) != null);
  }

  private static boolean hasNonPrivateInstanceMethodsOrFields(ClassTree classTree) {
    return classTree.members().stream().anyMatch(member -> {
      boolean isPrivateOrStatic = true;
      if (member.is(Tree.Kind.METHOD)) {
        var symbol = ((MethodTree) member).symbol();
        return !symbol.isPrivate() && !symbol.isStatic();
      } else if (member.is(Tree.Kind.VARIABLE)) {
        var symbol = ((VariableTree) member).symbol();
        return !symbol.isPrivate() && !symbol.isStatic();
      } else {
        return false;
      }
    });
  }
}
