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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2257")
public class CustomCryptographicAlgorithmCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE_DIGEST_QUALIFIED_NAME = "java.security.MessageDigest";
  private static final String MESSAGE = "Make sure using a non-standard cryptographic algorithm is safe here.";

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (hasSemantic() && isJavaSecurityMessageDigestSubClass(classTree)) {
      reportIssue(ExpressionsHelper.reportOnClassTree(classTree), MESSAGE);
    }
  }

  private static boolean isJavaSecurityMessageDigestSubClass(ClassTree tree) {
    Symbol.TypeSymbol classSymbol = tree.symbol();
    // Corner case: A type is a subtype of itself
    return classSymbol != null && !classSymbol.type().is(MESSAGE_DIGEST_QUALIFIED_NAME) &&
      classSymbol.type().isSubtypeOf(MESSAGE_DIGEST_QUALIFIED_NAME);
  }
}
