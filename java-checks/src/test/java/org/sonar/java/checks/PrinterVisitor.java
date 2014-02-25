/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.checks;

import com.sonar.sslr.api.AstNode;

import org.sonar.java.model.JavaTree;
import org.apache.commons.lang.StringUtils;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.List;

public class PrinterVisitor extends BaseTreeVisitor {

  private static final int INDENT_SPACES = 2;

  private final StringBuilder sb;
  private int indentLevel;

  public PrinterVisitor() {
    sb = new StringBuilder();
    indentLevel = 0;
  }

  public static String print(Tree tree) {
    PrinterVisitor pv = new PrinterVisitor();
    pv.scan(tree);
    return pv.sb.toString();
  }

  public static String print(List<? extends Tree> trees) {
    StringBuilder result = new StringBuilder();
    for (Tree tree : trees) {
      result.append(print(tree));
    }
    return result.toString();
  }

  private StringBuilder indent() {
    return sb.append(StringUtils.leftPad("", INDENT_SPACES * indentLevel));
  }

  @Override
  protected void scan(List<? extends Tree> trees) {
    if (!trees.isEmpty()) {
      sb.deleteCharAt(sb.length() - 1);
      sb.append(" : [\n");
      super.scan(trees);
      indent().append("]\n");
    }
  }

  @Override
  protected void scan(@Nullable Tree tree) {
    if (tree != null) {
      Tree.Kind kind = ((JavaTree) tree).getKind();
      String nodeName = ((JavaTree) tree).getClass().getSimpleName();
      if (kind != null) {
        nodeName = kind.getAssociatedInterface().getSimpleName();
      }
      indent().append(nodeName);
      AstNode node = ((JavaTree) tree).getAstNode();
      if (node != null) {
        sb.append(" ").append(node.getTokenLine());
      }
      sb.append("\n");
    }
    indentLevel++;
    super.scan(tree);
    indentLevel--;
  }
}
