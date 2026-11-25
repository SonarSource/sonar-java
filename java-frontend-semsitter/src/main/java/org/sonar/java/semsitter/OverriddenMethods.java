/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.Type;

/** Finds the methods a method overrides, walking the supertypes of its class. */
final class OverriddenMethods {

  private final JMethodSymbol symbol;
  private final JSema sema;

  OverriddenMethods(JMethodSymbol symbol) {
    this.symbol = symbol;
    this.sema = symbol.sema;
  }

  List<MethodSymbol> find() {
    if (!(symbol.owner() instanceof JTypeSymbol typeSymbol) || symbol.isUnknown() || symbol.isStatic() || symbol.isPrivate()) {
      return List.of();
    }
    List<MethodSymbol> found = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    if (typeSymbol.isInterface()) {
      match(found, sema.typeSymbol("java.lang.Object"), null);
      visited.add("java.lang.Object");
    }
    walk(found, visited, typeSymbol.superClass());
    for (Type iface : typeSymbol.interfaces()) {
      walk(found, visited, iface);
    }
    return found;
  }

  private void walk(List<MethodSymbol> found, Set<String> visited, @Nullable Type type) {
    if (type == null || type.isUnknown() || !visited.add(type.fullyQualifiedName())) {
      return;
    }
    if (type.symbol() instanceof JTypeSymbol parent) {
      match(found, parent, type);
      walk(found, visited, parent.superClass());
      for (Type iface : parent.interfaces()) {
        walk(found, visited, iface);
      }
    }
  }

  private void match(List<MethodSymbol> found, JTypeSymbol parent, @Nullable Type declaring) {
    for (Symbol member : parent.membersNamed(symbol.name())) {
      if (member instanceof JMethodSymbol method && !method.isStatic() && !method.isPrivate() && sameSignature(method, declaring)) {
        found.add(method);
        return;
      }
    }
  }

  private boolean viaClassVariable(JMethodSymbol library, int index, Type declaring, String mine) {
    Type argument = sema.substitution().classVariableArgument(library, index, declaring);
    return argument != null && mine.equals(argument.erasure().fullyQualifiedName());
  }

  private boolean sameSignature(JMethodSymbol other, @Nullable Type declaring) {
    List<Type> theirs = other.parameterTypes();
    if (symbol.parameterTypes().size() != theirs.size()) {
      return false;
    }
    for (int i = 0; i < symbol.parameterTypes().size(); i++) {
      String mine = symbol.parameterTypes().get(i).erasure().fullyQualifiedName();
      boolean same = mine.equals(theirs.get(i).erasure().fullyQualifiedName())
        || (declaring != null && (mine.equals(sema.substitution().asMemberOf(theirs.get(i), declaring).erasure().fullyQualifiedName())
          || viaClassVariable(other, i, declaring, mine)));
      if (!same) {
        return false;
      }
    }
    return true;
  }
}
