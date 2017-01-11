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

import com.google.common.collect.Maps;
import org.sonar.java.cfg.CFG;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.symbolicvalues.BinarySymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExplodedGraph {

  private Map<Node, Node> nodes = Maps.newHashMap();

  /**
   * Returns node associated with given (programPoint,programState) pair. If no node for this pair exists, it is created.
   */
  Node getNode(ProgramPoint programPoint, @Nullable ProgramState programState) {
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

  public Map<Node, Node> getNodes() {
    return nodes;
  }

  public static class ProgramPoint {
    private int hashcode;
    final CFG.Block block;
    final int i;

    public ProgramPoint(CFG.Block block, int i) {
      this.block = block;
      this.i = i;
    }

    @Override
    public int hashCode() {
      if (hashcode == 0) {
        hashcode = block.id() * 31 + i;
      }
      return hashcode;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ProgramPoint) {
        ProgramPoint other = (ProgramPoint) obj;
        return this.block.id() == other.block.id()
          && this.i == other.i;
      }
      return false;
    }

    @Override
    public String toString() {
      String tree = "";
      if (i < block.elements().size()) {
        tree = "" + block.elements().get(i).kind() + block.elements().get(i).firstToken().line();
      }
      return "B" + block.id() + "." + i + "  " + tree;
    }

    public Tree syntaxTree() {
      if (block.elements().isEmpty()) {
        return block.terminator();
      }
      return block.elements().get(Math.min(i, block.elements().size() - 1));
    }
  }

  public static class Node {
    boolean isNew;
    boolean exitPath = false;
    boolean happyPath = true;

    /**
     * Execution location. Currently only pre-statement, but tomorrow we might add post-statement.
     */
    public final ProgramPoint programPoint;
    @Nullable
    public final ProgramState programState;
    private final List<Node> parents;
    private final List<LearnedConstraint> learnedConstraints;

    private final List<LearnedValue> learnedSymbols;

    public Node(ProgramPoint programPoint, @Nullable ProgramState programState) {
      this.programPoint = programPoint;
      this.programState = programState;
      learnedConstraints = new ArrayList<>();
      learnedSymbols = new ArrayList<>();
      parents = new ArrayList<>();
    }

    public void setParent(@Nullable Node parent) {
      if (parent != null) {
        if (parents.isEmpty()) {
          programState.constraints.forEach((sv, c) -> {
            if (parent.programState.getConstraint(sv) != c) {
              addConstraint(sv, c);
            }
          });
          programState.values.forEach((s, sv) -> {
            if (parent.programState.getValue(s) != sv) {
              learnedSymbols.add(new LearnedValue(sv, s));
            }
          });
        }
        parents.add(parent);
      }
    }

    private void addConstraint(SymbolicValue sv, @Nullable Constraint constraint) {
      // FIXME : this might end up adding twice the same SV in learned constraints. Safe because of find first in SECheck.flows
      if (sv instanceof BinarySymbolicValue) {
        BinarySymbolicValue binarySymbolicValue = (BinarySymbolicValue) sv;
        addConstraint(binarySymbolicValue.getLeftOp(), null);
        addConstraint(binarySymbolicValue.getRightOp(), null);
      }
      learnedConstraints.add(new LearnedConstraint(sv, constraint));
    }

    public void addParent(Node node) {
      parents.add(node);
    }

    @Nullable
    public Node parent() {
      return parents.isEmpty() ? null : parents.get(0);
    }

    public List<Node> getParents() {
      return parents;
    }

    public List<LearnedConstraint> getLearnedConstraints() {
      return learnedConstraints;
    }

    public List<LearnedValue> getLearnedSymbols() {
      return learnedSymbols;
    }

    public static class LearnedConstraint {
      final SymbolicValue sv;

      @Nullable
      final Constraint constraint;

      public LearnedConstraint(SymbolicValue sv, @Nullable Constraint constraint) {
        this.sv = sv;
        this.constraint = constraint;
      }

      public SymbolicValue getSv() {
        return sv;
      }

      @CheckForNull
      public Constraint getConstraint() {
        return constraint;
      }

      @Override
      public String toString() {
        return sv + " - " + constraint;
      }
    }

    public static class LearnedValue {
      final SymbolicValue sv;
      final Symbol symbol;

      public LearnedValue(SymbolicValue sv, Symbol symbol) {
        this.sv = sv;
        this.symbol = symbol;
      }

      public Symbol getSymbol() {
        return symbol;
      }

      public SymbolicValue getSv() {
        return sv;
      }

      @Override
      public String toString() {
        return sv + " - " + symbol.name();
      }
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
  }
}
