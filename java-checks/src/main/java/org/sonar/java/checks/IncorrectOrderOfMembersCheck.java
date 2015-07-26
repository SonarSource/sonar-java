/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S1213",
  name = "The members of an interface declaration or class should appear in a pre-defined order",
  tags = {"convention"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
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
      if (member.is(Tree.Kind.VARIABLE)) {
        priority = 0;
      } else if (member.is(Tree.Kind.CONSTRUCTOR)) {
        priority = 1;
      } else if (member.is(Tree.Kind.METHOD)) {
        priority = 2;
      } else {
        continue;
      }
      if (priority < prev) {
        context.addIssue(member, this, "Move this " + NAMES[priority] + " to comply with Java Code Conventions.");
      } else {
        prev = priority;
      }
    }

    super.visitClass(tree);
  }

}
