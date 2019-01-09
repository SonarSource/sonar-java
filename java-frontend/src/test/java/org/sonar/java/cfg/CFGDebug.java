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
package org.sonar.java.cfg;

import java.text.MessageFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.model.SyntaxTreeDebug;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

public class CFGDebug {

  private static final String DOT_PADDING = "  ";
  private static final String NEW_LINE = "\n";

  private static final int MAX_KINDNAME = Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT.name().length() + 5;

  private CFGDebug() {
  }

  /**
   * Convert the CFG to DOT format (graph description language).
   * See language specification: http://www.graphviz.org/content/dot-language
   */
  public static String toDot(CFG cfg) {
    StringBuilder sb = new StringBuilder();
    List<Block> blocks = cfg.blocks();

    // nodes
    int firstBlockId = blocks.size() - 1;
    blocks.stream().map(block -> dotBlockLabel(block.id(), firstBlockId)).forEach(sb::append);

    sb.append(NEW_LINE);

    // edges
    for (Block block : blocks) {
      block.successors().stream().map(successor -> dotSuccessorFormat(block, successor)).forEach(sb::append);
      block.exceptions().stream().map(exception -> dotExceptionFormat(block, exception)).forEach(sb::append);
    }

    return escapeNewLines("graph cfg {" + NEW_LINE + sb.toString() + "}");
  }

  private static String escapeNewLines(String dotGraph) {
    return dotGraph.replaceAll("\\n", "\\\\n");
  }

  private static String dotBlockLabel(int blockId, int firstBlockId) {
    String label = "B" + blockId;
    String color = "";
    if (blockId == 0) {
      label += " (EXIT)";
      color = "red";
    } else if (blockId == firstBlockId) {
      label += " (START)";
      color = "green";
    }

    if (!color.isEmpty()) {
      color = MessageFormat.format(",fillcolor=\"{0}\",fontcolor=\"white\"", color);
    }

    return MessageFormat.format(DOT_PADDING + "{0}[label=\"{1}\"{2}];" + NEW_LINE, blockId, label, color);
  }

  private static String dotSuccessorFormat(Block block, Block successor) {
    return MessageFormat.format(DOT_PADDING + "{0}->{1}{2};" + NEW_LINE, block.id(), successor.id(), dotSuccessorLabel(block, successor));
  }

  private static String dotSuccessorLabel(Block block, Block successor) {
    String edgeLabel = "";
    if (successor == block.trueBlock()) {
      edgeLabel = "TRUE";
    } else if (successor == block.falseBlock()) {
      edgeLabel = "FALSE";
    } else if (successor == block.exitBlock()) {
      edgeLabel = "EXIT";
    }
    if (!edgeLabel.isEmpty()) {
      return "[label=\"" + edgeLabel + "\"]";
    }
    return "";
  }

  private static String dotExceptionFormat(Block block, Block exception) {
    return MessageFormat.format(DOT_PADDING + "{0}->{1}[label=\"EXCEPTION\",color=\"orange\",fontcolor=\"orange\"];" + NEW_LINE, block.id(), exception.id());
  }

  public static String toString(CFG cfg) {
    StringBuilder buffer = new StringBuilder();
    buffer.append("Starts at B");
    buffer.append(cfg.entryBlock().id());
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
    for (Block successor : block.successors().stream().sorted(Comparator.comparingInt(Block::id).reversed()).collect(Collectors.toList())) {
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
    first = true;
    for (Block exception : block.exceptions()) {
      if (first) {
        first = false;
        buffer.append('\n');
        buffer.append("\texceptions to: ");
      } else {
        buffer.append(' ');
      }
      buffer.append('B');
      buffer.append(exception.id());
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
