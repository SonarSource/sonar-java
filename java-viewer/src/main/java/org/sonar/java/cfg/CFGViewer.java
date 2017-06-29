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
package org.sonar.java.cfg;

import com.sonar.sslr.api.typed.ActionParser;

import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

public class CFGViewer {

  private CFGViewer() {
  }

  private static final String DOT_PADDING = "  ";
  private static final String NEW_LINE = "\n";
  private static final ActionParser<Tree> PARSER = JavaParser.createParser();

  public static CFG buildCFG(String source) {
    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse(source);
    SemanticModel.createFor(cut, new SquidClassLoader(Collections.emptyList()));
    MethodTree firstMethod = ((MethodTree) ((ClassTree) cut.types().get(0)).members().get(0));
    return CFG.build(firstMethod);
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

    return dotLine("{0}[label=\"{1}\"{2}];", blockId, label, color);
  }

  private static String dotSuccessorFormat(Block block, Block successor) {
    return dotLine("{0}->{1}{2};", block.id(), successor.id(), dotSuccessorLabel(block, successor));
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
    return dotLine("{0}->{1}[label=\"EXCEPTION\",color=\"orange\",fontcolor=\"orange\"];", block.id(), exception.id());
  }

  private static String dotLine(String text, Object... args) {
    return DOT_PADDING + MessageFormat.format(text, args) + NEW_LINE;
  }

}
