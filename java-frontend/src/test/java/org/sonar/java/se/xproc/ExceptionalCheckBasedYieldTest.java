/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.model.Sema;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.Pair;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.se.SETestUtils.createSymbolicExecutionVisitor;
import static org.sonar.java.se.SETestUtils.createSymbolicExecutionVisitorAndSemantic;
import static org.sonar.java.se.SETestUtils.getMethodBehavior;
import static org.sonar.java.se.SETestUtils.mockMethodBehavior;

public class ExceptionalCheckBasedYieldTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final String FILENAME = "src/test/files/se/ExceptionalCheckBasedYields.java";
  private static final SymbolicValue SV_CAUSING_EXCEPTION = new SymbolicValue();

  @Test
  public void creation_of_new_yield() {
    String methodName = "method";
    SymbolicExecutionVisitor sev;
    MethodBehavior mb;

    Pair<SymbolicExecutionVisitor, Sema> visitorAndSemantic = createSymbolicExecutionVisitorAndSemantic(FILENAME);
    sev = visitorAndSemantic.a;
    Sema semanticModel = visitorAndSemantic.b;
    mb = getMethodBehavior(sev, methodName);

    // no creation of custom yields, 3 method yields
    assertThat(mb.yields()).hasSize(3);

    assertThat(mb.happyPathYields()).hasSize(1);
    assertThat(mb.happyPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == null)).hasSize(1);
    assertThat(mb.happyPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == BooleanConstraint.TRUE)).hasSize(0);
    assertThat(mb.happyPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == BooleanConstraint.FALSE)).hasSize(0);

    assertThat(mb.exceptionalPathYields()).hasSize(2);
    assertThat(mb.exceptionalPathYields()).as("All the exceptional yields are runtime exceptions").allMatch(y -> y.exceptionType(semanticModel).isUnknown());
    assertThat(mb.exceptionalPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == BooleanConstraint.TRUE)).hasSize(1);
    assertThat(mb.exceptionalPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == BooleanConstraint.FALSE)).hasSize(1);

    // new rule discard any call to plantFlowers(true) by creating a new yield
    SECheck check = new TestSECheck();
    sev = createSymbolicExecutionVisitor(FILENAME, check);
    mb = getMethodBehavior(sev, methodName);

    assertThat(mb.yields()).hasSize(4);
    // 2nd param can never be null
    assertThat(mb.yields().stream().filter(y -> y.parametersConstraints.get(0).isEmpty() && y.parametersConstraints.get(1).get(ObjectConstraint.class)== ObjectConstraint.NULL)).hasSize(1);
    assertThat(mb.yields().stream().filter(y -> y.parametersConstraints.get(1).get(ObjectConstraint.class) != ObjectConstraint.NULL)).hasSize(3);

    // happyPath with first parameter being true is discarded
    assertThat(mb.happyPathYields()).hasSize(2);

    // still 2 exceptional path
    assertThat(mb.exceptionalPathYields()).hasSize(2);
    assertThat(mb.exceptionalPathYields().filter(y -> y.exceptionType(semanticModel).isUnknown())).hasSize(1);
    assertThat(mb.exceptionalPathYields().filter(y -> y.exceptionType(semanticModel).isClass())).hasSize(1);
    assertThat(mb.exceptionalPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == BooleanConstraint.FALSE)).hasSize(1);

    ExceptionalYield exceptionalYield = mb.exceptionalPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == BooleanConstraint.TRUE).findFirst().get();
    assertThat(exceptionalYield).isInstanceOf(ExceptionalCheckBasedYield.class);

    ExceptionalCheckBasedYield seCheckExceptionalYield = (ExceptionalCheckBasedYield) exceptionalYield;
    assertThat(seCheckExceptionalYield.check()).isEqualTo(TestSECheck.class);
    assertThat(seCheckExceptionalYield.exceptionType(semanticModel)).isNotNull();
    assertThat(seCheckExceptionalYield.exceptionType(semanticModel).is("java.lang.UnsupportedOperationException")).isTrue();
    assertThat(seCheckExceptionalYield.exceptionType(semanticModel).isSubtypeOf("java.lang.RuntimeException")).isTrue();
    assertThat(seCheckExceptionalYield.generatedByCheck(check)).isTrue();
    assertThat(seCheckExceptionalYield.generatedByCheck(new SECheck() { })).isFalse();
    assertThat(seCheckExceptionalYield.parameterCausingExceptionIndex()).isEqualTo(0);

  }

  private static class TestSECheck extends SECheck {

    private static final MethodMatchers MATCHER = MethodMatchers.create().ofTypes("foo.bar.A").names("plantFlowers").addParametersMatcher("boolean").build();

    @Override
    public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
      ProgramState state = context.getState();
      if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION) && MATCHER.matches((MethodInvocationTree) syntaxNode)) {
        SymbolicValue param = state.peekValue();
        BooleanConstraint paramConstraint = state.getConstraint(param, BooleanConstraint.class);
        if (paramConstraint == BooleanConstraint.TRUE) {
          // create new yield with the exception
          context.addExceptionalYield(param, state, "java.lang.UnsupportedOperationException", this);
          // interrupt exploration (theoretical runtime exception)
          return null;
        }
      }
      return state;
    }
  }

  @Test
  public void exceptionType_is_required() {
    thrown.expect(IllegalArgumentException.class);
    final Class<? extends SECheck> seCheckClass = new SECheck() {
    }.getClass();
    String exceptionType = null;
    new ExceptionalCheckBasedYield(SV_CAUSING_EXCEPTION, exceptionType, seCheckClass, null, mockMethodBehavior());
  }

  @Test
  public void exceptionType_cannot_be_changed() {
    thrown.expect(UnsupportedOperationException.class);
    final Class<? extends SECheck> seCheckClass = new SECheck() {
    }.getClass();
    String exceptionType = "someException";
    ExceptionalCheckBasedYield yield = new ExceptionalCheckBasedYield(SV_CAUSING_EXCEPTION, exceptionType, seCheckClass, null, mockMethodBehavior());
    yield.setExceptionType("anotherException");
  }

  @Test
  public void test_toString() {
    String exceptionType = "org.foo.MyException";
    ExceptionalCheckBasedYield yield = new ExceptionalCheckBasedYield(SV_CAUSING_EXCEPTION, exceptionType, SECheck.class, null, mockMethodBehavior());

    assertThat(yield.toString()).isEqualTo("{params: [], exceptional (org.foo.MyException), check: SECheck}");
  }

  @Test
  public void test_equals() {
    final Class<? extends SECheck> seCheckClass1 = new SECheck() {
    }.getClass();
    final Class<? extends SECheck> seCheckClass2 = (new SECheck() {
    }).getClass();

    MethodBehavior mb = mockMethodBehavior();

    String mockedExceptionType1 = "SomeException";

    ExceptionalCheckBasedYield yield = new ExceptionalCheckBasedYield(SV_CAUSING_EXCEPTION, mockedExceptionType1, seCheckClass1, null, mb);
    ExceptionalYield otherYield = new ExceptionalCheckBasedYield(SV_CAUSING_EXCEPTION, mockedExceptionType1, seCheckClass1, null, mb);

    assertThat(yield).isNotEqualTo(null);
    assertThat(yield).isEqualTo(yield);
    assertThat(yield).isEqualTo(otherYield);

    // same exception, but simple exceptional yield
    otherYield = new ExceptionalYield(null, mb);
    otherYield.setExceptionType(mockedExceptionType1);
    assertThat(yield).isNotEqualTo(otherYield);

    // same exception, different SV
    otherYield = new ExceptionalCheckBasedYield(new SymbolicValue(), mockedExceptionType1, seCheckClass2, null, mb);
    assertThat(yield).isNotEqualTo(otherYield);

    // same exception, different check
    otherYield = new ExceptionalCheckBasedYield(SV_CAUSING_EXCEPTION, mockedExceptionType1, seCheckClass2, null, mb);
    assertThat(yield).isNotEqualTo(otherYield);

    // different exception, same check
    otherYield = new ExceptionalCheckBasedYield(SV_CAUSING_EXCEPTION, "SomeOtherException", seCheckClass1, null, mb);
    assertThat(yield).isNotEqualTo(otherYield);
  }

}
