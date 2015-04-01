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

import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.InternalPrefixUnaryExpression;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.FALSE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.TRUE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.UNKNOWN;

public class ExpressionEvaluatorVisitorTest {

  private static LiteralTree EXPRESSION_UNKNOWN;
  private static LiteralTree LITERAL_FALSE;
  private static LiteralTree LITERAL_TRUE;
  private static Symbol.VariableSymbol SYMBOL1;
  private static Symbol.VariableSymbol SYMBOL2;
  private static IdentifierTreeImpl VARIABLE1;
  private static IdentifierTreeImpl VARIABLE2;

  private static final ExpressionEvaluatorVisitor VISITOR = new ExpressionEvaluatorVisitor();

  @BeforeClass
  public static void setup() {
    InternalSyntaxToken tokenNull = mock(InternalSyntaxToken.class);
    when(tokenNull.text()).thenReturn("null");
    EXPRESSION_UNKNOWN = new LiteralTreeImpl(Tree.Kind.BOOLEAN_LITERAL, tokenNull);

    InternalSyntaxToken tokenFalse = mock(InternalSyntaxToken.class);
    when(tokenFalse.text()).thenReturn("false");
    LITERAL_FALSE = new LiteralTreeImpl(Tree.Kind.BOOLEAN_LITERAL, tokenFalse);

    InternalSyntaxToken tokenTrue = mock(InternalSyntaxToken.class);
    when(tokenTrue.text()).thenReturn("true");
    LITERAL_TRUE = new LiteralTreeImpl(Tree.Kind.BOOLEAN_LITERAL, tokenTrue);

    Symbol ownerSymbol = Mockito.mock(Symbol.class);
    when(ownerSymbol.isMethodSymbol()).thenReturn(true);

    InternalSyntaxToken tokenVariable1 = mock(InternalSyntaxToken.class);
    when(tokenVariable1.text()).thenReturn("variable1");
    SYMBOL1 = Mockito.mock(Symbol.VariableSymbol.class);
    when(SYMBOL1.isVariableSymbol()).thenReturn(true);
    when(SYMBOL1.owner()).thenReturn(ownerSymbol);

    VARIABLE1 = new IdentifierTreeImpl(mock(InternalSyntaxToken.class));
    VARIABLE1.setSymbol(SYMBOL1);

    InternalSyntaxToken tokenVariable2 = mock(InternalSyntaxToken.class);
    when(tokenVariable2.text()).thenReturn("variable2");
    SYMBOL2 = Mockito.mock(Symbol.VariableSymbol.class);
    when(SYMBOL2.isVariableSymbol()).thenReturn(true);
    when(SYMBOL2.owner()).thenReturn(ownerSymbol);

    VARIABLE2 = new IdentifierTreeImpl(mock(InternalSyntaxToken.class));
    VARIABLE2.setSymbol(SYMBOL2);
  }

  @Test
  public void test_boolean_literal() {
    assertThat(VISITOR.evaluate(new ExecutionState(), LITERAL_FALSE)).isSameAs(SymbolicBooleanConstraint.FALSE);
    assertThat(VISITOR.evaluate(new ExecutionState(), LITERAL_TRUE)).isSameAs(SymbolicBooleanConstraint.TRUE);
    assertThat(VISITOR.evaluate(new ExecutionState(), EXPRESSION_UNKNOWN)).isSameAs(SymbolicBooleanConstraint.UNKNOWN);
  }

  @Test
  public void test_conditional_and() {
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_AND, LITERAL_FALSE, LITERAL_TRUE)).isSameAs(FALSE);
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_AND, LITERAL_FALSE, LITERAL_TRUE)).isSameAs(FALSE);
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_AND, LITERAL_FALSE, EXPRESSION_UNKNOWN)).isSameAs(FALSE);

    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_AND, LITERAL_TRUE, LITERAL_FALSE)).isSameAs(FALSE);
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_AND, LITERAL_TRUE, LITERAL_TRUE)).isSameAs(TRUE);
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_AND, LITERAL_TRUE, EXPRESSION_UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_AND, EXPRESSION_UNKNOWN, LITERAL_FALSE)).isSameAs(FALSE);
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_AND, EXPRESSION_UNKNOWN, LITERAL_TRUE)).isSameAs(UNKNOWN);
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_AND, EXPRESSION_UNKNOWN, EXPRESSION_UNKNOWN)).isSameAs(UNKNOWN);
  }

  @Test
  public void test_conditional_or() {
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_OR, LITERAL_FALSE, LITERAL_FALSE)).isSameAs(FALSE);
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_OR, LITERAL_FALSE, LITERAL_TRUE)).isSameAs(TRUE);
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_OR, LITERAL_FALSE, EXPRESSION_UNKNOWN)).isSameAs(UNKNOWN);

    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_OR, LITERAL_TRUE, LITERAL_FALSE)).isSameAs(TRUE);
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_OR, LITERAL_TRUE, LITERAL_TRUE)).isSameAs(TRUE);
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_OR, LITERAL_TRUE, EXPRESSION_UNKNOWN)).isSameAs(TRUE);

    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_OR, EXPRESSION_UNKNOWN, LITERAL_FALSE)).isSameAs(UNKNOWN);
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_OR, EXPRESSION_UNKNOWN, LITERAL_TRUE)).isSameAs(TRUE);
    assertThat(evaluateBinaryOperator(Tree.Kind.CONDITIONAL_OR, EXPRESSION_UNKNOWN, EXPRESSION_UNKNOWN)).isSameAs(UNKNOWN);
  }

  private SymbolicBooleanConstraint evaluateBinaryOperator(Tree.Kind operatorKind, ExpressionTree leftTree, ExpressionTree rightTree) {
    return VISITOR.evaluate(new ExecutionState(), new BinaryExpressionTreeImpl(operatorKind, leftTree, mock(InternalSyntaxToken.class), rightTree));
  }

  @Test
  public void test_identifier() {
    Symbol.VariableSymbol ownerSymbol = mock(Symbol.VariableSymbol.class);
    when(ownerSymbol.isMethodSymbol()).thenReturn(true);
    Symbol.VariableSymbol identifierSymbol = mock(Symbol.VariableSymbol.class);
    when(identifierSymbol.isVariableSymbol()).thenReturn(true);
    when(identifierSymbol.owner()).thenReturn(ownerSymbol);
    IdentifierTreeImpl identifierTree = new IdentifierTreeImpl(mock(InternalSyntaxToken.class));
    identifierTree.setSymbol(identifierSymbol);

    ExecutionState state = new ExecutionState();

    ConditionalState defaultConditionalState = new ConditionalState(state);
    assertThat(VISITOR.evaluate(state, defaultConditionalState, identifierTree)).isSameAs(SymbolicBooleanConstraint.UNKNOWN);
    assertThat(defaultConditionalState.falseState.constraints.size()).isEqualTo(1);
    assertThat(defaultConditionalState.falseState.getBooleanConstraint(identifierSymbol)).isSameAs(SymbolicBooleanConstraint.FALSE);
    assertThat(defaultConditionalState.trueState.constraints.size()).isEqualTo(1);
    assertThat(defaultConditionalState.trueState.getBooleanConstraint(identifierSymbol)).isSameAs(SymbolicBooleanConstraint.TRUE);

    ConditionalState falseConditionalState = new ConditionalState(state);
    state.setBooleanConstraint(identifierSymbol, SymbolicBooleanConstraint.FALSE);
    assertThat(VISITOR.evaluate(state, falseConditionalState, identifierTree)).isSameAs(SymbolicBooleanConstraint.FALSE);
    assertThat(falseConditionalState.falseState.constraints.isEmpty()).isTrue();
    assertThat(falseConditionalState.trueState.constraints.isEmpty()).isTrue();

    ConditionalState trueConditionalState = new ConditionalState(state);
    state.setBooleanConstraint(identifierSymbol, SymbolicBooleanConstraint.TRUE);
    assertThat(VISITOR.evaluate(state, trueConditionalState, identifierTree)).isSameAs(SymbolicBooleanConstraint.TRUE);
    assertThat(falseConditionalState.falseState.constraints.isEmpty()).isTrue();
    assertThat(falseConditionalState.trueState.constraints.isEmpty()).isTrue();

    ConditionalState unknownConditionalState = new ConditionalState(state);
    state.setBooleanConstraint(identifierSymbol, SymbolicBooleanConstraint.UNKNOWN);
    assertThat(VISITOR.evaluate(state, unknownConditionalState, identifierTree)).isSameAs(SymbolicBooleanConstraint.UNKNOWN);
    assertThat(unknownConditionalState.falseState.constraints.size()).isEqualTo(1);
    assertThat(unknownConditionalState.trueState.constraints.size()).isEqualTo(1);
  }

  @Test
  public void test_logical_not() {
    Symbol.VariableSymbol ownerSymbol = mock(Symbol.VariableSymbol.class);
    when(ownerSymbol.isMethodSymbol()).thenReturn(true);
    Symbol.VariableSymbol identifierSymbol = mock(Symbol.VariableSymbol.class);
    when(identifierSymbol.isVariableSymbol()).thenReturn(true);
    when(identifierSymbol.owner()).thenReturn(ownerSymbol);
    IdentifierTreeImpl identifierTree = new IdentifierTreeImpl(mock(InternalSyntaxToken.class));
    identifierTree.setSymbol(identifierSymbol);
    InternalPrefixUnaryExpression logicalNotTree = new InternalPrefixUnaryExpression(Tree.Kind.LOGICAL_COMPLEMENT, mock(InternalSyntaxToken.class), identifierTree);

    ExecutionState state = new ExecutionState();

    ConditionalState defaultConditionalState = new ConditionalState(state);
    assertThat(VISITOR.evaluate(state, defaultConditionalState, logicalNotTree)).isSameAs(SymbolicBooleanConstraint.UNKNOWN);
    assertThat(defaultConditionalState.trueState.constraints.size()).isEqualTo(1);
    assertThat(defaultConditionalState.trueState.getBooleanConstraint(identifierSymbol)).isSameAs(SymbolicBooleanConstraint.FALSE);
    assertThat(defaultConditionalState.falseState.constraints.size()).isEqualTo(1);
    assertThat(defaultConditionalState.falseState.getBooleanConstraint(identifierSymbol)).isSameAs(SymbolicBooleanConstraint.TRUE);

    ConditionalState falseConditionalState = new ConditionalState(state);
    state.setBooleanConstraint(identifierSymbol, SymbolicBooleanConstraint.FALSE);
    assertThat(VISITOR.evaluate(state, falseConditionalState, logicalNotTree)).isSameAs(SymbolicBooleanConstraint.TRUE);
    assertThat(falseConditionalState.falseState.constraints.isEmpty()).isTrue();
    assertThat(falseConditionalState.trueState.constraints.isEmpty()).isTrue();

    ConditionalState trueConditionalState = new ConditionalState(state);
    state.setBooleanConstraint(identifierSymbol, SymbolicBooleanConstraint.TRUE);
    assertThat(VISITOR.evaluate(state, trueConditionalState, logicalNotTree)).isSameAs(SymbolicBooleanConstraint.FALSE);
    assertThat(falseConditionalState.falseState.constraints.isEmpty()).isTrue();
    assertThat(falseConditionalState.trueState.constraints.isEmpty()).isTrue();

    ConditionalState unknownConditionalState = new ConditionalState(state);
    state.setBooleanConstraint(identifierSymbol, SymbolicBooleanConstraint.UNKNOWN);
    assertThat(VISITOR.evaluate(state, unknownConditionalState, logicalNotTree)).isSameAs(SymbolicBooleanConstraint.UNKNOWN);
    assertThat(unknownConditionalState.falseState.constraints.size()).isEqualTo(1);
    assertThat(unknownConditionalState.trueState.constraints.size()).isEqualTo(1);
  }

  @Test
  public void test_relational() {
    ExecutionState state = new ExecutionState();

    ConditionalState greaterThanConditionalState = new ConditionalState(state);
    assertThat(evaluateBinaryOperator(state, greaterThanConditionalState, Tree.Kind.GREATER_THAN, VARIABLE1, VARIABLE2)).isSameAs(UNKNOWN);
    validateState(greaterThanConditionalState.falseState, SymbolicRelation.LESS_EQUAL, SymbolicRelation.GREATER_EQUAL);
    validateState(greaterThanConditionalState.trueState, SymbolicRelation.GREATER_THAN, SymbolicRelation.LESS_THAN);

    ConditionalState greaterEqualConditionalState = new ConditionalState(state);
    assertThat(evaluateBinaryOperator(state, greaterEqualConditionalState, Tree.Kind.GREATER_THAN_OR_EQUAL_TO, VARIABLE1, VARIABLE2)).isSameAs(UNKNOWN);
    validateState(greaterEqualConditionalState.falseState, SymbolicRelation.LESS_THAN, SymbolicRelation.GREATER_THAN);
    validateState(greaterEqualConditionalState.trueState, SymbolicRelation.GREATER_EQUAL, SymbolicRelation.LESS_EQUAL);

    ConditionalState equalToConditionalState = new ConditionalState(state);
    assertThat(evaluateBinaryOperator(state, equalToConditionalState, Tree.Kind.EQUAL_TO, VARIABLE1, VARIABLE2)).isSameAs(UNKNOWN);
    validateState(equalToConditionalState.falseState, SymbolicRelation.NOT_EQUAL, SymbolicRelation.NOT_EQUAL);
    validateState(equalToConditionalState.trueState, SymbolicRelation.EQUAL_TO, SymbolicRelation.EQUAL_TO);

    ConditionalState lessThanConditionalState = new ConditionalState(state);
    assertThat(evaluateBinaryOperator(state, lessThanConditionalState, Tree.Kind.LESS_THAN, VARIABLE1, VARIABLE2)).isSameAs(UNKNOWN);
    validateState(lessThanConditionalState.falseState, SymbolicRelation.GREATER_EQUAL, SymbolicRelation.LESS_EQUAL);
    validateState(lessThanConditionalState.trueState, SymbolicRelation.LESS_THAN, SymbolicRelation.GREATER_THAN);

    ConditionalState lessEqualConditionalState = new ConditionalState(state);
    assertThat(evaluateBinaryOperator(state, lessEqualConditionalState, Tree.Kind.LESS_THAN_OR_EQUAL_TO, VARIABLE1, VARIABLE2)).isSameAs(UNKNOWN);
    validateState(lessEqualConditionalState.falseState, SymbolicRelation.GREATER_THAN, SymbolicRelation.LESS_THAN);
    validateState(lessEqualConditionalState.trueState, SymbolicRelation.LESS_EQUAL, SymbolicRelation.GREATER_EQUAL);

    ConditionalState notEqualConditionalState = new ConditionalState(state);
    assertThat(evaluateBinaryOperator(state, notEqualConditionalState, Tree.Kind.NOT_EQUAL_TO, VARIABLE1, VARIABLE2)).isSameAs(UNKNOWN);
    validateState(notEqualConditionalState.falseState, SymbolicRelation.EQUAL_TO, SymbolicRelation.EQUAL_TO);
    validateState(notEqualConditionalState.trueState, SymbolicRelation.NOT_EQUAL, SymbolicRelation.NOT_EQUAL);
  }

  private SymbolicBooleanConstraint evaluateBinaryOperator(ExecutionState state, ConditionalState conditionalState, Tree.Kind operatorKind, ExpressionTree leftTree,
    ExpressionTree rightTree) {
    return VISITOR.evaluate(state, conditionalState, new BinaryExpressionTreeImpl(operatorKind, leftTree, mock(InternalSyntaxToken.class), rightTree));
  }

  private void validateState(ExecutionState state, SymbolicRelation leftRight, SymbolicRelation rightLeft) {
    assertThat(state.relations.size()).isEqualTo(2);
    assertThat(state.getRelation(SYMBOL1, SYMBOL2)).isSameAs(leftRight);
    assertThat(state.getRelation(SYMBOL2, SYMBOL1)).isSameAs(rightLeft);
  }

}
