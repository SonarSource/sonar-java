/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Charsets;
import org.junit.Test;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.fest.assertions.Assertions.assertThat;

public class JavaParserTest {

  @Test
  public void parent_link_should_be_computed() {
    CompilationUnitTree cut = (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse("class A { void foo() {} }");
    ClassTree classTree = (ClassTree) cut.types().get(0);
    MethodTree method = (MethodTree) classTree.members().get(0);
    assertThat(method.parent()).isSameAs(classTree);
    assertThat(classTree.parent()).isSameAs(cut);
    assertThat(cut.parent()).isNull();
  }
}