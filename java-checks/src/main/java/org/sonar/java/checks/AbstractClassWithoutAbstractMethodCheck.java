/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Rule(key = "S1694")
public class AbstractClassWithoutAbstractMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    Symbol.TypeSymbol typeSymbol = classTree.symbol();
    if (typeSymbol.isAbstract()) {
      Collection<Symbol> members = typeSymbol.memberSymbols();
      int nbAbstractMethod = countAbstractMethods(members);
      // don't count this and super as members
      int nbOfMembers = members.size();
      if (hasDefaultConstructor(members)) {
        //remove default constructor from members
        nbOfMembers -=1;
      }
      if (isExtendingObject(classTree) && nbAbstractMethod == nbOfMembers) {
        // emtpy abstract class or only abstract method
        context.reportIssue(this, classTree.simpleName(), "Convert this \"" + typeSymbol.name() + "\" class to an interface");
      }
      if (nbOfMembers > 0 && nbAbstractMethod == 0 && !isPartialImplementation(classTree)) {
        // Not empty abstract class with no abstract method
        context.reportIssue(this, classTree.simpleName(), "Convert this \"" + typeSymbol.name() + "\" class to a concrete class with a private constructor");
      }
    }
  }

  private static boolean hasDefaultConstructor(Collection<Symbol> members) {
    for (Symbol member : members) {
      if ("<init>".equals(member.name()) && member.declaration() == null) {
        return true;
      }
    }
    return false;
  }

  private static boolean isExtendingObject(ClassTree tree) {
    TypeTree superClass = tree.superClass();
    return superClass == null || superClass.symbolType().is("java.lang.Object");
  }

  private static boolean isPartialImplementation(ClassTree tree) {
    return tree.superClass() != null || !tree.superInterfaces().isEmpty();
  }

  private static int countAbstractMethods(Collection<? extends Symbol> symbols) {
    int abstractMethod = 0;
    for (Symbol sym : symbols) {
      if (isAbstractMethod(sym)) {
        abstractMethod++;
      }
    }
    return abstractMethod;
  }

  private static boolean isAbstractMethod(Symbol sym) {
    return sym.isMethodSymbol() && sym.isAbstract();
  }
}
