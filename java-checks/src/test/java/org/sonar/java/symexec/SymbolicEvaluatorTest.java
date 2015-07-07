/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.java.symexec.SymbolicEvaluator.PackedStatementStates;
import org.sonar.java.symexec.SymbolicEvaluator.PackedStates;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.FALSE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.TRUE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.UNKNOWN;

public class SymbolicEvaluatorTest {

  @Test
  public void test_expression_array() {
    assertThat(evaluateExpression("array[0] = 0;")).isSameAs(UNKNOWN);
  }

  @Test
  public void test_expression_assign() {
    // must evaluate to FALSE and set local1 to FALSE
    ExpressionTree tree = analyze("local1 = false;");
    ExecutionState state = new ExecutionState();
    assertThat(evaluateExpression(state, tree)).isSameAs(FALSE);
    assertThat(state.getBooleanConstraint(local1Symbol())).isSameAs(FALSE);

    // must evaluate to FALSE and set local2 to FALSE when local1 = false;
    ExpressionTree transitiveTree = analyze("local2 = local1;");
    ExecutionState transitiveState = new ExecutionState();
    transitiveState.setBooleanConstraint(local1Symbol(), FALSE);
    assertThat(evaluateExpression(transitiveState, transitiveTree)).isSameAs(FALSE);
    assertThat(transitiveState.getBooleanConstraint(local2Symbol())).isSameAs(FALSE);

    // must evaluate to UNKNOWN and set local1 to FALSE
    ExpressionTree nestedTree = analyze("array[(local1 = true) ? 1 : 0] = null;");
    ExecutionState nestedState = new ExecutionState();
    assertThat(evaluateExpression(nestedState, nestedTree)).isSameAs(UNKNOWN);
    assertThat(nestedState.getBooleanConstraint(local1Symbol())).isSameAs(TRUE);
  }

  @Test
  public void test_condition_conditional_and() {
    // evaluation
    assertThat(evaluateCondition("false && false").isAlwaysFalse()).isTrue();
    assertThat(evaluateCondition("false && true").isAlwaysFalse()).isTrue();
    assertThat(evaluateCondition("false && local2").isAlwaysFalse()).isTrue();
    assertThat(evaluateCondition("true && false").isAlwaysFalse()).isTrue();
    assertThat(evaluateCondition("true && true").isAlwaysTrue()).isTrue();
    assertThat(evaluateCondition("true && local2").isUnknown()).isTrue();
    assertThat(evaluateCondition("local1 && false").isAlwaysFalse()).isTrue();
    assertThat(evaluateCondition("local1 && true").isUnknown()).isTrue();
    assertThat(evaluateCondition("local1 && local").isUnknown()).isTrue();

    // number of spawned states
    assertOutputStates(evaluateCondition("false && false"), 0, 1, 0);
    assertOutputStates(evaluateCondition("false && true"), 0, 1, 0);
    assertOutputStates(evaluateCondition("false && local2"), 0, 1, 0);
    assertOutputStates(evaluateCondition("true && false"), 0, 1, 0);
    assertOutputStates(evaluateCondition("true && true"), 1, 0, 0);
    assertOutputStates(evaluateCondition("true && local2"), 1, 1, 0);
    assertOutputStates(evaluateCondition("local1 && false"), 0, 2, 0);
    assertOutputStates(evaluateCondition("local1 && true"), 1, 1, 0);
    assertOutputStates(evaluateCondition("local1 && local2"), 1, 2, 0);
  }

  @Test
  public void test_condition_conditional_or() {
    // evaluation
    assertThat(evaluateCondition("false || false").isAlwaysFalse()).isTrue();
    assertThat(evaluateCondition("false || true").isAlwaysTrue()).isTrue();
    assertThat(evaluateCondition("false || local2").isUnknown()).isTrue();
    assertThat(evaluateCondition("true || false").isAlwaysTrue()).isTrue();
    assertThat(evaluateCondition("true || true").isAlwaysTrue()).isTrue();
    assertThat(evaluateCondition("true || local2").isAlwaysTrue()).isTrue();
    assertThat(evaluateCondition("local1 || false").isUnknown()).isTrue();
    assertThat(evaluateCondition("local1 || true").isAlwaysTrue()).isTrue();
    assertThat(evaluateCondition("local1 || local").isUnknown()).isTrue();

    // number of spawned states
    assertOutputStates(evaluateCondition("false || false"), 0, 1, 0);
    assertOutputStates(evaluateCondition("false || true"), 1, 0, 0);
    assertOutputStates(evaluateCondition("false || local2"), 1, 1, 0);
    assertOutputStates(evaluateCondition("true || false"), 1, 0, 0);
    assertOutputStates(evaluateCondition("true || true"), 1, 0, 0);
    assertOutputStates(evaluateCondition("true || local2"), 1, 0, 0);
    assertOutputStates(evaluateCondition("local1 || false"), 1, 1, 0);
    assertOutputStates(evaluateCondition("local1 || true"), 2, 0, 0);
    assertOutputStates(evaluateCondition("local1 || local2"), 2, 1, 0);
  }

  @Test
  public void test_expression_conditional_and() {
    assertThat(evaluateExpression("false && false")).isSameAs(FALSE);
    assertThat(evaluateExpression("false && true")).isSameAs(FALSE);
    assertThat(evaluateExpression("false && local2")).isSameAs(FALSE);
    assertThat(evaluateExpression("true && false")).isSameAs(FALSE);
    assertThat(evaluateExpression("true && true")).isSameAs(TRUE);
    assertThat(evaluateExpression("true && local2")).isSameAs(UNKNOWN);
    assertThat(evaluateExpression("local1 && false")).isSameAs(FALSE);
    assertThat(evaluateExpression("local1 && true")).isSameAs(UNKNOWN);
    assertThat(evaluateExpression("local1 && local")).isSameAs(UNKNOWN);
  }

  @Test
  public void test_expression_conditional_or() {
    assertThat(evaluateExpression("false || false")).isSameAs(FALSE);
    assertThat(evaluateExpression("false || true")).isSameAs(TRUE);
    assertThat(evaluateExpression("false || local2")).isSameAs(UNKNOWN);
    assertThat(evaluateExpression("true || false")).isSameAs(TRUE);
    assertThat(evaluateExpression("true || true")).isSameAs(TRUE);
    assertThat(evaluateExpression("true || local2")).isSameAs(TRUE);
    assertThat(evaluateExpression("local1 || false")).isSameAs(UNKNOWN);
    assertThat(evaluateExpression("local1 || true")).isSameAs(TRUE);
    assertThat(evaluateExpression("local1 || local")).isSameAs(UNKNOWN);
  }

  @Test
  public void test_condition_identifier() {
    ExecutionState state = new ExecutionState();
    ExpressionTree identifierTree = analyze("local1");

    // no constraint. must spawn a true state and false state with constraints
    validateEvaluationUnknownWithConstraints(state, evaluateCondition(state, identifierTree), local1Symbol());

    // unconditionally evaluates to false if there is already a false constraint
    state.setBooleanConstraint(local1Symbol(), FALSE);
    validateEvaluationFalse(state, evaluateCondition(state, identifierTree));

    // unconditionally evaluates to true if there is already a true constraint
    state.setBooleanConstraint(local1Symbol(), TRUE);
    validateEvaluationTrue(state, evaluateCondition(state, identifierTree));

    // unknown constraint. must spawn a true state and false state with constraints
    state.setBooleanConstraint(local1Symbol(), UNKNOWN);
    validateEvaluationUnknownWithConstraints(state, evaluateCondition(state, identifierTree), local1Symbol());

    state = new ExecutionState();
    validateEvaluationUnknownWithConstraints(state, evaluateCondition(state, analyze("field1")), field1Symbol());
  }

  @Test
  public void test_expression_identifier() {
    ExpressionTree identifierTree = analyze("local1");

    // no constraint. must propagate as unknown state
    assertThat(evaluateExpression(identifierTree)).isSameAs(UNKNOWN);

    ExecutionState state = new ExecutionState();

    // unconditionally evaluates to false if there is already a false constraint
    state.setBooleanConstraint(local1Symbol(), FALSE);
    assertThat(evaluateExpression(state, identifierTree)).isSameAs(FALSE);

    // unconditionally evaluates to true if there is already a true constraint
    state.setBooleanConstraint(local1Symbol(), TRUE);
    assertThat(evaluateExpression(state, identifierTree)).isSameAs(TRUE);

    // unknown constraint. must spawn a true state and false state with constraints
    state.setBooleanConstraint(local1Symbol(), UNKNOWN);
    assertThat(evaluateExpression(state, identifierTree)).isSameAs(UNKNOWN);

    assertThat(evaluateExpression(analyze("field1"))).isSameAs(UNKNOWN);
  }

  @Test
  public void test_condition_instanceof() {
    ExecutionState state = new ExecutionState();
    validateEvaluationUnknownWithoutConstraints(state, evaluateCondition(state, parse("local1 instanceof Object")));
  }

  @Test
  public void test_expression_instanceof() {
    assertThat(evaluateExpression("local1 instanceof Object")).isSameAs(UNKNOWN);
  }

  @Test
  public void test_condition_literal() {
    ExecutionState state = new ExecutionState();
    validateEvaluationFalse(state, evaluateCondition(state, parse("false")));
    validateEvaluationUnknownWithoutConstraints(state, evaluateCondition(state, parse("null")));
    validateEvaluationTrue(state, evaluateCondition(state, parse("true")));
  }

  @Test
  public void test_expression_literal() {
    assertThat(evaluateExpression("false")).isSameAs(FALSE);
    assertThat(evaluateExpression("null")).isSameAs(UNKNOWN);
    assertThat(evaluateExpression("true")).isSameAs(TRUE);
  }

  @Test
  public void test_condition_logical_not() {
    ExpressionTree logicalNotTree = analyze("!local1");
    ExecutionState state = new ExecutionState();

    // result is unknown without constraint.
    validateEvaluationUnknownWithConstraints(state, evaluateCondition(state, logicalNotTree), local1Symbol(), FALSE);

    state.setBooleanConstraint(local1Symbol(), FALSE);
    validateEvaluationTrue(state, evaluateCondition(state, logicalNotTree));

    state.setBooleanConstraint(local1Symbol(), TRUE);
    validateEvaluationFalse(state, evaluateCondition(state, logicalNotTree));

    // result is unknown with unknown constraint.
    state.setBooleanConstraint(local1Symbol(), UNKNOWN);
    validateEvaluationUnknownWithConstraints(state, evaluateCondition(state, logicalNotTree), local1Symbol(), FALSE);
  }

  @Test
  public void test_expression_logical_not() {
    ExpressionTree logicalNotTree = analyze("!local1");
    ExecutionState state = new ExecutionState();

    // result is unknown without constraint.
    assertThat(evaluateExpression(logicalNotTree)).isSameAs(UNKNOWN);

    state.setBooleanConstraint(local1Symbol(), FALSE);
    assertThat(evaluateExpression(state, logicalNotTree)).isSameAs(TRUE);

    state.setBooleanConstraint(local1Symbol(), TRUE);
    assertThat(evaluateExpression(state, logicalNotTree)).isSameAs(FALSE);

    // result is unknown with unknown constraint.
    state.setBooleanConstraint(local1Symbol(), UNKNOWN);
    assertThat(evaluateExpression(state, logicalNotTree)).isSameAs(UNKNOWN);
  }

  @Test
  public void test_condition_member_select() {
    ExecutionState state = new ExecutionState();
    validateEvaluationUnknownWithoutConstraints(state, evaluateCondition(parse("expression.identifier")));
  }

  @Test
  public void test_expression_member_select() {
    assertThat(evaluateExpression("field.field;")).isSameAs(UNKNOWN);
  }

  @Test
  public void test_condition_method_invocation() {
    ExecutionState state = new ExecutionState();
    validateEvaluationUnknownWithoutConstraints(state, evaluateCondition(parse("method()")));
  }

  @Test
  public void test_expression_method_invocation() {
    assertThat(evaluateExpression("field.method();")).isSameAs(UNKNOWN);
  }

  @Test
  public void test_condition_relational() {
    // tests registration of relations in the spawned true and false states.
    evaluateRelationalOperator("local1 > local2", SymbolicRelation.GREATER_THAN, SymbolicRelation.LESS_EQUAL);
    evaluateRelationalOperator("local1 >= local2", SymbolicRelation.GREATER_EQUAL, SymbolicRelation.LESS_THAN);
    evaluateRelationalOperator("local1 == local2", SymbolicRelation.EQUAL_TO, SymbolicRelation.NOT_EQUAL);
    evaluateRelationalOperator("local1 < local2", SymbolicRelation.LESS_THAN, SymbolicRelation.GREATER_EQUAL);
    evaluateRelationalOperator("local1 <= local2", SymbolicRelation.LESS_EQUAL, SymbolicRelation.GREATER_THAN);
    evaluateRelationalOperator("local1 != local2", SymbolicRelation.NOT_EQUAL, SymbolicRelation.EQUAL_TO);

    ExecutionState state = new ExecutionState();

    // no constraint must be registered if there is a field.
    evaluateRelationalOperator(analyze("field1 != field2"), SymbolicRelation.NOT_EQUAL, SymbolicRelation.EQUAL_TO, field1Symbol(), field2Symbol());
    evaluateRelationalOperator(analyze("field1 != local2"), SymbolicRelation.NOT_EQUAL, SymbolicRelation.EQUAL_TO, field1Symbol(), local2Symbol());
    evaluateRelationalOperator(analyze("local1 != field2"), SymbolicRelation.NOT_EQUAL, SymbolicRelation.EQUAL_TO, local1Symbol(), field2Symbol());

    ExpressionTree notEqualTree = analyze("local1 != local2");

    // under constraint local1 != local2 evaluation or local1 != local2 must return unconditionally true
    state.setRelation(local1Symbol(), SymbolicRelation.NOT_EQUAL, local2Symbol());
    validateEvaluationTrue(state, evaluateCondition(state, notEqualTree));

    // under constraint local1 == local2 evaluation or local1 == local2 must return unconditionally false
    state.setRelation(local1Symbol(), SymbolicRelation.EQUAL_TO, local2Symbol());
    validateEvaluationFalse(state, evaluateCondition(state, notEqualTree));

    // comparison must not fail if either or both operands are not identifiers.
    new SymbolicEvaluator().evaluateCondition(state, parse("null == null"));
  }

  private void evaluateRelationalOperator(String input, @Nullable SymbolicRelation trueRelation, @Nullable SymbolicRelation falseRelation) {
    evaluateRelationalOperator(analyze(input), trueRelation, falseRelation, local1Symbol(), local2Symbol());
  }

  private SymbolicEvaluator.PackedStates evaluateRelationalOperator(ExpressionTree tree, @Nullable SymbolicRelation trueRelation,
    @Nullable SymbolicRelation falseRelation, SymbolicValue.SymbolicVariableValue value1, SymbolicValue.SymbolicVariableValue value2) {
    SymbolicEvaluator.PackedStates result = new SymbolicEvaluator().evaluateCondition(new ExecutionState(), tree);
    if (falseRelation != null) {
      assertThat(result.falseStates).hasSize(1);
      assertThat(result.falseStates.get(0).relations.get(value1, value2)).isSameAs(falseRelation);
      assertThat(result.falseStates.get(0).relations.get(value2, value1)).isSameAs(falseRelation.swap());
    } else {
      assertThat(result.falseStates).isEmpty();
    }
    if (trueRelation != null) {
      assertThat(result.trueStates).hasSize(1);
      assertThat(result.trueStates.get(0).relations.get(value1, value2)).isSameAs(trueRelation);
      assertThat(result.trueStates.get(0).relations.get(value2, value1)).isSameAs(trueRelation.swap());
    } else {
      assertThat(result.trueStates).isEmpty();
    }
    return result;
  }

  @Test
  public void test_expression_relational() {
    ExecutionState state = new ExecutionState();
    ExpressionTree notEqualTree = analyze("local1 != local2");

    // unknown without relations and constraints.
    assertThat(evaluateExpression(state, notEqualTree)).isSameAs(UNKNOWN);

    // unknown for now with relations. should be false
    state.setBooleanConstraint(local1Symbol(), TRUE);
    state.setBooleanConstraint(local2Symbol(), TRUE);
    assertThat(evaluateExpression(state, notEqualTree)).isSameAs(UNKNOWN);

    // unknown for now with relations. should be true
    state.setBooleanConstraint(local1Symbol(), TRUE);
    state.setBooleanConstraint(local2Symbol(), FALSE);
    assertThat(evaluateExpression(state, notEqualTree)).isSameAs(UNKNOWN);

    state.setBooleanConstraint(local1Symbol(), TRUE);
    state.setBooleanConstraint(local2Symbol(), UNKNOWN);
    assertThat(evaluateExpression(state, notEqualTree)).isSameAs(UNKNOWN);

    // true with not equal relation
    state.setRelation(local1Symbol(), SymbolicRelation.NOT_EQUAL, local2Symbol());
    assertThat(evaluateExpression(state, notEqualTree)).isSameAs(TRUE);

    // false with equal to relation
    state.setRelation(local1Symbol(), SymbolicRelation.EQUAL_TO, local2Symbol());
    assertThat(evaluateExpression(state, notEqualTree)).isSameAs(FALSE);

    // comparison must not fail if either or both operands are not identifiers.
    assertThat(evaluateExpression(state, parse("null == null"))).isSameAs(UNKNOWN);
  }

  @Test
  public void test_expression_statement() {
    ExecutionState state = new ExecutionState();
    PackedStatementStates result = new SymbolicEvaluator().evaluateStatement(ImmutableList.of(state), parseStatement("local1 && local1;"));
    assertThat(result.states).containsExactly(state);
    assertThat(state.relations.isEmpty()).isTrue();
  }

  @Test
  public void test_condition_unary() {
    ExecutionState state = new ExecutionState();
    validateEvaluationUnknownWithoutConstraints(state, evaluateCondition(parse("+local")));
  }

  @Test
  public void test_expression_unary() {
    assertThat(evaluateExpression("+local")).isSameAs(UNKNOWN);
  }

  @Test
  public void test_dowhile_merge() {
    ExecutionState falseState = new ExecutionState();
    ExecutionState trueState = new ExecutionState();
    ExecutionState unknownState = new ExecutionState();

    StatementTree blockingTree = analyzeStatement("{ local1 = true; do { return; } while(local1); }");
    PackedStatementStates blockingResult = new SymbolicEvaluator().evaluateStatement(ImmutableList.of(unknownState), blockingTree);
    assertThat(blockingResult.states).isEmpty();

    StatementTree tree = analyzeStatement("{ local2 = true; do { local2 = false; } while(local1); }");
    falseState.setBooleanConstraint(local1Symbol(), FALSE);
    trueState.setBooleanConstraint(local1Symbol(), TRUE);
    unknownState.setBooleanConstraint(local1Symbol(), UNKNOWN);
    PackedStatementStates result = new SymbolicEvaluator().evaluateStatement(ImmutableList.of(falseState, trueState, unknownState), tree);
    assertThat(result.states).containsOnly(falseState, unknownState);
    assertThat(falseState.getBooleanConstraint(local1Symbol())).isSameAs(FALSE);
    assertThat(trueState.getBooleanConstraint(local1Symbol())).isSameAs(TRUE);
    assertThat(unknownState.getBooleanConstraint(local1Symbol())).isSameAs(UNKNOWN);
    assertThat(falseState.getBooleanConstraint(local2Symbol())).isSameAs(UNKNOWN);
    assertThat(unknownState.getBooleanConstraint(local2Symbol())).isSameAs(UNKNOWN);
  }

  @Test
  public void test_for_merge() {
    ExecutionState falseState = new ExecutionState();
    ExecutionState trueState = new ExecutionState();
    ExecutionState unknownState = new ExecutionState();

    StatementTree blockingTree = analyzeStatement("{ local1 = true; for(; local1; ) { return; } }");
    PackedStatementStates blockingResult = new SymbolicEvaluator().evaluateStatement(ImmutableList.of(unknownState), blockingTree);
    assertThat(blockingResult.states).isEmpty();

    StatementTree emptyTree = analyzeStatement("{ local2 = true; for(; ; ) { local2 = false; } }");
    falseState.setBooleanConstraint(local1Symbol(), FALSE);
    trueState.setBooleanConstraint(local1Symbol(), TRUE);
    unknownState.setBooleanConstraint(local1Symbol(), UNKNOWN);
    PackedStatementStates emptyResult = new SymbolicEvaluator().evaluateStatement(ImmutableList.of(falseState, trueState, unknownState), emptyTree);
    assertThat(emptyResult.states).isEmpty();

    StatementTree tree = analyzeStatement("{ local2 = true; for(; local1; ) { local2 = false; } }");
    falseState.setBooleanConstraint(local1Symbol(), FALSE);
    trueState.setBooleanConstraint(local1Symbol(), TRUE);
    unknownState.setBooleanConstraint(local1Symbol(), UNKNOWN);
    PackedStatementStates result = new SymbolicEvaluator().evaluateStatement(ImmutableList.of(falseState, trueState, unknownState), tree);
    assertThat(result.states).containsOnly(falseState, trueState, unknownState);
    assertThat(falseState.getBooleanConstraint(local1Symbol())).isSameAs(FALSE);
    assertThat(trueState.getBooleanConstraint(local1Symbol())).isSameAs(TRUE);
    assertThat(unknownState.getBooleanConstraint(local1Symbol())).isSameAs(UNKNOWN);
    assertThat(falseState.getBooleanConstraint(local2Symbol())).isSameAs(UNKNOWN);
    assertThat(trueState.getBooleanConstraint(local2Symbol())).isSameAs(UNKNOWN);
    assertThat(unknownState.getBooleanConstraint(local2Symbol())).isSameAs(UNKNOWN);
  }

  @Test
  public void test_if_merge() {
    ExecutionState falseState = new ExecutionState();
    ExecutionState trueState = new ExecutionState();
    ExecutionState unknownState = new ExecutionState();

    StatementTree blockingIfTree = analyzeStatement("{ local1 = true; if(local1) { return; } }");
    PackedStatementStates blockingResult = new SymbolicEvaluator().evaluateStatement(ImmutableList.of(unknownState), blockingIfTree);
    assertThat(blockingResult.states).isEmpty();

    StatementTree ifTree = analyzeStatement("if(local1) { local2 = true; } else { local2 = false; }");
    falseState.setBooleanConstraint(local1Symbol(), FALSE);
    trueState.setBooleanConstraint(local1Symbol(), TRUE);
    unknownState.setBooleanConstraint(local1Symbol(), UNKNOWN);
    PackedStatementStates result = new SymbolicEvaluator().evaluateStatement(ImmutableList.of(falseState, trueState, unknownState), ifTree);
    assertThat(result.states).containsOnly(falseState, trueState, unknownState);
    assertThat(falseState.getBooleanConstraint(local1Symbol())).isSameAs(FALSE);
    assertThat(trueState.getBooleanConstraint(local1Symbol())).isSameAs(TRUE);
    assertThat(unknownState.getBooleanConstraint(local1Symbol())).isSameAs(UNKNOWN);
    assertThat(falseState.getBooleanConstraint(local2Symbol())).isSameAs(FALSE);
    assertThat(trueState.getBooleanConstraint(local2Symbol())).isSameAs(TRUE);
    assertThat(unknownState.getBooleanConstraint(local2Symbol())).isSameAs(UNKNOWN);
  }

  @Test
  public void test_while_merge() {
    ExecutionState falseState = new ExecutionState();
    ExecutionState trueState = new ExecutionState();
    ExecutionState unknownState = new ExecutionState();

    StatementTree blockingTree = analyzeStatement("{ local1 = true; while(local1) { return; } }");
    PackedStatementStates blockingResult = new SymbolicEvaluator().evaluateStatement(ImmutableList.of(unknownState), blockingTree);
    assertThat(blockingResult.states).isEmpty();

    StatementTree tree = analyzeStatement("{ local2 = true; while(local1) { local2 = false; } }");
    falseState.setBooleanConstraint(local1Symbol(), FALSE);
    trueState.setBooleanConstraint(local1Symbol(), TRUE);
    unknownState.setBooleanConstraint(local1Symbol(), UNKNOWN);
    PackedStatementStates result = new SymbolicEvaluator().evaluateStatement(ImmutableList.of(falseState, trueState, unknownState), tree);
    assertThat(result.states).containsOnly(falseState, trueState, unknownState);
    assertThat(falseState.getBooleanConstraint(local1Symbol())).isSameAs(FALSE);
    assertThat(trueState.getBooleanConstraint(local1Symbol())).isSameAs(TRUE);
    assertThat(unknownState.getBooleanConstraint(local1Symbol())).isSameAs(UNKNOWN);
    assertThat(falseState.getBooleanConstraint(local2Symbol())).isSameAs(UNKNOWN);
    assertThat(trueState.getBooleanConstraint(local2Symbol())).isSameAs(UNKNOWN);
    assertThat(unknownState.getBooleanConstraint(local2Symbol())).isSameAs(UNKNOWN);
  }

  private CompilationUnitTree compilationUnit;

  private ExpressionTree analyze(String input) {
    ExpressionTree result = parse(input);
    SemanticModel.createFor(compilationUnit, ImmutableList.<File>of());
    return result;
  }

  private StatementTree analyzeStatement(String input) {
    StatementTree result = parseStatement(input);
    SemanticModel.createFor(compilationUnit, ImmutableList.<File>of());
    return result;
  }

  private ExpressionTree parse(String input) {
    String p = "class Test { boolean field1; boolean field2; void wrapperMethod(boolean local1, boolean local2) { " + input + "; } }";
    compilationUnit = (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(p);
    return ((ExpressionStatementTree) ((MethodTree) ((ClassTree) compilationUnit.types().get(0)).members().get(2)).block().body().get(0)).expression();
  }

  private StatementTree parseStatement(String input) {
    String p = "class Test { boolean field1; boolean field2; void wrapperMethod(boolean local1, boolean local2) { " + input + "; } }";
    compilationUnit = (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(p);
    return ((MethodTree) ((ClassTree) compilationUnit.types().get(0)).members().get(2)).block().body().get(0);
  }

  private SymbolicValue.SymbolicVariableValue field1Symbol() {
    return new SymbolicValue.SymbolicVariableValue((Symbol.VariableSymbol) ((VariableTree) ((ClassTree) compilationUnit.types().get(0)).members().get(0)).symbol());
  }

  private SymbolicValue.SymbolicVariableValue field2Symbol() {
    return new SymbolicValue.SymbolicVariableValue((Symbol.VariableSymbol) ((VariableTree) ((ClassTree) compilationUnit.types().get(0)).members().get(1)).symbol());
  }

  private SymbolicValue.SymbolicVariableValue local1Symbol() {
    return new SymbolicValue.SymbolicVariableValue((Symbol.VariableSymbol) ((MethodTree) ((ClassTree) compilationUnit.types().get(0)).members().get(2)).parameters().get(0)
      .symbol());
  }

  private SymbolicValue.SymbolicVariableValue local2Symbol() {
    return new SymbolicValue.SymbolicVariableValue((Symbol.VariableSymbol) ((MethodTree) ((ClassTree) compilationUnit.types().get(0)).members().get(2)).parameters().get(1)
      .symbol());
  }

  private SymbolicEvaluator.PackedStates evaluateCondition(String input) {
    return evaluateCondition(new ExecutionState(), parse(input));
  }

  private SymbolicEvaluator.PackedStates evaluateCondition(ExpressionTree tree) {
    return evaluateCondition(new ExecutionState(), tree);
  }

  private SymbolicEvaluator.PackedStates evaluateCondition(ExecutionState state, ExpressionTree tree) {
    return new SymbolicEvaluator().evaluateCondition(state, tree);
  }

  private SymbolicBooleanConstraint evaluateExpression(String input) {
    return evaluateExpression(new ExecutionState(), parse(input));
  }

  private SymbolicBooleanConstraint evaluateExpression(ExpressionTree tree) {
    return evaluateExpression(new ExecutionState(), tree);
  }

  private SymbolicBooleanConstraint evaluateExpression(ExecutionState state, ExpressionTree tree) {
    return new SymbolicEvaluator().evaluateExpression(state, tree);
  }

  private void assertOutputStates(SymbolicEvaluator.PackedStates states, int trueCount, int falseCount, int unknownCount) {
    assertThat(states.falseStates).hasSize(falseCount);
    assertThat(states.trueStates).hasSize(trueCount);
    assertThat(states.unknownStates).hasSize(unknownCount);
  }

  private void validateEvaluationFalse(ExecutionState state, SymbolicEvaluator.PackedStates result) {
    assertThat(result.falseStates.get(0)).isSameAs(state);
    assertThat(result.trueStates).isEmpty();
  }

  private void validateEvaluationTrue(ExecutionState state, SymbolicEvaluator.PackedStates result) {
    assertThat(result.falseStates).isEmpty();
    assertThat(result.trueStates.get(0)).isSameAs(state);
  }

  private void validateEvaluationUnknownWithConstraints(ExecutionState state, PackedStates result, SymbolicValue.SymbolicVariableValue constrainedSymbol) {
    validateEvaluationUnknownWithConstraints(state, result, constrainedSymbol, TRUE);
  }

  private void validateEvaluationUnknownWithConstraints(ExecutionState state, PackedStates result, SymbolicValue.SymbolicVariableValue constrainedSymbol,
    SymbolicBooleanConstraint trueConstraint) {
    assertThat(result.isAlwaysFalse()).isFalse();
    assertThat(result.isAlwaysTrue()).isFalse();
    assertThat(result.falseStates.get(0).getBooleanConstraint(constrainedSymbol)).isSameAs(trueConstraint.negate());
    assertThat(result.trueStates.get(0).getBooleanConstraint(constrainedSymbol)).isSameAs(trueConstraint);
  }

  private void validateEvaluationUnknownWithoutConstraints(ExecutionState state, PackedStates result) {
    assertThat(result.isAlwaysFalse()).isFalse();
    assertThat(result.isAlwaysTrue()).isFalse();
    assertThat(result.falseStates.get(0).relations.size()).isEqualTo(0);
    assertThat(result.trueStates.get(0).relations.size()).isEqualTo(0);
  }

}
