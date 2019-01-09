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
package org.sonar.java.model;

import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Modifier;

import java.io.File;
import java.lang.reflect.Constructor;

import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isPrivate;
import static org.assertj.core.api.Assertions.assertThat;

public class ModifiersUtilsTest {

  @Test
  public void private_constructor() throws Exception {
    assertThat(isFinal(ModifiersUtils.class.getModifiers())).isTrue();
    Constructor constructor = ModifiersUtils.class.getDeclaredConstructor();
    assertThat(isPrivate(constructor.getModifiers())).isTrue();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

  @Test
  public void test_int_and_long_value() throws Exception {
    File file = new File("src/test/files/model/ModifiersUtilsTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    ClassTree classTree = (ClassTree) tree.types().get(0);
    assertThat(ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.PUBLIC)).isTrue();
    assertThat(ModifiersUtils.getModifier(classTree.modifiers(), Modifier.PUBLIC).keyword().text()).isEqualTo("public");
    assertThat(ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.ABSTRACT)).isFalse();
    assertThat(ModifiersUtils.getModifier(classTree.modifiers(), Modifier.ABSTRACT)).isNull();
  }

  @Test
  public void test_find_modifier() {
    File file = new File("src/test/files/model/ModifiersUtilsTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    ClassTree classTree = (ClassTree) tree.types().get(0);
    assertThat(ModifiersUtils.findModifier(classTree.modifiers(), Modifier.PUBLIC).isPresent()).isTrue();
    assertThat(ModifiersUtils.findModifier(classTree.modifiers(), Modifier.ABSTRACT).isPresent()).isFalse();
  }
}
