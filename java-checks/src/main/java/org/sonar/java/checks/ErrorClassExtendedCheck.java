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
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S1194")
public class ErrorClassExtendedCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    TypeTree superClass = tree.superClass();
    if (tree.is(Tree.Kind.CLASS) && superClass != null) {
      if (superClass.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree idt = (IdentifierTree) superClass;
        if ("Error".equals(idt.name())) {
          context.reportIssue(this, superClass, "Extend \"java.lang.Exception\" or one of its subclasses.");
        }
      } else if (superClass.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) superClass;
        if ("Error".equals(mse.identifier().name()) && isJavaLang(mse.expression())) {
          context.reportIssue(this, superClass, "Extend \"java.lang.Exception\" or one of its subclasses.");
        }
      }
    }
    super.visitClass(tree);
  }

  private static boolean isJavaLang(ExpressionTree tree) {
    if (tree.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) tree;
      if (!"lang".equals(mse.identifier().name())) {
        return false;
      }
      if (mse.expression().is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree idt = (IdentifierTree) mse.expression();
        return "java".equals(idt.name());
      }
    }
    return false;
  }

}
