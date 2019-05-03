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
package org.sonar.java.checks.unused;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1068")
public class UnusedPrivateFieldCheck extends IssuableSubscriptionVisitor {

  private static final Tree.Kind[] ASSIGNMENT_KINDS = {
    Tree.Kind.ASSIGNMENT,
    Tree.Kind.MULTIPLY_ASSIGNMENT,
    Tree.Kind.DIVIDE_ASSIGNMENT,
    Tree.Kind.REMAINDER_ASSIGNMENT,
    Tree.Kind.PLUS_ASSIGNMENT,
    Tree.Kind.MINUS_ASSIGNMENT,
    Tree.Kind.LEFT_SHIFT_ASSIGNMENT,
    Tree.Kind.RIGHT_SHIFT_ASSIGNMENT,
    Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT,
    Tree.Kind.AND_ASSIGNMENT,
    Tree.Kind.XOR_ASSIGNMENT,
    Tree.Kind.OR_ASSIGNMENT};

  private List<ClassTree> classes = new ArrayList<>();
  private ListMultimap<Symbol, IdentifierTree> assignments = ArrayListMultimap.create();
  private Set<String> unknownIdentifiers = new HashSet<>();
  private boolean hasNativeMethod = false;

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.METHOD, Tree.Kind.EXPRESSION_STATEMENT, Tree.Kind.IDENTIFIER);
  }

  @Override
  public void leaveFile(JavaFileScannerContext context) {
    if (!hasNativeMethod) {
      classes.forEach(this::checkClassFields);
    }
    classes.clear();
    assignments.clear();
    unknownIdentifiers.clear();
    hasNativeMethod = false;
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    switch (tree.kind()) {
      case METHOD:
        checkIfNativeMethod((MethodTree) tree);
        break;
      case CLASS:
        classes.add((ClassTree) tree);
        break;
      case EXPRESSION_STATEMENT:
        collectAssignment(((ExpressionStatementTree) tree).expression());
        break;
      case IDENTIFIER:
        collectUnknownIdentifier((IdentifierTree) tree);
        break;
      default:
        throw new IllegalStateException("Unexpected subscribed tree.");
    }
  }

  private void collectUnknownIdentifier(IdentifierTree identifier) {
    if (identifier.symbol().isUnknown() && !isMethodIdentifier(identifier)) {
      unknownIdentifiers.add(identifier.name());
    }
  }

  private static boolean isMethodIdentifier(IdentifierTree identifier) {
    Tree parent = identifier.parent();
    while (parent != null && !parent.is(Tree.Kind.METHOD_INVOCATION, Tree.Kind.METHOD_REFERENCE)) {
      parent = parent.parent();
    }
    if (parent == null) {
      return false;
    }
    if (parent.is(Tree.Kind.METHOD_INVOCATION)) {
      return identifier.equals(ExpressionUtils.methodName((MethodInvocationTree) parent));
    } else {
      return identifier.equals(((MethodReferenceTree) parent).method());
    }
  }

  private void checkIfNativeMethod(MethodTree method) {
    if (ModifiersUtils.hasModifier(method.modifiers(), Modifier.NATIVE)) {
      hasNativeMethod = true;
    }
  }

  private void checkClassFields(ClassTree classTree) {
    classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.VARIABLE))
      .map(VariableTree.class::cast)
      .forEach(this::checkIfUnused);
  }

  public void checkIfUnused(VariableTree tree) {
    if (hasNoAnnotation(tree)) {
      Symbol symbol = tree.symbol();
      String name = symbol.name();
      if (symbol.isPrivate()
        && onlyUsedInVariableAssignment(symbol)
        && !"serialVersionUID".equals(name)
        && !unknownIdentifiers.contains(name)) {
        reportIssue(tree.simpleName(), "Remove this unused \"" + name + "\" private field.");
      }
    }
  }

  private boolean onlyUsedInVariableAssignment(Symbol symbol) {
    return symbol.usages().size() == assignments.get(symbol).size();
  }

  private static boolean hasNoAnnotation(VariableTree tree) {
    return tree.modifiers().annotations().isEmpty();
  }

  private void collectAssignment(ExpressionTree expressionTree) {
    if (expressionTree.is(ASSIGNMENT_KINDS)) {
      addAssignment(((AssignmentExpressionTree) expressionTree).variable());
    }
  }

  private void addAssignment(ExpressionTree tree) {
    ExpressionTree variable = ExpressionUtils.skipParentheses(tree);
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      addAssignment((IdentifierTree) variable);
    } else if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      addAssignment(((MemberSelectExpressionTree) variable).identifier());
    }
  }

  private void addAssignment(IdentifierTree identifier) {
    Symbol reference = identifier.symbol();
    if (!reference.isUnknown()) {
      assignments.put(reference, identifier);
    }
  }

}
