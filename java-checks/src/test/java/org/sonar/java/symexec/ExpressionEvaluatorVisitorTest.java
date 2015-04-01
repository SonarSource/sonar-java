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

import com.google.common.collect.ImmutableList;
import com.sonar.sslr.api.AstNode;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.InstanceOfTreeImpl;
import org.sonar.java.model.expression.InternalPrefixUnaryExpression;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import javax.annotation.Nullable;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.FALSE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.TRUE;
import static org.sonar.java.symexec.SymbolicBooleanConstraint.UNKNOWN;

public class ExpressionEvaluatorVisitorTest {

  private static IdentifierTreeImpl FIELD;
  private static LiteralTree LITERAL_NULL;
  private static LiteralTree LITERAL_FALSE;
  private static LiteralTree LITERAL_TRUE;
  private static Symbol.VariableSymbol SYMBOL1;
  private static Symbol.VariableSymbol SYMBOL2;
  private static IdentifierTreeImpl VARIABLE1;
  private static IdentifierTreeImpl VARIABLE2;
  private static InternalSyntaxToken TOKEN = mock(InternalSyntaxToken.class);

  @BeforeClass
  public static void setup() {
    Symbol classSymbol = Mockito.mock(Symbol.class);
    when(classSymbol.isMethodSymbol()).thenReturn(false);

    Symbol.VariableSymbol fieldSymbol = Mockito.mock(Symbol.VariableSymbol.class);
    when(fieldSymbol.isVariableSymbol()).thenReturn(true);
    when(fieldSymbol.owner()).thenReturn(classSymbol);

    FIELD = new IdentifierTreeImpl(mock(InternalSyntaxToken.class));
    FIELD.setSymbol(fieldSymbol);

    InternalSyntaxToken tokenNull = mock(InternalSyntaxToken.class);
    when(tokenNull.text()).thenReturn("null");
    LITERAL_NULL = new LiteralTreeImpl(Tree.Kind.NULL_LITERAL, tokenNull);

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

    VARIABLE2 = new IdentifierTreeImpl(TOKEN);
    VARIABLE2.setSymbol(SYMBOL2);
  }

  @Test
  public void test_conditional_and() {
    validateLogicalOperator(Tree.Kind.CONDITIONAL_AND, LITERAL_FALSE, LITERAL_TRUE, 0, 1);
    validateLogicalOperator(Tree.Kind.CONDITIONAL_AND, LITERAL_FALSE, LITERAL_TRUE, 0, 1);
    validateLogicalOperator(Tree.Kind.CONDITIONAL_AND, LITERAL_FALSE, VARIABLE2, 0, 1);

    validateLogicalOperator(Tree.Kind.CONDITIONAL_AND, LITERAL_TRUE, LITERAL_FALSE, 0, 1);
    validateLogicalOperator(Tree.Kind.CONDITIONAL_AND, LITERAL_TRUE, LITERAL_TRUE, 1, 0);
    validateLogicalOperator(Tree.Kind.CONDITIONAL_AND, LITERAL_TRUE, VARIABLE2, 1, 1);

    validateLogicalOperator(Tree.Kind.CONDITIONAL_AND, VARIABLE1, LITERAL_FALSE, 0, 2);
    validateLogicalOperator(Tree.Kind.CONDITIONAL_AND, VARIABLE1, LITERAL_TRUE, 1, 1);
    validateLogicalOperator(Tree.Kind.CONDITIONAL_AND, VARIABLE1, VARIABLE2, 1, 2);
  }

  @Test
  public void test_conditional_or() {
    validateLogicalOperator(Tree.Kind.CONDITIONAL_OR, LITERAL_FALSE, LITERAL_FALSE, 0, 1);
    validateLogicalOperator(Tree.Kind.CONDITIONAL_OR, LITERAL_FALSE, LITERAL_TRUE, 1, 0);
    validateLogicalOperator(Tree.Kind.CONDITIONAL_OR, LITERAL_FALSE, VARIABLE2, 1, 1);

    validateLogicalOperator(Tree.Kind.CONDITIONAL_OR, LITERAL_TRUE, LITERAL_FALSE, 1, 0);
    validateLogicalOperator(Tree.Kind.CONDITIONAL_OR, LITERAL_TRUE, LITERAL_TRUE, 1, 0);
    validateLogicalOperator(Tree.Kind.CONDITIONAL_OR, LITERAL_TRUE, VARIABLE2, 1, 0);

    validateLogicalOperator(Tree.Kind.CONDITIONAL_OR, VARIABLE1, LITERAL_FALSE, 1, 1);
    validateLogicalOperator(Tree.Kind.CONDITIONAL_OR, VARIABLE1, LITERAL_TRUE, 2, 0);
    validateLogicalOperator(Tree.Kind.CONDITIONAL_OR, VARIABLE1, VARIABLE2, 2, 1);
  }

  private void validateLogicalOperator(Tree.Kind operatorKind, ExpressionTree leftTree, ExpressionTree rightTree, int trueCount, int falseCount) {
    ExpressionEvaluatorVisitor visitor = new ExpressionEvaluatorVisitor(new ExecutionState(), new BinaryExpressionTreeImpl(operatorKind, leftTree, TOKEN, rightTree));
    assertThat(visitor.falseStates.size()).isSameAs(falseCount);
    assertThat(visitor.trueStates.size()).isSameAs(trueCount);
  }

  @Test
  public void test_identifier() {
    Symbol.VariableSymbol ownerSymbol = mock(Symbol.VariableSymbol.class);
    when(ownerSymbol.isMethodSymbol()).thenReturn(true);
    Symbol.VariableSymbol identifierSymbol = mock(Symbol.VariableSymbol.class);
    when(identifierSymbol.isVariableSymbol()).thenReturn(true);
    when(identifierSymbol.owner()).thenReturn(ownerSymbol);
    IdentifierTreeImpl identifierTree = new IdentifierTreeImpl(TOKEN);
    identifierTree.setSymbol(identifierSymbol);

    ExecutionState state = new ExecutionState();

    ExpressionEvaluatorVisitor defaultVisitor = new ExpressionEvaluatorVisitor(state, identifierTree);
    assertThat(defaultVisitor.falseStates.size()).isEqualTo(1);
    assertThat(defaultVisitor.falseStates.get(0).constraints.size()).isEqualTo(1);
    assertThat(defaultVisitor.falseStates.get(0).getBooleanConstraint(identifierSymbol)).isSameAs(FALSE);
    assertThat(defaultVisitor.trueStates.size()).isEqualTo(1);
    assertThat(defaultVisitor.trueStates.get(0).constraints.size()).isEqualTo(1);
    assertThat(defaultVisitor.trueStates.get(0).getBooleanConstraint(identifierSymbol)).isSameAs(TRUE);

    state.setBooleanConstraint(identifierSymbol, FALSE);
    ExpressionEvaluatorVisitor falseVisitor = new ExpressionEvaluatorVisitor(state, identifierTree);
    assertThat(falseVisitor.falseStates.size()).isEqualTo(1);
    assertThat(falseVisitor.falseStates.get(0).constraints.size()).isEqualTo(1);
    assertThat(falseVisitor.trueStates).isEmpty();

    state.setBooleanConstraint(identifierSymbol, TRUE);
    ExpressionEvaluatorVisitor trueVisitor = new ExpressionEvaluatorVisitor(state, identifierTree);
    assertThat(trueVisitor.falseStates).isEmpty();
    assertThat(trueVisitor.trueStates.size()).isEqualTo(1);
    assertThat(trueVisitor.trueStates.get(0).constraints.size()).isEqualTo(1);

    state.setBooleanConstraint(identifierSymbol, UNKNOWN);

    ExpressionEvaluatorVisitor unknownVisitor = new ExpressionEvaluatorVisitor(state, identifierTree);
    assertThat(unknownVisitor.falseStates.size()).isEqualTo(1);
    assertThat(unknownVisitor.falseStates.get(0).constraints.size()).isEqualTo(1);
    assertThat(unknownVisitor.falseStates.get(0).getBooleanConstraint(identifierSymbol)).isSameAs(FALSE);
    assertThat(unknownVisitor.trueStates.size()).isEqualTo(1);
    assertThat(unknownVisitor.trueStates.get(0).constraints.size()).isEqualTo(1);
    assertThat(unknownVisitor.trueStates.get(0).getBooleanConstraint(identifierSymbol)).isSameAs(TRUE);

    when(ownerSymbol.isMethodSymbol()).thenReturn(false);
    validateUnknownResult(identifierTree);
  }

  @Test
  public void test_instanceof() {
    InstanceOfTreeImpl tree = new InstanceOfTreeImpl(TOKEN, mock(TypeTree.class), mock(AstNode.class));
    tree.complete(VARIABLE1);
    validateUnknownResult(tree);
  }

  @Test
  public void test_literal() {
    ExpressionEvaluatorVisitor falseVisitor = new ExpressionEvaluatorVisitor(new ExecutionState(), LITERAL_FALSE);
    assertThat(falseVisitor.falseStates.size()).isEqualTo(1);
    assertThat(falseVisitor.falseStates.get(0).constraints).isEmpty();
    assertThat(falseVisitor.trueStates).isEmpty();

    validateUnknownResult(LITERAL_NULL);

    ExpressionEvaluatorVisitor trueVisitor = new ExpressionEvaluatorVisitor(new ExecutionState(), LITERAL_TRUE);
    assertThat(trueVisitor.falseStates).isEmpty();
    assertThat(trueVisitor.trueStates.size()).isEqualTo(1);
    assertThat(trueVisitor.trueStates.get(0).constraints).isEmpty();
    assertThat(trueVisitor.trueStates.get(0).relations.isEmpty()).isEqualTo(true);
  }

  @Test
  public void test_logical_not() {
    Symbol.VariableSymbol ownerSymbol = mock(Symbol.VariableSymbol.class);
    when(ownerSymbol.isMethodSymbol()).thenReturn(true);
    Symbol.VariableSymbol identifierSymbol = mock(Symbol.VariableSymbol.class);
    when(identifierSymbol.isVariableSymbol()).thenReturn(true);
    when(identifierSymbol.owner()).thenReturn(ownerSymbol);
    IdentifierTreeImpl identifierTree = new IdentifierTreeImpl(TOKEN);
    identifierTree.setSymbol(identifierSymbol);
    InternalPrefixUnaryExpression logicalNotTree = new InternalPrefixUnaryExpression(Tree.Kind.LOGICAL_COMPLEMENT, TOKEN, identifierTree);

    ExecutionState state = new ExecutionState();

    evaluateUnaryOperator(state, logicalNotTree, identifierSymbol, FALSE, TRUE);

    state.setBooleanConstraint(identifierSymbol, FALSE);
    evaluateUnaryOperator(state, logicalNotTree, identifierSymbol, FALSE, null);

    state.setBooleanConstraint(identifierSymbol, TRUE);
    evaluateUnaryOperator(state, logicalNotTree, identifierSymbol, null, TRUE);

    state.setBooleanConstraint(identifierSymbol, UNKNOWN);
    evaluateUnaryOperator(state, logicalNotTree, identifierSymbol, FALSE, TRUE);
  }

  public void evaluateUnaryOperator(ExecutionState state, Tree tree, Symbol.VariableSymbol symbol,
    @Nullable SymbolicBooleanConstraint trueValue, @Nullable SymbolicBooleanConstraint falseValue) {
    ExpressionEvaluatorVisitor defaultVisitor = new ExpressionEvaluatorVisitor(state, tree);
    if (trueValue != null) {
      assertThat(defaultVisitor.trueStates.size()).isEqualTo(1);
      assertThat(defaultVisitor.trueStates.get(0).constraints.size()).isEqualTo(1);
      assertThat(defaultVisitor.trueStates.get(0).getBooleanConstraint(symbol)).isSameAs(trueValue);
    } else {
      assertThat(defaultVisitor.trueStates).isEmpty();
    }
    if (falseValue != null) {
      assertThat(defaultVisitor.falseStates.size()).isEqualTo(1);
      assertThat(defaultVisitor.falseStates.get(0).constraints.size()).isEqualTo(1);
      assertThat(defaultVisitor.falseStates.get(0).getBooleanConstraint(symbol)).isSameAs(falseValue);
    } else {
      assertThat(defaultVisitor.falseStates).isEmpty();
    }
  }

  @Test
  public void test_member_select() {
    MemberSelectExpressionTreeImpl tree = new MemberSelectExpressionTreeImpl(VARIABLE1, VARIABLE2);
    validateUnknownResult(tree);
  }

  @Test
  public void test_method_invocation() {
    MethodInvocationTreeImpl tree = new MethodInvocationTreeImpl(VARIABLE1, null, ImmutableList.<ExpressionTree>of());
    validateUnknownResult(tree);
  }

  @Test
  public void test_relational() {
    ExecutionState state = new ExecutionState();

    ExpressionEvaluatorVisitor greaterThanVisitor = evaluateRelationalOperator(state, Tree.Kind.GREATER_THAN);
    validateState(greaterThanVisitor.falseStates.get(0), SymbolicRelation.LESS_EQUAL, SymbolicRelation.GREATER_EQUAL);
    validateState(greaterThanVisitor.trueStates.get(0), SymbolicRelation.GREATER_THAN, SymbolicRelation.LESS_THAN);

    ExpressionEvaluatorVisitor greaterEqualVisitor = evaluateRelationalOperator(state, Tree.Kind.GREATER_THAN_OR_EQUAL_TO);
    validateState(greaterEqualVisitor.falseStates.get(0), SymbolicRelation.LESS_THAN, SymbolicRelation.GREATER_THAN);
    validateState(greaterEqualVisitor.trueStates.get(0), SymbolicRelation.GREATER_EQUAL, SymbolicRelation.LESS_EQUAL);

    ExpressionEvaluatorVisitor equalToVisitor = evaluateRelationalOperator(state, Tree.Kind.EQUAL_TO);
    validateState(equalToVisitor.falseStates.get(0), SymbolicRelation.NOT_EQUAL, SymbolicRelation.NOT_EQUAL);
    validateState(equalToVisitor.trueStates.get(0), SymbolicRelation.EQUAL_TO, SymbolicRelation.EQUAL_TO);

    ExpressionEvaluatorVisitor lessThanVisitor = evaluateRelationalOperator(state, Tree.Kind.LESS_THAN);
    validateState(lessThanVisitor.falseStates.get(0), SymbolicRelation.GREATER_EQUAL, SymbolicRelation.LESS_EQUAL);
    validateState(lessThanVisitor.trueStates.get(0), SymbolicRelation.LESS_THAN, SymbolicRelation.GREATER_THAN);

    ExpressionEvaluatorVisitor lessEqualVisitor = evaluateRelationalOperator(state, Tree.Kind.LESS_THAN_OR_EQUAL_TO);
    validateState(lessEqualVisitor.falseStates.get(0), SymbolicRelation.GREATER_THAN, SymbolicRelation.LESS_THAN);
    validateState(lessEqualVisitor.trueStates.get(0), SymbolicRelation.LESS_EQUAL, SymbolicRelation.GREATER_EQUAL);

    ExpressionEvaluatorVisitor notEqualVisitor = evaluateRelationalOperator(state, Tree.Kind.NOT_EQUAL_TO);
    validateState(notEqualVisitor.falseStates.get(0), SymbolicRelation.EQUAL_TO, SymbolicRelation.EQUAL_TO);
    validateState(notEqualVisitor.trueStates.get(0), SymbolicRelation.NOT_EQUAL, SymbolicRelation.NOT_EQUAL);

    ExpressionEvaluatorVisitor fieldFieldVisitor = new ExpressionEvaluatorVisitor(state, new BinaryExpressionTreeImpl(Tree.Kind.NOT_EQUAL_TO, FIELD, TOKEN, FIELD));
    validateUnknownResult(state, fieldFieldVisitor);

    ExpressionEvaluatorVisitor fieldLocalVisitor = new ExpressionEvaluatorVisitor(state, new BinaryExpressionTreeImpl(Tree.Kind.NOT_EQUAL_TO, FIELD, TOKEN, VARIABLE2));
    validateUnknownResult(state, fieldLocalVisitor);

    ExpressionEvaluatorVisitor localFieldVisitor = new ExpressionEvaluatorVisitor(state, new BinaryExpressionTreeImpl(Tree.Kind.NOT_EQUAL_TO, VARIABLE1, TOKEN, FIELD));
    validateUnknownResult(state, localFieldVisitor);

    ExpressionEvaluatorVisitor nestedFalseVisitor = new ExpressionEvaluatorVisitor(notEqualVisitor.falseStates.get(0),
      new BinaryExpressionTreeImpl(Tree.Kind.NOT_EQUAL_TO, VARIABLE1, TOKEN, VARIABLE2));
    validateState(nestedFalseVisitor.falseStates.get(0), SymbolicRelation.EQUAL_TO, SymbolicRelation.EQUAL_TO);
    assertThat(nestedFalseVisitor.trueStates).isEmpty();

    ExpressionEvaluatorVisitor nestedTrueVisitor = new ExpressionEvaluatorVisitor(notEqualVisitor.trueStates.get(0),
      new BinaryExpressionTreeImpl(Tree.Kind.NOT_EQUAL_TO, VARIABLE1, TOKEN, VARIABLE2));
    assertThat(nestedTrueVisitor.falseStates).isEmpty();
    validateState(nestedTrueVisitor.trueStates.get(0), SymbolicRelation.NOT_EQUAL, SymbolicRelation.NOT_EQUAL);

    // comparison must not fail if either or both operands are not identifiers.
    new ExpressionEvaluatorVisitor(state, new BinaryExpressionTreeImpl(Tree.Kind.EQUAL_TO, LITERAL_NULL, TOKEN, LITERAL_NULL));
  }

  private ExpressionEvaluatorVisitor evaluateRelationalOperator(ExecutionState state, Tree.Kind operatorKind) {
    ExpressionEvaluatorVisitor result = new ExpressionEvaluatorVisitor(state, new BinaryExpressionTreeImpl(operatorKind, VARIABLE1, TOKEN, VARIABLE2));
    assertThat(result.falseStates.size()).isEqualTo(1);
    assertThat(result.trueStates.size()).isEqualTo(1);
    return result;
  }

  @Test
  public void test_unary() {
    Symbol.VariableSymbol ownerSymbol = mock(Symbol.VariableSymbol.class);
    when(ownerSymbol.isMethodSymbol()).thenReturn(true);
    Symbol.VariableSymbol identifierSymbol = mock(Symbol.VariableSymbol.class);
    when(identifierSymbol.isVariableSymbol()).thenReturn(true);
    when(identifierSymbol.owner()).thenReturn(ownerSymbol);
    IdentifierTreeImpl identifierTree = new IdentifierTreeImpl(TOKEN);
    identifierTree.setSymbol(identifierSymbol);
    InternalPrefixUnaryExpression tree = new InternalPrefixUnaryExpression(Tree.Kind.UNARY_PLUS, TOKEN, identifierTree);
    validateUnknownResult(tree);
  }

  private void validateState(ExecutionState state, SymbolicRelation leftRight, SymbolicRelation rightLeft) {
    assertThat(state.relations.size()).isEqualTo(2);
    assertThat(state.getRelation(SYMBOL1, SYMBOL2)).isSameAs(leftRight);
    assertThat(state.getRelation(SYMBOL2, SYMBOL1)).isSameAs(rightLeft);
  }

  private void validateUnknownResult(Tree tree) {
    ExecutionState state = new ExecutionState();
    validateUnknownResult(state, new ExpressionEvaluatorVisitor(state, tree));
  }

  private void validateUnknownResult(ExecutionState state, ExpressionEvaluatorVisitor result) {
    assertThat(result.falseStates).containsOnly(state);
    assertThat(result.trueStates).containsOnly(state);
  }

}
