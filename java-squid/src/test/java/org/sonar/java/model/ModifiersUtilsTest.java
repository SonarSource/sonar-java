/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.model;

import com.google.common.base.Charsets;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Modifier;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class ModifiersUtilsTest {
  @Test
  public void test_int_and_long_value() throws Exception {
    File file = new File("src/test/files/model/ModifiersUtilsTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(file);
    ClassTree classTree = (ClassTree) tree.types().get(0);
    assertThat(ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.PUBLIC)).isTrue();
    assertThat(ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.ABSTRACT)).isFalse();
  }
}
