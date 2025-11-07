/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.helpers;

import java.lang.reflect.Constructor;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.*;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;
import static org.assertj.core.api.Assertions.assertThat;

class ReassignmentFinderTest extends JParserTestUtils {

  @Test
  void private_constructor() throws Exception {
    assertThat(isFinal(ReassignmentFinder.class.getModifiers())).isTrue();
    Constructor<ReassignmentFinder> constructor = ReassignmentFinder.class.getDeclaredConstructor();
    assertThat(isPrivate(constructor.getModifiers())).isTrue();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  void parameter() {
    String code = newCode(
      "int foo(int a) {",
      "  return a;",
      "}");

    MethodTree method = methodTree(code);
    List<StatementTree> statements = method.block().body();
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(statements, null);
  }

  @Test
  void parameter_with_usage() {
    String code = newCode(
      "int foo(boolean test) {",
      "  if (test) {}",
      "  return test;",
      "}");

    MethodTree method = methodTree(code);
    List<StatementTree> statements = method.block().body();
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(statements, null);
  }

  @Test
  void declaration() {
    String code = newCode(
      "int foo() {",
      "  int a = 0;",
      "  return a;",
      "}");

    List<StatementTree> statements = methodBody(code);
    ExpressionTree aDeclarationInitializer = initializerFromVariableDeclarationStatement(statements.get(0));
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(statements, aDeclarationInitializer);
  }

  @Test
  void unknown_variable() {
    String code = newCode(
      "int foo() {",
      "  return a;",
      "}");

    List<StatementTree> statements = methodBody(code);
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(statements, null);
  }

  @Test
  void array_declaration() {
    String code = newCode(
      "int foo() {",
      "  int a[] = new int[42];",
      "  a[0] = 42;",
      "  return a;",
      "}");

    List<StatementTree> statements = methodBody(code);
    ExpressionTree arrayAssignmentExpression = initializerFromVariableDeclarationStatement(statements.get(0));
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(statements, arrayAssignmentExpression);
  }

  @Test
  void assignment() {
    String code = newCode(
      "int foo() {",
      "  int a;",
      "  a = 0;",
      "  return a;",
      "}");

    List<StatementTree> statements = methodBody(code);
    ExpressionTree aAssignmentExpression = assignmentExpressionFromStatement(statements.get(1));
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(statements, aAssignmentExpression);
  }

  @Test
  void assignment_with_other_variable() {
    String code = newCode(
      "int foo() {",
      "  int a;",
      "  int b;",
      "  a = 0;",
      "  b = 0;",
      "  return a;",
      "}");

    List<StatementTree> statements = methodBody(code);
    ExpressionTree aAssignmentExpression = assignmentExpressionFromStatement(statements.get(2));
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(statements, aAssignmentExpression);
  }

  @Test
  void assignment_with_parenthesis() {
    String code = newCode(
      "int foo() {",
      "  int a;",
      "  a = 0;",
      "  int b = ((a));",
      "  return a;",
      "}");

    List<StatementTree> statements = methodBody(code);
    ExpressionTree aAssignmentExpression = assignmentExpressionFromStatement(statements.get(1));
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(statements, aAssignmentExpression);
  }

  @Test
  void last_assignment() {
    String code = newCode(
      "int foo() {",
      "  int a;",
      "  a = 0;",
      "  a = 1;",
      "  return a;",
      "}");

    List<StatementTree> statements = methodBody(code);
    ExpressionTree secondAssignment = assignmentExpressionFromStatement(statements.get(2));
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(statements, secondAssignment);
  }

  @Test
  void last_assignment_on_same_line() {
    String code = newCode(
      "int foo() {",
      "  int a;",
      "  a = 0;",
      "  a = 1; return a;",
      "}");

    List<StatementTree> statements = methodBody(code);
    ExpressionTree secondAssignment = assignmentExpressionFromStatement(statements.get(2));
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(statements, secondAssignment);
  }

  @Test
  void outside_method() {
    String code = newCode(
      "int b;",
      "int foo() {",
      "  return b;",
      "}");

    ClassTree classTree = classTree(code);
    List<StatementTree> statements = ((MethodTree) classTree.members().get(1)).block().body();
    ExpressionTree variableDeclaration = ((VariableTree) (classTree.members().get(0))).initializer();
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(statements, variableDeclaration);
  }

  @Test
  void in_enum() {
    String code = newCode(
      "enum E { E1 {} }",
      "E foo() {",
      "  return E.E1;",
      "}");
    ClassTree classTree = classTree(code);
    ClassTree enumClass = (ClassTree) classTree.members().get(0);
    VariableTree constant = (VariableTree) enumClass.members().get(0);
    List<StatementTree> statements = ((MethodTree) classTree.members().get(1)).block().body();
    ReturnStatementTree returnStatement = (ReturnStatementTree) statements.get(0);
    assertThat(ReassignmentFinder.getClosestReassignmentOrDeclarationExpression(returnStatement.expression(), constant.symbol()))
      .isEqualTo(constant.initializer());
  }

  @Test
  void ignore_assignation_after_starting_point() {
    String code = newCode(
      "int foo() {",
      "  int b = 0;",
      "  doSomething(b);",
      "  b = 1;",
      "}");

    List<StatementTree> statements = methodBody(code);
    Tree expectedVariableDeclaration = initializerFromVariableDeclarationStatement(statements.get(0));
    MethodInvocationTree startingPoint = (MethodInvocationTree) ((ExpressionStatementTree) statements.get(1)).expression();
    Symbol searchedVariable = ((IdentifierTree) startingPoint.arguments().get(0)).symbol();
    assertThatLastReassignmentsOfVariableIsEqualTo(searchedVariable, startingPoint, expectedVariableDeclaration);
  }

  @Test
  void ignore_assignation_after_starting_point_same_line() {
    String code = newCode(
      "int foo() {",
      "  int b = 0;",
      "  doSomething(b); b = 1;",
      "}");

    List<StatementTree> statements = methodBody(code);
    Tree expectedVariableDeclaration = initializerFromVariableDeclarationStatement(statements.get(0));
    MethodInvocationTree startingPoint = (MethodInvocationTree) ((ExpressionStatementTree) statements.get(1)).expression();
    Symbol searchedVariable = ((IdentifierTree) startingPoint.arguments().get(0)).symbol();
    assertThatLastReassignmentsOfVariableIsEqualTo(searchedVariable, startingPoint, expectedVariableDeclaration);
  }

  private static void assertThatLastReassignmentsOfVariableIsEqualTo(Symbol searchedVariable, Tree startingPoint, Tree expectedVariableDeclaration) {
    assertThat(ReassignmentFinder.getClosestReassignmentOrDeclarationExpression(startingPoint, searchedVariable)).isEqualTo(expectedVariableDeclaration);
  }

  @Test
  void known_limitation() {
    String code = newCode(
      "int foo(boolean test) {",
      "  int a;",
      "  if (test) {",
      "    a = 0;",
      "  } else {",
      "    a = 1;", // Should have returned both thenAssignment and elseAssignment. CFG?
      "  }",
      "  return a;",
      "}");

    List<StatementTree> statements = methodBody(code);
    StatementTree elseAssignment = ((BlockTree) ((IfStatementTree) statements.get(1)).elseStatement()).body().get(0);
    ExpressionTree expression = assignmentExpressionFromStatement(elseAssignment);
    assertThatLastReassignmentsOfReturnedVariableIsEqualTo(statements, expression);
  }

  private static void assertThatLastReassignmentsOfReturnedVariableIsEqualTo(List<StatementTree> statements, ExpressionTree target) {
    assertThat(getLastReassignment(statements)).isEqualTo(target);
  }

  private static Tree getLastReassignment(List<StatementTree> statements) {
    return ReassignmentFinder.getClosestReassignmentOrDeclarationExpression(statements.get(statements.size() - 1), variableFromLastReturnStatement(statements).symbol());
  }

}
