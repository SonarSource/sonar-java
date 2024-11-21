/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.ast.visitors.LinesOfCodeVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1188")
public class AnonymousClassesTooBigCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final int DEFAULT_MAX = 20;

  @RuleProperty(key = "Max",
    description = "Maximum allowed lines in an anonymous class",
    defaultValue = "" + DEFAULT_MAX)
  public int max = DEFAULT_MAX;

  private JavaFileScannerContext context;
  /**
   * Flag to skip check for class bodies of EnumConstants.
   */
  private boolean isEnumConstantBody;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    isEnumConstantBody = false;
    scan(context.getTree());
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (tree.classBody() != null && !isEnumConstantBody) {
      int lines = getNumberOfLines(tree.classBody());
      if (lines > max) {
        context.reportIssue(this, tree.newKeyword(), tree.identifier(),
          "Reduce this anonymous class number of lines from " + lines + " to at most " + max + ", or make it a named class.");
      }
    }
    isEnumConstantBody = false;
    super.visitNewClass(tree);
  }

  @Override
  public void visitEnumConstant(EnumConstantTree tree) {
    isEnumConstantBody = true;
    super.visitEnumConstant(tree);
  }

  private static int getNumberOfLines(Tree tree) {
    return new LinesOfCodeVisitor().linesOfCode(tree);
  }
}
