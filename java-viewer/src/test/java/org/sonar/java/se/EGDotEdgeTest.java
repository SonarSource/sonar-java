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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.semantic.Symbol;

import javax.annotation.Nullable;
import javax.json.JsonArray;
import javax.json.JsonObject;

import static org.assertj.core.api.Assertions.assertThat;

public class EGDotEdgeTest {

  private ExplodedGraph eg;
  private static final Symbol A_SYMBOL = mockSymbol("a");
  private static final Symbol B_SYMBOL = mockSymbol("b");
  private static final SymbolicValue SV_42 = mockSymbolicValue(42);
  private static final SymbolicValue SV_21 = mockSymbolicValue(21);

  @Before
  public void setUp() {
    eg = new ExplodedGraph();
  }

  @Test
  public void no_label_if_nothing_learned() {
    ExplodedGraph.Node n1 = node(1, ProgramState.EMPTY_STATE);
    ExplodedGraph.Node n2 = node(2, ProgramState.EMPTY_STATE);

    ExplodedGraph.Edge edge = addEdge(n1, n2);
    EGDotEdge egEdgeDataProvider = new EGDotEdge(1, 2, edge);

    assertThat(egEdgeDataProvider.label()).isEmpty();
  }

  @Test
  public void label_contains_learned_associations() {
    ExplodedGraph.Node n1 = node(1, ProgramState.EMPTY_STATE);
    ExplodedGraph.Node n2 = node(2, ProgramState.EMPTY_STATE.put(A_SYMBOL, SV_42));

    ExplodedGraph.Edge edge = addEdge(n1, n2);
    String label = new EGDotEdge(1, 2, edge).label();
    assertThat(label).contains("SV_42");
    assertThat(label).contains("a");
  }

  @Test
  public void label_contains_learned_constraint() {
    ExplodedGraph.Node n1 = node(1, ProgramState.EMPTY_STATE);
    ExplodedGraph.Node n2 = node(2, ProgramState.EMPTY_STATE.addConstraint(SV_42, ObjectConstraint.NOT_NULL));

    ExplodedGraph.Edge edge = addEdge(n1, n2);
    String label = new EGDotEdge(1, 2, edge).label();
    assertThat(label).contains("SV_42");
    assertThat(label).contains("NOT_NULL");
  }

  @Test
  public void label_contains_learned_constraints_and_learned_associations() {
    ExplodedGraph.Node n1 = node(1, ProgramState.EMPTY_STATE);
    ExplodedGraph.Node n2 = node(2, ProgramState.EMPTY_STATE
      .put(A_SYMBOL, SV_42)
      .addConstraint(SV_42, ObjectConstraint.NOT_NULL));

    ExplodedGraph.Edge edge = addEdge(n1, n2);
    String label = new EGDotEdge(1, 2, edge).label();
    String[] split = label.split(",\\\\n");

    assertThat(split).hasSize(2);
    assertThat(split[0]).contains("SV_42");
    assertThat(split[0]).contains("NOT_NULL");

    assertThat(split[1]).contains("SV_42");
    assertThat(split[1]).contains("a");
  }

  @Test
  public void details_contains_learned_constraints_sorted_by_sv() {
    ExplodedGraph.Node n1 = node(1, ProgramState.EMPTY_STATE);
    ExplodedGraph.Node n2 = node(2, ProgramState.EMPTY_STATE
      .addConstraint(SV_21, ObjectConstraint.NOT_NULL)
      .addConstraint(SV_42, BooleanConstraint.FALSE));

    ExplodedGraph.Edge edge = addEdge(n1, n2);
    JsonObject details = new EGDotEdge(1, 2, edge).details();

    JsonArray learnedConstraints = details.getJsonArray("learnedConstraints");
    assertThat(learnedConstraints).isNotNull();
    assertThat(learnedConstraints).hasSize(2);

    JsonObject lc1 = (JsonObject) learnedConstraints.get(0);
    assertThat(lc1.getString("sv")).isEqualTo("SV_21");
    assertThat(lc1.getString("constraint")).isEqualTo("NOT_NULL");

    JsonObject lc2 = (JsonObject) learnedConstraints.get(1);
    assertThat(lc2.getString("sv")).isEqualTo("SV_42");
    assertThat(lc2.getString("constraint")).isEqualTo("FALSE");
  }

  @Test
  public void details_contains_learned_associations_sorted_by_sv() {
    ExplodedGraph.Node n1 = node(1, ProgramState.EMPTY_STATE);
    ExplodedGraph.Node n2 = node(2, ProgramState.EMPTY_STATE
      .put(A_SYMBOL, SV_42)
      .put(B_SYMBOL, SV_21));

    ExplodedGraph.Edge edge = addEdge(n1, n2);
    JsonObject details = new EGDotEdge(1, 2, edge).details();

    JsonArray learnedAssociations = details.getJsonArray("learnedAssociations");
    assertThat(learnedAssociations).isNotNull();
    assertThat(learnedAssociations).hasSize(2);

    JsonObject la1 = (JsonObject) learnedAssociations.get(0);
    assertThat(la1.getString("sv")).isEqualTo("SV_21");
    assertThat(la1.getString("symbol")).isEqualTo("b");

    JsonObject la2 = (JsonObject) learnedAssociations.get(1);
    assertThat(la2.getString("sv")).isEqualTo("SV_42");
    assertThat(la2.getString("symbol")).isEqualTo("a");
  }

  @Test
  public void details_contains_learned_constraints_and_learned_associations() {
    ExplodedGraph.Node n1 = node(1, ProgramState.EMPTY_STATE);
    ExplodedGraph.Node n2 = node(2, ProgramState.EMPTY_STATE
      .put(A_SYMBOL, SV_42)
      .addConstraint(SV_42, ObjectConstraint.NOT_NULL));

    ExplodedGraph.Edge edge = addEdge(n1, n2, null);
    JsonObject details = new EGDotEdge(1, 2, edge).details();

    JsonArray learnedConstraints = details.getJsonArray("learnedConstraints");
    assertThat(learnedConstraints).isNotNull();
    assertThat(learnedConstraints).hasSize(1);

    JsonObject lc = (JsonObject) learnedConstraints.get(0);
    assertThat(lc.getString("sv")).isEqualTo("SV_42");
    assertThat(lc.getString("constraint")).isEqualTo("NOT_NULL");

    JsonArray learnedAssociations = details.getJsonArray("learnedAssociations");
    assertThat(learnedAssociations).isNotNull();
    assertThat(learnedAssociations).hasSize(1);

    JsonObject la = (JsonObject) learnedAssociations.get(0);
    assertThat(la.getString("sv")).isEqualTo("SV_42");
    assertThat(la.getString("symbol")).isEqualTo("a");

    JsonArray selectedMethodYields = details.getJsonArray("selectedMethodYields");
    assertThat(selectedMethodYields).isNull();
  }

  private static ExplodedGraph.Edge addEdge(ExplodedGraph.Node parent, ExplodedGraph.Node child) {
    return addEdge(parent, child, null);
  }

  private static ExplodedGraph.Edge addEdge(ExplodedGraph.Node parent, ExplodedGraph.Node child, @Nullable MethodYield yield) {
    child.addParent(parent, yield);

    ExplodedGraph.Edge edge = child.edges().stream().findFirst().orElse(null);
    assertThat(edge).isNotNull();
    return edge;
  }

  private ExplodedGraph.Node node(int blockId, ProgramState ps) {
    return eg.node(new ProgramPoint(new CFG.Block(blockId)), ps);
  }

  private static Symbol mockSymbol(String symbolName) {
    Symbol mock = Mockito.mock(Symbol.class);
    Mockito.when(mock.toString()).thenReturn(symbolName);
    Mockito.when(mock.name()).thenReturn(symbolName);
    return mock;
  }

  private static SymbolicValue mockSymbolicValue(int id) {
    SymbolicValue sv = Mockito.mock(SymbolicValue.class);
    Mockito.when(sv.toString()).thenReturn("SV_" + id);
    return sv;
  }
}
