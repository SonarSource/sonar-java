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

import org.sonar.java.viewer.DotHelper;

import javax.annotation.CheckForNull;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EGEdgeDataProvider {

  private static final Comparator<LearnedConstraint> LC_BY_SV = (lc1, lc2) -> lc1.sv.toString().compareTo(lc2.sv.toString());
  private static final Comparator<LearnedAssociation> LA_BY_SV = (la1, la2) -> la1.sv.toString().compareTo(la2.sv.toString());

  private final ExplodedGraph.Edge edge;

  public EGEdgeDataProvider(ExplodedGraph.Edge edge) {
    this.edge = edge;
  }

  @CheckForNull
  private JsonArray learnedConstraints() {
    Set<LearnedConstraint> learnedConstraints = edge.learnedConstraints();
    if (learnedConstraints.isEmpty()) {
      return null;
    }
    JsonArrayBuilder builder = Json.createArrayBuilder();
    learnedConstraints.stream()
      .sorted(LC_BY_SV)
      .forEach(lc -> {
      JsonObject lcObject = Json.createObjectBuilder()
        .add("sv", lc.sv.toString())
        .add("constraint", lc.constraint.toString())
        .build();
      builder.add(lcObject);
    });
    return builder.build();
  }

  @CheckForNull
  private JsonArray learnedAssociations() {
    Set<LearnedAssociation> learnedAssociations = edge.learnedAssociations();
    if (learnedAssociations.isEmpty()) {
      return null;
    }
    JsonArrayBuilder builder = Json.createArrayBuilder();
    learnedAssociations.stream()
      .sorted(LA_BY_SV)
      .forEach(la -> {
      JsonObject laObject = Json.createObjectBuilder()
        .add("sv", la.sv.toString())
        .add("symbol", la.symbol.toString())
        .build();
      builder.add(laObject);
    });
    return builder.build();
  }

  public boolean hasYields() {
    return !edge.yields().isEmpty();
  }

  @CheckForNull
  private JsonArray yields() {
    if (!hasYields()) {
      return null;
    }
    JsonArrayBuilder builder = Json.createArrayBuilder();
    edge.yields().stream().map(EGNodeDataProvider::yield).forEach(builder::add);
    return builder.build();
  }

  public String label() {
    Stream<String> learnedConstraints = edge.learnedConstraints().stream().sorted(LC_BY_SV).map(LearnedConstraint::toString);
    Stream<String> learnedAssociations = edge.learnedAssociations().stream().sorted(LA_BY_SV).map(LearnedAssociation::toString);
    return Stream.concat(learnedConstraints, learnedAssociations).collect(Collectors.joining(",\\n"));
  }

  public JsonObject details() {
    JsonObjectBuilder builder = Json.createObjectBuilder();
    DotHelper.addIfNotNull(builder, "learnedConstraints", learnedConstraints());
    DotHelper.addIfNotNull(builder, "learnedAssociations", learnedAssociations());
    DotHelper.addIfNotNull(builder, "selectedMethodYields", yields());
    return builder.build();
  }

}
