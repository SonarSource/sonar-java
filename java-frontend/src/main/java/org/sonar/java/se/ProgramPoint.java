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
import org.sonar.java.cfg.CFG;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

public class ProgramPoint {
  private final int hashcode;
  public final CFG.IBlock<?> block;
  public final int i;

  public ProgramPoint(CFG.IBlock<?> block) {
    this(block, 0);
  }

  /**
   * {@code i == blockSize} means we are pointing to terminator block, {@code i == blockSize + 1} is valid if terminator block is branching
   * @see ExplodedGraphWalker#execute
   */
  private ProgramPoint(CFG.IBlock<?> block, int i) {
    int blockSize = block.elements().size();
    Preconditions.checkState(i < blockSize + 2, "CFG Block has %s elements but PP at %s was requested", blockSize, i);
    this.block = block;
    this.i = i;
    this.hashcode = block.id() * 31 + i;
  }

  public ProgramPoint next() {
    int nextPP = this.i + 1;
    return new ProgramPoint(block, nextPP);
  }

  @Override
  public int hashCode() {
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
    if (block instanceof CFG.Block) {
      List<Tree> elements = ((CFG.Block) block).elements();
      if (i < elements.size()) {
        tree = "" + elements.get(i).kind() + elements.get(i).firstToken().line();
      }
    }
    return "B" + block.id() + "." + i + "  " + tree;
  }

  public Tree syntaxTree() {
    if (block instanceof CFG.Block) {
      CFG.Block syntaxCFGblock = (CFG.Block) this.block;
      if (block.elements().isEmpty()) {
        return syntaxCFGblock.terminator();
      }
      return syntaxCFGblock.elements().get(Math.min(i, syntaxCFGblock.elements().size() - 1));
    }
    return null;
  }
}
