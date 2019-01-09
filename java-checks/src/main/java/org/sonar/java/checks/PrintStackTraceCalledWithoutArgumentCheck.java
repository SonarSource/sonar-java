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
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Deque;
import java.util.LinkedList;

@Rule(key = "S1148")
public class PrintStackTraceCalledWithoutArgumentCheck extends BaseTreeVisitor implements JavaFileScanner {

  private final Deque<Symbol.TypeSymbol> enclosingClass = new LinkedList<>();
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    Symbol.TypeSymbol enclosingSymbol = tree.symbol();
    enclosingClass.push(enclosingSymbol);
    super.visitClass(tree);
    enclosingClass.pop();
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    super.visitMethodInvocation(tree);
    if (tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelectExpressionTree = (MemberSelectExpressionTree) tree.methodSelect();
      IdentifierTree identifierTree = memberSelectExpressionTree.identifier();
      if (!enclosingClassExtendsThrowable()
        && "printStackTrace".equals(identifierTree.name())
        && calledOnTypeInheritedFromThrowable(memberSelectExpressionTree.expression())
        && tree.arguments().isEmpty()) {
        context.reportIssue(this, identifierTree, "Use a logger to log this exception.");
      }
    }
  }

  private boolean enclosingClassExtendsThrowable() {
    return enclosingClass.peek() != null && enclosingClass.peek().type().isSubtypeOf("java.lang.Throwable");
  }

  private static boolean calledOnTypeInheritedFromThrowable(ExpressionTree tree) {
    return tree.symbolType().isSubtypeOf("java.lang.Throwable");
  }
}
