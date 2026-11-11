/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.jdt;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.java.jdt.JDTTestUtils.sema;
import static org.sonar.java.model.assertions.SymbolAssert.assertThat;

class JDTSymbolTest {

  @Test
  void test_with_type_null() {
    ASTParser astParser = ASTParser.newParser(AST.getJLSLatest());
    astParser.setSource("class C { void m(String p) { } }".toCharArray());
    CompilationUnit cu = (CompilationUnit) astParser.createAST(null);
    JDTSema jSema = new JDTSema(cu.getAST());
    IVariableBinding mock = mock(IVariableBinding.class);
    when(mock.getKind()).thenReturn(IBinding.VARIABLE);
    JDTVariableSymbol jVariableSymbol = new JDTVariableSymbol(jSema, mock);
    assertThat(jVariableSymbol.metadata()).isNotNull();
  }

  @Test
  void null_package_binding_is_the_root_package() {
    assertThat(sema(test("")).packageSymbol(null)).isEqualTo(Symbol.ROOT_PACKAGE);
  }

  @Test
  void initializer_symbols_are_cached() {
    var cu = test("class C { static { int a; } { int b; } }");
    var c = (ClassTreeImpl) cu.types().get(0);
    var staticBlock = (BlockTree) c.members().get(0);
    var block = (BlockTree) c.members().get(1);
    var a = (VariableTree) staticBlock.body().get(0);
    var b = (VariableTree) block.body().get(0);

    assertThat(a.symbol().owner()).isSameAs(sema(cu).staticInitializerBlockSymbol((JDTTypeSymbol) c.symbol()));
    assertThat(b.symbol().owner()).isSameAs(sema(cu).initializerBlockSymbol((JDTTypeSymbol) c.symbol()));
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }
}
