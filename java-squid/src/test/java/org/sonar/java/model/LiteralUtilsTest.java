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
package org.sonar.java.model;

import com.google.common.base.Charsets;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class LiteralUtilsTest {

  int x1 = 42;
  int x2 = -7;
  int x3 = +3;
  int x4 = 42 + x1;
  int x5 = -x1;
  int x6 = 0xff;

  @Test
  public void intValue() throws Exception {
    Integer[] expectedIntegerValues = {42, -7, 3, null, null, null};
    File file = new File("src/test/java/org/sonar/java/model/LiteralUtilsTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser(Charsets.UTF_8).parse(file);
    ClassTree classTree = (ClassTree) tree.types().get(0);
    int i = 0;
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) member;
        assertThat(LiteralUtils.intLiteralValue(variableTree.initializer())).isEqualTo(expectedIntegerValues[i++]);
      }
    }
  }

}
