/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3329")
public class CipherBlockChainingCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodMatcher.create().typeDefinition("javax.crypto.spec.IvParameterSpec").name("<init>").withAnyParameters());
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    Tree mTree = findEnclosingMethod(newClassTree);
    if (mTree != null) {
      MethodInvocationVisitor mitVisit = new MethodInvocationVisitor(newClassTree);
      mTree.accept(mitVisit);
      if (!mitVisit.secureRandomFound) {
        reportIssue(newClassTree, "Use a dynamically-generated, random IV.");
      }
    }
  }

  private static MethodTree findEnclosingMethod(Tree tree) {
    while (!tree.is(Tree.Kind.CLASS, Tree.Kind.METHOD)) {
      tree = tree.parent();
      if (tree.is(Tree.Kind.METHOD)) {
        return (MethodTree) tree;
      }
    }
    return null;
  }

  private static class MethodInvocationVisitor extends BaseTreeVisitor {

    private NewClassTree newClass;
    private boolean secureRandomFound = false;

    public MethodInvocationVisitor(NewClassTree newClassTree) {
      this.newClass = newClassTree;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if ("nextBytes".equals(tree.symbol().name())) {
        Symbol initVector = returnSymbolOfInitializerVectorBuffer(newClass.arguments().get(0));
        if (initVector != null && initVector.equals(returnSymbolOfInitializerVectorBuffer(tree.arguments().get(0)))) {
          secureRandomFound = true;
        }
      }
      super.visitMethodInvocation(tree);
    }

    @CheckForNull
    private static Symbol returnSymbolOfInitializerVectorBuffer(ExpressionTree argument) {
      Symbol symbolArgument = null;
      if (argument.is(Tree.Kind.IDENTIFIER)) {
        symbolArgument = ((IdentifierTree) argument).symbol();
      } else if (argument.is(Tree.Kind.MEMBER_SELECT)) {
        symbolArgument = ((MemberSelectExpressionTree) argument).identifier().symbol();
      }
      return symbolArgument;
    }
  }
}
