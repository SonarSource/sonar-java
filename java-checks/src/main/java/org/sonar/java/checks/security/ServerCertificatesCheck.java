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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4830")
// FIXME SONAR-17167: S4424 has been dropped and causes crashes in SQ - Only keeping the implicit "squid:S4830" (see JavaRulesDefinition)
// @DeprecatedRuleKey(ruleKey = "S4424", repositoryKey = "squid")
// @DeprecatedRuleKey(ruleKey = "S4424", repositoryKey = "java")
public class ServerCertificatesCheck extends IssuableSubscriptionVisitor {
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final String X509_CERTIFICATE_ARRAY = "java.security.cert.X509Certificate[]";

  private static final MethodMatchers TRUST_MANAGER_MATCHER = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("javax.net.ssl.X509TrustManager")
      .names("checkClientTrusted", "checkServerTrusted")
      .addParametersMatcher(X509_CERTIFICATE_ARRAY, JAVA_LANG_STRING)
      .build(),
    MethodMatchers.create()
      .ofSubTypes("javax.net.ssl.X509ExtendedTrustManager")
      .names("checkClientTrusted", "checkServerTrusted")
      .addParametersMatcher(X509_CERTIFICATE_ARRAY, JAVA_LANG_STRING, "java.net.Socket")
      .addParametersMatcher(X509_CERTIFICATE_ARRAY, JAVA_LANG_STRING, "javax.net.ssl.SSLEngine")
      .build()
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    BlockTree blockTree = methodTree.block();
    if (blockTree == null) {
      return;
    }
    if (TRUST_MANAGER_MATCHER.matches(methodTree) &&
      (blockTree.body().isEmpty() || !ThrowExceptionVisitor.throwsException(blockTree))) {
      reportIssue(methodTree.simpleName(), "Enable server certificate validation on this SSL/TLS connection.");
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
