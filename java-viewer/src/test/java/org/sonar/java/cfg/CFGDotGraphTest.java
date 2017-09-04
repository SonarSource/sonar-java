/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.cfg;

import org.junit.Test;
import org.sonar.java.viewer.Viewer;

import static org.assertj.core.api.Assertions.assertThat;

public class CFGDotGraphTest {
  @Test
  public void simple_code() {
    String code = "class A {"
      + "  int foo(boolean a) {"
      + "    if (a) {"
      + "      return 42;"
      + "    }"
      + "    return 21;"
      + "  }"
      + "}";
    Viewer.Base base = new Viewer.Base(code);

    CFGDotGraph cfgDotGraph = new CFGDotGraph(base.cfgFirstMethod);
    cfgDotGraph.build();

    assertThat(cfgDotGraph.toDot())
      .isEqualTo("graph CFG {3[label=\"B3 (START)\",highlighting=\"firstNode\"];2[label=\"B2\"];1[label=\"B1\"];0[label=\"B0 (EXIT)\",highlighting=\"exitNode\"];3->1[label=\"FALSE\"];3->2[label=\"TRUE\"];2->0[label=\"EXIT\"];1->0[label=\"EXIT\"];3[label=\"B3 (START)\",highlighting=\"firstNode\"];2[label=\"B2\"];1[label=\"B1\"];0[label=\"B0 (EXIT)\",highlighting=\"exitNode\"];3->1[label=\"FALSE\"];3->2[label=\"TRUE\"];2->0[label=\"EXIT\"];1->0[label=\"EXIT\"];}");
  }

  @Test
  public void code_with_exception() {
    String code = "abstract class A {"
      + "  int foo() {"
      + "    int result = 42;"
      + "    try {"
      + "      result = getValue();"
      + "    } catch (Exception e) {"
      + "      result = -1;"
      + "    }"
      + "    return result;"
      + "  }"
      + "  abstract int getValue() throws IllegalStateException;"
      + "}";

    Viewer.Base base = new Viewer.Base(code);

    CFGDotGraph cfgDotGraph = new CFGDotGraph(base.cfgFirstMethod);
    cfgDotGraph.build();

    assertThat(cfgDotGraph.toDot())
      .isEqualTo("graph CFG {5[label=\"B5 (START)\",highlighting=\"firstNode\"];4[label=\"B4\"];3[label=\"B3\"];2[label=\"B2\"];1[label=\"B1\"];0[label=\"B0 (EXIT)\",highlighting=\"exitNode\"];5->4[];4->2[];4->0[label=\"EXCEPTION\",highlighting=\"exceptionEdge\"];4->3[label=\"EXCEPTION\",highlighting=\"exceptionEdge\"];3->1[];2->1[];1->0[label=\"EXIT\"];5[label=\"B5 (START)\",highlighting=\"firstNode\"];4[label=\"B4\"];3[label=\"B3\"];2[label=\"B2\"];1[label=\"B1\"];0[label=\"B0 (EXIT)\",highlighting=\"exitNode\"];5->4[];4->2[];4->0[label=\"EXCEPTION\",highlighting=\"exceptionEdge\"];4->3[label=\"EXCEPTION\",highlighting=\"exceptionEdge\"];3->1[];2->1[];1->0[label=\"EXIT\"];}");
  }
}
