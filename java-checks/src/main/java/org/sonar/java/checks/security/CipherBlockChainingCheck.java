/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.ReassignmentFinder.getReassignments;

@Rule(key = "S3329")
public class CipherBlockChainingCheck extends AbstractMethodDetection {

  private static final MethodMatchers SECURE_RANDOM_GENERATE_SEED = MethodMatchers.create()
    .ofTypes("java.security.SecureRandom")
    .names("generateSeed")
    .withAnyParameters()
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create().ofTypes("javax.crypto.spec.IvParameterSpec").constructor().withAnyParameters().build();
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if (newClassTree.arguments().isEmpty() || isDynamicallyGenerated(newClassTree.arguments().get(0))) {
      return;
    }

    Tree mTree = ExpressionUtils.getEnclosingMethod(newClassTree);
    if (mTree != null) {
      MethodInvocationVisitor mitVisit = new MethodInvocationVisitor(newClassTree);
      mTree.accept(mitVisit);
      if (!mitVisit.secureRandomFound) {
        reportIssue(newClassTree, "Use a dynamically-generated, random IV.");
      }
    }
  }

  private static boolean isDynamicallyGenerated(ExpressionTree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = ((IdentifierTree) tree).symbol();
      if (!symbol.isVariableSymbol()) {
        return false;
      }
      VariableTree declaration = ((Symbol.VariableSymbol) symbol).declaration();
      return declaration != null &&
        (isSecureRandomGenerateSeed(declaration.initializer()) ||
          getReassignments(declaration, symbol.usages()).stream()
            .map(AssignmentExpressionTree::expression)
            .anyMatch(CipherBlockChainingCheck::isSecureRandomGenerateSeed));
    } else {
      return isSecureRandomGenerateSeed(tree);
    }
  }

  private static boolean isSecureRandomGenerateSeed(@Nullable ExpressionTree tree) {
    return tree != null && tree.is(Tree.Kind.METHOD_INVOCATION) && SECURE_RANDOM_GENERATE_SEED.matches((MethodInvocationTree) tree);
  }

  private static class MethodInvocationVisitor extends BaseTreeVisitor {

    private boolean secureRandomFound = false;
    private NewClassTree ivParameterSpecInstantiation = null;

    public MethodInvocationVisitor(NewClassTree newClassTree) {
      ivParameterSpecInstantiation = newClassTree;
    }

    private static final MethodMatchers SECURE_RANDOM_NEXT_BYTES = MethodMatchers.create()
      .ofTypes("java.security.SecureRandom")
      .names("nextBytes")
      .withAnyParameters()
      .build();

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
