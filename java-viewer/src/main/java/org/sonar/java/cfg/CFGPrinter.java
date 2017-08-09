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

import org.sonar.java.cfg.CFG.Block;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CFGPrinter {

  private static final int MAX_KINDNAME = Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT.name().length() + 5;

  private CFGPrinter() {
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

  private static String toString(CFG.Block block) {
    StringBuilder buffer = new StringBuilder();
    buffer.append('B');
    buffer.append(block.id());
    if (block.id() == 0) {
      buffer.append(" (Exit):");
    }

    appendElements(buffer, block);
    appendTerminator(buffer, block);
    appendSuccessors(buffer, block);
    appendExceptions(buffer, block);

    buffer.append('\n');
    buffer.append('\n');
    return buffer.toString();
  }

  private static void appendElements(StringBuilder buffer, CFG.Block block) {
    int i = 0;
    for (Tree tree : block.elements()) {
      buffer.append('\n');
      buffer.append(i);
      buffer.append(":\t");
      appendKind(buffer, tree.kind());
      buffer.append(toString(tree));
      i++;
    }
  }

  private static void appendKind(StringBuilder buffer, Kind kind) {
    String name = kind.name();
    int n = MAX_KINDNAME - name.length();
    buffer.append(name);
    --n;
    while (n >= 0) {
      buffer.append(' ');
      --n;
    }
    buffer.append('\t');
  }

  private static void appendTerminator(StringBuilder buffer, CFG.Block block) {
    Tree terminator = block.terminator();
    if (terminator != null) {
      buffer.append("\nT:\t");
      appendKind(buffer, terminator.kind());
      buffer.append(toString(terminator));
    }
  }

  private static void appendSuccessors(StringBuilder buffer, CFG.Block block) {
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
  }

  private static void appendExceptions(StringBuilder buffer, CFG.Block block) {
    boolean first = true;
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
  }

  private static String toString(Tree tree) {
    Stream.Builder<String> sb = Stream.builder();
    switch (tree.kind()) {
      case TOKEN:
        sb.add(((SyntaxToken) tree).text());
        break;
      case VARIABLE:
        VariableTree vt = (VariableTree) tree;
        // skip initializer
        addTrees(sb, vt.type(), vt.simpleName());
        break;
      case NEW_CLASS:
        NewClassTree nct = (NewClassTree) tree;
        // skip body for anonymous classes
        addTrees(sb, nct.newKeyword(), nct.identifier(), nct.arguments());
        break;
      case MEMBER_SELECT:
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) tree;
        if (mset.expression().is(Tree.Kind.METHOD_INVOCATION)) {
          // skip method invocation
          addTrees(sb, mset.identifier());
        } else {
          addChildren(sb, tree);
        }
        break;
      case IF_STATEMENT:
        IfStatementTree ist = (IfStatementTree) tree;
        // skip thenClause and elseClause
        addTrees(sb, ist.ifKeyword(), ist.openParenToken(), ist.condition(), ist.closeParenToken());
        break;
      default:
        addChildren(sb, tree);
        break;
    }
    return sb.build().filter(text -> !text.isEmpty()).collect(Collectors.joining(" "));
  }

  private static void addChildren(Stream.Builder<String> sb, Tree tree) {
    addTrees(sb, ((JavaTree) tree).getChildren());
  }

  private static void addTrees(Stream.Builder<String> sb, Tree... trees) {
    for (Tree tree : trees) {
      sb.add(toString(tree));
    }
  }

  private static void addTrees(Stream.Builder<String> sb, List<Tree> trees) {
    for (Tree tree : trees) {
      sb.add(toString(tree));
    }
  }
}
