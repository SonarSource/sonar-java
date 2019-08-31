/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.model;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class JParserSemanticTest {

  @Test
  void expression_null_literal() {
    AbstractTypedTree e = (AbstractTypedTree) expression("null");
    assertThat(e.typeBinding).isNotNull();
  }

  @Test
  void expression_switch() {
    assertThat(expression("switch (0) { default -> 0; case 0 -> 0; }"))
      .isNotInstanceOf(AbstractTypedTree.class);
  }

  @Test
  void declaration_type() {
    CompilationUnitTree cu = test("class C { }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    assertThat(c.typeBinding).isNotNull();
  }

  private ExpressionTree expression(String expression) {
    CompilationUnitTree cu = test("class C { Object m() { return " + expression + " ; } }");
    ClassTree c = (ClassTree) cu.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    ReturnStatementTree s = (ReturnStatementTree) Objects.requireNonNull(m.block()).body().get(0);
    return Objects.requireNonNull(s.expression());
  }

  private CompilationUnitTree test(String source) {
    List<File> classpath = Collections.emptyList();
    CompilationUnitTree t = JParser.parse("12", "File.java", source, true, classpath);
    SemanticModel.createFor(t, new SquidClassLoader(classpath));
    return t;
  }

  @Test
  void should_skip_implicit_break_statement() {
    final String source = "class C { void m() { switch (0) { case 0 -> { } } } }";
    CompilationUnit cu = createAST(source);
    TypeDeclaration c = (TypeDeclaration) cu.types().get(0);
    MethodDeclaration m = c.getMethods()[0];
    SwitchStatement s = (SwitchStatement) m.getBody().statements().get(0);
    Block block = (Block) s.statements().get(1);
    BreakStatement breakStatement = (BreakStatement) block.statements().get(0);
    assertThat(breakStatement.getLength())
      .isZero();

    test(source);
  }

  private CompilationUnit createAST(String source) {
    String version = "12";
    ASTParser astParser = ASTParser.newParser(AST.JLS12);
    Map<String, String> options = new HashMap<>();
    options.put(JavaCore.COMPILER_COMPLIANCE, version);
    options.put(JavaCore.COMPILER_SOURCE, version);
    options.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, "enabled");
    astParser.setCompilerOptions(options);
    astParser.setEnvironment(
      new String[]{},
      new String[]{},
      new String[]{},
      true
    );
    astParser.setUnitName("File.java");
    astParser.setResolveBindings(true);
    astParser.setBindingsRecovery(true);
    astParser.setSource(source.toCharArray());
    return (CompilationUnit) astParser.createAST(null);
  }

}
