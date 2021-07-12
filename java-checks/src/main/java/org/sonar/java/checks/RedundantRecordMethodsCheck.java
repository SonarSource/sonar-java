/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6207")
public class RedundantRecordMethodsCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.RECORD);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree targetRecord = (ClassTree) tree;

    List<Symbol.VariableSymbol> components = targetRecord.recordComponents().stream()
      .map(component -> (Symbol.VariableSymbol) component.symbol())
      .collect(Collectors.toList());
    Set<String> componentNames = components.stream()
      .map(Symbol.VariableSymbol::name)
      .collect(Collectors.toSet());

    for (Tree member : targetRecord.members()) {
      if (member.is(Tree.Kind.CONSTRUCTOR)) {
        checkConstructor((MethodTree) member, components);
      } else if (member.is(Tree.Kind.METHOD)) {
        checkMethod((MethodTree) member, components, componentNames);
      }
    }
  }

  private void checkConstructor(MethodTree constructor, List<Symbol.VariableSymbol> components) {
    if (constructor.block().body().isEmpty() || onlyDoesSimpleAssignments(constructor, components)) {
      reportIssue(constructor.simpleName(), "Remove this redundant constructor which is the same as a default one.");
    }
  }

  private void checkMethod(MethodTree method, List<Symbol.VariableSymbol> components, Set<String> componentsByName) {
    String methodName = method.symbol().name();
    if (!componentsByName.contains(methodName)) {
      return;
    }
    if (onlyReturnsRawValue(method, components)) {
      reportIssue(method.simpleName(), "Remove this redundant method which is the same as a default one.");
    }
  }

  public static boolean onlyReturnsRawValue(MethodTree method, Collection<Symbol.VariableSymbol> components) {
    Optional<ReturnStatementTree> returnStatement = getFirstReturnStatement(method);
    if (!returnStatement.isPresent()) {
      return false;
    }
    ExpressionTree expression = returnStatement.get().expression();
    Symbol identifierSymbol;
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      identifierSymbol = ((IdentifierTree) expression).symbol();
    } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      identifierSymbol = (((MemberSelectExpressionTree) expression).identifier()).symbol();
    } else {
      return false;
    }
    return components.stream().anyMatch(identifierSymbol::equals);
  }

  private static Optional<ReturnStatementTree> getFirstReturnStatement(MethodTree method) {
    return method.block().body().stream()
      .filter(statement -> statement.is(Tree.Kind.RETURN_STATEMENT))
      .map(ReturnStatementTree.class::cast)
      .findFirst();
  }

  public static boolean onlyDoesSimpleAssignments(MethodTree constructor, List<Symbol.VariableSymbol> components) {
    if (constructor.parameters().size() != components.size()) {
      return false;
    }
    List<Symbol.VariableSymbol> parameters = constructor.parameters().stream()
      .map(parameter -> (Symbol.VariableSymbol) parameter.symbol())
      .collect(Collectors.toList());
    List<AssignmentExpressionTree> assignments = extractAssignments(constructor.block().body());
    Set<Symbol> componentsAssignedInConstructor = new HashSet<>();
    for (AssignmentExpressionTree assignment : assignments) {
      Optional<Symbol.VariableSymbol> variableSymbol = assignsParameterToComponent(assignment, components, parameters);
      if (variableSymbol.isPresent()) {
        componentsAssignedInConstructor.add(variableSymbol.get());
      }
    }
    return componentsAssignedInConstructor.containsAll(components);
  }

  private static List<AssignmentExpressionTree> extractAssignments(List<StatementTree> statements) {
    return statements.stream()
      .map(RedundantRecordMethodsCheck::extractAssignment)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());
  }

  private static Optional<AssignmentExpressionTree> extractAssignment(StatementTree statement) {
    if (!statement.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      return Optional.empty();
    }
    ExpressionStatementTree initialStatement = (ExpressionStatementTree) statement;
    if (!initialStatement.expression().is(Tree.Kind.ASSIGNMENT)) {
      return Optional.empty();
    }
    return Optional.of((AssignmentExpressionTree) initialStatement.expression());
  }

  /**
   * Returns the symbol of a component used as the receiving end of the assignment if the matching parameter is used as the value.
   * @param assignment The assignment statement
   * @param components The components' symbols
   * @param parameters The parameters' symbols
   * @return Optional of the component's symbol receiving the matching parameter. Optional.empty() otherwise.
   */
  private static Optional<Symbol.VariableSymbol> assignsParameterToComponent(
    AssignmentExpressionTree assignment,
    List<Symbol.VariableSymbol> components,
    List<Symbol.VariableSymbol> parameters) {
    ExpressionTree leftHandSide = assignment.variable();
    if (!leftHandSide.is(Tree.Kind.MEMBER_SELECT)) {
      return Optional.empty();
    }
    Symbol variableSymbol = ((MemberSelectExpressionTree) leftHandSide).identifier().symbol();
    Optional<Symbol.VariableSymbol> component = components.stream()
      .filter(variableSymbol::equals)
      .findFirst();
    if (!component.isPresent()) {
      return Optional.empty();
    }
    ExpressionTree rightHandSide = assignment.expression();
    if (!rightHandSide.is(Tree.Kind.IDENTIFIER)) {
      return Optional.empty();
    }
    Symbol valueSymbol = ((IdentifierTree) rightHandSide).symbol();
    if (parameters.stream().anyMatch(valueSymbol::equals) && variableSymbol.name().equals(valueSymbol.name())) {
      return component;
    }
    return Optional.empty();
  }
}
