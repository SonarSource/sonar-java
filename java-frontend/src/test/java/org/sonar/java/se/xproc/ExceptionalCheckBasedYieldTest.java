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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.SECheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.se.SETestUtils.createSymbolicExecutionVisitor;
import static org.sonar.java.se.SETestUtils.getMethodBehavior;
import static org.sonar.java.se.SETestUtils.mockMethodBehavior;

public class ExceptionalCheckBasedYieldTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final String FILENAME = "src/test/files/se/ExceptionalCheckBasedYields.java";
  private static final SymbolicValue SV_CAUSING_EXCEPTION = new SymbolicValue(42);

  @Test
  public void creation_of_new_yield() {
    String methodName = "method";
    SymbolicExecutionVisitor sev;
    MethodBehavior mb;

    sev = createSymbolicExecutionVisitor(FILENAME);
    mb = getMethodBehavior(sev, methodName);

    // no creation of custom yields, 4 method yields
    assertThat(mb.yields()).hasSize(5);

    assertThat(mb.happyPathYields()).hasSize(3);
    assertThat(mb.happyPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == null)).hasSize(1);
    assertThat(mb.happyPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == BooleanConstraint.TRUE)).hasSize(1);
    assertThat(mb.happyPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == BooleanConstraint.FALSE)).hasSize(1);

    assertThat(mb.exceptionalPathYields()).hasSize(2);
    assertThat(mb.exceptionalPathYields()).as("All the exceptional yields are runtime exceptions").allMatch(y -> y.exceptionType() == null);
    assertThat(mb.exceptionalPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == BooleanConstraint.TRUE)).hasSize(1);
    assertThat(mb.exceptionalPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == BooleanConstraint.FALSE)).hasSize(1);

    // new rule discard any call to plantFlowers(true) by creating a new yield
    sev = createSymbolicExecutionVisitor(FILENAME, new TestSECheck());
    mb = getMethodBehavior(sev, methodName);

    assertThat(mb.yields()).hasSize(4);
    // 2nd param can never be null
    assertThat(mb.yields().stream().filter(y -> y.parametersConstraints.get(0).isEmpty() && y.parametersConstraints.get(1).get(ObjectConstraint.class)== ObjectConstraint.NULL)).hasSize(1);
    assertThat(mb.yields().stream().filter(y -> y.parametersConstraints.get(1).get(ObjectConstraint.class) != ObjectConstraint.NULL)).hasSize(3);

    // happyPath with first parameter being true is discarded
    assertThat(mb.happyPathYields()).hasSize(2);

    // still 2 exceptional path
    assertThat(mb.exceptionalPathYields()).hasSize(2);
    assertThat(mb.exceptionalPathYields().filter(y -> y.exceptionType() == null)).hasSize(1);
    assertThat(mb.exceptionalPathYields().filter(y -> y.exceptionType() != null)).hasSize(1);
    assertThat(mb.exceptionalPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == BooleanConstraint.FALSE)).hasSize(1);

    ExceptionalYield exceptionalYield = mb.exceptionalPathYields().filter(y -> y.parametersConstraints.get(0).get(BooleanConstraint.class) == BooleanConstraint.TRUE).findFirst().get();
    assertThat(exceptionalYield).isInstanceOf(ExceptionalCheckBasedYield.class);

    ExceptionalCheckBasedYield seCheckExceptionalYield = (ExceptionalCheckBasedYield) exceptionalYield;
    assertThat(seCheckExceptionalYield.check()).isEqualTo(TestSECheck.class);
    assertThat(seCheckExceptionalYield.exceptionType()).isNotNull();
    assertThat(seCheckExceptionalYield.exceptionType().is("java.lang.UnsupportedOperationException")).isTrue();
    assertThat(seCheckExceptionalYield.exceptionType().isSubtypeOf("java.lang.RuntimeException")).isTrue();
  }

  private static class TestSECheck extends SECheck {

    private static final MethodMatcher MATCHER = MethodMatcher.create().typeDefinition("foo.bar.A").name("plantFlowers").addParameter("boolean");

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
    Type exceptionType = null;
    new ExceptionalCheckBasedYield(SV_CAUSING_EXCEPTION, exceptionType, seCheckClass, null, mockMethodBehavior());
  }

  @Test
  public void exceptionType_cannot_be_changed() {
    thrown.expect(UnsupportedOperationException.class);
    final Class<? extends SECheck> seCheckClass = new SECheck() {
    }.getClass();
    Type exceptionType = mock(Type.class);
    ExceptionalCheckBasedYield yield = new ExceptionalCheckBasedYield(SV_CAUSING_EXCEPTION, exceptionType, seCheckClass, null, mockMethodBehavior());
    yield.setExceptionType(mock(Type.class));
  }

  @Test
  public void test_toString() {
    Type exceptionType = mock(Type.class);
    when(exceptionType.fullyQualifiedName()).thenReturn("org.foo.MyException");
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

    Type mockedExceptionType1 = mock(Type.class);

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
    otherYield = new ExceptionalCheckBasedYield(new SymbolicValue(7), mockedExceptionType1, seCheckClass2, null, mb);
    assertThat(yield).isNotEqualTo(otherYield);

    // same exception, different check
    otherYield = new ExceptionalCheckBasedYield(SV_CAUSING_EXCEPTION, mockedExceptionType1, seCheckClass2, null, mb);
    assertThat(yield).isNotEqualTo(otherYield);

    // different exception, same check
    otherYield = new ExceptionalCheckBasedYield(SV_CAUSING_EXCEPTION, mock(Type.class), seCheckClass1, null, mb);
    assertThat(yield).isNotEqualTo(otherYield);
  }

}
