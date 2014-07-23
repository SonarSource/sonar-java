/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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

import org.sonar.api.rule.RuleKey;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Deque;
import java.util.LinkedList;

@Rule(
    key = PrintStackTraceCalledWithoutArgumentCheck.RULE_KEY,
    priority = Priority.CRITICAL,
    tags = {"error-handling"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class PrintStackTraceCalledWithoutArgumentCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1148";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);
  private JavaFileScannerContext context;
  private final Deque<Symbol.TypeSymbol> enclosingClass = new LinkedList<Symbol.TypeSymbol>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    Symbol.TypeSymbol enclosingSymbol = ((ClassTreeImpl) tree).getSymbol();
    enclosingClass.push(enclosingSymbol);
    super.visitClass(tree);
    enclosingClass.pop();
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    super.visitMethodInvocation(tree);
    if (tree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      IdentifierTree identifierTree = ((MemberSelectExpressionTree) tree.methodSelect()).identifier();
      if (!enclosingClassExtendsThrowable() && "printStackTrace".equals(identifierTree.name()) && calledOnTypeInheritedFromThrowable(tree)) {
        context.addIssue(identifierTree, ruleKey, "Use a logger to log this exception.");
      }
    }
  }

  private boolean enclosingClassExtendsThrowable() {
    return enclosingClass.peek() != null && extendsThrowable((Type.ClassType) enclosingClass.peek().getType());
  }

  private boolean calledOnTypeInheritedFromThrowable(MethodInvocationTree tree) {
    //TODO this is painful way to access caller site of a method.
    Type type = ((AbstractTypedTree) ((MemberSelectExpressionTree) tree.methodSelect()).expression()).getType();
    return extendsThrowable((Type.ClassType) type);
  }

  private boolean extendsThrowable(Type.ClassType type) {
    Symbol.TypeSymbol site = type.getSymbol();
    if (isThrowable(site)) {
      return true;
    }
    while (site.getSuperclass() != null) {
      site = ((Type.ClassType) site.getSuperclass()).getSymbol();
      if (isThrowable(site)) {
        return true;
      }
    }
    return false;
  }

  private boolean isThrowable(Symbol.TypeSymbol site) {
    return "Throwable".equals(site.getName()) && "java.lang".equals(site.owner().getName());
  }

}
