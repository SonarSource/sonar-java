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
package org.sonar.java.se;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodYieldTest {
  @Test
  public void test_creation_of_states() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/XProcYields.java");
    Map.Entry<MethodSymbol, MethodBehavior> entry = getMethodBehavior(sev, "foo");
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
  public void test_toString() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/XProcYields.java");
    Set<String> yieldsToString = getMethodBehavior(sev, "bar").getValue().yields().stream().map(MethodYield::toString).collect(Collectors.toSet());
    assertThat(yieldsToString).contains(
      "{params: [TRUE, NOT_NULL], result: null (-1), exceptional: false}",
      "{params: [FALSE, null], result: null (-1), exceptional: false}");
  }

  private enum YieldStatus implements ObjectConstraint.Status {
    A, B
  }

  @Test
  public void all_constraints_should_be_valid_to_generate_a_new_state() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/XProcYields.java");
    Map.Entry<MethodSymbol, MethodBehavior> entry = getMethodBehavior(sev, "bar");
    Symbol.MethodSymbol methodSymbol = entry.getKey();
    List<MethodYield> yields = entry.getValue().yields();

    MethodYield trueYield = yields.stream()
      .filter(y -> y.parametersConstraints[0] instanceof BooleanConstraint && ((BooleanConstraint) y.parametersConstraints[0]).isTrue())
      .findFirst().get();
    // force status of the arg1 to be B
    trueYield.parametersConstraints[1] = ((ObjectConstraint<YieldStatus>) trueYield.parametersConstraints[1]).withStatus(YieldStatus.B);

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
    MethodYield yield = new MethodYield(1, false);
    MethodYield otherYield;

    assertThat(yield).isNotEqualTo(null);
    assertThat(yield).isNotEqualTo(new Object());

    // same instance
    assertThat(yield).isEqualTo(yield);

    // same constraints, same nb of parameters, same exceptional aspect
    assertThat(yield).isEqualTo(new MethodYield(1, false));

    // arity is taken into account
    assertThat(yield).isNotEqualTo(new MethodYield(0, false));

    // varargs is taken into account
    assertThat(yield).isNotEqualTo(new MethodYield(1, true));

    // same arity and constraints but exceptional path
    otherYield = new MethodYield(1, false);
    otherYield.exception = true;
    assertThat(yield).isNotEqualTo(otherYield);

    // same arity and constraints but different return value
    otherYield = new MethodYield(1, false);
    otherYield.resultIndex = 0;
    assertThat(yield).isNotEqualTo(otherYield);

    // same arity but different return constraint
    otherYield = new MethodYield(1, false);
    otherYield.resultConstraint = ObjectConstraint.notNull();
    assertThat(yield).isNotEqualTo(otherYield);

    // same return constraint
    yield.resultConstraint = ObjectConstraint.notNull();
    otherYield = new MethodYield(1, false);
    otherYield.resultConstraint = ObjectConstraint.notNull();
    assertThat(yield).isEqualTo(otherYield);

    // exceptional yields
    MethodYield exceptionalYield = new MethodYield(0, false);
    exceptionalYield.exception = true;
    otherYield = new MethodYield(0, false);

    otherYield.exception = false;
    assertThat(exceptionalYield).isNotEqualTo(otherYield);

    otherYield.exception = true;
    assertThat(exceptionalYield).isEqualTo(otherYield);

    Type exceptionType = Mockito.mock(Type.class);
    otherYield.exceptionType = exceptionType;
    assertThat(exceptionalYield).isNotEqualTo(otherYield);

    exceptionalYield.exceptionType = exceptionType;
    assertThat(exceptionalYield).isEqualTo(otherYield);
  }

  @Test
  public void test_hashCode() {
    MethodYield methodYield = new MethodYield(0, true);
    MethodYield other = new MethodYield(0, true);

    // same values for same yields
    assertThat(methodYield.hashCode()).isEqualTo(other.hashCode());

    // different values for different yields
    other.exception = true;
    assertThat(methodYield.hashCode()).isNotEqualTo(other.hashCode());
  }

  @Test
  public void exceptional_yields() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/ExceptionalYields.java");

    List<MethodYield> yields = getMethodBehavior(sev, "myMethod").getValue().yields();
    assertThat(yields).hasSize(4);

    List<MethodYield> exceptionalYields = yields.stream().filter(y -> y.exception).collect(Collectors.toList());
    assertThat(exceptionalYields).hasSize(3);

    // runtime exception
    Optional<MethodYield> runtimeException = exceptionalYields.stream().filter(y -> y.exceptionType == null).findFirst();
    assertThat(runtimeException.isPresent()).isTrue();
    MethodYield runtimeExceptionYield = runtimeException.get();
    assertThat(runtimeExceptionYield.resultIndex).isEqualTo(-1);
    assertThat(runtimeExceptionYield.resultConstraint).isNull();
    assertThat(runtimeExceptionYield.parametersConstraints[0]).isEqualTo(BooleanConstraint.FALSE);

    // exception from other method call
    Optional<MethodYield> implicitException = exceptionalYields.stream().filter(y -> y.exceptionType != null && y.exceptionType.is("org.foo.MyException2")).findFirst();
    assertThat(implicitException.isPresent()).isTrue();
    MethodYield implicitExceptionYield = implicitException.get();
    assertThat(implicitExceptionYield.resultIndex).isEqualTo(-1);
    assertThat(implicitExceptionYield.resultConstraint).isNull();
    assertThat(implicitExceptionYield.parametersConstraints[0]).isEqualTo(BooleanConstraint.FALSE);

    // explicitly thrown exception
    Optional<MethodYield> explicitException = exceptionalYields.stream().filter(y -> y.exceptionType != null && y.exceptionType.is("org.foo.MyException1")).findFirst();
    assertThat(explicitException.isPresent()).isTrue();
    MethodYield explicitExceptionYield = explicitException.get();
    assertThat(explicitExceptionYield.resultIndex).isEqualTo(-1);
    assertThat(explicitExceptionYield.resultConstraint).isNull();
    assertThat(explicitExceptionYield.parametersConstraints[0]).isEqualTo(BooleanConstraint.TRUE);
  }

  @Test
  public void exceptional_yields_void_method() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/ExceptionalYieldsVoidMethod.java");
    List<MethodYield> yields = getMethodBehavior(sev, "myVoidMethod").getValue().yields();
    assertThat(yields).hasSize(4);

    List<MethodYield> exceptionalYields = yields.stream().filter(y -> y.exception).collect(Collectors.toList());
    assertThat(exceptionalYields).hasSize(3);
    assertThat(exceptionalYields.stream().filter(y -> y.exceptionType == null).count()).isEqualTo(1);

    MethodYield explicitExceptionYield = exceptionalYields.stream().filter(y -> y.exceptionType != null && y.exceptionType.is("org.foo.MyException1")).findAny().get();
    assertThat(explicitExceptionYield.resultIndex).isEqualTo(-1);
    assertThat(explicitExceptionYield.resultConstraint).isNull();
    assertThat(explicitExceptionYield.parametersConstraints[0]).isEqualTo(ObjectConstraint.nullConstraint());

    MethodYield implicitExceptionYield = exceptionalYields.stream().filter(y -> y.exceptionType != null && y.exceptionType.is("org.foo.MyException2")).findAny().get();
    assertThat(implicitExceptionYield.resultIndex).isEqualTo(-1);
    assertThat(implicitExceptionYield.resultConstraint).isNull();
    assertThat(implicitExceptionYield.parametersConstraints[0]).isEqualTo(ObjectConstraint.notNull());
  }

  @Test
  public void constraints_on_varargs() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/VarArgsYields.java");

    Map.Entry<MethodSymbol, MethodBehavior> entry = getMethodBehavior(sev, "varArgMethod");
    Symbol.MethodSymbol methodSymbol = entry.getKey();
    List<MethodYield> yields = entry.getValue().yields();
    assertThat(yields).hasSize(3);
    assertThat(yields.stream().filter(y -> y.exception).count()).isEqualTo(2);

    MethodYield yield = yields.stream().filter(y -> !y.exception).findFirst().get();

    // check that we have NOT_NULL constraint on the first argument
    assertThat(yield.parametersConstraints[0].isNull()).isFalse();
    // check that we have NOT_NULL constraint on the variadic argument
    assertThat(yield.parametersConstraints[1].isNull()).isFalse();

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
    Map<Symbol.MethodSymbol, MethodBehavior> behaviorCache = createSymbolicExecutionVisitor("src/test/files/se/XProcNativeMethods.java").behaviorCache;
    behaviorCache.entrySet().stream().filter(e -> e.getKey().name().equals("foo")).forEach(e -> assertThat(e.getValue().yields()).hasSize(2));
  }

  @Test
  public void catch_class_cast_exception() throws Exception {
    Map<Symbol.MethodSymbol, MethodBehavior> behaviorCache = createSymbolicExecutionVisitor("src/test/files/se/XProcCatchClassCastException.java").behaviorCache;
    assertThat(behaviorCache.values()).hasSize(1);
    MethodBehavior methodBehavior = behaviorCache.values().iterator().next();
    assertThat(methodBehavior.yields()).hasSize(2);
    MethodYield[] expected = new MethodYield[] {
      buildMethodYield(0, null),
      buildMethodYield(-1, ObjectConstraint.nullConstraint())};
    assertThat(methodBehavior.yields()).contains(expected);
  }

  private MethodYield buildMethodYield(int resultIndex, @Nullable ObjectConstraint resultConstraint) {
    MethodYield methodYield = new MethodYield(1, false);
    methodYield.resultIndex = resultIndex;
    methodYield.parametersConstraints = new Constraint[] {null};
    methodYield.exception = false;
    methodYield.resultConstraint = resultConstraint;
    return methodYield;
  }

  private static SymbolicExecutionVisitor createSymbolicExecutionVisitor(String fileName) {
    ActionParser<Tree> p = JavaParser.createParser(Charsets.UTF_8);
    CompilationUnitTree cut = (CompilationUnitTree) p.parse(new File(fileName));
    SemanticModel semanticModel = SemanticModel.createFor(cut, new ArrayList<>());
    SymbolicExecutionVisitor sev = new SymbolicExecutionVisitor(Lists.newArrayList(new NullDereferenceCheck()));
    JavaFileScannerContext context = mock(JavaFileScannerContext.class);
    when(context.getTree()).thenReturn(cut);
    when(context.getSemanticModel()).thenReturn(semanticModel);
    sev.scanFile(context);
    return sev;
  }

  private static Map.Entry<Symbol.MethodSymbol, MethodBehavior> getMethodBehavior(SymbolicExecutionVisitor sev, String methodName) {
    Optional<Map.Entry<MethodSymbol, MethodBehavior>> mb = sev.behaviorCache.entrySet().stream()
      .filter(e -> methodName.equals(e.getKey().name()))
      .findFirst();
    assertThat(mb.isPresent()).isTrue();
    return mb.get();
  }
}
