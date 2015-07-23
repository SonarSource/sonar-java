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
package org.sonar.java.checks;

import com.google.common.base.Charsets;
import java.io.File;
import java.lang.reflect.Constructor;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.fest.assertions.Assertions.assertThat;

public class SerializableContractMethodTreeTest {

  @Test
  public void testMethodMatch() {
    CompilationUnitTree compilationUnit = (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(new File("src/test/files/checks/UnusedPrivateMethodCheck.java"));
    assertThat(SerializableContractMethodTree.methodMatch((MethodTree) ((ClassTree) compilationUnit.types().get(0)).members().get(2))).isTrue();
    assertThat(SerializableContractMethodTree.methodMatch((MethodTree) ((ClassTree) compilationUnit.types().get(0)).members().get(3))).isTrue();
    assertThat(SerializableContractMethodTree.methodMatch((MethodTree) ((ClassTree) compilationUnit.types().get(0)).members().get(4))).isTrue();
    assertThat(SerializableContractMethodTree.methodMatch((MethodTree) ((ClassTree) compilationUnit.types().get(0)).members().get(5))).isTrue();
    assertThat(SerializableContractMethodTree.methodMatch((MethodTree) ((ClassTree) compilationUnit.types().get(0)).members().get(6))).isTrue();
    assertThat(SerializableContractMethodTree.methodMatch((MethodTree) ((ClassTree) compilationUnit.types().get(0)).members().get(1))).isFalse();
  }

  @Test
  public void private_constructor() throws Exception {
    Constructor constructor = SerializableContract.class.getDeclaredConstructor();
    assertThat(constructor.isAccessible()).isFalse();
    constructor.setAccessible(true);
    constructor.newInstance();
  }

}
