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

import org.sonar.java.se.xproc.MethodYield;
import org.sonar.java.viewer.DotHelper;

import javax.annotation.CheckForNull;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EGEdgeDataProvider {

  private final ExplodedGraph.Edge edge;

  public EGEdgeDataProvider(ExplodedGraph.Edge edge) {
    this.edge = edge;
  }

  @CheckForNull
  public String learnedConstraints() {
    Set<LearnedConstraint> lcs = edge.learnedConstraints();
    if (lcs.isEmpty()) {
      return null;
    }
    return DotHelper.asObject(lcs.stream().map(lc -> DotHelper.escapeCouple(lc.symbolicValue(), lc.constraint)).collect(Collectors.joining(",")));
  }

  @CheckForNull
  public String learnedAssociations() {
    Set<LearnedAssociation> las = edge.learnedAssociations();
    if (las.isEmpty()) {
      return null;
    }
    return DotHelper.asObject(las.stream().map(la -> DotHelper.escapeCouple(la.symbol, la.sv)).collect(Collectors.joining(",")));
  }

  @CheckForNull
  public String yield() {
    Set<MethodYield> yields = edge.yields();
    if (yields.isEmpty()) {
      return null;
    }
    return yields.stream().map(EGNodeDataProvider::yield).collect(Collectors.joining(","));
  }

  public String label() {
    Stream<String> learnedConstraints = edge.learnedConstraints().stream().map(LearnedConstraint::toString);
    Stream<String> learnedAssociations = edge.learnedAssociations().stream().map(LearnedAssociation::toString);
    return Stream.concat(learnedConstraints, learnedAssociations).collect(Collectors.joining(",\\n"));
  }
}
