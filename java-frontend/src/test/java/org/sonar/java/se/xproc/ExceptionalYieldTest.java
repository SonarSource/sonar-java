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

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExceptionalYieldTest {

  @Test
  public void test_equals() {
    ExceptionalYield yield = new ExceptionalYield(1, false);
    ExceptionalYield otherYield = new ExceptionalYield(1, false);

    assertThat(yield).isNotEqualTo(null);
    assertThat(yield).isEqualTo(yield);
    assertThat(yield).isEqualTo(otherYield);

    Type exceptionType = Mockito.mock(Type.class);
    otherYield.setExceptionType(exceptionType);
    assertThat(yield).isNotEqualTo(otherYield);

    yield.setExceptionType(exceptionType);
    assertThat(yield).isEqualTo(otherYield);
    assertThat(yield.exceptionType()).isEqualTo(otherYield.exceptionType());

    // same arity and parameters but happy yield
    assertThat(yield).isNotEqualTo(new HappyPathYield(1, false));
  }

  @Test
  public void test_hashCode() {
    ExceptionalYield methodYield = new ExceptionalYield(0, true);
    ExceptionalYield other = new ExceptionalYield(0, true);

    // same values for same yields
    assertThat(methodYield.hashCode()).isEqualTo(other.hashCode());

    // different values for different yields
    other.setExceptionType(mock(Type.class));
    assertThat(methodYield.hashCode()).isNotEqualTo(other.hashCode());

    // happy Path Yield method yield
    assertThat(methodYield.hashCode()).isNotEqualTo(new HappyPathYield(0, true));
  }

  @Test
  public void exceptional_yields() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/ExceptionalYields.java");

    MethodBehavior mb = getMethodBehavior(sev, "myMethod");
    assertThat(mb.yields()).hasSize(4);

    List<ExceptionalYield> exceptionalYields = mb.exceptionalPathYields().collect(Collectors.toList());
    assertThat(exceptionalYields).hasSize(3);

    // runtime exception
    Optional<ExceptionalYield> runtimeException = exceptionalYields.stream().filter(y -> y.exceptionType() == null).findFirst();
    assertThat(runtimeException.isPresent()).isTrue();
    MethodYield runtimeExceptionYield = runtimeException.get();
    assertThat(runtimeExceptionYield.parameterConstraint(0)).isEqualTo(BooleanConstraint.FALSE);

    // exception from other method call
    Optional<ExceptionalYield> implicitException = exceptionalYields.stream().filter(y -> y.exceptionType() != null && y.exceptionType().is("org.foo.MyException2")).findFirst();
    assertThat(implicitException.isPresent()).isTrue();
    MethodYield implicitExceptionYield = implicitException.get();
    assertThat(implicitExceptionYield.parameterConstraint(0)).isEqualTo(BooleanConstraint.FALSE);

    // explicitly thrown exception
    Optional<ExceptionalYield> explicitException = exceptionalYields.stream().filter(y -> y.exceptionType() != null && y.exceptionType().is("org.foo.MyException1")).findFirst();
    assertThat(explicitException.isPresent()).isTrue();
    MethodYield explicitExceptionYield = explicitException.get();
    assertThat(explicitExceptionYield.parameterConstraint(0)).isEqualTo(BooleanConstraint.TRUE);
  }

  @Test
  public void test_toString() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/ExceptionalYields.java");
    Set<String> yieldsToString = getMethodBehavior(sev, "myMethod").exceptionalPathYields().map(MethodYield::toString).collect(Collectors.toSet());
    assertThat(yieldsToString).contains(
      "{params: [FALSE], exceptional}",
      "{params: [TRUE], exceptional (org.foo.MyException1)}",
      "{params: [FALSE], exceptional (org.foo.MyException2)}");
  }

  @Test
  public void exceptional_yields_void_method() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/ExceptionalYieldsVoidMethod.java");
    MethodBehavior mb = getMethodBehavior(sev, "myVoidMethod");
    assertThat(mb.yields()).hasSize(4);

    List<ExceptionalYield> exceptionalYields = mb.exceptionalPathYields().collect(Collectors.toList());
    assertThat(exceptionalYields).hasSize(3);
    assertThat(exceptionalYields.stream().filter(y -> y.exceptionType() == null).count()).isEqualTo(1);

    MethodYield explicitExceptionYield = exceptionalYields.stream().filter(y -> y.exceptionType() != null && y.exceptionType().is("org.foo.MyException1")).findAny().get();
    assertThat(explicitExceptionYield.parameterConstraint(0)).isEqualTo(ObjectConstraint.nullConstraint());

    MethodYield implicitExceptionYield = exceptionalYields.stream().filter(y -> y.exceptionType() != null && y.exceptionType().is("org.foo.MyException2")).findAny().get();
    assertThat(implicitExceptionYield.parameterConstraint(0)).isEqualTo(ObjectConstraint.notNull());
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

  private static MethodBehavior getMethodBehavior(SymbolicExecutionVisitor sev, String methodName) {
    Optional<Map.Entry<MethodSymbol, MethodBehavior>> mb = sev.behaviorCache.behaviors.entrySet().stream()
      .filter(e -> methodName.equals(e.getKey().name()))
      .findFirst();
    assertThat(mb.isPresent()).isTrue();
    return mb.get().getValue();
  }
}
