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
package org.sonar.java.checks.helpers;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.ReassignmentFinder.getInitializerOrExpression;
import static org.sonar.java.checks.helpers.ReassignmentFinder.getReassignments;

public class ExpressionsHelper {

  private ExpressionsHelper() {
  }

  public static String concatenate(@Nullable ExpressionTree tree) {
    if (tree == null) {
      return "";
    }
    Deque<String> pieces = new LinkedList<>();
    ExpressionTree expr = tree;
    while (expr.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expr;
      pieces.push(mse.identifier().name());
      pieces.push(".");
      expr = mse.expression();
    }
    if (expr.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree idt = (IdentifierTree) expr;
      pieces.push(idt.name());
    }

    StringBuilder sb = new StringBuilder();
    for (String piece : pieces) {
      sb.append(piece);
    }
    return sb.toString();
  }

  /**
   * Return the correct tree to report on for class trees.
   * @param classTree class tree raising an issue.
   * @return simple name of class tree or identifier in parent expression for anonymous class.
   */
  public static TypeTree reportOnClassTree(ClassTree classTree) {
    TypeTree reportTree = classTree.simpleName();
    if (reportTree == null) {
      return ((NewClassTree) classTree.parent()).identifier();
    }
    return reportTree;
  }

  public static ValueResolution<String> getConstantValueAsString(ExpressionTree expression) {
    return getConstantValueAsString(expression, "");
  }

  public static ValueResolution<String> getConstantValueAsString(ExpressionTree expression, String locationMessage) {
    return valueResolution(expression, expr -> expr.asConstant(String.class), new ValueResolution<>(locationMessage));
  }

  public static ValueResolution<Boolean> getConstantValueAsBoolean(ExpressionTree expression) {
    return valueResolution(expression, expr -> expr.asConstant(Boolean.class), new ValueResolution<>());
  }

  private static <T> ValueResolution<T> valueResolution(ExpressionTree expression, Function<ExpressionTree, Optional<T>> resolver, ValueResolution<T> valueResolution) {
    Optional<T> value = resolver.apply(expression);
    if (!value.isPresent() && expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol();
      ExpressionTree singleWriteUsage = getSingleWriteUsage(symbol);
      if (singleWriteUsage != null && !valueResolution.evaluatedSymbols.contains(symbol)) {
        valueResolution.addLocation(singleWriteUsage, symbol);
        return valueResolution(singleWriteUsage, resolver, valueResolution);
      }
    }
    valueResolution.value = value.orElse(null);
    return valueResolution;
  }

  @CheckForNull
  public static ExpressionTree getSingleWriteUsage(Symbol symbol) {
    ExpressionTree initializerOrExpression = getInitializerOrExpression(symbol.declaration());
    List<AssignmentExpressionTree> reassignments = getReassignments(symbol.owner().declaration(), symbol.usages());
    ExpressionTree singleWriteUsage = null;
    if (initializerOrExpression == null && reassignments.size() == 1) {
      singleWriteUsage = reassignments.get(0).expression();
    }
    if (initializerOrExpression != null && reassignments.isEmpty()) {
      singleWriteUsage = initializerOrExpression;
    }
    if (singleWriteUsage != null && isStrictAssignmentOrDeclaration(singleWriteUsage)) {
      return singleWriteUsage;
    }
    return null;
  }

  private static boolean isStrictAssignmentOrDeclaration(ExpressionTree expression) {
    if (expression.parent() instanceof AssignmentExpressionTree) {
      return expression.parent().is(Tree.Kind.ASSIGNMENT);
    }
    return true;
  }

  public static class ValueResolution<T> {
    private T value;
    private List<JavaFileScannerContext.Location> valuePath = new ArrayList<>();
    private Set<Symbol> evaluatedSymbols = new HashSet<>();
    private final String locationMessage;

    public ValueResolution() {
      this("");
    }

    public ValueResolution(String locationMessage) {
      this.locationMessage = locationMessage;
    }

    private void addLocation(ExpressionTree expressionTree, Symbol evaluatedSymbol) {
      evaluatedSymbols.add(evaluatedSymbol);
      valuePath.add(new JavaFileScannerContext.Location(locationMessage, expressionTree));
    }

    @CheckForNull
    public T value() {
      return value;
    }

    public List<JavaFileScannerContext.Location> valuePath() {
      return valuePath;
    }
  }

  public static boolean isNotSerializable(ExpressionTree expression) {
    Type symbolType = expression.symbolType();
    if (symbolType.isUnknown()) {
      return false;
    }
    return isNonSerializable(symbolType)
      || isAssignedToNonSerializable(expression);
  }

  private static boolean isNonSerializable(Type type) {
    if (type.isArray()) {
      return isNonSerializable(((Type.ArrayType) type).elementType());
    }
    if (type.typeArguments().stream().anyMatch(ExpressionsHelper::isNonSerializable)) {
      return true;
    }
    if (type.isPrimitive() ||
      type.is("java.lang.Object") ||
      type.isSubtypeOf("java.io.Serializable")) {
      return false;
    }
    // note: this is assuming that custom implementors of Collection
    // have the good sense to make it serializable just like all implementations in the JDK
    if (type.isSubtypeOf("java.lang.Iterable") ||
      type.isSubtypeOf("java.util.Map") ||
      type.isSubtypeOf("java.util.Enumeration")) {
      return false;
    }
    Type erasedType = type.erasure();
    return erasedType.equals(type) || isNonSerializable(erasedType);
  }

  private static boolean isAssignedToNonSerializable(ExpressionTree expression) {
    return ExpressionUtils.extractIdentifierSymbol(expression)
      .filter(symbol -> initializedAndAssignedExpressionStream(symbol)
        .map(ExpressionTree::symbolType)
        .filter(Predicate.not(Type::isUnknown))
        .anyMatch(ExpressionsHelper::isNonSerializable))
      .isPresent();
  }

  public static Stream<ExpressionTree> initializedAndAssignedExpressionStream(Symbol symbol) {
    Tree declaration = symbol.declaration();
    if (declaration == null) {
      return Stream.empty();
    }
    Stream<ExpressionTree> assignedExpressionStream = getReassignments(declaration, symbol.usages()).stream()
      .map(AssignmentExpressionTree::expression);
    ExpressionTree initializer = getInitializerOrExpression(declaration);
    if (initializer == null) {
      return assignedExpressionStream;
    } else {
      return Stream.concat(Stream.of(initializer), assignedExpressionStream);
    }
  }

  public static boolean alwaysReturnSameValue(ExpressionTree expression) {
    if (expression.is(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS)) {
      // Methods or constructors invoked two times can return different values.
      return false;
    } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      return alwaysReturnSameValue(((MemberSelectExpressionTree) expression).expression());
    } else if (expression.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      return alwaysReturnSameValue(((ParenthesizedTree) expression).expression());
    } else if (expression.is(Tree.Kind.NEW_ARRAY)) {
      return ((NewArrayTree) expression).initializers().stream().allMatch(ExpressionsHelper::alwaysReturnSameValue);
    }
    return true;
  }

  public static Optional<Symbol> getInvokedSymbol(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        return Optional.of(((IdentifierTree) expression).symbol());
      }
    }
    return Optional.empty();
  }

  public static boolean isNotReassigned(Symbol symbol) {
    return symbol.isFinal() || (symbol.isVariableSymbol() && ((Symbol.VariableSymbol) symbol).isEffectivelyFinal());
  }

  public static List<ExpressionTree> getIdentifierAssignments(IdentifierTree identifier) {
    List<ExpressionTree> assignments = new ArrayList<>();
    Symbol symbol = identifier.symbol();
    VariableTree variable = (VariableTree) symbol.declaration();
    if(variable.initializer() != null) {
      assignments.add(variable.initializer());
    }
    getReassignments(variable, symbol.usages()).stream()
      .map(AssignmentExpressionTree::expression)
      .forEach(assignments::add);
    return assignments;
  }

}
