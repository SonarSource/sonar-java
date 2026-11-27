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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

/** Subtyping between {@link JType}s, with type argument containment (JLS 4.5.1) and primitive widening. */
final class Subtyping {

  private static final String FLOAT = "float";
  private static final String DOUBLE = "double";

  private Subtyping() {
  }

  static boolean isSubtype(JType type, Type superType, Set<String> visited) {
    if (type.kind == JType.Kind.CAPTURE || type.kind == JType.Kind.WILDCARD) {
      return isSubtype(type.element, superType, visited);
    }
    if (superType == null || superType.isUnknown() || type.isUnknown()) {
      return false;
    }
    if (type.isNullType()) {
      return !superType.isPrimitive();
    }
    if (!visited.add(type.fullyQualifiedName() + ">" + superType.fullyQualifiedName())) {
      return false;
    }
    if (superType.isTypeVar() && !type.isTypeVar()) {
      return false;
    }
    if (superType.isTypeVar() && sameTypeVariable(type, superType)) {
      return true;
    }
    return isSubtypeByStructure(type, superType, visited);
  }

  private static boolean sameTypeVariable(JType type, Type superType) {
    return type.isTypeVar() && superType instanceof JType other && Objects.equals(type.typeVarId, other.typeVarId);
  }

  private static boolean isSubtypeByStructure(JType type, Type superType, Set<String> visited) {
    if (!type.isArray() && sameErasure(type, superType) && argumentsMatch(type, superType)) {
      return true;
    }
    if (type.isPrimitive() && superType.isPrimitive()) {
      return widensTo(type.fullyQualifiedName(), superType.fullyQualifiedName());
    }
    if (type.kind == JType.Kind.INTERSECTION) {
      return type.intersectionBounds.stream().anyMatch(bound -> isSubtype(bound, superType, visited));
    }
    if (type.isArray()) {
      return isArraySubtype(type, superType);
    }
    if (type.isTypeVar()) {
      return hasSubtypeBound(type, superType, visited);
    }
    if (type.isPrimitive() || superType.isPrimitive()) {
      return false;
    }
    return inheritsFrom(type, superType, visited);
  }

  private static boolean isArraySubtype(JType type, Type superType) {
    if (superType.isArray()) {
      return type.elementType().isSubtypeOf(((Type.ArrayType) superType).elementType());
    }
    return superType.is("java.lang.Object") || superType.is("java.lang.Cloneable") || superType.is("java.io.Serializable");
  }

  private static boolean hasSubtypeBound(JType type, Type superType, Set<String> visited) {
    return type.sema.memberFactory().typeVariableBounds(type.typeVarId).stream()
      .anyMatch(bound -> bound instanceof JType upper && !upper.isUnknown() && upper != type && isSubtype(upper, superType, visited));
  }

  private static boolean inheritsFrom(JType type, Type superType, Set<String> visited) {
    Symbol.TypeSymbol owner = type.declaredSymbol();
    // Supertypes are walked erased: their arguments name the owner's type variables, not this type's arguments.
    Type superClass = owner.superClass();
    if (superClass != null && superClass.erasure() instanceof JType erasedSuper && isSubtype(erasedSuper, superType, visited)) {
      return true;
    }
    for (Type iface : owner.interfaces()) {
      if (iface.erasure() instanceof JType erasedIface && isSubtype(erasedIface, superType, visited)) {
        return true;
      }
    }
    return false;
  }

  private static boolean sameErasure(JType type, Type superType) {
    return type.fullyQualifiedName().equals(superType.erasure().fullyQualifiedName());
  }

  private static boolean argumentsMatch(JType type, Type superType) {
    List<Type> theirs = superType.typeArguments();
    if (type.args.isEmpty() || theirs.isEmpty()) {
      return true;
    }
    if (type.args.size() != theirs.size()) {
      return false;
    }
    for (int i = 0; i < type.args.size(); i++) {
      if (!contains(theirs.get(i), type.args.get(i))) {
        return false;
      }
    }
    return true;
  }

  /** Whether the type argument {@code mine} is contained by {@code theirs} (JLS 4.5.1). */
  private static boolean contains(Type theirs, JType mine) {
    if (theirs instanceof JType wanted && wanted.isWildcard()) {
      if (wanted.lower != null) {
        return mine.isWildcard() ? (mine.lower != null && wanted.lower.isSubtypeOf(mine.lower)) : wanted.lower.isSubtypeOf(mine);
      }
      if (wanted.element.is("java.lang.Object")) {
        return true;
      }
      return mine.isSubtypeOf(wanted.element);
    }
    return !mine.isWildcard() && mine.is(theirs.fullyQualifiedName());
  }

  private static boolean widensTo(String from, String to) {
    return switch (from) {
      case "byte" -> Set.of("short", "int", "long", FLOAT, DOUBLE).contains(to);
      case "short", "char" -> Set.of("int", "long", FLOAT, DOUBLE).contains(to);
      case "int" -> Set.of("long", FLOAT, DOUBLE).contains(to);
      case "long" -> Set.of(FLOAT, DOUBLE).contains(to);
      case FLOAT -> DOUBLE.equals(to);
      default -> false;
    };
  }
}
