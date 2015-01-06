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
      Type type = classSymbol.getType();
      if (isSerializable(type) && !isSerializable(classSymbol.getSuperclass())) {
        Type superclass = classSymbol.getSuperclass();
        if (!hasNoArgConstructor(superclass) && !hasReadObjectMethod(type)) {
          String superclassName = superclass.toString();
          addIssue(tree, "Add a no-arg constructor to \"" + superclassName + "\" or implement \"writeObject()\" and \"readObject()\".");
        }
      }
    }
  }

  private boolean isSerializable(Type type) {
    return type.isSubtypeOf("java.io.Serializable");
  }

  private boolean hasNoArgConstructor(Type type) {
    List<Symbol> constructors = type.getSymbol().members().lookup("<init>");
    if (constructors.isEmpty()) {
      return true;
    }
    for (Symbol member : constructors) {
      if (member.isKind(Symbol.MTH)) {
        MethodSymbol method = (MethodSymbol) member;
        if (method.getParametersTypes().isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasReadObjectMethod(Type type) {
    List<Symbol> members = type.getSymbol().members().lookup("readObject");
    for (Symbol member : members) {
      if (member.isKind(Symbol.MTH) && isReadObjectMethod((MethodSymbol) member)) {
        return true;
      }
    }
    return false;
  }

  private boolean isReadObjectMethod(MethodSymbol method) {
    List<Type> parametersTypes = method.getParametersTypes();
    if (parametersTypes.size() != 1 || !parametersTypes.get(0).is("java.io.ObjectInputStream")) {
      return false;
    }
    boolean throwsClassNotFound = false;
    boolean throwsIOException = false;
    for (TypeSymbol thrownTypeSymbol : method.getThrownTypes()) {
      if (thrownTypeSymbol.getType().is("java.io.IOException")) {
        throwsIOException = true;
      } else if (thrownTypeSymbol.getType().is("java.lang.ClassNotFoundException")) {
        throwsClassNotFound = true;
      }
    }
    return throwsClassNotFound && throwsIOException;
  }

}
