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

import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3329")
public class CipherBlockChainingCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(
      MethodMatcher.create().typeDefinition("javax.crypto.spec.IvParameterSpec").name("<init>").withAnyParameters());
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    Tree mTree = ExpressionUtils.getEnclosingMethod(newClassTree);
    if (mTree != null) {
      MethodInvocationVisitor mitVisit = new MethodInvocationVisitor(newClassTree);
      mTree.accept(mitVisit);
      if (!mitVisit.secureRandomFound) {
        reportIssue(newClassTree, "Use a dynamically-generated, random IV.");
      }
    }
  }

  private static class MethodInvocationVisitor extends BaseTreeVisitor {

    private boolean secureRandomFound = false;
    private NewClassTree ivParameterSpecInstantiation = null;

    public MethodInvocationVisitor(NewClassTree newClassTree) {
      ivParameterSpecInstantiation = newClassTree;
    }

    private static final MethodMatcher SECURE_RANDOM_NEXT_BYTES = MethodMatcher.create()
      .typeDefinition("java.security.SecureRandom")
      .name("nextBytes")
      .withAnyParameters();

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      if (SECURE_RANDOM_NEXT_BYTES.matches(methodInvocation)) {
        Symbol initVector = symbol(ivParameterSpecInstantiation.arguments().get(0));
        if (initVector != null && initVector.equals(symbol(methodInvocation.arguments().get(0)))) {
          secureRandomFound = true;
        }
      }
      super.visitMethodInvocation(methodInvocation);
    }

    @CheckForNull
    private static Symbol symbol(ExpressionTree expression) {
      Symbol symbol = null;
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        symbol = ((IdentifierTree) expression).symbol();
      } else if (expression.is(Tree.Kind.MEMBER_SELECT)) {
        symbol = ((MemberSelectExpressionTree) expression).identifier().symbol();
      }
      return symbol;
    }
  }
}
