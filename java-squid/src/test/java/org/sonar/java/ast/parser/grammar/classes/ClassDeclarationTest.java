/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.ast.parser.grammar.classes;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class ClassDeclarationTest {

  private LexerlessGrammar g = JavaGrammar.createGrammar();

  @Test
  public void ok() {
    g.rule(JavaGrammar.TYPE_PARAMETERS).mock();
    g.rule(JavaGrammar.CLASS_TYPE).mock();
    g.rule(JavaGrammar.CLASS_TYPE_LIST).mock();
    g.rule(JavaGrammar.CLASS_BODY).mock();

    assertThat(g.rule(JavaGrammar.CLASS_DECLARATION))
        .matches("class identifier typeParameters extends classType implements classTypeList classBody")
        .matches("class identifier typeParameters extends classType classBody")
        .matches("class identifier typeParameters classBody")
        .matches("class identifier classBody");
  }

  @Test
  public void realLife() {
    assertThat(g.rule(JavaGrammar.CLASS_DECLARATION))
        .matches("class HelloWorld { }")
        .matches("class HelloWorld<@Foo T> { }")
        .matches("class AnnotationOnType<@Bar T extends @Foo HashMap & @Foo Serializable>{}")
        .matches("class AnnotationOnType<@Bar T extends @Foo HashMap & @Foo Serializable>  extends java.util. @Foo HashMap implements @Foo Serializable, InterfaceTest{}")
    ;
  }

}
