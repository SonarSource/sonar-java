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
package org.sonar.java.checks.spring;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3749")
public class S3749Check extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    SymbolMetadata symbolMeta = tree.symbol().metadata();

    if (symbolMeta.isAnnotatedWith("org.springframework.stereotype.Controller")
      || symbolMeta.isAnnotatedWith("org.springframework.stereotype.Service")
      || symbolMeta.isAnnotatedWith("org.springframework.stereotype.Repository")) {
      List<Tree> members = tree.members();
      for (Tree member : members) {
        if (member.is(Kind.VARIABLE)) {
          VariableTree var = (VariableTree) member;
          if (!var.symbol().isStatic() && !var.symbol().metadata().isAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")) {
            context.reportIssue(this, var.simpleName(), "Make this member @Autowired or remove it.");
          }
        }
      }
    }
  }

}