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
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S3014")
public class DisallowedThreadGroupCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final String JAVA_LANG_THREADGROUP = "java.lang.ThreadGroup";
  private static final String MESSAGE = "Remove this use of \"ThreadGroup\". Prefer the use of \"ThreadPoolExecutor\".";

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (context.getSemanticModel() == null) {
      return;
    }
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    TypeTree superClass = tree.superClass();
    if (superClass != null && superClass.symbolType().is(JAVA_LANG_THREADGROUP)) {
      context.reportIssue(this, superClass, MESSAGE);
    }
    super.visitClass(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    boolean notOverriding = Boolean.FALSE.equals(tree.isOverriding());
    boolean reported = false;
    boolean declaresThreadGroup = false;

    TypeTree returnType = tree.returnType();
    if (returnType != null && returnType.symbolType().is(JAVA_LANG_THREADGROUP)) {
      if (notOverriding) {
        context.reportIssue(this, returnType, MESSAGE);
        reported = true;
      }
      declaresThreadGroup = true;
    }
    for (VariableTree variableTree : tree.parameters()) {
      TypeTree type = variableTree.type();
      if (type.symbolType().is(JAVA_LANG_THREADGROUP)) {
        if (notOverriding) {
          context.reportIssue(this, type, MESSAGE);
          reported = true;
        }
        declaresThreadGroup = true;
      }
    }

    if (declaresThreadGroup && !reported) {
      // skip body of overrides to avoid FPs
      return;
    }
    super.scan(tree.block());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    TypeTree type = tree.type();
    if (type.symbolType().is(JAVA_LANG_THREADGROUP)) {
      context.reportIssue(this, type, MESSAGE);
    }
    super.visitVariable(tree);
  }
}
