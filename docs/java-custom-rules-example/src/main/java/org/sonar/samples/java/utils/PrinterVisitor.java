/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.samples.java.utils;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.Tree;

public class PrinterVisitor extends BaseTreeVisitor {
  private static final int INDENT_SPACES = 2;

  private final StringBuilder sb;
  private int indentLevel;

  public PrinterVisitor() {
    sb = new StringBuilder();
    indentLevel = 0;
  }

  public static void print(Tree tree, Consumer<String> output) {
    PrinterVisitor pv = new PrinterVisitor();
    pv.scan(tree);
    output.accept(pv.sb.toString());
  }

  private StringBuilder indent() {
    return sb.append(StringUtils.spaces(INDENT_SPACES * indentLevel));
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
      Class<?>[] interfaces = tree.getClass().getInterfaces();
      if (interfaces.length > 0) {
        indent().append(interfaces[0].getSimpleName()).append("\n");
      }
    }
    indentLevel++;
    super.scan(tree);
    indentLevel--;
  }
}
