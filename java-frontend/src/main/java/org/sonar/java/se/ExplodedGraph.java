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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import org.sonar.java.se.xproc.MethodYield;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class ExplodedGraph {

  private Map<Node, Node> nodes = Maps.newHashMap();

  /**
   * Returns node associated with given (programPoint,programState) pair. If no node for this pair exists, it is created.
   */
  public Node node(ProgramPoint programPoint, @Nullable ProgramState programState) {
    Node result = new Node(programPoint, programState);
    Node cached = nodes.get(result);
    if (cached != null) {
      cached.isNew = false;
      return cached;
    }
    result.isNew = true;
    nodes.put(result, result);
    return result;
  }

  public Map<Node, Node> nodes() {
    return nodes;
  }

  public static final class Node {

    public final ProgramPoint programPoint;
    @Nullable
    public final ProgramState programState;

    private final Map<Node, Edge> edges = new HashMap<>();

    boolean isNew;
    boolean exitPath = false;
    boolean happyPath = true;

    private Node(ProgramPoint programPoint, @Nullable ProgramState programState) {
      Objects.requireNonNull(programPoint);
      this.programPoint = programPoint;
      this.programState = programState;
    }

    public void addParent(@Nullable Node parent, @Nullable MethodYield methodYield) {
      if (parent == null) {
        return;
      }
      Edge edge = edges.computeIfAbsent(parent, p -> new Edge(this, p));
      if (methodYield != null) {
        edge.yields.add(methodYield);
      }
    }

    public boolean onHappyPath() {
      return happyPath;
    }

    @Nullable
    public Node parent() {
      return parents().stream().findFirst().orElse(null);
    }

    /**
     * @return the ordered (by insertion) sets of parents
     */
    public Set<Node> parents() {
      return edges.keySet();
    }

    public Stream<LearnedConstraint> learnedConstraints() {
      return edges.values().stream().flatMap(e -> e.learnedConstraints().stream());
    }

    public Stream<LearnedAssociation> learnedAssociations() {
      return edges.values().stream().flatMap(e -> e.learnedAssociations().stream());
    }

    @Override
    public int hashCode() {
      return programPoint.hashCode() * 31 + (programState == null ? 0 : programState.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Node) {
        Node other = (Node) obj;
        return this.programPoint.equals(other.programPoint)
          && Objects.equals(this.programState, other.programState);
      }
      return false;
    }

    @Override
    public String toString() {
      return "B" + programPoint.block.id() + "." + programPoint.i + ": " + programState;
    }

    @CheckForNull
    public MethodYield selectedMethodYield(Node from) {
      return edges.containsKey(from) ? edges.get(from).yields.stream().findFirst().orElse(null) : null;
    }

    public Collection<Edge> edges() {
      return edges.values();
    }
  }

  public static final class Edge {
    final Node child;
    final Node parent;

    private Set<LearnedConstraint> lc;
    private Set<LearnedAssociation> la;
    private final Set<MethodYield> yields = new LinkedHashSet<>();

    private Edge(Node child, Node parent) {
      Preconditions.checkState(!Objects.equals(child, parent));
      this.child = child;
      this.parent = parent;
    }

    public Node parent() {
      return parent;
    }

    Set<LearnedConstraint> learnedConstraints() {
      if (lc == null) {
        lc = child.programState.learnedConstraints(parent.programState);
      }
      return lc;
    }

    Set<LearnedAssociation> learnedAssociations() {
      if (la == null) {
        la = child.programState.learnedAssociations(parent.programState);
      }
      return la;
    }

    public Set<MethodYield> yields() {
      return yields;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Edge edge = (Edge) o;
      return Objects.equals(child, edge.child) &&
        Objects.equals(parent, edge.parent);
    }

    @Override
    public int hashCode() {
      return Objects.hash(child, parent);
    }
  }
}
