/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
    int startLine = tree.firstToken().range().start().line();
    int endLine = tree.lastToken().range().start().line();

    PrioritizedMember[] priorities = new PrioritizedMember[endLine - startLine];
    for (int i = 0; i < tree.members().size(); i++) {
      final Tree member = tree.members().get(i);
      int memberLine = member.firstToken().range().start().line() - startLine;
      if (member.is(Tree.Kind.VARIABLE)) {
        VariableTree variable = ((VariableTree) member);
        if (variable.symbol().isStatic()) {
          priorities[memberLine] = new PrioritizedMember(0, variable.simpleName());
        } else {
          priorities[memberLine] = new PrioritizedMember(1, variable.simpleName());
        }
      } else if (member.is(Tree.Kind.CONSTRUCTOR)) {
        priorities[memberLine] = new PrioritizedMember(2, ((MethodTree) member).simpleName());
      } else if (member.is(Tree.Kind.METHOD)) {
        priorities[memberLine] = new PrioritizedMember(3, ((MethodTree) member).simpleName());
      } else {
        continue;
      }
    }
    checkPriorityArray(priorities);
    super.visitClass(tree);
  }

  void checkPriorityArray(PrioritizedMember[] priorities) {
    int highestPriority = 0;
    for (int line = 0; line < priorities.length; line++) {
      PrioritizedMember pm = priorities[line];
      if (pm == null) {
        continue;
      }
      if (pm.priority() < highestPriority) {
        context.reportIssue(this, pm.member(), "Move this " + NAMES[pm.priority()] + " to comply with Java Code Conventions.");
      } else {
        highestPriority = pm.priority();
      }
    }
  }

  private static class PrioritizedMember {

    private int priority = 0;
    private Tree member;

    public PrioritizedMember(int p, Tree m) {
      priority = p;
      member = m;
    }

    public int priority() {
      return priority;
    }

    public Tree member() {
      return member;
    }

  }

}
