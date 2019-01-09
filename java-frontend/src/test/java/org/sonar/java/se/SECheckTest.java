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
package org.sonar.java.se;

import org.junit.Test;

import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFGTest;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SECheckTest {
  @Test(timeout = 3000)
  public void flow_from_exit_node_should_not_lead_to_infinite_recursion() throws Exception {
    CFG cfg = CFGTest.buildCFG("void foo(boolean a) { if(a) {foo(true);} foo(false); }");
    ExplodedGraph eg = new ExplodedGraph();
    ExplodedGraph.Node node = eg.node(new ProgramPoint(cfg.blocks().get(3)), ProgramState.EMPTY_STATE);
    node.addParent(eg.node(new ProgramPoint(cfg.blocks().get(2)).next().next(), ProgramState.EMPTY_STATE), null);
    Set<Flow> flows = FlowComputation.flow(node, new SymbolicValue(), Collections.singletonList(ObjectConstraint.class));
    assertThat(flows.iterator().next().isEmpty()).isTrue();
  }

}
