/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import java.util.Set;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1317")
public class StringBufferAndBuilderWithCharCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  private static final Set<String> TARGETED_CLASS = SetUtils.immutableSetOf("StringBuilder", "StringBuffer");

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (TARGETED_CLASS.contains(getClassName(tree)) && tree.arguments().size() == 1) {
      ExpressionTree argument = tree.arguments().get(0);

      if (argument.is(Tree.Kind.CHAR_LITERAL)) {
        String character = ((LiteralTree) argument).value();
        context.reportIssue(this, argument, "Replace the constructor character parameter " + character + " with string parameter " + character.replace("'", "\"") + ".");
      }
    }
  }

  private static String getClassName(NewClassTree newClasstree) {
    if (newClasstree.identifier().is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) newClasstree.identifier()).identifier().name();
    } else if (newClasstree.identifier().is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) newClasstree.identifier()).name();
    }
    return null;
  }

}
