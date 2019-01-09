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
package org.sonar.java.se.symbolicvalues;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.Symbols;
import org.sonar.java.se.JavaCheckVerifier;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SETestUtils;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.sonar.java.se.checks.DivisionByZeroCheck.ZeroConstraint.ZERO;
import static org.sonar.java.se.constraint.BooleanConstraint.FALSE;
import static org.sonar.java.se.constraint.BooleanConstraint.TRUE;
import static org.sonar.java.se.symbolicvalues.RelationState.FULFILLED;
import static org.sonar.java.se.symbolicvalues.RelationState.UNFULFILLED;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.METHOD_EQUALS;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.NOT_EQUAL;
import static org.sonar.java.se.symbolicvalues.SymbolicValue.NULL_LITERAL;

public class RelationalSymbolicValueTest {

  @Rule
  public LogTester logTester = new LogTester();

  ConstraintManager constraintManager = new ConstraintManager();
  SymbolicValue a = new SymbolicValue() {
    @Override
    public String toString() {
      return "SV_1";
    }
  };
  SymbolicValue b = new SymbolicValue() {
    @Override
    public String toString() {
      return "SV_2";
    }
  };
  SymbolicValue c = new SymbolicValue() {
    @Override
    public String toString() {
      return "SV_3";
    }
  };
  SymbolicValue d = new SymbolicValue();
  SymbolicValue e = new SymbolicValue();

  private final List<Tree.Kind> operators = Arrays.asList(
    Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO,
    Tree.Kind.GREATER_THAN, Tree.Kind.GREATER_THAN_OR_EQUAL_TO,
    Tree.Kind.LESS_THAN, Tree.Kind.LESS_THAN_OR_EQUAL_TO);

  @Test
  public void test_normalization() throws Exception {
    assertThat(relationalSV(Tree.Kind.EQUAL_TO, b, a)).hasToString("SV_1==SV_2");
    assertThat(relationalSV(Tree.Kind.NOT_EQUAL_TO, b, a)).hasToString("SV_1!=SV_2");
    assertThat(relationalSV(Tree.Kind.GREATER_THAN, b, a)).hasToString("SV_2<SV_1");
    assertThat(relationalSV(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, b, a)).hasToString("SV_1>=SV_2");
    assertThat(relationalSV(Tree.Kind.LESS_THAN, b, a)).hasToString("SV_1<SV_2");
    assertThat(relationalSV(Tree.Kind.LESS_THAN_OR_EQUAL_TO, b, a)).hasToString("SV_2>=SV_1");
  }

  private RelationalSymbolicValue relationalSV(Tree.Kind kind, SymbolicValue... computedFrom) {
    List<ProgramState.SymbolicValueSymbol> computedFromSymbols = Arrays.stream(computedFrom).map(sv -> new ProgramState.SymbolicValueSymbol(sv, null))
      .collect(Collectors.toList());
    return (RelationalSymbolicValue) constraintManager
      .createBinarySymbolicValue(new BinaryExpressionTreeImpl(kind, mock(ExpressionTree.class), mock(InternalSyntaxToken.class), mock(ExpressionTree.class)),
        computedFromSymbols);
  }

  @Test
  public void test_same_operand() {
    assertThat(sameOperandResolution(Tree.Kind.EQUAL_TO)).isEqualTo(FULFILLED);
    RelationalSymbolicValue eq = new RelationalSymbolicValue(METHOD_EQUALS, a, a);
    assertThat(eq.resolveRelationState(Collections.emptySet())).isEqualTo(FULFILLED);
    assertThat(sameOperandResolution(Tree.Kind.LESS_THAN_OR_EQUAL_TO)).isEqualTo(FULFILLED);
    assertThat(sameOperandResolution(Tree.Kind.GREATER_THAN_OR_EQUAL_TO)).isEqualTo(FULFILLED);

    assertThat(sameOperandResolution(Tree.Kind.NOT_EQUAL_TO)).isEqualTo(UNFULFILLED);
    assertThat(eq.inverse().resolveRelationState(Collections.emptySet())).isEqualTo(UNFULFILLED);
    assertThat(sameOperandResolution(Tree.Kind.LESS_THAN)).isEqualTo(UNFULFILLED);
    assertThat(sameOperandResolution(Tree.Kind.GREATER_THAN)).isEqualTo(UNFULFILLED);
  }

  private RelationState sameOperandResolution(Tree.Kind kind) {
    return relationalSV(kind, a, a).resolveRelationState(Collections.emptySet());
  }

  @Test
  public void test_different_operand() throws Exception {
    RelationalSymbolicValue ab = new RelationalSymbolicValue(EQUAL, a, b);
    RelationalSymbolicValue bc = new RelationalSymbolicValue(EQUAL, b, c);
    assertThat(ab.differentOperand(bc)).isEqualTo(a);
    assertThat(bc.differentOperand(ab)).isEqualTo(c);
  }

  @Test
  public void test_direct_deduction() throws Exception {
    List<String> actual = new ArrayList<>();
    for (Tree.Kind operator : operators) {
      actual.addAll(resolveRelationStateForAllKinds(relationalSV(operator, b, a), () -> relationToString(operator, a, b)));
      actual.addAll(resolveRelationStateForAllKinds(relationalSV(operator, a, b), () -> relationToString(operator, b, a)));
    }
    RelationalSymbolicValue eqAB = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS, a, b);
    RelationalSymbolicValue eqBA = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS, b, a);
    Stream.of(eqAB, eqBA, eqAB.inverse(), eqBA.inverse()).forEach(rel -> actual.addAll(resolveRelationStateForAllKinds(rel, rel::toString)));
    List<String> expected = IOUtils.readLines(getClass().getResourceAsStream("/relations/direct.txt"));
    assertThat(actual).isEqualTo(expected);
  }

  private List<String> resolveRelationStateForAllKinds(RelationalSymbolicValue known, Supplier<String> knownAsString) {
    List<String> actual = new ArrayList<>();
    for (Tree.Kind operator : operators) {
      RelationalSymbolicValue test = relationalSV(operator, b, a);
      RelationState relationState = test.resolveRelationState(Collections.singleton(known));
      actual.add(String.format("given %s when %s -> %s", knownAsString.get(), relationToString(operator, a, b), relationState));
    }
    RelationalSymbolicValue eq = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS, a, b);
    Stream.of(eq, eq.inverse()).forEach(rel -> {
        RelationState relationState = rel.resolveRelationState(Collections.singleton(known));
        actual.add(String.format("given %s when %s -> %s", knownAsString.get(), rel, relationState));
      }
    );
    return actual;
  }

  private ProgramState stateWithRelations(RelationalSymbolicValue... known) {
    ProgramState ps = ProgramState.EMPTY_STATE;
    for (RelationalSymbolicValue rel : known) {
      ps = Iterables.getOnlyElement(rel.setConstraint(ps, TRUE));
    }
    return ps;
  }

  @Test
  public void test_conjuction_equal() throws Exception {
    RelationalSymbolicValue aLEb = relationalSV(Tree.Kind.LESS_THAN_OR_EQUAL_TO, a, b);
    RelationalSymbolicValue bLEa = relationalSV(Tree.Kind.LESS_THAN_OR_EQUAL_TO, b, a);
    RelationalSymbolicValue aEb = relationalSV(Tree.Kind.EQUAL_TO, a, b);
    ProgramState state = Iterables.getOnlyElement(aEb.setConstraint(stateWithRelations(aLEb, bLEa), TRUE));
    assertThat(state.getConstraint(aEb, BooleanConstraint.class)).isEqualTo(TRUE);
  }

  @Test
  public void test_transitive_GE() throws Exception {
    RelationalSymbolicValue ab = relationalSV(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, a, b);
    RelationalSymbolicValue bc = relationalSV(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, b, c);
    RelationalSymbolicValue deduced = ab.deduceTransitiveOrSimplified(bc);
    assertThat(deduced).isEqualTo(relationalSV(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, a, c));
  }

  @Test
  public void test_transitive_method_equals() throws Exception {
    RelationalSymbolicValue equalAB = relationalSV(Tree.Kind.EQUAL_TO, a, b);
    RelationalSymbolicValue methodEqualBC = new RelationalSymbolicValue(METHOD_EQUALS, b, c);
    RelationalSymbolicValue deduced = equalAB.deduceTransitiveOrSimplified(methodEqualBC);
    RelationalSymbolicValue expected = new RelationalSymbolicValue(METHOD_EQUALS, a, c);
    assertThat(deduced).isEqualTo(expected);
    deduced = methodEqualBC.deduceTransitiveOrSimplified(equalAB);
    assertThat(deduced).isEqualTo(expected);
  }

  @Test
  public void test_chained_transitivity() throws Exception {
    // create chain of relations in the form sv1 < sv2 < ... < sv45
    // and test if relation sv1 < sv45 is deduced
    int chainLength = 45;
    SymbolicValue[] sv = new SymbolicValue[chainLength];
    RelationalSymbolicValue[] given = new RelationalSymbolicValue[chainLength - 1];
    sv[0] = new SymbolicValue();
    for (int i = 1; i < chainLength; i++) {
      sv[i] = new SymbolicValue();
      given[i - 1] = relationalSV(Tree.Kind.LESS_THAN, sv[i - 1], sv[i]);
    }
    RelationalSymbolicValue firstLessThanLast = relationalSV(Tree.Kind.LESS_THAN, sv[0], sv[chainLength - 1]);
    ProgramState programState = Iterables.getOnlyElement(firstLessThanLast.setConstraint(stateWithRelations(given), TRUE));
    assertThat(programState.getConstraint(firstLessThanLast, BooleanConstraint.class)).isEqualTo(TRUE);
  }

  @Test
  public void test_not_equals_is_not_transitive() throws Exception {
    RelationalSymbolicValue aNEb = relationalSV(Tree.Kind.NOT_EQUAL_TO, a, b);
    RelationalSymbolicValue bNEc = relationalSV(Tree.Kind.NOT_EQUAL_TO, b, c);
    RelationalSymbolicValue relation = aNEb.deduceTransitiveOrSimplified(bNEc);
    assertThat(relation).isNull();
  }

  @Test
  public void test_transitive_constraint_copy() throws Exception {
    SymbolicValue aNEb = relationalSV(Tree.Kind.NOT_EQUAL_TO, b, a);
    SymbolicValue bNEc = relationalSV(Tree.Kind.NOT_EQUAL_TO, c, b);
    ProgramState programState = ProgramState.EMPTY_STATE;
    List<ProgramState> programStates = aNEb.setConstraint(programState, TRUE);
    programState = Iterables.getOnlyElement(programStates);
    programStates = bNEc.setConstraint(programState, TRUE);
    programState = Iterables.getOnlyElement(programStates);

    SymbolicValue aNEc = relationalSV(Tree.Kind.NOT_EQUAL_TO, c, a);
    programStates = aNEc.setConstraint(programState, FALSE);
    assertThat(programStates).hasSize(1);
    programStates = aNEc.setConstraint(programState, TRUE);
    assertThat(programStates).hasSize(1);
  }

  @Test
  public void test_equals_hashCode() throws Exception {
    SymbolicValue ab = relationalSV(Tree.Kind.EQUAL_TO, a, b);
    SymbolicValue ba = relationalSV(Tree.Kind.EQUAL_TO, b, a);
    assertThat(ab).isEqualTo(ba);
    assertThat(ab.hashCode()).isEqualTo(ba.hashCode());

    ab = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS, a, b);
    ba = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS, b, a);
    assertThat(ab).isEqualTo(ba);
    assertThat(ab.hashCode()).isEqualTo(ba.hashCode());

    ab = relationalSV(Tree.Kind.LESS_THAN, a, b);
    ba = relationalSV(Tree.Kind.LESS_THAN, b, a);
    assertThat(ab).isNotEqualTo(ba);

    SymbolicValue eq = relationalSV(Tree.Kind.EQUAL_TO, a, b);
    SymbolicValue eq1 = relationalSV(Tree.Kind.EQUAL_TO, b, b);
    SymbolicValue eq2 = relationalSV(Tree.Kind.EQUAL_TO, a, a);
    SymbolicValue neq = relationalSV(Tree.Kind.NOT_EQUAL_TO, b, a);
    assertThat(eq).isNotEqualTo(neq);
    assertThat(eq).isEqualTo(eq);
    assertThat(eq).isNotEqualTo(eq1);
    assertThat(eq).isNotEqualTo(eq2);
    assertThat(eq).isNotEqualTo(null);
    assertThat(eq).isNotEqualTo(new Object());

    SymbolicValue ab1 = relationalSV(Tree.Kind.LESS_THAN, a, b);
    SymbolicValue ab2 = relationalSV(Tree.Kind.LESS_THAN, a, b);
    SymbolicValue ab3 = relationalSV(Tree.Kind.LESS_THAN, a, new SymbolicValue());
    SymbolicValue ab4 = relationalSV(Tree.Kind.LESS_THAN, new SymbolicValue(), b);
    assertThat(ab1).isEqualTo(ab2);
    assertThat(ab1).isNotEqualTo(ab3);
    assertThat(ab1).isNotEqualTo(ab4);
  }

  @Test
  public void test_constraint_copy() throws Exception {
    ProgramState ps = ProgramState.EMPTY_STATE;
    SymbolicValue a = new SymbolicValue();
    SymbolicValue b = new SymbolicValue();
    List<ProgramState> newProgramStates = a.setConstraint(ps, ZERO);
    ps = Iterables.getOnlyElement(newProgramStates);
    // 0 >= b
    SymbolicValue aGEb = relationalSV(Tree.Kind.GREATER_THAN_OR_EQUAL_TO, b, a);
    newProgramStates = aGEb.setConstraint(ps, TRUE);
    ps = Iterables.getOnlyElement(newProgramStates);

    // Zero constraint should stay when Zero is >= to SV without any constraint
    assertThat(ps.getConstraint(a, DivisionByZeroCheck.ZeroConstraint.class)).isEqualTo(ZERO);
    assertThat(ps.getConstraint(b, DivisionByZeroCheck.ZeroConstraint.class)).isNull();
  }

  @Test
  public void test_setting_operands() throws Exception {
    RelationalSymbolicValue relSV = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.EQUAL, a, b);
    assertThatThrownBy(() -> SymbolicValueTestUtil.computedFrom(relSV, b, a))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Operands already set!");

    assertThatThrownBy(() -> SymbolicValueTestUtil.computedFrom(relSV, a, b, a))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void test_transitive_deduction() throws Exception {
    List<String> actual = new ArrayList<>();
    for (Tree.Kind r : operators) {
      RelationalSymbolicValue first = relationalSV(r, b, a);
      actual.addAll(combineWithAll(first, () -> relationToString(r, a, b)));
    }
    RelationalSymbolicValue eqAB = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS, a, b);
    actual.addAll(combineWithAll(eqAB, eqAB::toString));
    RelationalSymbolicValue neqAB = eqAB.inverse();
    actual.addAll(combineWithAll(neqAB, neqAB::toString));

    List<String> expected = IOUtils.readLines(getClass().getResourceAsStream("/relations/transitive.txt"));
    assertThat(actual).isEqualTo(expected);
  }

  private List<String> combineWithAll(RelationalSymbolicValue relation, Supplier<String> relationAsString) {
    List<String> actual = new ArrayList<>();
    for (Tree.Kind r : operators) {
      actual.add(
        String.format("%s && %s => %s", relationAsString.get(), relationToString(r, b, c), nullableToCollection(relation.deduceTransitiveOrSimplified(relationalSV(r, c, b)))));
    }
    RelationalSymbolicValue eq = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS, b, c);
    Stream.of(eq, eq.inverse()).forEach(rel ->
      actual.add(String.format("%s && %s => %s", relationAsString.get(), rel, nullableToCollection(relation.deduceTransitiveOrSimplified(rel))))
    );
    return actual;
  }

  private Collection<SymbolicValue> nullableToCollection(@Nullable RelationalSymbolicValue symbolicValue) {
    return symbolicValue == null ? Collections.emptySet() : Collections.singleton(symbolicValue);
  }

  @Test
  public void test_all_transitive_relations_are_computed() throws Exception {
    RelationalSymbolicValue ab = relationalSV(Tree.Kind.EQUAL_TO, a, b);
    RelationalSymbolicValue bc = relationalSV(Tree.Kind.EQUAL_TO, b, c);
    RelationalSymbolicValue cd = relationalSV(Tree.Kind.EQUAL_TO, c, d);
    Set<RelationalSymbolicValue> transitive = ab.transitiveRelations(ImmutableSet.of(ab, bc, cd));
    assertThat(transitive).containsOnly(relationalSV(Tree.Kind.EQUAL_TO, a, c), relationalSV(Tree.Kind.EQUAL_TO, b, d), relationalSV(Tree.Kind.EQUAL_TO, a, d));
  }

  @Test
  public void test_constraints_are_copied_over_transitive_relations() throws Exception {
    ProgramState ps = ProgramState.EMPTY_STATE;
    ps = Iterables.getOnlyElement(a.setConstraint(ps, ObjectConstraint.NULL));
    RelationalSymbolicValue ab = relationalSV(Tree.Kind.EQUAL_TO, a, b);
    ps = setTrue(ps, ab);
    assertNullConstraint(ps, b);

    RelationalSymbolicValue cd = relationalSV(Tree.Kind.EQUAL_TO, c, d);
    ps = setTrue(ps, cd);
    RelationalSymbolicValue de = relationalSV(Tree.Kind.EQUAL_TO, d, e);
    ps = setTrue(ps, de);
    assertNoConstraints(ps, c);
    assertNoConstraints(ps, d);
    assertNoConstraints(ps, e);
    assertThat(ps.getConstraint(relationalSV(Tree.Kind.EQUAL_TO, c, e), BooleanConstraint.class)).isEqualTo(TRUE);

    // this relation will connect two distinct groups of relations (ab) -- (cde)
    RelationalSymbolicValue bc = relationalSV(Tree.Kind.EQUAL_TO, b, c);
    ps = setTrue(ps, bc);
    // we assert that NULL was copied over to all values
    assertNullConstraint(ps, b);
    assertNullConstraint(ps, c);
    assertNullConstraint(ps, d);
    assertNullConstraint(ps, e);
  }

  @Test
  public void relationships_transitivity_should_take_known_relationships_into_account() throws Exception {
    // Testcase in that file can fail with a stackoverflow if known relations in program state are not taken into account.
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/InifiniteTransitiveRelationshipConstraintCopy.java", new NullDereferenceCheck());
  }

  private void assertNullConstraint(ProgramState ps, SymbolicValue sv) {
    assertThat(ps.getConstraint(sv, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NULL);
  }

  private void assertNoConstraints(ProgramState ps, SymbolicValue sv) {
    ConstraintsByDomain constraints = ps.getConstraints(sv);
    if (constraints != null) {
      assertThat(constraints).isEqualTo(ConstraintsByDomain.empty());
    }
  }

  private ProgramState setTrue(ProgramState ps, RelationalSymbolicValue ab) {
    return Iterables.getOnlyElement(ab.setConstraint(ps, TRUE));
  }

  private String relationToString(Tree.Kind kind, SymbolicValue leftOp, SymbolicValue rightOp) {
    return leftOp.toString() + operatorToString(kind) + rightOp.toString();
  }

  private String operatorToString(Tree.Kind kind) {
    switch (kind) {
      case EQUAL_TO:
        return "==";
      case NOT_EQUAL_TO:
        return "!=";
      case GREATER_THAN:
        return ">";
      case GREATER_THAN_OR_EQUAL_TO:
        return ">=";
      case LESS_THAN:
        return "<";
      case LESS_THAN_OR_EQUAL_TO:
        return "<=";
      default:
        throw new IllegalStateException();
    }
  }

  @Test
  public void test_equality() throws Exception {
    RelationalSymbolicValue equalRSV = relationalSV(Tree.Kind.EQUAL_TO, a, b);
    assertThat(equalRSV.isEquality()).isTrue();
    RelationalSymbolicValue methodEqualsRSV = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS, a, b);
    assertThat(methodEqualsRSV.isEquality()).isTrue();
    Stream.of(Tree.Kind.NOT_EQUAL_TO, Tree.Kind.LESS_THAN, Tree.Kind.LESS_THAN_OR_EQUAL_TO, Tree.Kind.GREATER_THAN, Tree.Kind.GREATER_THAN_OR_EQUAL_TO)
      .map(k -> relationalSV(k, a, b))
      .forEach(sv -> assertThat(sv.isEquality()).isFalse());
  }

  @Test
  public void test_to_string() throws Exception {
    RelationalSymbolicValue rsv = new RelationalSymbolicValue(EQUAL);
    Symbol var = new JavaSymbol.VariableJavaSymbol(0, "x", new JavaSymbol(JavaSymbol.TYP, 0, "A", Symbols.unknownSymbol));
    SymbolicValue left = new SymbolicValue() {
      @Override
      public String toString() {
        return "left";
      }
    };
    SymbolicValue right = new SymbolicValue() {
      @Override
      public String toString() {
        return "right";
      }
    };

    rsv.computedFrom(ImmutableList.of(new ProgramState.SymbolicValueSymbol(right, null), new ProgramState.SymbolicValueSymbol(left, var)));
    assertThat(rsv).hasToString("left(A#x)==right");
  }

  @Test
  public void test_constraint_copy_of_sv_with_no_constraints_is_symmetric() throws Exception {
    ProgramState ps = ProgramState.EMPTY_STATE;
    SymbolicValue svZero = new SymbolicValue();
    ps = Iterables.getOnlyElement(svZero.setConstraint(ps, ZERO));
    SymbolicValue sv = new SymbolicValue();
    RelationalSymbolicValue neq = new RelationalSymbolicValue(NOT_EQUAL, svZero, sv);
    ProgramState psWithNeq = setTrue(ps, neq);
    assertThat(psWithNeq.getConstraint(svZero, ZERO.getClass())).isEqualTo(ZERO);

    neq = new RelationalSymbolicValue(NOT_EQUAL, sv, svZero);
    psWithNeq = setTrue(ps, neq);
    assertThat(psWithNeq.getConstraint(svZero, ZERO.getClass())).isEqualTo(ZERO);
  }

  @Test
  public void test_constraint_copy_of_not_null_constraint() throws Exception {
    ProgramState ps = ProgramState.EMPTY_STATE;
    SymbolicValue svNotNull = new SymbolicValue();
    ps = Iterables.getOnlyElement(svNotNull.setConstraint(ps, ObjectConstraint.NOT_NULL));
    SymbolicValue sv = new SymbolicValue();
    // sv != NOT_NULL
    RelationalSymbolicValue neq = new RelationalSymbolicValue(NOT_EQUAL, svNotNull, sv);
    ProgramState result = setTrue(ps, neq);
    assertNoConstraints(result, sv);

    // sv != NULL
    neq = new RelationalSymbolicValue(NOT_EQUAL, NULL_LITERAL, sv);
    result = setTrue(ps, neq);
    assertThat(result.getConstraint(sv, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);

    // sv == NULL
    RelationalSymbolicValue eq = new RelationalSymbolicValue(EQUAL, NULL_LITERAL, sv);
    result = setTrue(ps, eq);
    assertNullConstraint(result, sv);
    // sv == NOT_NULL
    eq = new RelationalSymbolicValue(METHOD_EQUALS, sv, svNotNull);
    result = setTrue(ps, eq);
    assertThat(result.getConstraint(sv, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
  }

  @Test
  public void recursion_on_copy_constraint_should_stop() throws Exception {
    JavaCheckVerifier.verifyNoIssue("src/test/files/se/RelationSV.java", new NullDereferenceCheck());
  }

  @Test
  public void too_many_relationship_should_stop_se_engine() throws Exception {
    SETestUtils.createSymbolicExecutionVisitor("src/test/files/se/ExceedTransitiveLimit.java", new NullDereferenceCheck());
    assertThat(logTester.logs(LoggerLevel.DEBUG)).contains("Could not complete symbolic execution: ");
  }

  @Test
  public void recursion_on_copy_constraint_should_stop_distilled() {
    ProgramState ps = ProgramState.EMPTY_STATE;
    SymbolicValue sv0 = new SymbolicValue();
    ps = ps.addConstraint(sv0, TRUE);
    ps = ps.addConstraint(neq(sv0, NULL_LITERAL), TRUE);

    SymbolicValue sv1 = new SymbolicValue();
    ps = ps.addConstraint(eq(sv1, NULL_LITERAL), TRUE);
    ps = ps.addConstraint(neq(sv1, sv0), TRUE);

    SymbolicValue sv2 = new SymbolicValue();
    ps = ps.addConstraint(sv2, ObjectConstraint.NULL);
    ps = ps.addConstraint(eq(sv2, sv1), TRUE);

    RelationalSymbolicValue sv2NeqNull = neq(sv2, NULL_LITERAL);
    ps = ps.addConstraint(eq(sv2, sv2NeqNull), TRUE);
    ps = ps.addConstraint(eq(sv1, sv2NeqNull), TRUE);

    List<ProgramState> result = sv2NeqNull.setConstraint(ps, FALSE);
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getConstraint(sv2, BooleanConstraint.class)).isEqualTo(BooleanConstraint.FALSE);
  }

  private RelationalSymbolicValue eq(SymbolicValue op1, SymbolicValue op2) {
    return new RelationalSymbolicValue(EQUAL, op1, op2);
  }

  private RelationalSymbolicValue neq(SymbolicValue op1, SymbolicValue op2) {
    return new RelationalSymbolicValue(NOT_EQUAL, op1, op2);
  }
}
