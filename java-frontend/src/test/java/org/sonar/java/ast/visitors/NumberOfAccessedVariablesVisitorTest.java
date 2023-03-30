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

class NumberOfAccessedVariablesVisitorTest {

  @Test
  void zeroVariables() throws Exception {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {" +
      " private Object foo(){ }" +
      "}");
    MethodTree methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int numberOfVariables = new NumberOfAccessedVariablesVisitor().getNumberOfAccessedVariables(methodTree);
    assertThat(numberOfVariables).isZero();
  }

  @Test
  void threeVariables() throws Exception {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {" +
      " private Object foo(String v1){"
      + " String v2 = null;  "
      + " for(int v3 = 0; v3 < 2; v3++) {}  "
      + "}" +
      "}");
    MethodTree methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int numberOfVariables = new NumberOfAccessedVariablesVisitor().getNumberOfAccessedVariables(methodTree);
    assertThat(numberOfVariables).isEqualTo(3);
  }

  @Test
  void multipleAccessesOnSameVariableDoNotCount() throws Exception {
    CompilationUnitTree cut = JParserTestUtils.parse("class A {" +
      " private Object foo(String v1){"
      + " String v2 = null; "
      + " v2 = v1; "
      + " v1 = \"another string\"; "
      + " v2 = null; "
      + "}" +
      "}");
    MethodTree methodTree = (MethodTree) ((ClassTree) cut.types().get(0)).members().get(0);
    int numberOfVariables = new NumberOfAccessedVariablesVisitor().getNumberOfAccessedVariables(methodTree);
    assertThat(numberOfVariables).isEqualTo(2);
  }

}
