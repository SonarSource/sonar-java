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

import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.List;

public abstract class AbstractSerializableInnerClassRule extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      visitClassTree((ClassTree) tree);
    }
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
    return !typeSymbol.equals(((JavaSymbol.TypeJavaSymbol) typeSymbol).outermostClass());
  }

  protected boolean isSerializable(Type type) {
    return type.isSubtypeOf("java.io.Serializable");
  }

  private static boolean directlyImplementsSerializable(Symbol.TypeSymbol symbol) {
    return symbol.interfaces().stream().anyMatch(t ->  t.is("java.io.Serializable"));
  }

  protected abstract boolean isMatchingOuterClass(Type outerClass);

}
