/*
 * Copyright (C) 2012-2023 SonarSource SA - mailto:info AT sonarsource DOT com
 * This code is released under [MIT No Attribution](https://opensource.org/licenses/MIT-0) license.
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
