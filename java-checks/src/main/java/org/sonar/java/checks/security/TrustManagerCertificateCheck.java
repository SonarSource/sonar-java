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
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4424")
public class TrustManagerCertificateCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher CHECK_TRUSTED_MATCHER = MethodMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf("javax.net.ssl.X509TrustManager"))
    .addParameter(TypeCriteria.is("java.security.cert.X509Certificate[]"))
    .addParameter(TypeCriteria.is("java.lang.String"));

  private static final MethodMatcher CHECK_CLIENT_TRUSTED_MATCHER = CHECK_TRUSTED_MATCHER.copy()
    .name("checkClientTrusted");

  private static final MethodMatcher CHECK_SERVER_TRUSTED_MATCHER = CHECK_TRUSTED_MATCHER.copy()
    .name("checkServerTrusted");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MethodTree methodTree = (MethodTree) tree;
    BlockTree blockTree = methodTree.block();
    if (blockTree == null) {
      return;
    }
    if ((CHECK_CLIENT_TRUSTED_MATCHER.matches(methodTree) || CHECK_SERVER_TRUSTED_MATCHER.matches(methodTree)) &&
      !ThrowExceptionVisitor.throwsException(blockTree)) {
      reportIssue(methodTree.simpleName(), "Change this method so it throws exceptions.");
    }
  }

  private static class ThrowExceptionVisitor extends BaseTreeVisitor {
    boolean throwsException = false;

    private static boolean throwsException(Tree tree) {
      ThrowExceptionVisitor visitor = new ThrowExceptionVisitor();
      tree.accept(visitor);
      return visitor.throwsException;
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      super.visitThrowStatement(tree);
      throwsException = true;
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      super.visitNewClass(tree);
      visitMethodSymbol(tree.constructorSymbol());
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      super.visitMethodInvocation(tree);
      visitMethodSymbol(tree.symbol());
    }

    private void visitMethodSymbol(Symbol symbol) {
      if (symbol.isMethodSymbol()) {
        throwsException |= !((Symbol.MethodSymbol) symbol).thrownTypes().isEmpty();
      } else {
        // JavaSymbolNotFound, to avoid FP, assumes it throws exceptions
        throwsException = true;
      }
    }

  }

}
