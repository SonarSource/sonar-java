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
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.xproc.ExceptionalYield;
import org.sonar.java.se.xproc.HappyPathYield;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HappyPathYieldTest {

  @Test
  public void test_equals() {
    HappyPathYield yield = new HappyPathYield(1, false);
    HappyPathYield otherYield = new HappyPathYield(1, false);

    assertThat(yield).isNotEqualTo(null);
    assertThat(yield).isEqualTo(yield);
    assertThat(yield).isEqualTo(otherYield);

    // same arity and constraints but different return value
    otherYield.setResult(0, yield.resultConstraint());
    assertThat(yield).isNotEqualTo(otherYield);

    // same arity but different return constraint
    otherYield = new HappyPathYield(1, false);
    otherYield.setResult(yield.resultIndex(), ObjectConstraint.notNull());
    assertThat(yield).isNotEqualTo(otherYield);

    // same return constraint
    yield.setResult(-1, ObjectConstraint.notNull());
    otherYield = new HappyPathYield(1, false);
    otherYield.setResult(-1, ObjectConstraint.notNull());
    assertThat(yield).isEqualTo(otherYield);

    // same arity and parameters but exceptional yield
    assertThat(yield).isNotEqualTo(new ExceptionalYield(1, false));
  }

  @Test
  public void test_hashCode() {
    HappyPathYield methodYield = new HappyPathYield(0, true);
    HappyPathYield other = new HappyPathYield(0, true);

    // same values for same yields
    assertThat(methodYield.hashCode()).isEqualTo(other.hashCode());

    // different values for different yields
    other.setResult(-1, ObjectConstraint.notNull());
    assertThat(methodYield.hashCode()).isNotEqualTo(other.hashCode());

    // exceptional method yield
    assertThat(methodYield.hashCode()).isNotEqualTo(new ExceptionalYield(0, true));
  }

  @Test
  public void test_toString() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/files/se/HappyPathYields.java");
    Set<String> yieldsToString = getMethodBehavior(sev, "bar").yields().stream().map(MethodYield::toString).collect(Collectors.toSet());
    assertThat(yieldsToString).contains(
      "{params: [TRUE, NOT_NULL], result: null (-1)}",
      "{params: [FALSE, null], result: null (-1)}");
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
