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
package org.sonar.java.checks.helpers;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.ReassignmentFinder.getInitializerOrExpression;
import static org.sonar.java.checks.helpers.ReassignmentFinder.getReassignments;

public class ExpressionsHelper {

  private ExpressionsHelper() {
  }

  public static String concatenate(@Nullable ExpressionTree tree) {
    if(tree == null) {
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
    for (String piece: pieces) {
      sb.append(piece);
    }
    return sb.toString();
  }

  /**
   * Return the correct tree to report on for class trees.
   * @param classTree class tree raising an issue.
   * @return simple name of class tree or identifier in parent expression for anonymous class.
   */
  public static Tree reportOnClassTree(ClassTree classTree) {
    Tree reportTree = classTree.simpleName();
    if(reportTree == null) {
      reportTree = ((NewClassTree) classTree.parent()).identifier();
    }
    return reportTree;
  }


  public static ValueResolution getConstantValue(ExpressionTree expression) {
    return valueResolution(expression, ConstantUtils::resolveAsConstant, new ValueResolution<>());
  }

  public static ValueResolution<String> getConstantValueAsString(ExpressionTree expression) {
    return valueResolution(expression, ConstantUtils::resolveAsStringConstant, new ValueResolution<>());
  }

  public static ValueResolution<Boolean> getConstantValueAsBoolean(ExpressionTree expression) {
    return valueResolution(expression, ConstantUtils::resolveAsBooleanConstant, new ValueResolution<>());
  }

  public static ValueResolution<Integer> getConstantValueAsInteger(ExpressionTree expression) {
    return valueResolution(expression, ConstantUtils::resolveAsIntConstant, new ValueResolution<>());
  }

  private static <T> ValueResolution<T> valueResolution(ExpressionTree expression, Function<ExpressionTree,T> resolver, ValueResolution<T> valueResolution) {
    T value = resolver.apply(expression);
    if (value == null && expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) expression).symbol();
      ExpressionTree singleWriteUsage = getSingleWriteUsage(symbol);
      if (singleWriteUsage != null && !valueResolution.evaluatedSymbols.contains(symbol)) {
        valueResolution.addLocation(singleWriteUsage, symbol);
        return valueResolution(singleWriteUsage, resolver, valueResolution);
      }
    }
    valueResolution.value = value;
    return valueResolution;
  }

  @CheckForNull
  private static ExpressionTree getSingleWriteUsage(Symbol symbol) {
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

    private void addLocation(ExpressionTree expressionTree, Symbol evaluatedSymbol) {
      evaluatedSymbols.add(evaluatedSymbol);
      valuePath.add(new JavaFileScannerContext.Location("", expressionTree));
    }

    @CheckForNull
    public T value() {
      return value;
    }

    public List<JavaFileScannerContext.Location> valuePath() {
      return valuePath;
    }
  }

}
