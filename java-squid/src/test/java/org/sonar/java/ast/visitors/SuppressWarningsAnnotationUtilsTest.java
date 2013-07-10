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
package org.sonar.java.ast.visitors;

import com.google.common.base.Charsets;
import com.sonar.sslr.api.AstNode;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;
import org.sonar.sslr.parser.ParserAdapter;

import java.lang.reflect.Constructor;

import static org.fest.assertions.Assertions.assertThat;

public class SuppressWarningsAnnotationUtilsTest {

  private ParserAdapter<LexerlessGrammar> p = new ParserAdapter<LexerlessGrammar>(Charsets.UTF_8, JavaGrammar.createGrammar());

  @Test
  public void suppress_warnings_at_class_level() {
    AstNode astNode;

    astNode = p.parse("@SuppressWarnings(\"all\") public class Foo {}").getFirstDescendant(JavaGrammar.CLASS_DECLARATION);
    assertThat(SuppressWarningsAnnotationUtils.isSuppressAllWarnings(astNode)).isTrue();

    astNode = p.parse("@java.lang.SuppressWarnings(\"all\") public class Foo {}").getFirstDescendant(JavaGrammar.CLASS_DECLARATION);
    assertThat(SuppressWarningsAnnotationUtils.isSuppressAllWarnings(astNode)).isTrue();

    astNode = p.parse("@SuppressWarnings(\"some\") public class Foo {}").getFirstDescendant(JavaGrammar.CLASS_DECLARATION);
    assertThat(SuppressWarningsAnnotationUtils.isSuppressAllWarnings(astNode)).isFalse();

    astNode = p.parse("@AnotherAnnotation(\"some\") public class Foo {}").getFirstDescendant(JavaGrammar.CLASS_DECLARATION);
    assertThat(SuppressWarningsAnnotationUtils.isSuppressAllWarnings(astNode)).isFalse();

    astNode = p.parse("public class Foo {}").getFirstDescendant(JavaGrammar.CLASS_DECLARATION);
    assertThat(SuppressWarningsAnnotationUtils.isSuppressAllWarnings(astNode)).isFalse();
  }

  @Test
  public void suppress_warnings_at_method_level() {
    AstNode astNode;

    astNode = p.parse("public class Foo { @SuppressWarnings(\"all\") public void method() {} }").getFirstDescendant(JavaGrammar.VOID_METHOD_DECLARATOR_REST);
    assertThat(SuppressWarningsAnnotationUtils.isSuppressAllWarnings(astNode)).isTrue();

    astNode = p.parse("public class Foo { public void method() {} }").getFirstDescendant(JavaGrammar.VOID_METHOD_DECLARATOR_REST);
    assertThat(SuppressWarningsAnnotationUtils.isSuppressAllWarnings(astNode)).isFalse();
  }

  @Test
  public void private_constructor() throws Exception {
    Constructor constructor = SuppressWarningsAnnotationUtils.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

}
