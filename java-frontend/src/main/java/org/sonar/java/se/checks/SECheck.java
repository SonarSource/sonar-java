/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.se.checks;

import com.google.common.collect.Multimap;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Map;

public abstract class SECheck implements JavaFileScanner {

  public void init(){
  }

  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    return context.getState();
  }

  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    return context.getState();
  }

  public void checkEndOfExecution(CheckerContext context) {
    // By default do nothing
  }

  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    // By default do nothing
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    Multimap<Tree, DefaultJavaFileScannerContext.SEIssue> issues = ((DefaultJavaFileScannerContext) context).getSEIssues(getClass());
    for (Map.Entry<Tree, DefaultJavaFileScannerContext.SEIssue> issue : issues.entries()) {
      DefaultJavaFileScannerContext.SEIssue seIssue = issue.getValue();
      context.reportIssue(this, seIssue.getTree(), seIssue.getMessage(), seIssue.getSecondary(), null);
    }
  }

}
