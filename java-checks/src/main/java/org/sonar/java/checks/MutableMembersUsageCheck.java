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

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2384")
public class MutableMembersUsageCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final List<String> MUTABLE_TYPES = Arrays.asList(
    "java.util.Collection",
    "java.util.Date",
    "java.util.Map");
  private static final List<String> IMMUTABLE_TYPES = Arrays.asList(
    "java.util.Collections.UnmodifiableCollection",
    "java.util.Collections.UnmodifiableMap",
    "com.google.common.collect.ImmutableCollection",
    "com.google.common.collect.ImmutableMap");

  private static final MethodMatchers UNMODIFIABLE_COLLECTION_CALL = MethodMatchers.or(
    MethodMatchers.create().ofType(type -> MutableMembersUsageCheck.containsImmutableLikeTerm(type.name())).anyName().withAnyParameters().build(),
    MethodMatchers.create().ofAnyType().name(MutableMembersUsageCheck::containsImmutableLikeTerm).withAnyParameters().build(),
    MethodMatchers.create().ofTypes("java.util.Collections")
      .name(name -> name.startsWith("singleton") || name.startsWith("empty"))
      .withAnyParameters().build(),
    MethodMatchers.create().ofTypes("java.util.Set", "java.util.List").names("of", "copyOf").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("com.google.common.collect.Sets").names("union", "intersection", "difference", "symmetricDifference").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("com.google.common.collect.Lists").names("asList").withAnyParameters().build()
  );

  private static final MethodMatchers STREAM_COLLECT_CALL = MethodMatchers.create().
    ofTypes("java.util.stream.Stream")
    .names("collect")
    .addParametersMatcher("java.util.stream.Collector")
    .build();

  private static final MethodMatchers UNMODIFIABLE_COLLECTOR_CALL = MethodMatchers.create().
    ofTypes("java.util.stream.Collectors")
    .names("toUnmodifiableSet", "toUnmodifiableList", "toUnmodifiableMap")
    .withAnyParameters()
    .build();

  private JavaFileScannerContext context;
  private Deque<Set<Symbol>> parametersStack = new LinkedList<>();

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (tree.is(Tree.Kind.CONSTRUCTOR)) {
      Symbol.TypeSymbol enclosingClass = tree.symbol().enclosingClass();
      if (enclosingClass.isEnum()) {
        return;
      }
    }
    parametersStack.push(tree.parameters().stream()
      .map(VariableTree::symbol)
      .collect(Collectors.toSet()));
    super.visitMethod(tree);
    parametersStack.pop();
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    super.visitAssignmentExpression(tree);
    if (!isMutableType(tree.expression())) {
      return;
    }
    ExpressionTree variable = tree.variable();
    Symbol leftSymbol = null;
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) variable;
      leftSymbol = identifierTree.symbol();
    } else if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mit = (MemberSelectExpressionTree) variable;
      leftSymbol = mit.identifier().symbol();
    }
    if (leftSymbol != null && leftSymbol.isPrivate()) {
      checkStore(tree.expression());
    }
  }

  private void checkStore(ExpressionTree expression) {
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) expression;
      if (!parametersStack.isEmpty() && parametersStack.peek().contains(identifierTree.symbol())) {
        context.reportIssue(this, identifierTree, "Store a copy of \"" + identifierTree.name() + "\".");
      }
    }
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    super.visitReturnStatement(tree);
    ExpressionTree expressionTree = tree.expression();
    if (expressionTree == null || !isMutableType(expressionTree)) {
      return;
    }
    checkReturnedExpression(expressionTree);
  }

  private void checkReturnedExpression(ExpressionTree expression) {
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expression;
      if (isThis(mse.expression())) {
        checkReturnedExpression(mse.identifier());
      }
    }
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) expression;
      if (identifierTree.symbol().isPrivate() && !isOnlyAssignedImmutableVariable((Symbol.VariableSymbol) identifierTree.symbol())) {
        context.reportIssue(this, identifierTree, "Return a copy of \"" + identifierTree.name() + "\".");
      }
    }
  }

  private static boolean isThis(ExpressionTree expression) {
    return expression.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) expression).name().equals("this");
  }

  private static boolean isOnlyAssignedImmutableVariable(Symbol.VariableSymbol symbol) {
    VariableTree declaration = symbol.declaration();
    if (declaration != null) {
      ExpressionTree initializer = declaration.initializer();
      if (initializer != null) {
        boolean isInitializerImmutable = !isMutableType(initializer) || isEmptyArray(initializer);
        if (symbol.isFinal() || !isInitializerImmutable) {
          // If the symbol is final or it is assigned something mutable, no need to look at re-assignment:
          // we already know if it is immutable or not.
          return isInitializerImmutable;
        }
      }
    }

    return !assignementsOfMutableType(symbol.usages());
  }

  private static boolean isEmptyArray(ExpressionTree initializer) {
    return initializer.is(Tree.Kind.NEW_ARRAY) &&
      !((NewArrayTree) initializer).dimensions().isEmpty() &&
      ((NewArrayTree) initializer).dimensions().stream().allMatch(adt -> isZeroLiteralValue(adt.expression()));
  }

  private static boolean isZeroLiteralValue(@Nullable ExpressionTree expressionTree) {
    if (expressionTree == null) {
      return false;
    }
    Integer integer = LiteralUtils.intLiteralValue(expressionTree);
    return integer != null && integer == 0;
  }

  private static boolean assignementsOfMutableType(List<IdentifierTree> usages) {
    for (IdentifierTree usage : usages) {
      Tree current = usage;
      Tree parent = usage.parent();
      do {
        if (parent.is(Tree.Kind.ASSIGNMENT)) {
          break;
        }
        current = parent;
        parent = current.parent();
      } while (parent != null);
      if (parent != null) {
        AssignmentExpressionTree assignment = (AssignmentExpressionTree) parent;
        if (assignment.variable().equals(current) && isMutableType(assignment.expression())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isMutableType(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.NULL_LITERAL)) {
      // In case of incomplete semantic, working with "nulltype" returns strange results, we can return early as the null will never be mutable anyway.
      return false;
    }
    if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) expressionTree;
      if (UNMODIFIABLE_COLLECTION_CALL.matches(methodInvocationTree) || (isUnmodifiableCollector(methodInvocationTree))) {
        return false;
      }
    }
    return isMutableType(expressionTree.symbolType());
  }

  private static boolean isUnmodifiableCollector(MethodInvocationTree methodInvocationTree) {
    if (STREAM_COLLECT_CALL.matches(methodInvocationTree) && methodInvocationTree.arguments().get(0).is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree collector = (MethodInvocationTree) methodInvocationTree.arguments().get(0);
      return UNMODIFIABLE_COLLECTOR_CALL.matches(collector);
    }
    return false;
  }

  private static boolean isMutableType(Type type) {
    if (type.isArray()) {
      return true;
    }
    for (String mutableType : MUTABLE_TYPES) {
      if (type.isSubtypeOf(mutableType) && isNotImmutable(type)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isNotImmutable(Type type) {
    for (String immutableType : IMMUTABLE_TYPES) {
      if (type.isSubtypeOf(immutableType)) {
        return false;
      }
    }
    return true;
  }

  public static boolean containsImmutableLikeTerm(String methodName) {
    String lowerCaseName = methodName.toLowerCase(Locale.ROOT);
    return lowerCaseName.contains("unmodifiable") || lowerCaseName.contains("immutable");
  }

}
