/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import java.util.Collections;
import java.util.List;

public abstract class AbstractSerializableInnerClassRule extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    visitClassTree((ClassTree) tree);
  }

  private void visitClassTree(ClassTree classTree) {
    Symbol.TypeSymbol symbol = classTree.symbol();
    if (isInnerClass(symbol) && directlyImplementsSerializable(symbol)) {
      Tree reportTree = ExpressionsHelper.reportOnClassTree(classTree);
      Symbol owner = symbol.owner();
      if (owner.isTypeSymbol()) {
        Symbol.TypeSymbol ownerType = (Symbol.TypeSymbol) owner;
        if (isMatchingOuterClass(ownerType.type()) && !symbol.isStatic()) {
          reportIssue(reportTree, "Make this inner class static");
        }
      } else if (owner.isMethodSymbol()) {
        Symbol.TypeSymbol methodOwner = (Symbol.TypeSymbol) owner.owner();
        if (isMatchingOuterClass(methodOwner.type()) && !owner.isStatic()) {
          String methodName = owner.name();
          reportIssue(reportTree, "Make \"" + methodName + "\" static");
        }
      }
    }
  }

  private static boolean isInnerClass(Symbol.TypeSymbol typeSymbol) {
    return !typeSymbol.equals(typeSymbol.outermostClass());
  }

  protected boolean isSerializable(Type type) {
    return type.isSubtypeOf("java.io.Serializable");
  }

  private static boolean directlyImplementsSerializable(Symbol.TypeSymbol symbol) {
    return symbol.interfaces().stream().anyMatch(t ->  t.is("java.io.Serializable"));
  }

  protected abstract boolean isMatchingOuterClass(Type outerClass);

}
