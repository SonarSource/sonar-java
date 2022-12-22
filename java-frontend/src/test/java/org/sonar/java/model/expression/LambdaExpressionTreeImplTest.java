/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.model.expression;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph.Block;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.model.assertions.SymbolAssert.assertThat;

class LambdaExpressionTreeImplTest {

  private static final String CLASS_WITH_METHOD_WITH_LAMBDA = "class A {\n"
    + "  void foo(Runnable r) {\n"
    + "    foo(%s);\n"
    + "  }\n"
    + "\n"
    + "  void bar() { }\n"
    + "  void qix() { }\n"
    + "  boolean test() { return false; }\n"
    + "}\n";

  @ParameterizedTest
  @ValueSource(strings = {"() -> bar()", "() -> { bar(); }"})
  void compute_cfg_with_simple_cases(String runnableLambda) {
    LambdaExpressionTree lambda = lambda(runnableLambda);
    ControlFlowGraph cfg = lambda.cfg();

    assertThat(cfg).isNotNull();
    // not recomputed
    assertThat(lambda.cfg()).isSameAs(cfg);

    List<? extends Block> blocks = cfg.blocks();
    // two blocks: B1 -> B0
    assertThat(blocks).hasSize(2);

    // identifier("bar") -> methodInvocation("bar()")
    List<Tree> elements = blocks.get(0).elements();
    assertThat(elements).hasSize(2);

    // semantically resolved
    Symbol symbol = ((MethodInvocationTree) elements.get(1)).methodSymbol();
    assertThat(symbol.isUnknown()).isFalse();
    assertThat(symbol.isMethodSymbol()).isTrue();
    assertThat(symbol).hasName("bar");
    assertThat(symbol.owner()).hasName("A");
  }

  @Test
  void compute_cfg_with_complexe_cases() {
    LambdaExpressionTree lambda = lambda("() -> {\n"
      + "if (test()) { bar(); } else { qix(); }\n"
      + "}");

    ControlFlowGraph cfg = lambda.cfg();
    assertThat(cfg).isNotNull();
    assertThat(cfg.blocks()).hasSize(4);

    List<Symbol> methodInvocations = cfg.blocks()
      .stream()
      .map(ControlFlowGraph.Block::elements)
      .flatMap(List::stream)
      .filter(t -> t.is(Tree.Kind.METHOD_INVOCATION))
      .map(MethodInvocationTree.class::cast)
      .map(MethodInvocationTree::methodSymbol)
      .collect(Collectors.toList());

    assertThat(methodInvocations)
      .hasSize(3)
      .noneMatch(Symbol::isUnknown)
      .allMatch(s -> "A".equals(s.owner().name()))
      .extracting(Symbol::name)
      .containsExactly("test", "bar", "qix");
  }

  private static LambdaExpressionTree lambda(String runnableLambda) {
    CompilationUnitTree cut = JParserTestUtils.parse(String.format(CLASS_WITH_METHOD_WITH_LAMBDA, runnableLambda));
    ClassTree classTree = (ClassTree) cut.types().get(0);
    MethodTree methodTree = (MethodTree) classTree.members().get(0);
    ExpressionStatementTree statement = (ExpressionStatementTree) methodTree.block().body().get(0);
    MethodInvocationTree mit = (MethodInvocationTree) statement.expression();
    return (LambdaExpressionTree) mit.arguments().get(0);
  }
}
