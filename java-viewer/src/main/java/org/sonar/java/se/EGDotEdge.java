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

import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.viewer.DotDataProvider;
import org.sonar.java.viewer.JsonHelper;

import javax.annotation.CheckForNull;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EGDotEdge extends DotDataProvider.Edge {

  private final ExplodedGraph.Edge edge;

  public EGDotEdge(int from, int to, ExplodedGraph.Edge edge) {
    super(from, to);
    this.edge = edge;
  }

  @Override
  public String label() {
    Stream<String> learnedConstraints = edge.learnedConstraints().stream()
      .sorted(Comparator.comparing(lc -> lc.sv.toString()))
      .map(LearnedConstraint::toString);
    Stream<String> learnedAssociations = edge.learnedAssociations().stream()
      .sorted(Comparator.comparing(la -> la.sv.toString()))
      .map(LearnedAssociation::toString);
    return Stream.concat(learnedConstraints, learnedAssociations).collect(Collectors.joining(",\\n"));
  }

  @CheckForNull
  @Override
  public Highlighting highlighting() {
    if (hasYields()) {
      return Highlighting.YIELD_EDGE;
    } else if (edge.child.programState.peekValue() instanceof SymbolicValue.ExceptionalSymbolicValue) {
      return Highlighting.EXCEPTION_EDGE;
    }
    return null;
  }

  @Override
  public JsonObject details() {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    JsonHelper.addIfNotNull(builder, "learnedConstraints", learnedConstraints());
    JsonHelper.addIfNotNull(builder, "learnedAssociations", learnedAssociations());
    JsonHelper.addIfNotNull(builder, "selectedMethodYields", yields());
    return builder.build();
  }

  @CheckForNull
  private JsonArray learnedConstraints() {
    Set<LearnedConstraint> learnedConstraints = edge.learnedConstraints();
    if (learnedConstraints.isEmpty()) {
      return null;
    }
    return JsonHelper.toArraySortedByField(learnedConstraints.stream().map(EGDotEdge::toJsonObject), "sv");
  }

  private static JsonObject toJsonObject(LearnedConstraint lc) {
    return Json.createObjectBuilder()
      .add("sv", lc.sv.toString())
      .add("constraint", lc.constraint.toString())
      .build();
  }

  @CheckForNull
  private JsonArray learnedAssociations() {
    Set<LearnedAssociation> learnedAssociations = edge.learnedAssociations();
    if (learnedAssociations.isEmpty()) {
      return null;
    }
    return JsonHelper.toArraySortedByField(learnedAssociations.stream().map(EGDotEdge::toJsonObject), "sv");
  }

  private static final JsonObject toJsonObject(LearnedAssociation la) {
    return Json.createObjectBuilder()
      .add("sv", la.sv.toString())
      .add("symbol", la.symbol.toString())
      .build();
  }

  private boolean hasYields() {
    return !edge.yields().isEmpty();
  }

  @CheckForNull
  private JsonArray yields() {
    if (!hasYields()) {
      return null;
    }
    return JsonHelper.toArray(edge.yields().stream().map(EGDotNode::yield));
  }

}
