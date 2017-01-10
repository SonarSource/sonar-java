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
package org.sonar.java.se;

import org.junit.Test;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFGTest;

import static org.assertj.core.api.Assertions.assertThat;

public class ProgramPointTest {
  @Test
  public void test_to_string_method() throws Exception {
    // ToString method of program point is used by viewer.
    CFG cfg = CFGTest.buildCFG("void foo() {foo();}");
    ExplodedGraph.ProgramPoint pp = new ExplodedGraph.ProgramPoint(cfg.blocks().get(0), 0);
    assertThat(pp.toString()).isEqualTo("B1.0  IDENTIFIER1");
    pp = new ExplodedGraph.ProgramPoint(cfg.blocks().get(0), 2);
    assertThat(pp.toString()).isEqualTo("B1.2  ");
  }

}