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
package org.sonar.java.checks.naming;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S2166")
public class ClassNamedLikeExceptionCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      ClassTree classTree = (ClassTree) tree;
      Symbol.TypeSymbol symbol = classTree.symbol();
      String className = symbol.name();
      if (endsWithException(className) && !isSubtypeOfException(symbol) && !hasUnknownSuperType(symbol)) {
        String suffix = className.substring(className.length() - "exception".length());
        reportIssue(classTree.simpleName(), "Rename this class to remove \"" + suffix + "\" or correct its inheritance.");
      }
    }
  }

  private static boolean endsWithException(String className) {
    return className.toLowerCase(Locale.US).endsWith("exception");
  }

  private static boolean isSubtypeOfException(Symbol symbol) {
    return symbol.type().isSubtypeOf("java.lang.Exception");
  }

  private static boolean hasUnknownSuperType(Symbol.TypeSymbol symbol) {
    Type superClass = symbol.superClass();
    return superClass != null && (superClass.isUnknown() || hasUnknownSuperType(superClass.symbol()));
  }

}
