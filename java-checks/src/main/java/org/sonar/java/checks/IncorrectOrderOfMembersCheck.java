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

  private static final String[] NAMES = {"variable", "constructor", "method"};

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
        priority = 0;
        identifier = ((VariableTree) member).simpleName();
      } else if (member.is(Tree.Kind.CONSTRUCTOR)) {
        priority = 1;
        identifier = ((MethodTree) member).simpleName();
      } else if (member.is(Tree.Kind.METHOD)) {
        priority = 2;
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
