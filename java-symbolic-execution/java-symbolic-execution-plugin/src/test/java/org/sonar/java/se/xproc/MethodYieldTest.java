/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.se.xproc;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.Flow;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramPoint;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.utils.JParserTestUtils;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonarsource.analyzer.commons.collections.PCollections;
import org.sonarsource.analyzer.commons.collections.PMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.se.utils.SETestUtils.createSymbolicExecutionVisitor;
import static org.sonar.java.se.utils.SETestUtils.getMethodBehavior;
import static org.sonar.java.se.utils.SETestUtils.mockMethodBehavior;
import static org.sonar.java.se.utils.SETestUtils.variable;

class MethodYieldTest {
  @Test
  void test_creation_of_states() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/XProcYields.java", new NullDereferenceCheck());
    MethodBehavior mb = getMethodBehavior(sev, "foo");

    ProgramState ps = ProgramState.EMPTY_STATE;
    SymbolicValue sv1 = new SymbolicValue();
    SymbolicValue sv2 = new SymbolicValue();
    SymbolicValue sv3 = new SymbolicValue();
    Symbol sym = variable("myVar");
    ps = ps.put(sym, sv1);

    MethodYield methodYield = mb.happyPathYields().findFirst().get();
    Stream<ProgramState> generatedStatesFromFirstYield = methodYield.statesAfterInvocation(Arrays.asList(sv1, sv2), new ArrayList<>(), ps, () -> sv3);
    assertThat(generatedStatesFromFirstYield).hasSize(1);
  }

  @Test
  void test_creation_of_flows() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/XProcYieldsFlows.java",
      new NullDereferenceCheck());
    MethodBehavior mb = getMethodBehavior(sev, "foo");

    MethodYield methodYield = mb.happyPathYields()
      .filter(y -> y.resultConstraint() != null && y.resultConstraint().get(ObjectConstraint.class) != ObjectConstraint.NULL).findFirst().get();

    Set<Flow> flowReturnValue = methodYield.flow(Collections.singletonList(-1), Collections.singletonList(ObjectConstraint.class), FlowComputation.MAX_REPORTED_FLOWS);
    assertThat(flowReturnValue.iterator().next().isEmpty()).isFalse();

    Set<Flow> flowFirstParam = methodYield.flow(Collections.singletonList(0), Arrays.asList(ObjectConstraint.class, BooleanConstraint.class), FlowComputation.MAX_REPORTED_FLOWS);
    assertThat(flowFirstParam.iterator().next().isEmpty()).isFalse();
  }

  @Test
  void test_yield_on_reassignments() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/XProcYieldsReassignments.java",
      new NullDereferenceCheck());
    MethodBehavior mb = getMethodBehavior(sev, "foo");
    assertThat(mb.happyPathYields())
      .isNotEmpty()
      .allMatch(y -> y.parametersConstraints.get(0) != null && !ObjectConstraint.NULL.equals(y.parametersConstraints.get(0).get(ObjectConstraint.class)));
  }

  @Test
  void flow_is_empty_when_yield_has_no_node() {
    MethodYield methodYield = new HappyPathYield(null, mockMethodBehavior(1, false));
    assertThat(methodYield.flow(Collections.singletonList(0), Arrays.asList(ObjectConstraint.class, BooleanConstraint.class), FlowComputation.MAX_REPORTED_FLOWS)).isEmpty();
  }

  @Test
  void flow_should_fail_if_no_parameters_are_passed() {
    MethodYield methodYield = new HappyPathYield(null, mockMethodBehavior(1, false));
    List<Integer> parameters = Collections.emptyList();
    List<Class<? extends Constraint>> domains = Collections.singletonList(ObjectConstraint.class);
    try {
      methodYield.flow(parameters, domains, FlowComputation.MAX_REPORTED_FLOWS);
      fail("calling flow with empty list should have failed");
    } catch (IllegalArgumentException iae) {
      assertThat(iae).hasMessage("computing flow on empty symbolic value list should never happen");
    }
  }

  private static ExplodedGraph.Node mockNode() {
    return new ExplodedGraph().node(mock(ProgramPoint.class), null);
  }

  @Test
  void yields_are_not_generated_by_check() {
    MethodYield methodYield = newMethodYield(mockMethodBehavior(1, false));

    assertThat(methodYield.generatedByCheck(null)).isFalse();
    assertThat(methodYield.generatedByCheck(new SECheck() {
    })).isFalse();
  }

  @Test
  void test_yield_equality() {
    MethodBehavior methodBehavior = mockMethodBehavior(1, false);
    MethodYield methodYield = newMethodYield(methodBehavior);
    assertThat(methodYield)
      .isNotEqualTo(null)
      .isNotEqualTo(new Object())
      // same instance
      .isEqualTo(methodYield)
      // method behavior not taken into account
      .isEqualTo(newMethodYield(null))
      // node not taken into account
      .isEqualTo(newMethodYield(mockNode(), methodBehavior))
      // same arity and constraints on parameters but exceptional path
      .isNotEqualTo(new ExceptionalYield(methodBehavior))
      // same arity and constraints on parameters but happy path path
      .isNotEqualTo(new HappyPathYield(methodBehavior));

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
  void test_pmapToStream() {
    assertThat(MethodYield.pmapToStream(null)).isEmpty();

    PMap<Class<? extends Constraint>, Constraint> pmap = PCollections.emptyMap();
    assertThat(MethodYield.pmapToStream(pmap)).isEmpty();

    pmap = pmap.put(ObjectConstraint.class, ObjectConstraint.NOT_NULL);
    assertThat(MethodYield.pmapToStream(pmap)).containsOnly(ObjectConstraint.NOT_NULL);
  }

  @Test
  void calling_varargs_method_with_no_arg() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/VarArgsWithNoArgYield.java",
      new NullDereferenceCheck());
    MethodBehavior mb = getMethodBehavior(sev, "toArr");
    List<MethodYield> yields = mb.yields();
    assertThat(yields).hasSize(1);
    assertThat(mb.isMethodVarArgs()).isTrue();
  }

  @Test
  void constraints_on_varargs() {
    JavaTree.CompilationUnitTreeImpl cut = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(new File("src/test/files/se/VarArgsYields.java"));
    Sema semanticModel = cut.sema;
    SymbolicExecutionVisitor sev = new SymbolicExecutionVisitor(Collections.singletonList(new NullDereferenceCheck()));
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    when(context.getTree()).thenReturn(cut);
    when(context.getSemanticModel()).thenReturn(semanticModel);
    when(context.getJavaVersion()).thenReturn(new JavaVersionImpl(11));
    sev.scanFile(context);

    MethodSymbol methodSymbol = ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0)).symbol();
    MethodBehavior mb = getMethodBehavior(sev, "varArgMethod");
    List<MethodYield> yields = mb.yields();
    assertThat(yields).hasSize(5);
    assertThat(mb.exceptionalPathYields()).hasSize(4);

    MethodYield methodYield = mb.happyPathYields().findFirst().get();

    // check that we have NOT_NULL constraint on the first argument
    assertThat(methodYield.parametersConstraints.get(0).get(ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    // check that we have NOT_NULL constraint on the variadic argument
    assertThat(methodYield.parametersConstraints.get(1).get(ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);

    List<IdentifierTree> usages = methodSymbol.usages();
    assertThat(usages).hasSize(6);

    List<List<Type>> arguments = usages.stream()
      .map(MethodYieldTest::getMethodIncoationArgumentsTypes)
      .toList();

    ProgramState ps = ProgramState.EMPTY_STATE;
    ProgramState psResult;
    SymbolicValue svFirstArg = new SymbolicValue();
    SymbolicValue svVarArg1 = new SymbolicValue();
    SymbolicValue svVarArg2 = new SymbolicValue();
    SymbolicValue svResult = new SymbolicValue();

    // apply constraint NotNull to parameter
    Collection<ProgramState> arrayOfA = methodYield.statesAfterInvocation(Arrays.asList(svFirstArg, svVarArg1), arguments.get(0), ps, () -> svResult).toList();
    assertThat(arrayOfA).hasSize(1);
    psResult = arrayOfA.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    assertThat(psResult.getConstraint(svVarArg1, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);

    // apply constraint NotNull to parameter (B[] is a subtype of A[])
    Collection<ProgramState> arrayOfB = methodYield.statesAfterInvocation(Arrays.asList(svFirstArg, svVarArg1), arguments.get(1), ps, () -> svResult).toList();
    assertThat(arrayOfB).hasSize(1);
    psResult = arrayOfB.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    assertThat(psResult.getConstraint(svVarArg1, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    // no constraint, as 'a' is not an array
    Collection<ProgramState> objectA = methodYield.statesAfterInvocation(Arrays.asList(svFirstArg, svVarArg1), arguments.get(2), ps, () -> svResult).toList();
    assertThat(objectA).hasSize(1);
    psResult = objectA.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    assertThat(psResult.getConstraint(svVarArg1, ObjectConstraint.class)).isNull();

    // no constraint, as 'a' and 'b' can not receive the constraint of the array
    Collection<ProgramState> objectsAandB = methodYield.statesAfterInvocation(Arrays.asList(svFirstArg, svVarArg1, svVarArg2), arguments.get(3), ps, () -> svResult).toList();
    assertThat(objectsAandB).hasSize(1);
    psResult = objectsAandB.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    assertThat(psResult.getConstraint(svVarArg1, ObjectConstraint.class)).isNull();
    assertThat(psResult.getConstraint(svVarArg2, ObjectConstraint.class)).isNull();

    // no param, we only learn something about the argument which is not variadic
    Collection<ProgramState> noParam = methodYield.statesAfterInvocation(Collections.singletonList(svFirstArg), arguments.get(4), ps, () -> svResult).toList();
    assertThat(noParam).hasSize(1);
    psResult = noParam.iterator().next();
    assertThat(psResult.getConstraint(svFirstArg, ObjectConstraint.class)).isEqualTo(ObjectConstraint.NOT_NULL);
    // null param, contradiction, no resulting program state
    ps = ProgramState.EMPTY_STATE.addConstraint(svFirstArg, ObjectConstraint.NULL);
    Collection<ProgramState> nullParam = methodYield.statesAfterInvocation(Arrays.asList(svFirstArg, svVarArg1), arguments.get(5), ps, () -> svResult).toList();
    assertThat(nullParam).isEmpty();
  }

  private static List<Type> getMethodIncoationArgumentsTypes(IdentifierTree identifier) {
    Tree tree = identifier;
    while(!tree.is(Tree.Kind.METHOD_INVOCATION)) {
      tree = tree.parent();
    }
    return ((MethodInvocationTree) tree).arguments().stream().map(ExpressionTree::symbolType).toList();
  }

  @Test
  void native_methods_behavior_should_not_be_used() {
    Map<String, MethodBehavior> behaviorCache = createSymbolicExecutionVisitor("src/test/files/se/XProcNativeMethods.java").behaviorCache.behaviors;
    behaviorCache.entrySet().stream().filter(e -> "foo".contains(e.getKey())).forEach(e -> assertThat(e.getValue().yields()).hasSize(2));
  }

  @Test
  void catch_class_cast_exception() {
    Map<String, MethodBehavior> behaviorCache =
      createSymbolicExecutionVisitor("src/test/files/se/XProcCatchClassCastException.java", new NullDereferenceCheck())
        .behaviorCache.behaviors;
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
