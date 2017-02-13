/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.se.xproc;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ProgramPoint;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.java.se.SETestUtils.createSymbolicExecutionVisitor;
import static org.sonar.java.se.SETestUtils.getSymbolWithMethodBehavior;
import static org.sonar.java.se.SETestUtils.mockMethodBehavior;

public class MethodYieldTest {
  @Test
  public void test_creation_of_states() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/XProcYields.java");
    Map.Entry<MethodSymbol, MethodBehavior> entry = getSymbolWithMethodBehavior(sev, "foo");
    Symbol.MethodSymbol methodSymbol = entry.getKey();
    List<MethodYield> yields = entry.getValue().yields();

    ProgramState ps = ProgramState.EMPTY_STATE;
    SymbolicValue sv1 = new SymbolicValue(41);
    SymbolicValue sv2 = new SymbolicValue(42);
    SymbolicValue sv3 = new SymbolicValue(43);
    Symbol sym = new JavaSymbol.VariableJavaSymbol(0, "myVar", (JavaSymbol) methodSymbol);
    ps = ps.put(sym, sv1);

    MethodYield methodYield = yields.get(0);
    Stream<ProgramState> generatedStatesFromFirstYield = methodYield.statesAfterInvocation(Lists.newArrayList(sv1, sv2), Lists.newArrayList(), ps, () -> sv3);
    assertThat(generatedStatesFromFirstYield).hasSize(1);
  }

  @Test
  public void test_creation_of_flows() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/XProcYieldsFlows.java");
    MethodBehavior mb = getSymbolWithMethodBehavior(sev, "foo").getValue();

    MethodYield methodYield = mb.happyPathYields().filter(y -> y.resultConstraint() != null && !y.resultConstraint().isNull()).findFirst().get();

    List<JavaFileScannerContext.Location> flowReturnValue = methodYield.flow(-1);
    assertThat(flowReturnValue).isNotEmpty();

    List<JavaFileScannerContext.Location> flowFirstParam = methodYield.flow(0);
    assertThat(flowFirstParam).isNotEmpty();
  }

  @Test
  public void test_yield_on_reassignments() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/XProcYieldsReassignments.java");
    Map.Entry<MethodSymbol, MethodBehavior> entry = getSymbolWithMethodBehavior(sev, "foo");
    MethodBehavior mb = entry.getValue();

    assertThat(mb.happyPathYields()).allMatch(y -> y.parameterConstraint(0) != null && !y.parameterConstraint(0).isNull());
  }

  @Test
  public void flow_is_empty_when_yield_has_no_node() {
    MethodYield methodYield = new HappyPathYield(null, mockMethodBehavior(1, false));
    assertThat(methodYield.flow(0)).isEmpty();
  }

  private static ExplodedGraph.Node mockNode() {
    return new ExplodedGraph().node(mock(ProgramPoint.class), null);
  }

  private enum YieldStatus implements ObjectConstraint.Status {
    A, B
  }

  @Test
  public void all_constraints_should_be_valid_to_generate_a_new_state() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/XProcYields.java");
    Map.Entry<MethodSymbol, MethodBehavior> entry = getSymbolWithMethodBehavior(sev, "bar");
    Symbol.MethodSymbol methodSymbol = entry.getKey();
    List<MethodYield> yields = entry.getValue().yields();

    MethodYield trueYield = yields.stream()
      .filter(y -> y.parameterConstraint(0) instanceof BooleanConstraint && ((BooleanConstraint) y.parameterConstraint(0)).isTrue())
      .findFirst().get();
    // force status of the arg1 to be B
    trueYield.setParameterConstraint(1, ((ObjectConstraint<YieldStatus>) trueYield.parameterConstraint(1)).withStatus(YieldStatus.B));

    ProgramState ps = ProgramState.EMPTY_STATE;
    SymbolicValue sv1 = new SymbolicValue(41);
    SymbolicValue sv2 = new SymbolicValue(42);
    SymbolicValue sv3 = new SymbolicValue(43);

    Symbol myBoolean = new JavaSymbol.VariableJavaSymbol(0, "myBoolean", (JavaSymbol) methodSymbol);
    ps = ps.put(myBoolean, sv1);
    ps = ps.addConstraint(sv1, BooleanConstraint.TRUE);

    Symbol myVar = new JavaSymbol.VariableJavaSymbol(0, "myVar", (JavaSymbol) methodSymbol);
    ps = ps.put(myVar, sv2);
    ps = ps.addConstraint(sv2, new ObjectConstraint(false, false, YieldStatus.A));

    // status of sv2 should be changed from A to B
    Collection<ProgramState> generatedStatesFromFirstYield = trueYield.statesAfterInvocation(Lists.newArrayList(sv1, sv2), Lists.newArrayList(), ps, () -> sv3)
      .collect(Collectors.toList());
    assertThat(generatedStatesFromFirstYield).hasSize(1);
    assertThat(generatedStatesFromFirstYield.iterator().next().getConstraintWithStatus(sv2, YieldStatus.B)).isNotNull();
  }

  @Test
  public void test_yield_equality() {
    MethodBehavior methodBehavior = mockMethodBehavior(1, false);
    MethodYield yield = new HappyPathYield(methodBehavior);
    MethodYield otherYield;

    assertThat(yield).isNotEqualTo(null);
    assertThat(yield).isNotEqualTo(new Object());

    // same instance
    assertThat(yield).isEqualTo(yield);

    // same constraints, same number of parameters, same exceptional aspect
    assertThat(yield).isEqualTo(new HappyPathYield(methodBehavior));

    // node and behavior are not taken into account
    otherYield = new HappyPathYield(mockNode(), methodBehavior);
    assertThat(yield).isEqualTo(otherYield);

    // same arity and constraints on parameters but exceptional path
    otherYield = new ExceptionalYield(methodBehavior);
    assertThat(yield).isNotEqualTo(otherYield);
  }

  @Test
  public void constraints_on_varargs() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/VarArgsYields.java");

    Map.Entry<MethodSymbol, MethodBehavior> entry = getSymbolWithMethodBehavior(sev, "varArgMethod");
    Symbol.MethodSymbol methodSymbol = entry.getKey();
    List<MethodYield> yields = entry.getValue().yields();
    assertThat(yields).hasSize(3);
    assertThat(yields.stream().filter(y -> y instanceof ExceptionalYield).count()).isEqualTo(2);

    MethodYield yield = yields.stream().filter(y -> y instanceof HappyPathYield).findFirst().get();

    // check that we have NOT_NULL constraint on the first argument
    assertThat(yield.parameterConstraint(0).isNull()).isFalse();
    // check that we have NOT_NULL constraint on the variadic argument
    assertThat(yield.parameterConstraint(1).isNull()).isFalse();

    List<IdentifierTree> usages = methodSymbol.usages();
    assertThat(usages).hasSize(6);

    List<List<Type>> arguments = usages.stream()
      .map(MethodYieldTest::getMethodIncoationArgumentsTypes)
      .collect(Collectors.toList());

    ProgramState ps = ProgramState.EMPTY_STATE;
    ProgramState psResult;
    SymbolicValue svFirstArg = new SymbolicValue(41);
    SymbolicValue svVarArg1 = new SymbolicValue(42);
    SymbolicValue svVarArg2 = new SymbolicValue(43);
    SymbolicValue svResult = new SymbolicValue(43);

    // apply constraint NotNull to parameter
    Collection<ProgramState> arrayOfA = yield.statesAfterInvocation(Lists.newArrayList(svFirstArg, svVarArg1), arguments.get(0), ps, () -> svResult).collect(Collectors.toList());
    assertThat(arrayOfA).hasSize(1);
    psResult = arrayOfA.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg).isNull()).isFalse();
    assertThat(psResult.getConstraint(svVarArg1).isNull()).isFalse();

    // apply constraint NotNull to parameter (B[] is a subtype of A[])
    Collection<ProgramState> arrayOfB = yield.statesAfterInvocation(Lists.newArrayList(svFirstArg, svVarArg1), arguments.get(1), ps, () -> svResult).collect(Collectors.toList());
    assertThat(arrayOfB).hasSize(1);
    psResult = arrayOfB.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg).isNull()).isFalse();
    assertThat(psResult.getConstraint(svVarArg1).isNull()).isFalse();

    // no constraint, as 'a' is not an array
    Collection<ProgramState> objectA = yield.statesAfterInvocation(Lists.newArrayList(svFirstArg, svVarArg1), arguments.get(2), ps, () -> svResult).collect(Collectors.toList());
    assertThat(objectA).hasSize(1);
    psResult = objectA.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg).isNull()).isFalse();
    assertThat(psResult.getConstraint(svVarArg1)).isNull();

    // no constraint, as 'a' and 'b' can not receive the constraint of the array
    Collection<ProgramState> objectsAandB = yield.statesAfterInvocation(Lists.newArrayList(svFirstArg, svVarArg1, svVarArg2), arguments.get(3), ps, () -> svResult).collect(Collectors.toList());
    assertThat(objectsAandB).hasSize(1);
    psResult = objectsAandB.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg).isNull()).isFalse();
    assertThat(psResult.getConstraint(svVarArg1)).isNull();
    assertThat(psResult.getConstraint(svVarArg2)).isNull();

    // no param, we only learn something about the argument which is not variadic
    Collection<ProgramState> noParam = yield.statesAfterInvocation(Lists.newArrayList(svFirstArg), arguments.get(4), ps, () -> svResult).collect(Collectors.toList());
    assertThat(noParam).hasSize(1);
    psResult = noParam.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg).isNull()).isFalse();

    // null param, contradiction, no resulting program state
    ps = ProgramState.EMPTY_STATE.addConstraint(svFirstArg, ObjectConstraint.nullConstraint());
    Collection<ProgramState> nullParam = yield.statesAfterInvocation(Lists.newArrayList(svFirstArg, svVarArg1), arguments.get(5), ps, () -> svResult).collect(Collectors.toList());
    assertThat(nullParam).isEmpty();
  }

  private static List<Type> getMethodIncoationArgumentsTypes(IdentifierTree identifier) {
    Tree tree = identifier;
    while(!tree.is(Tree.Kind.METHOD_INVOCATION)) {
      tree = tree.parent();
    }
    return ((MethodInvocationTree) tree).arguments().stream().map(ExpressionTree::symbolType).collect(Collectors.toList());
  }

  @Test
  public void native_methods_behavior_should_not_be_used() throws Exception {
    Map<Symbol.MethodSymbol, MethodBehavior> behaviorCache = createSymbolicExecutionVisitor("src/test/files/se/XProcNativeMethods.java").behaviorCache.behaviors;
    behaviorCache.entrySet().stream().filter(e -> e.getKey().name().equals("foo")).forEach(e -> assertThat(e.getValue().yields()).hasSize(2));
  }

  @Test
  public void catch_class_cast_exception() throws Exception {
    Map<Symbol.MethodSymbol, MethodBehavior> behaviorCache = createSymbolicExecutionVisitor("src/test/files/se/XProcCatchClassCastException.java").behaviorCache.behaviors;
    assertThat(behaviorCache.values()).hasSize(1);
    MethodBehavior methodBehavior = behaviorCache.values().iterator().next();
    assertThat(methodBehavior.yields()).hasSize(2);
    MethodYield[] expected = new MethodYield[] {
      buildMethodYield(0, null),
      buildMethodYield(-1, ObjectConstraint.nullConstraint())};
    assertThat(methodBehavior.yields()).contains(expected);
  }

  private static MethodYield buildMethodYield(int resultIndex, @Nullable ObjectConstraint resultConstraint) {
    HappyPathYield methodYield = new HappyPathYield(mockMethodBehavior(1, false));
    methodYield.setResult(resultIndex, resultConstraint);
    return methodYield;
  }


}
