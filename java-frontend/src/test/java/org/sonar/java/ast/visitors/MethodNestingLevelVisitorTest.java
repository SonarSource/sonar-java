/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.ast.visitors;

import org.junit.jupiter.api.Test;
import org.sonar.java.model.JParserTestUtils;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import static org.assertj.core.api.Assertions.assertThat;

class MethodNestingLevelVisitorTest {

  @Test
  void ifHandling() throws Exception {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {" +
        " Object foo(){" +
        " if(a) { " +
        "    return new Object(); " +
        "    };" +
        " } " +
        "}");
    MethodTree methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isEqualTo(1);
  }
  
  @Test
  void forHandling() throws Exception {
    CompilationUnitTree cut = JParserTestUtils.parse(
        "class A {" +
        "  Object foo(){" +
        "    if(a) { " +
        "      for(int i=0; i<3; i++) { if(i==2){ return null; } }" +
        "      for(int i : new int[]{1,2}) { if(i==2){ return null; } }" +
        "      return new Object();   " +
        "    };" +
        "  } " +
        "}");
    MethodTree methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isEqualTo(3);
  }

  @Test
  void switchHandling() throws Exception {
    CompilationUnitTree cut = JParserTestUtils.parse(
      "class A {" +
        "  String foo(int a) {" +
        "    switch (a) {" +
        "      case 0:" +
        "        return \"none\";" +
        "      case 1:" +
        "        return \"one\";" +
        "      case 2:" +
        "        if(i==2){ return null; } else { return null; } " +
        "      default:" +
        "        return \"it's complicated\";" +
        "    }" +
        "  }" +
        "}");
    MethodTree methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isEqualTo(2);
  }
  
  @Test
  void tryHandling() throws Exception {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {" +
        " Object foo(){" +
        " try { " +
        "    if(a) { " +
        "      for(int i=0; i<3; i++) { if(i==2){ return null; } }" +
        "      return new Object();   " +
        "    };" +
        "    }catch(Exception e){}" +
        " } " +
        "}");
    MethodTree methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isEqualTo(4);
  }
  
  @Test
  void emptyMethod() throws Exception {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {" +
        " abstract Object foo();" +
        "}");
    MethodTree methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int nesting = new MethodNestingLevelVisitor().getMaxNestingLevel(methodTree);
    assertThat(nesting).isZero();
  }
}
