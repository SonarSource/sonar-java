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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6548")
public class SingletonUsageCheck extends IssuableSubscriptionVisitor {
  private static final String MESSAGE = "A Singleton implementation was detected." + " " +
    "Make sure the use of the Singleton pattern is required and the implementation is the right one for the context.";
  private static final String MESSAGE_FOR_ENUMS = "An Enum-based Singleton implementation was detected." + " " +
    "Make sure the use of the Singleton pattern is required and an Enum-based implementation is the right one for the context.";

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
    if (enumConstants.size() == 1) {
      EnumConstantTree constant = (EnumConstantTree) enumConstants.get(0);
      if (isInitializedWithParameterFreeConstructor(constant) &&
        hasNonPrivateInstanceMethodsOrFields(classTree)) {
        reportIssue(classTree.simpleName(), MESSAGE_FOR_ENUMS,
          Collections.singletonList(new JavaFileScannerContext.Location("Single enum", constant)), null);
      }
    }
  }

  private void visitClass(ClassTree classTree) {
    var classAndInstance = collectClassAndField(classTree);

    if (classAndInstance == null) return;

    ClassTree singletonClass = classAndInstance.getKey();
    VariableTree singletonField = classAndInstance.getValue();

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
      allConstructors.forEach(constructor -> {
        IdentifierTree methodName = allConstructors.get(0).simpleName();
        flows.add(new JavaFileScannerContext.Location("Private constructor", methodName));
      });
      extractAssignments(singletonField).forEach(assignment -> flows.add(new JavaFileScannerContext.Location("Value assignment", assignment)));

      reportIssue(singletonClass.simpleName(), MESSAGE, flows, null);
    }
  }

  @CheckForNull
  private static Map.Entry<ClassTree, VariableTree> collectClassAndField(ClassTree classTree) {
    ClassTree wrappingClass = null;
    final var parent = classTree.parent();
    if (parent != null && parent.is(Tree.Kind.CLASS)) {
      wrappingClass = (ClassTree) parent;
    }

    List<VariableTree> staticFields = collectStaticFields(classTree, wrappingClass);
    if (staticFields.size() != 1) return null;

    var field = staticFields.get(0);

    final var fieldSymbol = field.symbol();
    ClassTree singletonClass = null;
    if (fieldSymbol.type().equals(classTree.symbol().type())) {
      singletonClass = classTree;
    } else {
      singletonClass = wrappingClass;
    }

    if (!isEffectivelyFinal(fieldSymbol)) return null;

    return new AbstractMap.SimpleEntry<>(singletonClass, field);
  }

  private static List<VariableTree> collectStaticFields(ClassTree classTree, @Nullable ClassTree wrappingClass) {
    Type type = classTree.symbol().type();
    Type wrappingType = wrappingClass != null ? wrappingClass.symbol().type() : null;
    return classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE) && ((VariableTree) member).symbol().isStatic())
      .map(VariableTree.class::cast)
      .filter(field -> {
        Type fieldType = field.symbol().type();
        return fieldType.equals(type) || (wrappingType != null && fieldType.equals(wrappingType));
      }).collect(Collectors.toList());
  }

  private static boolean isEffectivelyFinal(Symbol symbol) {
    return symbol.isFinal() ||
      (symbol.isPrivate() && ExpressionsHelper.getSingleWriteUsage(symbol) != null);
  }

  private static boolean isInitializedWithParameterFreeConstructor(EnumConstantTree constant) {
    return constant.initializer().methodSymbol().parameterTypes().isEmpty();
  }

  private static boolean hasNonPrivateInstanceMethodsOrFields(ClassTree classTree) {
    return classTree.members().stream().anyMatch(member -> {
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

  private static List<AssignmentExpressionTree> extractAssignments(VariableTree variable) {
    return variable.symbol().usages().stream()
      .map(Tree::parent)
      .filter(usage -> usage.is(Tree.Kind.ASSIGNMENT))
      .map(AssignmentExpressionTree.class::cast)
      .collect(Collectors.toList());
  }
}
