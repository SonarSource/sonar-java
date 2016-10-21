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
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.checks.NullDereferenceCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SymbolicExecutionVisitorTest {

  @Test
  public void method_behavior_cache_should_be_filled() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/MethodBehavior.java");
    assertThat(sev.behaviorCache.entrySet()).hasSize(5);
    assertThat(sev.behaviorCache.values().stream().filter(mb -> mb != null).count()).isEqualTo(5);
    // check order of method exploration : last is the topMethod as it requires the other to get its behavior.
    // Then, as we explore fully a path before switching to another one (see the LIFO in EGW) : qix is handled before foo.
    assertThat(sev.behaviorCache.keySet().stream().map(Symbol.MethodSymbol::name).collect(Collectors.toList())).containsSequence("topMethod", "bar", "qix", "foo", "independent");
  }

  @Test
  public void method_behavior_yields() {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/MethodYields.java");
    Optional<MethodSymbol> topMethod = sev.behaviorCache.keySet().stream().filter(s -> "method".equals(s.name())).findFirst();

    assertThat(topMethod.isPresent()).isTrue();
    MethodBehavior mb = sev.behaviorCache.get(topMethod.get());
    List<MethodYield> yields = mb.yields();
    assertThat(yields).hasSize(3);

    List<MethodYield> trueResults = yields.stream().filter(my -> BooleanConstraint.TRUE.equals(my.resultConstraint)).collect(Collectors.toList());
    assertThat(trueResults).hasSize(1);
    MethodYield trueResult = trueResults.get(0);

    // 'a' has constraint "null"
    assertThat(trueResult.parametersConstraints[0].isNull()).isTrue();
    // no constraint on 'b'
    assertThat(trueResult.parametersConstraints[1]).isNull();
    // result SV is a different SV than 'a' and 'b'
    assertThat(trueResult.resultIndex).isEqualTo(-1);

    List<MethodYield> falseResults = yields.stream().filter(my -> BooleanConstraint.FALSE.equals(my.resultConstraint)).collect(Collectors.toList());
    assertThat(falseResults).hasSize(2);
    // for both "False" results, 'a' has the constraint "not null"
    assertThat(falseResults.stream().filter(my -> !my.parametersConstraints[0].isNull()).count()).isEqualTo(2);
    // 1) 'b' has constraint "false", result is 'b'
    assertThat(falseResults.stream().filter(my -> BooleanConstraint.FALSE.equals(my.parametersConstraints[1]) && my.resultIndex == 1).count()).isEqualTo(1);

    // 2) 'b' is "true", result is a different SV than 'a' and 'b'
    assertThat(falseResults.stream().filter(my -> BooleanConstraint.TRUE.equals(my.parametersConstraints[1]) && my.resultIndex == -1).count()).isEqualTo(1);


    Optional<MethodSymbol> readFile = sev.behaviorCache.keySet().stream().filter(s -> "readFile".equals(s.name())).findFirst();
    assertThat(readFile.isPresent()).isTrue();
    MethodBehavior mbReadFile = sev.behaviorCache.get(readFile.get());
    System.out.println("foo");
  }

  @Test
  public void explore_method_with_recursive_call() throws Exception {
    SymbolicExecutionVisitor sev = createSymbolicExecutionVisitor("src/test/resources/se/RecursiveCall.java");
    assertThat(sev.behaviorCache.entrySet()).hasSize(1);
    assertThat(sev.behaviorCache.keySet().iterator().next().name()).isEqualTo("foo");
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
}