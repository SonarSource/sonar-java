/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MethodYieldTest {
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
  @Test
  public void test_creation_of_states() throws Exception {
    Map<Symbol.MethodSymbol, MethodBehavior> behaviorCache = createSymbolicExecutionVisitor("src/test/files/se/XProcYields.java").behaviorCache;
    Symbol.MethodSymbol methodSymbol = behaviorCache.keySet().iterator().next();
    List<MethodYield> yields =
      behaviorCache.values().stream().flatMap(mb -> mb.yields().stream()).collect(Collectors.toList());

    ProgramState ps = ProgramState.EMPTY_STATE;
    SymbolicValue sv1 = new SymbolicValue(41);
    SymbolicValue sv2 = new SymbolicValue(42);
    SymbolicValue sv3 = new SymbolicValue(43);
    Symbol sym = new JavaSymbol.VariableJavaSymbol(0, "myVar", (JavaSymbol) methodSymbol);
    ps = ps.put(sym, sv1);

    MethodYield methodYield = yields.get(0);
    Collection<ProgramState> generatedStatesFromFirstYield = methodYield.statesAfterInvocation(Lists.newArrayList(sv1, sv2), ps, () -> sv3);
    assertThat(generatedStatesFromFirstYield).hasSize(1);
    assertThat(generatedStatesFromFirstYield.stream().collect(Collectors.toSet())).hasSize(1);

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
    assertThat(methodBehavior.yields().get(1).resultIndex).isEqualTo(0);
    assertThat(methodBehavior.yields().get(0).resultConstraint.isNull()).isTrue();
  }
}
