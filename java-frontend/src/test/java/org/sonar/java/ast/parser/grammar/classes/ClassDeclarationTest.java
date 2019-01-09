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
package org.sonar.java.ast.parser.grammar.classes;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class ClassDeclarationTest {

  @Test
  public void ok() {
    LexerlessGrammarBuilder b = JavaLexer.createGrammarBuilder();

    assertThat(b, JavaLexer.CLASS_DECLARATION)
      .matches("class identifier <T, U extends Foo & Bar> extends Foo implements Foo, Bar<Integer> {}")
      .matches("class identifier <T, U extends Foo & Bar> extends Foo {}")
      .matches("class identifier <T, U extends Foo & Bar> {}")
      .matches("class identifier {}");
  }

  @Test
  public void realLife() {
    assertThat(JavaLexer.CLASS_DECLARATION)
      .matches("class HelloWorld { }")
      .matches("class HelloWorld<@Foo T> { }")
      .matches("class AnnotationOnType<@Bar T extends @Foo HashMap & @Foo Serializable>{}")
      .matches("class AnnotationOnType<@Bar T extends @Foo HashMap & @Foo Serializable>  extends java.util. @Foo HashMap implements @Foo Serializable, InterfaceTest{}");
  }

}
