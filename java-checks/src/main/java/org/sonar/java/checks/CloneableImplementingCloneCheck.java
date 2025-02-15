/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S2157")
public class CloneableImplementingCloneCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers CLONE_MATCHER = MethodMatchers.create()
    .ofAnyType()
    .names("clone")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Symbol.TypeSymbol classSymbol = classTree.symbol();
    if (isCloneable(classTree) && !classSymbol.isAbstract() && !declaresCloneMethod(classSymbol)) {
      reportIssue(classTree.simpleName(), "Add a \"clone()\" method to this class.");
    }
  }

  private static boolean declaresCloneMethod(Symbol.TypeSymbol classSymbol) {
    return classSymbol.lookupSymbols("clone").stream().anyMatch(CLONE_MATCHER::matches);
  }

  private static boolean isCloneable(ClassTree classTree) {
    return classTree.superInterfaces().stream().map(TypeTree::symbolType).anyMatch(t -> t.is("java.lang.Cloneable"));
  }
}
