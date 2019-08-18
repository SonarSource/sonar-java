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

import com.sonar.sslr.api.RecognitionException;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

public class JParserTest {

  @Test
  public void should_throw_RecognitionException_in_case_of_syntax_error() {
    try { // Note that without check for syntax errors will cause IndexOutOfBoundsException
      test("class C");
      fail("exception expected");
    } catch (RecognitionException e) {
      assertEquals(1, e.getLine());
      assertEquals("Parse error at line 1 column 6: Syntax error, insert \"ClassBody\" to complete CompilationUnit", e.getMessage());
    }
    try { // Note that syntax tree will be correct even in presence of this syntax error
      // javac doesn't produce error in this case, however this is not allowed according to JLS 12
      test("import a; ; import b;");
      fail("exception expected");
    } catch (RecognitionException e) {
      assertEquals("Parse error at line 1 column 10: Syntax error on token \";\", delete this token", e.getMessage());
    }
  }

  @Test
  public void should_throw_RecognitionException_in_case_of_lexical_error() {
    try { // Note that without check for errors will cause InvalidInputException
      testExpression("''");
      fail("exception expected");
    } catch (RuntimeException e) {
      assertEquals("Parse error at line 1 column 30: Invalid character constant", e.getMessage());
    }
  }

  @Test
  public void err() {
    try {
      // ASTNode.METHOD_DECLARATION with flag ASTNode.MALFORMED
      test("interface Foo { public foo(); // comment\n }");
      fail("exception expected");
    } catch (IndexOutOfBoundsException ignore) {
    }
  }

  @Test
  public void eof() {
    {
      CompilationUnitTree t = test("");
      assertEquals("", t.eofToken().text());
      assertEquals(1, t.eofToken().line());
      assertEquals(0, t.eofToken().column());
    }
    {
      CompilationUnitTree t = test(" ");
      assertEquals("", t.eofToken().text());
      assertEquals(1, t.eofToken().line());
      assertEquals(1, t.eofToken().column());
    }
    {
      CompilationUnitTree t = test(" \n");
      assertEquals("", t.eofToken().text());
      assertEquals(2, t.eofToken().line());
      assertEquals(0, t.eofToken().column());
    }
  }

  /**
   * @see org.eclipse.jdt.core.dom.InfixExpression#extendedOperands()
   */
  @Test
  public void extended_operands() {
    test("class C { void m() { m( 1 - 2 - 3 ); } }");

    // no extendedOperands in case of parentheses:
    test("class C { void m() { m( (1 - 2) - 3 ); } }");
    test("class C { void m() { m( 1 - (2 - 3) ); } }");
  }

  /**
   * @see org.eclipse.jdt.core.dom.MethodDeclaration#extraDimensions()
   * @see org.eclipse.jdt.core.dom.VariableDeclarationFragment#extraDimensions()
   * @see org.eclipse.jdt.core.dom.SingleVariableDeclaration#extraDimensions()
   */
  @Test
  public void extra_dimensions() {
    test("interface I { int m()[]; }");
    test("interface I { int m(int p[]); }");
    test("interface I { int f1[], f2[][]; }");
  }

  /**
   * @see Tree.Kind#VAR_TYPE
   */
  @Test
  public void type_var() {
    test("class C { void m() { var i = 42; } }");
  }

  @Test
  public void type_arguments() {
    test("class C<P> {"
      + "  void m() {"
      + "    this.<C  /**/> m();"
      + "    this.<C<C   >> m();"
      + "  }"
      + "}");
  }

  @Test
  public void type_parameters() {
    test("class C<P> {"
      + "  <P         /**/> void m1() {}"
      + "  <P extends C<C>> void m2() {}"
      + "}");
  }

  @Test
  public void empty_declarations() {
    // as the only declaration
    test(";");

    // after last import declaration
    test("import a; import b; ;");

    // before first and after each body declaration
    test("class C { ; void m(); ; }");
  }

  /**
   * @see org.eclipse.jdt.core.dom.SingleVariableDeclaration#isVarargs()
   */
  @Test
  public void varargs() {
    test("class I { void m(int... p) { m(1); } }");
  }

  @Test
  public void declaration_package() {
    test("@Annotation package org.example;");
  }

  @Test
  public void declaration_class() {
    test("class A extends B implements I1, I2 { }");
  }

  @Test
  public void declaration_enum() {
    test("enum E { C1 , C2 }");
    test("enum E { C1 , C2 ; }");

    test("enum E { C() }");
    test("enum E { C { } }");

    CompilationUnitTree cu = test("enum E { C }");
    ClassTree t = (ClassTree) cu.types().get(0);
    EnumConstantTree c = (EnumConstantTree) t.members().get(0);
    assertSame(
      c.simpleName(),
      c.initializer().identifier()
    );
  }

  @Test
  public void statement_variable_declaration() {
    CompilationUnitTree t = test("class C { void m() { int a, b; } }");
    ClassTree c = (ClassTree) t.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    BlockTree s = m.block();
    assertNotNull(s);
    VariableTree s1 = (VariableTree) s.body().get(0);
    VariableTree s2 = (VariableTree) s.body().get(1);
    assertSame(s1.type(), s2.type());
  }

  @Test
  public void statement_for() {
    testStatement("for ( ; ; ) ;");
    testStatement("for (int i = 0, j = 0 ; ; i++, j++) ;");
  }

  @Test
  public void statement_switch() {
    test("class C { void m() { switch (0) { case 0: } } }");

    // Java 12 preview feature
    test("class C { void m() { switch (0) { case 0, 1: } } }");
    test("class C { void m() { switch (0) { case 0, 1 -> { break; } } } }");
  }

  @Test
  public void statement_try() {
    test("class C { void m() { try (R r1 = open(); r2;) { } } }");
  }

  @Test
  public void statement_constructor_invocation() {
    test("class C<T> { C() { <T>this(null); } C(Object o) { } }");
  }

  @Test
  public void expression_literal() {
    testExpression("-2147483648"); // Integer.MIN_VALUE
    testExpression("-9223372036854775808L"); // Long.MIN_VALUE
  }

  @Test
  public void expression_array_creation() {
    testExpression("new int[0]");
    testExpression("new int[0][1]");

    testExpression("new int[][] { { } , { } }");
  }

  @Test
  public void expression_type_method_reference() {
    testExpression("java.util.Map.Entry<String, String>::getClass");
  }

  @Test
  public void expression_super_method_reference() {
    testExpression("C.super::<T, T>m");
    testExpression("super::m");
  }

  @Test
  public void expression_creation_reference() {
    testExpression("C<T, T>::new");
  }

  @Test
  public void expression_super_method_invocation() {
    test("class C { class Inner { Inner() { C.super.toString(); } } }");
  }

  @Test
  public void expression_lambda() {
    testExpression("lambda( (p1, p2) -> {} )");
  }

  @Test
  public void expression_switch() {
    // Java 12 preview feature
    testExpression("switch (0) { case 0 -> 0; }");
    testExpression("switch (0) { case 0: break 0; }");
  }

  @Test
  public void type_qualified() {
    testExpression("new a<b>. @Annotation c()");
  }

  @Test
  public void type_name_qualified() {
    testExpression("new a. @Annotation d()");
  }

  @Test
  public void module() {
    testModule("module a { }");
    testModule("module a { requires static transitive b ; }");
    try { // bug in ECJ
      testModule("module a { requires transitive ; }");
      fail("exception expected");
    } catch (ComparisonFailure expected) {
    }
    testModule("module a { exports b to c , d ; }");
    testModule("module a { opens b to c , d ; }");
    testModule("module a { uses b ; }");
    testModule("module a { provides b with c , d ; }");
  }

  private static void testStatement(String statement) {
    test("class C { void m() { " + statement + " } }");
  }

  private static void testExpression(String expression) {
    test("class C { Object m() { return " + expression + " ; } }");
  }

  private static CompilationUnitTree test(String source) {
    return test("File.java", source);
  }

  private static void testModule(String source) {
    test("module-info.java", source);
  }

  private static CompilationUnitTree test(String unitName, String source) {
    CompilationUnitTree newTree = JParser.parse("12", unitName, source, Collections.emptyList());
    CompilationUnitTree oldTree = (CompilationUnitTree) JavaParser.createParser().parse(source);
    TreeFormatter.compare(oldTree, newTree);
    return newTree;
  }

}
