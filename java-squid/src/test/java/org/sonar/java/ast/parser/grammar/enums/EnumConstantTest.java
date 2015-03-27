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
package org.sonar.java.ast.parser.grammar.enums;

import com.google.common.base.Charsets;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.ast.parser.JavaLexer;
import org.sonar.java.ast.parser.TreeFactory;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.parser.sslr.ActionParser2;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.sslr.grammar.LexerlessGrammarBuilder;

import static org.sonar.sslr.tests.Assertions.assertThat;

public class EnumConstantTest {

  @Test
  public void ok() {
    LexerlessGrammarBuilder b = JavaLexer.createGrammarBuilder();

    assertThat(b, JavaLexer.ENUM_CONSTANT)
      .matches("identifier")
      .matches("identifier,")
      .matches("@Foo identifier")
      .matches("@Foo identifier()")
      .matches("@Foo identifier {}")
      .matches("@Foo identifier() {}");
  }

  @Test
  public void test_annotation() {
    LexerlessGrammarBuilder b = JavaLexer.createGrammarBuilder();
    ActionParser2 parser = new ActionParser2(Charsets.UTF_8, b, JavaGrammar.class, new TreeFactory(), JavaLexer.ENUM_CONSTANT);
    EnumConstantTreeImpl node = (EnumConstantTreeImpl) parser.parse("@Foo CONSTANT");
    org.fest.assertions.Assertions.assertThat(node.modifiers().size()).isEqualTo(1);
    org.fest.assertions.Assertions.assertThat(((IdentifierTree)((AnnotationTreeImpl) node.modifiers().get(0)).annotationType()).identifierToken().text()).isEqualTo("Foo");
  }

}
