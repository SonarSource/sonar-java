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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class JParserTest {

  @Test
  void should_throw_RecognitionException_in_case_of_syntax_error() {
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
  void should_recover_if_parser_fails() {
    List<File> classpath = Collections.singletonList(new File("unknownFile"));
    assertThrows(
      RecognitionException.class,
      () -> JParser.parse(
        "12",
        "A",
        "class A { }",
        classpath));
  }

  @Test
  void should_throw_RecognitionException_in_case_of_lexical_error() {
    try { // Note that without check for errors will cause InvalidInputException
      testExpression("''");
      fail("exception expected");
    } catch (RuntimeException e) {
      assertEquals("Parse error at line 1 column 30: Invalid character constant", e.getMessage());
    }
  }

  @Test
  void err() {
    try {
      // ASTNode.METHOD_DECLARATION with flag ASTNode.MALFORMED
      test("interface Foo { public foo(); // comment\n }");
      fail("exception expected");
    } catch (IndexOutOfBoundsException ignore) {
    }
  }

  @Test
  void eof() {
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
  void declaration_enum() {
    CompilationUnitTree cu = test("enum E { C }");
    ClassTree t = (ClassTree) cu.types().get(0);
    EnumConstantTree c = (EnumConstantTree) t.members().get(0);
    assertSame(
      c.simpleName(),
      c.initializer().identifier());
  }

  @Test
  void statement_variable_declaration() {
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
  void doesnt_include_running_VM_Bootclasspath_if_jvm_sdk_already_provided_in_classpath(@TempDir Path tempFolder) throws IOException {
    VariableTree s1 = parseAndGetVariable("class C { void m() { String a; } }");
    assertThat(s1.type().symbolType().fullyQualifiedName()).isEqualTo("java.lang.String");

    Path fakeRt = tempFolder.resolve("rt.jar");
    Files.createFile(fakeRt);
    s1 = parseAndGetVariable("class C { void m() { String a; } }", fakeRt.toFile());
    assertThat(s1.type().symbolType().fullyQualifiedName()).isEqualTo("Recovered#typeBindingLString;0");

    Path fakeAndroidSdk = tempFolder.resolve("android.jar");
    Files.createFile(fakeAndroidSdk);
    s1 = parseAndGetVariable("class C { void m() { String a; } }", fakeAndroidSdk.toFile());
    assertThat(s1.type().symbolType().fullyQualifiedName()).isEqualTo("Recovered#typeBindingLString;0");

    Path fakeJrtFs = tempFolder.resolve("lib/jrt-fs.jar");
    Files.createDirectories(fakeJrtFs.getParent());
    Files.createFile(fakeJrtFs);
    String javaVersion = System.getProperty("java.version");
    if (javaVersion != null && javaVersion.startsWith("1.8")) {
      File fakeJrtFsFile = fakeJrtFs.toFile();
      RecognitionException expected = assertThrows(RecognitionException.class, () -> parseAndGetVariable("class C { void m() { String a; } }", fakeJrtFsFile));
      assertThat(expected).hasCauseExactlyInstanceOf(ProviderNotFoundException.class).hasRootCauseMessage("Provider \"jrt\" not found");
    } else {
      // Seems that it will still fallback on the parent classloader so there will be no error
    }

  }

  private VariableTree parseAndGetVariable(String code, File... classpath) {
    CompilationUnitTree t = JParserTestUtils.parse("Foo.java", code, Arrays.asList(classpath));
    ClassTree c = (ClassTree) t.types().get(0);
    MethodTree m = (MethodTree) c.members().get(0);
    BlockTree s = m.block();
    assertNotNull(s);
    VariableTree s1 = (VariableTree) s.body().get(0);
    return s1;
  }

  private static void testExpression(String expression) {
    test("class C { Object m() { return " + expression + " ; } }");
  }

  private static CompilationUnitTree test(String source) {
    return JParserTestUtils.parse(source);
  }

}
