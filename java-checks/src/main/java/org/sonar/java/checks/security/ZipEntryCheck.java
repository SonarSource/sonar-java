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
package org.sonar.java.checks.security;

import java.util.function.Predicate;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S5042")
public class ZipEntryCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final Predicate<Type> IS_ZIP_ENTRY = type -> type.isSubtypeOf("java.util.zip.ZipEntry")
    || type.isSubtypeOf("org.apache.commons.compress.archivers.ArchiveEntry");
  private static final String ISSUE_MESSAGE = "Make sure that expanding this archive file is safe here.";

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
  public void visitVariable(VariableTree tree) {
    if (isField(tree)) {
      // skip fields
      return;
    }
    super.visitVariable(tree);
  }

  private static boolean isField(VariableTree tree) {
    return tree.symbol().owner().isTypeSymbol();
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (tree.block() == null || tree.is(Tree.Kind.CONSTRUCTOR)) {
      // skip everything for abstract methods (from interfaces or abstract class) and constructors
      return;
    }

    tree.parameters().stream()
      .filter(p -> IS_ZIP_ENTRY.test(p.symbol().type()))
      .forEach(p -> context.reportIssue(this, p, ISSUE_MESSAGE));

    super.visitMethod(tree);
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    if (IS_ZIP_ENTRY.test(tree.symbolType())) {
      context.reportIssue(this, tree, ISSUE_MESSAGE);
    }
    super.visitMethodInvocation(tree);
  }
}
