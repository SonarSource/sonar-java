/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.checks.spring;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3751")
public class RequestMappingMethodPublicCheck extends IssuableSubscriptionVisitor {

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
    Symbol.MethodSymbol methodSymbol = methodTree.symbol();

    if (isClassController(methodSymbol)
      && isRequestMappingAnnotated(methodSymbol)
      && !isPublicMethod(methodSymbol)) {
      reportIssue(methodTree.simpleName(), "Make this method \"public\".");
    }
  }

  private static boolean isClassController(Symbol.MethodSymbol methodSymbol) {
    SymbolMetadata parentClassOwner = methodSymbol.owner().metadata();
    return parentClassOwner.isAnnotatedWith("org.springframework.stereotype.Controller");
  }

  private static boolean isRequestMappingAnnotated(MethodSymbol methodSymbol) {
    return methodSymbol.metadata().isAnnotatedWith("org.springframework.web.bind.annotation.RequestMapping");
  }

  private static boolean isPublicMethod(MethodSymbol methodSymbol) {
    return methodSymbol.isPublic();
  }

}
