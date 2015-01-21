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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.resolve.Symbol.TypeSymbol;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(
  key = "S2257",
  priority = Priority.BLOCKER,
  tags = {"cwe", "owasp-top10", "sans-top25", "security"})
public class CustomCryptographicAlgorithmCheck extends SubscriptionBaseVisitor {

  private static final String MESSAGE_DIGEST_QUALIFIED_NAME = "java.security.MessageDigest";

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      if (isJavaSecurityMessageDigestSubClass((ClassTreeImpl) tree)) {
        addIssue(tree, "Use a standard algorithm instead of creating a custom one.");
      }
    }
  }

  private boolean isJavaSecurityMessageDigestSubClass(ClassTreeImpl tree) {
    TypeSymbol classSymbol = tree.getSymbol();
    // Corner case: A type is a subtype of itself
    return classSymbol != null && !classSymbol.getType().is(MESSAGE_DIGEST_QUALIFIED_NAME) &&
      classSymbol.getType().isSubtypeOf(MESSAGE_DIGEST_QUALIFIED_NAME);
  }
}
