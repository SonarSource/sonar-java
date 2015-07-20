/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.se;

import com.google.common.collect.Maps;
import org.sonar.java.cfg.CFG;

import javax.annotation.Nullable;

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
  public static class ProgramPoint {
    final CFG.Block block;
    final int i;

    public ProgramPoint(CFG.Block block, int i) {
      this.block = block;
      this.i = i;
    }

    @Override
    public int hashCode() {
      return block.id * 31 + i;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ProgramPoint) {
        ProgramPoint other = (ProgramPoint) obj;
        return this.block.id == other.block.id
            && this.i == other.i;
      }
      return false;
    }
  }


  public static class Node {
    boolean isNew;

    /**
     * Execution location. Currently only pre-statement, but tomorrow we might add post-statement.
     */
    final ProgramPoint programPoint;
    @Nullable
    final ProgramState programState;

    Node(ProgramPoint programPoint, @Nullable ProgramState programState) {
      this.programPoint = programPoint;
      this.programState = programState;
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
      return "B" + programPoint.block.id + "." + programPoint.i + ": " + programState;
    }
  }
}
