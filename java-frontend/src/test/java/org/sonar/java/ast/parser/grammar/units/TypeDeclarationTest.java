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
package org.sonar.java.ast.parser.grammar.units;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class TypeDeclarationTest {

  @Test
  public void ok() {
    LexerlessGrammarBuilder b = JavaLexer.createGrammarBuilder();

    assertThat(b, JavaLexer.TYPE_DECLARATION)
      .matches("class Foo {}")
      .matches("enum Foo {}")
      .matches("interface Foo {}")
      .matches("@interface Foo {}")
      .matches("public class Foo {}")
      .matches("public private class Foo {}")
      // javac accepts empty statements in type declarations
      .matches(";");
  }

  @Test
  public void realLife() {
    assertThat(JavaLexer.TYPE_DECLARATION)
      .matches("public static final class HelloWorld { }")
      .matches("class AnnotationOnType<@Bar T extends @Foo HashMap & @Foo Serializable>  extends java.util. @Foo HashMap implements @Foo Serializable, InterfaceTest{}");
  }

}
