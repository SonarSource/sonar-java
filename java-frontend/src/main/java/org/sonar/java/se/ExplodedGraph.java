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

import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import org.sonar.java.se.xproc.MethodYield;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ExplodedGraph {

  private final Map<Node, Node> nodes = new HashMap<>();
  private final Multimap<ProgramPoint, Node> nodesByProgramPoint = LinkedListMultimap.create();

  /**
   * Returns node associated with given (programPoint,programState) pair. If no node for this pair exists, it is created.
   */
  public Node node(ProgramPoint programPoint, @Nullable ProgramState programState) {
    Node result = new Node(programPoint, programState, this);
    Node cached = nodes.get(result);
    if (cached != null) {
      cached.isNew = false;
      return cached;
    }
    result.isNew = true;
    nodes.put(result, result);
    nodesByProgramPoint.put(programPoint, result);
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

    private boolean isNew;
    boolean exitPath = false;
    private final int hashcode;
    private final ExplodedGraph explodedGraph;

    private Node(ProgramPoint programPoint, @Nullable ProgramState programState, ExplodedGraph explodedGraph) {
      Objects.requireNonNull(programPoint);
      this.programPoint = programPoint;
      this.programState = programState;
      this.explodedGraph = explodedGraph;
      hashcode = programPoint.hashCode() * 31 + (programState == null ? 0 : programState.hashCode());
    }

    public void addParent(@Nullable Node parent, @Nullable MethodYield methodYield) {
      if (parent == null) {
        return;
      }
      Edge edge = edges.computeIfAbsent(parent, p -> new Edge(this, p));
      if (methodYield != null) {
        Preconditions.checkState(parent.programPoint.syntaxTree().is(Tree.Kind.METHOD_INVOCATION), "Yield on edge where parent is not MIT");
        edge.yields.add(methodYield);
      }
    }

    public Collection<Node> siblings() {
      Collection<Node> collection = explodedGraph.nodesByProgramPoint.get(programPoint);
      collection.remove(this);
      return collection;
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

    @Override
    public int hashCode() {
      return hashcode;
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

    public Collection<Edge> edges() {
      return edges.values();
    }

    public boolean isNew() {
      return isNew;
    }
  }

  public static final class Edge {
    final Node child;
    final Node parent;
    final int hashcode;

    private Set<LearnedConstraint> lc;
    private Set<LearnedAssociation> la;
    private final Set<MethodYield> yields = new LinkedHashSet<>();

    private Edge(Node child, Node parent) {
      Preconditions.checkState(!child.equals(parent));
      this.child = child;
      this.parent = parent;
      hashcode = Objects.hash(child, parent);
    }

    public Node child() {
      return child;
    }

    public Node parent() {
      return parent;
    }

    public Set<LearnedConstraint> learnedConstraints() {
      if (lc == null) {
        lc = child.programState.learnedConstraints(parent.programState);
      }
      return lc;
    }

    public Set<LearnedAssociation> learnedAssociations() {
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
      return child.equals(edge.child) &&
        parent.equals(edge.parent);
    }

    @Override
    public int hashCode() {
      return hashcode;
    }
  }
}
