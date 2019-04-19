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
package org.sonar.java.ast.parser;

import com.sonar.sslr.api.typed.ActionParser;
import org.junit.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;

public class JavaParserTest {

  @Test
  public void should_throw_exception_when_missing_semicolon_after_enum_constants() {
    ActionParser<Tree> parser = JavaParser.createParser();
    parser.parse("enum E { ; int field; }");

    try {
      parser.parse("enum E { int field; }");
      fail("exception expected");
    } catch (RuntimeException e) {
      assertEquals("missing semicolon after enum constants", e.getCause().getCause().getMessage());
    }
  }

  @Test
  public void parent_link_should_be_computed() {
    CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser().parse("class A { void foo() {} }");
    ClassTree classTree = (ClassTree) cut.types().get(0);
    MethodTree method = (MethodTree) classTree.members().get(0);
    assertThat(method.parent()).isSameAs(classTree);
    assertThat(classTree.parent()).isSameAs(cut);
    assertThat(cut.parent()).isNull();
  }

  @Test
  public void receiver_type_should_be_parsed() throws Exception {
    try {
      String code = "class Main { class Inner { Inner(Main Main.this) {}};}";
      CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser().parse(code);
      Tree constructor = ((ClassTree) ((ClassTree) cut.types().get(0)).members().get(0)).members().get(0);
      assertThat(constructor).isInstanceOf(MethodTree.class);
      assertThat(((MethodTree) constructor).parameters().get(0).simpleName().name()).isEqualTo("this");
    } catch (Exception ex) {
      fail("Receiver type of inner classes should be parsed correctly", ex);
    }

  }
}
