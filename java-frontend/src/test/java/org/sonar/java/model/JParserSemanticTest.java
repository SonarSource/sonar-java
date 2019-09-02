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
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.jupiter.api.Test;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.statement.ForStatementTreeImpl;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class JParserSemanticTest {

  @Test
  void should_not_throw_RecognitionException_in_case_of_non_syntax_errors() {
    test("import nonexisting;");
  }

  @Test
  void expression_null_literal() {
    AbstractTypedTree e = (AbstractTypedTree) expression("null");
    assertThat(e.typeBinding).isNotNull();
  }

  /**
   * @see org.eclipse.jdt.core.dom.InfixExpression#extendedOperands()
   */
  @Test
  void extended_operands() {
    BinaryExpressionTreeImpl e = (BinaryExpressionTreeImpl) expression("1 - 2 - 3");
    BinaryExpressionTreeImpl leftOperand = (BinaryExpressionTreeImpl) e.leftOperand();
    assertThat(e.typeBinding).isNotNull();
    assertThat(leftOperand.typeBinding).isSameAs(e.typeBinding);
  }

  @Test
  void expression_class_instance_creation() {
    NewClassTreeImpl e = (NewClassTreeImpl) expression("new Object() { }");
    ClassTreeImpl c = (ClassTreeImpl) e.classBody();
    assertThat(c.typeBinding).isNotNull();
  }

  @Test
  void expression_switch() {
    assertThat(expression("switch (0) { default -> 0; case 0 -> 0; }"))
      .isNotInstanceOf(AbstractTypedTree.class);
  }

  @Test
  void expression_method_invocation() {
    MethodInvocationTreeImpl e = (MethodInvocationTreeImpl) expression("m()");
    assertThat(e.methodBinding).isNotNull();
  }

  @Test
  void expression_super_method_invocation() {
    MethodInvocationTreeImpl e = (MethodInvocationTreeImpl) expression("super.toString()");
    assertThat(e.methodBinding).isNotNull();
  }

  @Test
  void statement_constructor_invocation() {
    CompilationUnitTree cu = test("class C { C() { this(null); } C(Object p) { } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ExpressionStatementTree s = (ExpressionStatementTree) m.block().body().get(0);
    MethodInvocationTreeImpl e = (MethodInvocationTreeImpl) s.expression();
    assertThat(e.methodBinding).isNotNull();
  }

  @Test
  void statement_super_constructor_invocation() {
    CompilationUnitTree cu = test("class C { C() { super(); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ExpressionStatementTree s = (ExpressionStatementTree) m.block().body().get(0);
    MethodInvocationTreeImpl e = (MethodInvocationTreeImpl) s.expression();
    assertThat(e.methodBinding).isNotNull();
  }

  @Test
  void statement_variable_declaration() {
    CompilationUnitTree cu = test("class C { void m() { int v; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    VariableTreeImpl s = (VariableTreeImpl) m.block().body().get(0);
    assertThat(s.variableBinding).isNotNull();
  }

  @Test
  void statement_for() {
    CompilationUnitTree cu = test("class C { void m() { for (int v;;) ; } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ForStatementTreeImpl s = (ForStatementTreeImpl) m.block().body().get(0);
    VariableTreeImpl v = (VariableTreeImpl) s.initializer().get(0);
    assertThat(v.variableBinding).isNotNull();
  }

  @Test
  void declaration_type() {
    CompilationUnitTree cu = test("class C { }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    assertThat(c.typeBinding).isNotNull();
  }

  @Test
  void declaration_method() {
    CompilationUnitTree cu = test("class C { void m() {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    assertThat(m.methodBinding).isNotNull();
  }

  @Test
  void declaration_parameter() {
    CompilationUnitTree cu = test("class C { void m(int p) {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    VariableTreeImpl p = (VariableTreeImpl) m.parameters().get(0);
    assertThat(p.variableBinding).isNotNull();
  }

  @Test
  void declaration_field() {
    CompilationUnitTree cu = test("class C { int f; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl f = (VariableTreeImpl) c.members().get(0);
    assertThat(f.variableBinding).isNotNull();
  }

  @Test
  void type_primitive() {
    CompilationUnitTree cu = test("interface I { int v; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
  }

  @Test
  void type_array() {
    CompilationUnitTree cu = test("interface I { int[][] v; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    JavaTree.ArrayTypeTreeImpl t = (JavaTree.ArrayTypeTreeImpl) v.type();
    assertThat(t.typeBinding).isNotNull();
    assertThat(t.typeBinding.getDimensions()).isEqualTo(2);
    t = (JavaTree.ArrayTypeTreeImpl) t.type();
    assertThat(t.typeBinding).isNotNull();
    assertThat(t.typeBinding.getDimensions()).isEqualTo(1);
  }

  @Test
  void type_parameterized() {
    CompilationUnitTree cu = test("interface I<T> { I<String> v; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
  }

  @Test
  void type_simple() {
    CompilationUnitTree cu = test("interface I { I1.I2 v; interface I2 {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
  }

  @Test
  void type_qualified() {
    CompilationUnitTree cu = test("interface I1<T> { I1<String>. @Annotation I2 v; interface I2 {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
  }

  @Test
  void type_name_qualified() {
    CompilationUnitTree cu = test("interface I1 { I1. @Annotation I2 v; interface I2 {} }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
  }

  @Test
  void type_wildcard() {
    CompilationUnitTree cu = test("interface I<T> { I<? extends Object> v; }");
    ClassTree c = (ClassTree) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    ParameterizedTypeTree p = (ParameterizedTypeTree) v.type();
    AbstractTypedTree t = ((AbstractTypedTree) p.typeArguments().get(0));
    assertThat(t.typeBinding).isNotNull();
  }

  @Test
  void type_union() {
    CompilationUnitTree cu = test("class C { void m() { try { } catch (E1 | E2 v) { } } }");
    ClassTree c = (ClassTree) cu.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    TryStatementTree s = (TryStatementTree) m.block().body().get(0);
    VariableTreeImpl v = (VariableTreeImpl) s.catches().get(0).parameter();
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
  }

  /**
   * @see Tree.Kind#VAR_TYPE
   */
  @Test
  void type_var() {
    CompilationUnitTree cu = test("class C { void m() { var v = 42; } }");
    ClassTree c = (ClassTree) cu.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    VariableTreeImpl v = (VariableTreeImpl) m.block().body().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
  }

  @Test
  void type_extra_dimensions() {
    CompilationUnitTree cu = test("interface I { I v[][]; }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    VariableTreeImpl v = (VariableTreeImpl) c.members().get(0);
    JavaTree.ArrayTypeTreeImpl t = (JavaTree.ArrayTypeTreeImpl) v.type();
    assertThat(t.typeBinding).isNotNull();
    assertThat(t.typeBinding.getDimensions()).isEqualTo(2);
    t = (JavaTree.ArrayTypeTreeImpl) t.type();
    assertThat(t.typeBinding).isNotNull();
    assertThat(t.typeBinding.getDimensions()).isEqualTo(1);
  }

  @Test
  void type_vararg() {
    CompilationUnitTree cu = test("interface I { void m(int[]... v); }");
    ClassTree c = (ClassTree) cu.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    VariableTreeImpl v = (VariableTreeImpl) m.parameters().get(0);
    AbstractTypedTree t = (AbstractTypedTree) v.type();
    assertThat(t.typeBinding).isNotNull();
    assertThat(t.typeBinding.getDimensions()).isEqualTo(2);
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
