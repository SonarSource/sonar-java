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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.MethodSymbol;
import org.sonar.java.resolve.Symbol.TypeSymbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(
  key = "S2055",
  priority = Priority.CRITICAL,
  tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class SerializableSuperConstructorCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      ClassTreeImpl classTree = (ClassTreeImpl) tree;
      TypeSymbol classSymbol = classTree.getSymbol();
      if (isSerializable(classSymbol.getType()) && !isSerializable(classSymbol.getSuperclass())) {
        Type superclass = classSymbol.getSuperclass();
        if (!hasNonPrivateNoArgConstructor(superclass)) {
          addIssue(tree, "Add a no-arg constructor to \"" + superclass + "\".");
        }
      }
    }
  }

  private boolean isSerializable(Type type) {
    return type.isSubtypeOf("java.io.Serializable");
  }

  private boolean hasNonPrivateNoArgConstructor(Type type) {
    List<Symbol> constructors = type.getSymbol().members().lookup("<init>");
    for (Symbol member : constructors) {
      if (member.isKind(Symbol.MTH)) {
        MethodSymbol method = (MethodSymbol) member;
        if (method.getParametersTypes().isEmpty() && !method.isPrivate()) {
          return true;
        }
      }
    }
    return constructors.isEmpty();
  }

}
