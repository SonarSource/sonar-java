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

import org.sonar.java.cfg.CFG;
import org.sonar.plugins.java.api.tree.Tree;

public class ProgramPoint {
  private int hashcode;
  final CFG.Block block;
  final int i;

  ProgramPoint(CFG.Block block) {
    this.block = block;
    this.i = 0;
  }

  private ProgramPoint(CFG.Block block, int i) {
    this.block = block;
    this.i = i;
  }

  ProgramPoint next() {
    int blockSize = block.elements().size();
    int nextPP = this.i + 1;
    // pointing one element above block size is allowed, it represents terminator of the CFG block
    // FIXME when RelationalSV is involved, we can point up to two elements behind, which is fishy
    Preconditions.checkState(nextPP < blockSize + 2,
      "CFG Block has %s elements but PP at %s was requested", blockSize, nextPP);
    return new ProgramPoint(block, nextPP);
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
