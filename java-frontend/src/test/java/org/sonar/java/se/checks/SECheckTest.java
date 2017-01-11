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
package org.sonar.java.se.checks;

import org.junit.Test;
import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFGTest;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SECheckTest {
  @Test(timeout = 3000)
  public void flow_from_exit_node_should_not_lead_to_infinite_recursion() throws Exception {
    CFG cfg = CFGTest.buildCFG("void foo(boolean a) { if(a) {foo(true);} foo(false); }");
    ExplodedGraph.Node node = new ExplodedGraph.Node(new ExplodedGraph.ProgramPoint(cfg.blocks().get(3), 0), mock(ProgramState.class));
    node.addParent(new ExplodedGraph.Node(new ExplodedGraph.ProgramPoint(cfg.blocks().get(2), 2), mock(ProgramState.class)));
    List<JavaFileScannerContext.Location> flow = FlowComputation.flow(node, new SymbolicValue(12));
    assertThat(flow).isEmpty();
  }

}
