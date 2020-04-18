/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import java.io.File;
import java.util.Collections;
import org.junit.Test;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
  public void should_recover_if_parser_fails() {
    assertThrows(
      RecognitionException.class,
      () -> JParser.parse(
        "12",
        "A",
        "class A { }",
        Collections.singletonList(new File("unknownFile"))));
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

  @Test
  public void declaration_enum() {
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

  private static void testExpression(String expression) {
    test("class C { Object m() { return " + expression + " ; } }");
  }

  private static CompilationUnitTree test(String source) {
    return JParserTestUtils.parse(source);
  }

}
