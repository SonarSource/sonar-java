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
import org.sonar.java.viewer.DotHelper;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;

import java.util.Collections;
import java.util.List;

public class CFGViewer {

  private CFGViewer() {
  }

  private static final String NEW_LINE = "\n";
  private static final ActionParser<Tree> PARSER = JavaParser.createParser();

  public static CFG buildCFG(String source) {
    CompilationUnitTree cut = (CompilationUnitTree) PARSER.parse(source);
    SemanticModel.createFor(cut, new SquidClassLoader(Collections.emptyList()));
    return CFG.build(getFirstMethod(cut));
  }

  private static MethodTree getFirstMethod(CompilationUnitTree cut) {
    ClassTree classTree = (ClassTree) cut.types().get(0);
    return (MethodTree) classTree.members().stream()
      .filter(m -> m.is(Tree.Kind.METHOD))
      .findFirst()
      .orElse(null);
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
    DotHelper.Highlighting highlighting = null;
    if (blockId == 0) {
      label += " (EXIT)";
      highlighting = DotHelper.Highlighting.EXIT_NODE;
    } else if (blockId == firstBlockId) {
      label += " (START)";
      highlighting = DotHelper.Highlighting.FIRST_NODE;
    }

    return DotHelper.node(blockId, label, highlighting);
  }

  private static String dotSuccessorFormat(Block block, Block successor) {
    return DotHelper.edge(block.id(), successor.id(), dotSuccessorLabel(block, successor));
  }

  @CheckForNull
  private static String dotSuccessorLabel(Block block, Block successor) {
    String edgeLabel = null;
    if (successor == block.trueBlock()) {
      edgeLabel = "TRUE";
    } else if (successor == block.falseBlock()) {
      edgeLabel = "FALSE";
    } else if (successor == block.exitBlock()) {
      edgeLabel = "EXIT";
    }
    return edgeLabel;
  }

  private static String dotExceptionFormat(Block block, Block exception) {
    return DotHelper.edge(block.id(), exception.id(), "EXCEPTION", DotHelper.Highlighting.EXCEPTION_EDGE);
  }
}
