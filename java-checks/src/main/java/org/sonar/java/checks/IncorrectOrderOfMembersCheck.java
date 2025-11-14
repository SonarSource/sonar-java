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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1213")
public class IncorrectOrderOfMembersCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String[] NAMES = {"static variable", "variable", "constructor", "method"};

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    int prev = 0;
    for (int i = 0; i < tree.members().size(); i++) {
      final Tree member = tree.members().get(i);
      final int priority;
      IdentifierTree identifier;
      if (member.is(Tree.Kind.VARIABLE)) {
        VariableTree variable = ((VariableTree) member);
        if (variable.symbol().isStatic()) {
          priority = 0;
        } else {
          priority = 1;
        }
        identifier = variable.simpleName();
      } else if (member.is(Tree.Kind.CONSTRUCTOR)) {
        priority = 2;
        identifier = ((MethodTree) member).simpleName();
      } else if (member.is(Tree.Kind.METHOD)) {
        priority = 3;
        identifier = ((MethodTree) member).simpleName();
      } else {
        continue;
      }
      if (priority < prev) {
        context.reportIssue(this, identifier, "Move this " + NAMES[priority] + " to comply with Java Code Conventions.");
      } else {
        prev = priority;
      }
    }

    super.visitClass(tree);
  }

}
