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
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.TypeSymbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

public abstract class AbstractSerializableInnerClassRule extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      visitClassTree((ClassTreeImpl) tree);
    }
  }

  private void visitClassTree(ClassTreeImpl classTree) {
    TypeSymbol symbol = classTree.getSymbol();
    if (isInnerClass(symbol) && directlyImplementsSerializable(symbol)) {
      Symbol owner = symbol.owner();
      if (owner.isTypeSymbol()) {
        TypeSymbol ownerType = (TypeSymbol) owner;
        if (isMatchingOuterClass(ownerType.getType()) && !symbol.isStatic()) {
          addIssue(classTree, "Make this inner class static");
        }
      } else if (owner.isMethodSymbol()) {
        TypeSymbol methodOwner = (TypeSymbol) owner.owner();
        if (isMatchingOuterClass(methodOwner.getType()) && !owner.isStatic()) {
          String methodName = owner.getName();
          addIssue(classTree, "Make \"" + methodName + "\" static");
        }
      }
    }
  }

  private boolean isInnerClass(TypeSymbol typeSymbol) {
    return !typeSymbol.equals(typeSymbol.outermostClass());
  }

  protected boolean isSerializable(Type type) {
    return type.isSubtypeOf("java.io.Serializable");
  }

  private boolean directlyImplementsSerializable(TypeSymbol symbol) {
    for (Type type : symbol.getInterfaces()) {
      if (type.is("java.io.Serializable")) {
        return true;
      }
    }
    return false;
  }

  protected abstract boolean isMatchingOuterClass(Type outerClass);

}
