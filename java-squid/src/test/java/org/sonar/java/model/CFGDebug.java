/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.model;

import org.sonar.java.cfg.CFG;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

public class CFGDebug {

  private static final int MAX_KINDNAME = Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT.name().length() + 5;

  private CFGDebug() {
  }

  public static String toString(CFG cfg) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("Starts at B");
    buffer.append(cfg.entry().id());
    buffer.append('\n');
    buffer.append('\n');
    for (Block block : cfg.blocks()) {
      buffer.append(toString(block));
    }
    return buffer.toString();
  }

  public static String toString(CFG.Block block) {
    StringBuilder buffer = new StringBuilder();
    buffer.append('B');
    buffer.append(block.id());
    if (block.id() == 0) {
      buffer.append(" (Exit):");
    }
    int i = 0;
    for (Tree tree : block.elements()) {
      buffer.append('\n');
      buffer.append(i);
      buffer.append(":\t");
      appendKind(buffer, tree.kind());
      buffer.append(SyntaxTreeDebug.toString(tree));
      i++;
    }
    Tree terminator = block.terminator();
    if (terminator != null) {
      buffer.append("\nT:\t");
      appendKind(buffer, terminator.kind());
      buffer.append(SyntaxTreeDebug.toString(terminator));
    }
    boolean first = true;
    for (Block successor : block.successors()) {
      if (first) {
        first = false;
        buffer.append('\n');
        buffer.append("\tjumps to: ");
      } else {
        buffer.append(' ');
      }
      buffer.append('B');
      buffer.append(successor.id());
      if (successor == block.trueBlock()) {
        buffer.append("(true)");
      }
      if (successor == block.falseBlock()) {
        buffer.append("(false)");
      }
      if (successor == block.exitBlock()) {
        buffer.append("(exit)");
      }

    }
    buffer.append('\n');
    buffer.append('\n');
    return buffer.toString();
  }

  private static void appendKind(StringBuilder buffer, Kind kind) {
    String name = kind.name();
    int n = MAX_KINDNAME - name.length();
    buffer.append(name);
    while (--n >= 0) {
      buffer.append(' ');
    }
    buffer.append('\t');
  }
}
