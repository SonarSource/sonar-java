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
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

@Rule(key = "S1163")
public class ThrowsFromFinallyCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  private int finallyLevel = 0;
  private boolean isInMethodWithinFinally;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitTryStatement(TryStatementTree tree) {
    scan(tree.resourceList());
    scan(tree.block());
    scan(tree.catches());
    finallyLevel++;
    scan(tree.finallyBlock());
    finallyLevel--;
  }

  @Override
  public void visitThrowStatement(ThrowStatementTree tree) {
    if(isInFinally() && !isInMethodWithinFinally){
      context.reportIssue(this, tree, "Refactor this code to not throw exceptions in finally blocks.");
    }
    super.visitThrowStatement(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    isInMethodWithinFinally = isInFinally();
    super.visitMethod(tree);
    isInMethodWithinFinally = false;
  }

  private boolean isInFinally(){
    return finallyLevel>0;
  }

}
