/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.symexec;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.FALSE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.TRUE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.UNKNOWN;

public class ExpressionEvaluatorVisitorTest {

  private CompilationUnitTree compilationUnit;

  @Test
  public void test_conditional_and() {
    // evaluation
    validateLogicalOperator("false && false", FALSE);
    validateLogicalOperator("false && true", FALSE);
    validateLogicalOperator("false && local2", FALSE);
    validateLogicalOperator("true && false", FALSE);
    validateLogicalOperator("true && true", TRUE);
    validateLogicalOperator("true && local2", UNKNOWN);
    validateLogicalOperator("local1 && false", FALSE);
    validateLogicalOperator("local1 && true", UNKNOWN);
    validateLogicalOperator("local1 && local", UNKNOWN);

    // number of spawned states
    validateLogicalOperator("false && false", 0, 1);
    validateLogicalOperator("false && true", 0, 1);
    validateLogicalOperator("false && local2", 0, 1);
    validateLogicalOperator("true && false", 0, 1);
    validateLogicalOperator("true && true", 1, 0);
    validateLogicalOperator("true && local2", 1, 1);
    validateLogicalOperator("local1 && false", 0, 2);
    validateLogicalOperator("local1 && true", 1, 1);
    validateLogicalOperator("local1 && local2", 1, 2);
  }

  @Test
  public void test_conditional_or() {
    // evaluation
    validateLogicalOperator("false || false", FALSE);
    validateLogicalOperator("false || true", TRUE);
    validateLogicalOperator("false || local2", UNKNOWN);
    validateLogicalOperator("true || false", TRUE);
    validateLogicalOperator("true || true", TRUE);
    validateLogicalOperator("true || local2", TRUE);
    validateLogicalOperator("local1 || false", UNKNOWN);
    validateLogicalOperator("local1 || true", TRUE);
    validateLogicalOperator("local1 || local", UNKNOWN);

    // number of spawned states
    validateLogicalOperator("false || false", 0, 1);
    validateLogicalOperator("false || true", 1, 0);
    validateLogicalOperator("false || local2", 1, 1);
    validateLogicalOperator("true || false", 1, 0);
    validateLogicalOperator("true || true", 1, 0);
    validateLogicalOperator("true || local2", 1, 0);
    validateLogicalOperator("local1 || false", 1, 1);
    validateLogicalOperator("local1 || true", 2, 0);
    validateLogicalOperator("local1 || local2", 2, 1);
  }

  private void validateLogicalOperator(String input, SymbolicBooleanConstraint expectedResult) {
    validateEvaluation(new ExecutionState(), parse(input), expectedResult == TRUE, expectedResult == FALSE);
  }

  private void validateLogicalOperator(String input, int trueCount, int falseCount) {
    ExpressionEvaluatorVisitor visitor = new ExpressionEvaluatorVisitor(new ExecutionState(), parse(input));
    assertThat(visitor.falseStates.size()).isSameAs(falseCount);
    assertThat(visitor.trueStates.size()).isSameAs(trueCount);
  }

  @Test
  public void test_identifier() {
    ExecutionState state = new ExecutionState();
    ExpressionTree identifierTree = analyze("local1");

    // no constraint. must spawn a true state and false state with constraints
    validateEvaluationUnknownWithConstraints(state, identifierTree, local1Symbol());

    // unconditionally evaluates to false if there is already a false constraint
    state.setBooleanConstraint(local1Symbol(), FALSE);
    validateEvaluationFalseWithoutConstraints(state, identifierTree);

    // unconditionally evaluates to true if there is already a true constraint
    state.setBooleanConstraint(local1Symbol(), TRUE);
    validateEvaluationTrueWithoutConstraints(state, identifierTree);

    // unknown constraint. must spawn a true state and false state with constraints
    state.setBooleanConstraint(local1Symbol(), UNKNOWN);
    validateEvaluationUnknownWithConstraints(state, identifierTree, local1Symbol());

    validateEvaluationUnknownWithoutConstraints(new ExecutionState(), analyze("field1"));
  }

  @Test
  public void test_instanceof() {
    validateEvaluationUnknownWithoutConstraints(new ExecutionState(), parse("local1 instanceof Object"));
  }

  @Test
  public void test_literal() {
    validateEvaluationFalseWithoutConstraints(new ExecutionState(), parse("false"));
    validateEvaluationUnknownWithoutConstraints(new ExecutionState(), parse("null"));
    validateEvaluationTrueWithoutConstraints(new ExecutionState(), parse("true"));
  }

  @Test
  public void test_logical_not() {
    ExpressionTree logicalNotTree = analyze("!local1");
    ExecutionState state = new ExecutionState();

    // result is unknown without constraint.
    validateEvaluationUnknownWithConstraints(state, logicalNotTree, local1Symbol(), FALSE);

    state.setBooleanConstraint(local1Symbol(), FALSE);
    validateEvaluationTrueWithoutConstraints(state, logicalNotTree);

    state.setBooleanConstraint(local1Symbol(), TRUE);
    validateEvaluationFalseWithoutConstraints(state, logicalNotTree);

    // result is unknown with unknown constraint.
    state.setBooleanConstraint(local1Symbol(), UNKNOWN);
    validateEvaluationUnknownWithConstraints(state, logicalNotTree, local1Symbol(), FALSE);
  }

  @Test
  public void test_member_select() {
    validateEvaluationUnknownWithoutConstraints(new ExecutionState(), parse("expression.identifier"));
  }

  @Test
  public void test_method_invocation() {
    validateEvaluationUnknownWithoutConstraints(new ExecutionState(), parse("method()"));
  }

  @Test
  public void test_relational() {
    // tests registration of relations in the spawned true and false states.
    evaluateRelationalOperator("local1 > local2", SymbolicRelation.GREATER_THAN, SymbolicRelation.LESS_EQUAL);
    evaluateRelationalOperator("local1 >= local2", SymbolicRelation.GREATER_EQUAL, SymbolicRelation.LESS_THAN);
    evaluateRelationalOperator("local1 == local2", SymbolicRelation.EQUAL_TO, SymbolicRelation.NOT_EQUAL);
    evaluateRelationalOperator("local1 < local2", SymbolicRelation.LESS_THAN, SymbolicRelation.GREATER_EQUAL);
    evaluateRelationalOperator("local1 <= local2", SymbolicRelation.LESS_EQUAL, SymbolicRelation.GREATER_THAN);
    evaluateRelationalOperator("local1 != local2", SymbolicRelation.NOT_EQUAL, SymbolicRelation.EQUAL_TO);

    ExecutionState state = new ExecutionState();

    // no constraint must be registered if there is a field.
    validateEvaluationUnknownWithoutConstraints(state, analyze("field1 != field2"));
    validateEvaluationUnknownWithoutConstraints(state, analyze("field1 != local2"));
    validateEvaluationUnknownWithoutConstraints(state, analyze("local1 != field2"));

    ExpressionTree notEqualTree = analyze("local1 != local2");

    // under constraint local1 != local2 evaluation or local1 != local2 must return unconditionally true
    state.setRelation(local1Symbol(), SymbolicRelation.NOT_EQUAL, local2Symbol());
    validateEvaluationTrueWithoutConstraints(state, notEqualTree);

    // under constraint local1 == local2 evaluation or local1 == local2 must return unconditionally false
    state.setRelation(local1Symbol(), SymbolicRelation.EQUAL_TO, local2Symbol());
    validateEvaluationFalseWithoutConstraints(state, notEqualTree);

    // comparison must not fail if either or both operands are not identifiers.
    new ExpressionEvaluatorVisitor(state, parse("null == null"));
  }

  private void evaluateRelationalOperator(String input, @Nullable SymbolicRelation trueRelation, @Nullable SymbolicRelation falseRelation) {
    evaluateRelationalOperator(new ExecutionState(), analyze(input), trueRelation, falseRelation);
  }

  private ExpressionEvaluatorVisitor evaluateRelationalOperator(ExecutionState state, Tree tree, @Nullable SymbolicRelation trueRelation,
    @Nullable SymbolicRelation falseRelation) {
    ExpressionEvaluatorVisitor result = new ExpressionEvaluatorVisitor(state, tree);
    if (falseRelation != null) {
      assertThat(result.falseStates.size()).isEqualTo(1);
      assertThat(result.falseStates.get(0).relations.get(local1Symbol(), local2Symbol())).isSameAs(falseRelation);
      assertThat(result.falseStates.get(0).relations.get(local2Symbol(), local1Symbol())).isSameAs(falseRelation.swap());
    } else {
      assertThat(result.falseStates.size()).isEqualTo(0);
    }
    if (trueRelation != null) {
      assertThat(result.trueStates.size()).isEqualTo(1);
      assertThat(result.trueStates.get(0).relations.get(local1Symbol(), local2Symbol())).isSameAs(trueRelation);
      assertThat(result.trueStates.get(0).relations.get(local2Symbol(), local1Symbol())).isSameAs(trueRelation.swap());
    } else {
      assertThat(result.trueStates.size()).isEqualTo(0);
    }
    return result;
  }

  @Test
  public void test_unary() {
    validateEvaluationUnknownWithoutConstraints(new ExecutionState(), parse("+local"));
  }

  private ExpressionEvaluatorVisitor validateEvaluation(ExecutionState state, Tree tree, boolean isAlwaysTrue, boolean isAlwaysFalse) {
    ExpressionEvaluatorVisitor visitor = new ExpressionEvaluatorVisitor(state, tree);
    assertThat(visitor.isAlwaysFalse()).isSameAs(isAlwaysFalse);
    assertThat(visitor.isAwlaysTrue()).isSameAs(isAlwaysTrue);
    return visitor;
  }

  private void validateEvaluationFalseWithoutConstraints(ExecutionState state, Tree tree) {
    ExpressionEvaluatorVisitor visitor = validateEvaluation(state, tree, false, true);
    assertThat(visitor.falseStates.get(0)).isSameAs(state);
    assertThat(visitor.trueStates).isEmpty();
  }

  private void validateEvaluationTrueWithoutConstraints(ExecutionState state, Tree tree) {
    ExpressionEvaluatorVisitor visitor = validateEvaluation(state, tree, true, false);
    assertThat(visitor.falseStates).isEmpty();
    assertThat(visitor.trueStates.get(0)).isSameAs(state);
  }

  private void validateEvaluationUnknownWithConstraints(ExecutionState state, Tree tree, Symbol.VariableSymbol constrainedSymbol) {
    validateEvaluationUnknownWithConstraints(state, tree, constrainedSymbol, TRUE);
  }

  private void validateEvaluationUnknownWithConstraints(ExecutionState state, Tree tree, Symbol.VariableSymbol constrainedSymbol, SymbolicBooleanConstraint trueConstraint) {
    ExpressionEvaluatorVisitor visitor = validateEvaluation(state, tree, false, false);
    assertThat(visitor.falseStates.get(0).constraints.size()).isEqualTo(1);
    assertThat(visitor.falseStates.get(0).getBooleanConstraint(constrainedSymbol)).isSameAs(trueConstraint.negate());
    assertThat(visitor.trueStates.get(0).constraints.size()).isEqualTo(1);
    assertThat(visitor.trueStates.get(0).getBooleanConstraint(constrainedSymbol)).isSameAs(trueConstraint);
  }

  private void validateEvaluationUnknownWithoutConstraints(ExecutionState state, Tree tree) {
    ExpressionEvaluatorVisitor visitor = validateEvaluation(state, tree, false, false);
    assertThat(visitor.falseStates.get(0).constraints.size()).isEqualTo(0);
    assertThat(visitor.falseStates.get(0).relations.size()).isEqualTo(0);
    assertThat(visitor.trueStates.get(0).constraints.size()).isEqualTo(0);
    assertThat(visitor.trueStates.get(0).relations.size()).isEqualTo(0);
  }

  private ExpressionTree analyze(String input) {
    ExpressionTree result = parse(input);
    SemanticModel.createFor(compilationUnit, ImmutableList.<File>of());
    return result;
  }

  private ExpressionTree parse(String input) {
    String p = "class Test { boolean field1; boolean field2; void wrapperMethod(boolean local1, boolean local2) { " + input + "; } }";
    compilationUnit = (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(p);
    return ((ExpressionStatementTree) ((MethodTree) ((ClassTree) compilationUnit.types().get(0)).members().get(2)).block().body().get(0)).expression();
  }

  private Symbol.VariableSymbol local1Symbol() {
    return (Symbol.VariableSymbol) ((MethodTree) ((ClassTree) compilationUnit.types().get(0)).members().get(2)).parameters().get(0).symbol();
  }

  private Symbol.VariableSymbol local2Symbol() {
    return (Symbol.VariableSymbol) ((MethodTree) ((ClassTree) compilationUnit.types().get(0)).members().get(2)).parameters().get(1).symbol();
  }

}
