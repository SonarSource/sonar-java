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
package org.sonar.java.se.xproc;

import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.collections.PCollections;
import org.sonar.java.collections.PMap;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.Flow;
import org.sonar.java.se.ProgramPoint;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.se.SETestUtils.createSymbolicExecutionVisitor;
import static org.sonar.java.se.SETestUtils.getMethodBehavior;
import static org.sonar.java.se.SETestUtils.mockMethodBehavior;

public class MethodYieldTest {
  @Test
  public void test_creation_of_states() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/XProcYields.java");
    MethodBehavior mb = getMethodBehavior(sev, "foo");

    ProgramState ps = ProgramState.EMPTY_STATE;
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    Symbol sym = new JavaSymbol.VariableJavaSymbol(0, "myVar", new JavaSymbol.MethodJavaSymbol(0, "dummy", null));
    ps = ps.put(sym, sv1);

    MethodYield methodYield = mb.happyPathYields().findFirst().get();
    Stream<ProgramState> generatedStatesFromFirstYield = methodYield.statesAfterInvocation(Lists.newArrayList(sv1, sv2), new ArrayList<>(), ps, () -> sv3);
    assertThat(generatedStatesFromFirstYield).hasSize(1);
  }

  @Test
  public void test_creation_of_flows() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/XProcYieldsFlows.java");
    MethodBehavior mb = getMethodBehavior(sev, "foo");

    MethodYield methodYield = mb.happyPathYields()
      .filter(y -> y.resultConstraint() != null && y.resultConstraint().get(ObjectConstraint.class) != ObjectConstraint.NULL).findFirst().get();

    Set<Flow> flowReturnValue = methodYield.flow(Collections.singletonList(-1), Lists.newArrayList(ObjectConstraint.class));
    assertThat(flowReturnValue.iterator().next().isEmpty()).isFalse();

    Set<Flow> flowFirstParam = methodYield.flow(Collections.singletonList(0), Lists.newArrayList(ObjectConstraint.class, BooleanConstraint.class));
    assertThat(flowFirstParam.iterator().next().isEmpty()).isFalse();
  }

  @Test
  public void test_yield_on_reassignments() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/XProcYieldsReassignments.java");
    MethodBehavior mb = getMethodBehavior(sev, "foo");
    assertThat(mb.happyPathYields())
      .allMatch(y -> y.parametersConstraints.get(0) != null && !ObjectConstraint.NULL.equals(y.parametersConstraints.get(0).get(ObjectConstraint.class)));
  }

  @Test
  public void flow_is_empty_when_yield_has_no_node() {
    MethodYield methodYield = new HappyPathYield(null, mockMethodBehavior(1, false));
    assertThat(methodYield.flow(Collections.singletonList(0), Lists.newArrayList(ObjectConstraint.class, BooleanConstraint.class))).isEmpty();
  }

  @Test
  public void flow_should_fail_if_no_parameters_are_passed() throws Exception {
    MethodYield methodYield = new HappyPathYield(null, mockMethodBehavior(1, false));
    try {
      methodYield.flow(Collections.emptyList(), Collections.singletonList(ObjectConstraint.class));
      fail("calling flow with empty list should have failed");
    } catch (IllegalArgumentException iae) {
      assertThat(iae).hasMessage("computing flow on empty symbolic value list should never happen");
    }
  }

  private static ExplodedGraph.Node mockNode() {
    return new ExplodedGraph().node(mock(ProgramPoint.class), null);
  }

  @Test
  public void yields_are_not_generated_by_check() {
    MethodYield yield = newMethodYield(mockMethodBehavior(1, false));

    assertThat(yield.generatedByCheck(null)).isFalse();
    assertThat(yield.generatedByCheck(new SECheck() {
    })).isFalse();
  }

  @Test
  public void test_yield_equality() {
    MethodBehavior methodBehavior = mockMethodBehavior(1, false);
    MethodYield yield = newMethodYield(methodBehavior);
    MethodYield otherYield;

    assertThat(yield).isNotEqualTo(null);
    assertThat(yield).isNotEqualTo(new Object());

    // same instance
    assertThat(yield).isEqualTo(yield);

    // method behavior not taken into account
    MethodYield myYield = newMethodYield(null);
    assertThat(yield).isEqualTo(myYield);

    // node not taken into account
    otherYield = newMethodYield(mockNode(), methodBehavior);
    assertThat(yield).isEqualTo(otherYield);

    // same arity and constraints on parameters but exceptional path
    otherYield = new ExceptionalYield(methodBehavior);
    assertThat(yield).isNotEqualTo(otherYield);

    // same arity and constraints on parameters but happy path path
    otherYield = new HappyPathYield(methodBehavior);
    assertThat(yield).isNotEqualTo(otherYield);

    ConstraintsByDomain nullConstraint = ConstraintsByDomain.empty().put(ObjectConstraint.NULL);
    MethodYield yield1 = newMethodYield(methodBehavior);
    yield1.parametersConstraints.add(nullConstraint);
    yield1.parametersConstraints.add(ConstraintsByDomain.empty());
    yield1.parametersConstraints.add(nullConstraint);
    MethodYield yield2 = newMethodYield(methodBehavior);
    yield2.parametersConstraints.add(nullConstraint);
    yield2.parametersConstraints.add(nullConstraint);
    yield2.parametersConstraints.add(ConstraintsByDomain.empty());

    assertThat(yield1).isNotEqualTo(yield2);
  }

  private static MethodYield newMethodYield(MethodBehavior methodBehavior) {
    return newMethodYield(null, methodBehavior);
  }

  private static MethodYield newMethodYield(ExplodedGraph.Node node, MethodBehavior methodBehavior) {
    return new MethodYield(node, methodBehavior) {

      @Override
      public String toString() {
        return null;
      }

      @Override
      public Stream<ProgramState> statesAfterInvocation(List<SymbolicValue> invocationArguments, List<Type> invocationTypes, ProgramState programState,
        Supplier<SymbolicValue> svSupplier) {
        return null;
      }
    };
  }

  @Test
  public void test_pmapToStream() {
    assertThat(MethodYield.pmapToStream(null)).isEmpty();

    PMap<Class<? extends Constraint>, Constraint> pmap = PCollections.emptyMap();
    assertThat(MethodYield.pmapToStream(pmap)).isEmpty();

    pmap = pmap.put(ObjectConstraint.class, ObjectConstraint.NOT_NULL);
    assertThat(MethodYield.pmapToStream(pmap)).containsOnly(ObjectConstraint.NOT_NULL);
  }

  @Test
  public void calling_varargs_method_with_no_arg() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/VarArgsWithNoArgYield.java");
    MethodBehavior mb = getMethodBehavior(sev, "toArr");
    List<MethodYield> yields = mb.yields();
    assertThat(yields).hasSize(1);
    assertThat(mb.isMethodVarArgs()).isTrue();
  }

  @Test
  public void constraints_on_varargs() throws Exception {
    ActionParser<Tree> p = JavaParser.createParser();
    CompilationUnitTree cut = (CompilationUnitTree) p.parse(new File("src/test/files/se/VarArgsYields.java"));
    SemanticModel semanticModel = SemanticModel.createFor(cut, new SquidClassLoader(new ArrayList<>()));
    SymbolicExecutionVisitor sev = new SymbolicExecutionVisitor(Lists.newArrayList(new SECheck[]{}), new BehaviorCache(new SquidClassLoader(new ArrayList<>())));
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    when(context.getTree()).thenReturn(cut);
    when(context.getSemanticModel()).thenReturn(semanticModel);
    sev.scanFile(context);

    MethodSymbol methodSymbol = ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0)).symbol();
    MethodBehavior mb = getMethodBehavior(sev, "varArgMethod");
    List<MethodYield> yields = mb.yields();
    assertThat(yields).hasSize(5);
    assertThat(mb.exceptionalPathYields()).hasSize(4);

    MethodYield yield = mb.happyPathYields().findFirst().get();

    // check that we have NOT_NULL constraint on the first argument
    assertThat(yield.parametersConstraints.get(0).get(ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    // check that we have NOT_NULL constraint on the variadic argument
    assertThat(yield.parametersConstraints.get(1).get(ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);

    List<IdentifierTree> usages = methodSymbol.usages();
    assertThat(usages).hasSize(6);

    List<List<Type>> arguments = usages.stream()
      .map(MethodYieldTest::getMethodIncoationArgumentsTypes)
      .collect(Collectors.toList());

    ProgramState ps = ProgramState.EMPTY_STATE;
    ProgramState psResult;
    SymbolicValue svFirstArg = new SymbolicValue();
    SymbolicValue svVarArg1 = new SymbolicValue();
    SymbolicValue svVarArg2 = new SymbolicValue();
    SymbolicValue svResult = new SymbolicValue();

    // apply constraint NotNull to parameter
    Collection<ProgramState> arrayOfA = yield.statesAfterInvocation(Lists.newArrayList(svFirstArg, svVarArg1), arguments.get(0), ps, () -> svResult).collect(Collectors.toList());
    assertThat(arrayOfA).hasSize(1);
    psResult = arrayOfA.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    assertThat(psResult.getConstraint(svVarArg1, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);

    // apply constraint NotNull to parameter (B[] is a subtype of A[])
    Collection<ProgramState> arrayOfB = yield.statesAfterInvocation(Lists.newArrayList(svFirstArg, svVarArg1), arguments.get(1), ps, () -> svResult).collect(Collectors.toList());
    assertThat(arrayOfB).hasSize(1);
    psResult = arrayOfB.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    assertThat(psResult.getConstraint(svVarArg1, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    // no constraint, as 'a' is not an array
    Collection<ProgramState> objectA = yield.statesAfterInvocation(Lists.newArrayList(svFirstArg, svVarArg1), arguments.get(2), ps, () -> svResult).collect(Collectors.toList());
    assertThat(objectA).hasSize(1);
    psResult = objectA.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    assertThat(psResult.getConstraint(svVarArg1, ObjectConstraint.class)).isNull();

    // no constraint, as 'a' and 'b' can not receive the constraint of the array
    Collection<ProgramState> objectsAandB = yield.statesAfterInvocation(Lists.newArrayList(svFirstArg, svVarArg1, svVarArg2), arguments.get(3), ps, () -> svResult).collect(Collectors.toList());
    assertThat(objectsAandB).hasSize(1);
    psResult = objectsAandB.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    assertThat(psResult.getConstraint(svVarArg1, ObjectConstraint.class)).isNull();
    assertThat(psResult.getConstraint(svVarArg2, ObjectConstraint.class)).isNull();

    // no param, we only learn something about the argument which is not variadic
    Collection<ProgramState> noParam = yield.statesAfterInvocation(Lists.newArrayList(svFirstArg), arguments.get(4), ps, () -> svResult).collect(Collectors.toList());
    assertThat(noParam).hasSize(1);
    psResult = noParam.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    // null param, contradiction, no resulting program state
    ps = ProgramState.EMPTY_STATE.addConstraint(svFirstArg, ObjectConstraint.NULL);
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
    Map<String, MethodBehavior> behaviorCache = createSymbolicExecutionVisitor("src/test/files/se/XProcNativeMethods.java").behaviorCache.behaviors;
    behaviorCache.entrySet().stream().filter(e -> "foo".contains(e.getKey())).forEach(e -> assertThat(e.getValue().yields()).hasSize(2));
  }

  @Test
  public void catch_class_cast_exception() throws Exception {
    Map<String, MethodBehavior> behaviorCache = createSymbolicExecutionVisitor("src/test/files/se/XProcCatchClassCastException.java").behaviorCache.behaviors;
    assertThat(behaviorCache.values()).hasSize(1);
    MethodBehavior methodBehavior = behaviorCache.values().iterator().next();
    assertThat(methodBehavior.yields()).hasSize(2);
    MethodYield[] expected = new MethodYield[] {
      buildMethodYield(0, null),
      buildMethodYield(-1, ConstraintsByDomain.empty().put(ObjectConstraint.NULL))};
    assertThat(methodBehavior.yields()).contains(expected);
  }

  private static MethodYield buildMethodYield(int resultIndex, @Nullable ConstraintsByDomain resultConstraint) {
    HappyPathYield methodYield = new HappyPathYield(mockMethodBehavior(1, false));
    methodYield.parametersConstraints = new ArrayList<>();
    methodYield.parametersConstraints.add(ConstraintsByDomain.empty());
    methodYield.setResult(resultIndex, resultConstraint);
    return methodYield;
  }


}
