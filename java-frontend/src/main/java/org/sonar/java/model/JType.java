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
package org.sonar.java.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

/**
 * A type recovered from a SemSitter symbol fact or a JVM descriptor.
 * Fully-qualified names are erased binary names ({@code java.util.Map$Entry}).
 */
final class JType implements Type, Type.ArrayType {

  enum Kind {
    NAMED, PRIMITIVE, VOID, NULL, ARRAY, TYPEVAR, UNKNOWN, INTERSECTION
  }

  private static final Set<String> NUMERIC = Set.of("byte", "char", "short", "int", "long", "float", "double");

  final JSema sema;
  final Kind kind;
  final String fqn;
  final List<JType> args;
  @Nullable
  final JType element;
  @Nullable
  final String typeVarId;
  private final List<JType> intersection;

  private JType(JSema sema, Kind kind, String fqn, List<JType> args, @Nullable JType element, @Nullable String typeVarId, List<JType> intersection) {
    this.sema = sema;
    this.kind = kind;
    this.fqn = fqn;
    this.args = args;
    this.element = element;
    this.typeVarId = typeVarId;
    this.intersection = intersection;
  }

  static JType named(JSema sema, String fqn, List<JType> args) {
    return new JType(sema, Kind.NAMED, fqn, List.copyOf(args), null, null, List.of());
  }

  static JType primitive(JSema sema, String name) {
    return new JType(sema, "void".equals(name) ? Kind.VOID : Kind.PRIMITIVE, name, List.of(), null, null, List.of());
  }

  static JType nullType(JSema sema) {
    return new JType(sema, Kind.NULL, "<nulltype>", List.of(), null, null, List.of());
  }

  static JType unknown(JSema sema) {
    return new JType(sema, Kind.UNKNOWN, "!Unknown!", List.of(), null, null, List.of());
  }

  static JType array(JSema sema, JType element) {
    return new JType(sema, Kind.ARRAY, element.fullyQualifiedName() + "[]", List.of(), element, null, List.of());
  }

  static JType typeVar(JSema sema, String name, String id) {
    return new JType(sema, Kind.TYPEVAR, name, List.of(), null, id, List.of());
  }

  /** Replaces type variables named in {@code substitution} inside this type. */
  JType substitute(Map<String, Type> substitution) {
    if (substitution.isEmpty()) {
      return this;
    }
    if (kind == Kind.TYPEVAR) {
      Type found = typeVarId == null ? null : substitution.get(typeVarId);
      if (found == null) {
        found = substitution.get(fqn);
      }
      if (found instanceof JType replaced) {
        return replaced;
      }
    }
    if (kind == Kind.ARRAY && element != null) {
      JType replaced = element.substitute(substitution);
      return replaced == element ? this : array(sema, replaced);
    }
    if (kind == Kind.NAMED && !args.isEmpty()) {
      List<JType> replaced = new ArrayList<>();
      boolean changed = false;
      for (JType argument : args) {
        JType next = argument.substitute(substitution);
        changed |= next != argument;
        replaced.add(next);
      }
      return changed ? named(sema, fqn, replaced) : this;
    }
    return this;
  }

  static JType intersection(JSema sema, List<JType> bounds) {
    String name = bounds.stream().map(JType::fullyQualifiedName).sorted().reduce((a, b) -> a + " & " + b).orElse("!Unknown!");
    return new JType(sema, Kind.INTERSECTION, name, List.of(), null, null, List.copyOf(bounds));
  }

  @Override
  public boolean is(String fullyQualifiedName) {
    return fullyQualifiedName.equals(this.fullyQualifiedName());
  }

  @Override
  public boolean isSubtypeOf(String fullyQualifiedName) {
    return isSubtypeOf(sema.getClassType(fullyQualifiedName));
  }

  @Override
  public boolean isSubtypeOf(Type superType) {
    return isSubtypeOf(superType, new HashSet<>());
  }

  private boolean isSubtypeOf(Type superType, Set<String> visited) {
    if (superType == null || superType.isUnknown() || isUnknown()) {
      return false;
    }
    if (isNullType()) {
      return !superType.isPrimitive();
    }
    if (!visited.add(fullyQualifiedName() + ">" + superType.fullyQualifiedName())) {
      return false;
    }
    if (sameErasure(superType) && argumentsMatch(superType)) {
      return true;
    }
    if (isPrimitive() && superType.isPrimitive()) {
      return widensTo(fullyQualifiedName(), superType.fullyQualifiedName());
    }
    if (isArray()) {
      if (superType.isArray()) {
        return elementType().isSubtypeOf(((Type.ArrayType) superType).elementType());
      }
      return superType.is("java.lang.Object") || superType.is("java.lang.Cloneable") || superType.is("java.io.Serializable");
    }
    if (isPrimitive() || superType.isPrimitive() || isTypeVar()) {
      return false;
    }
    Symbol.TypeSymbol owner = declaredSymbol();
    if (owner.isUnknown()) {
      return false;
    }
    // Supertypes are walked erased: their arguments name the owner's type variables, not this type's arguments.
    Type superClass = owner.superClass();
    if (superClass != null && ((JType) superClass.erasure()).isSubtypeOf(superType, visited)) {
      return true;
    }
    for (Type iface : owner.interfaces()) {
      if (((JType) iface.erasure()).isSubtypeOf(superType, visited)) {
        return true;
      }
    }
    return false;
  }

  private boolean sameErasure(Type superType) {
    return fullyQualifiedName().equals(superType.erasure().fullyQualifiedName());
  }

  private boolean argumentsMatch(Type superType) {
    if (!isParameterized() || !superType.isParameterized()) {
      return true;
    }
    List<Type> theirs = superType.typeArguments();
    if (args.size() != theirs.size()) {
      return false;
    }
    for (int i = 0; i < args.size(); i++) {
      if (!args.get(i).is(theirs.get(i).fullyQualifiedName())) {
        return false;
      }
    }
    return true;
  }

  private static boolean widensTo(String from, String to) {
    if (from.equals(to)) {
      return true;
    }
    return switch (from) {
      case "byte" -> Set.of("short", "int", "long", "float", "double").contains(to);
      case "short", "char" -> Set.of("int", "long", "float", "double").contains(to);
      case "int" -> Set.of("long", "float", "double").contains(to);
      case "long" -> Set.of("float", "double").contains(to);
      case "float" -> "double".equals(to);
      default -> false;
    };
  }

  @Override
  public boolean isArray() {
    return kind == Kind.ARRAY;
  }

  @Override
  public boolean isClass() {
    return kind == Kind.NAMED && !isUnknown();
  }

  @Override
  public boolean isVoid() {
    return kind == Kind.VOID;
  }

  @Override
  public boolean isPrimitive() {
    return kind == Kind.PRIMITIVE;
  }

  @Override
  public boolean isPrimitive(Primitives primitive) {
    return primitive.name().toLowerCase(Locale.ROOT).equals(fullyQualifiedName());
  }

  @Override
  public boolean isPrimitiveWrapper() {
    return isClass() && JUtils.WRAPPER_TO_PRIMITIVE.containsKey(fullyQualifiedName());
  }

  @Nullable
  @Override
  public Type primitiveWrapperType() {
    String name = JUtils.PRIMITIVE_TO_WRAPPER.get(fullyQualifiedName());
    return name == null ? null : sema.getClassType(name);
  }

  @Nullable
  @Override
  public Type primitiveType() {
    String name = JUtils.WRAPPER_TO_PRIMITIVE.get(fullyQualifiedName());
    return name == null ? null : sema.getClassType(name);
  }

  @Override
  public boolean isNullType() {
    return kind == Kind.NULL;
  }

  @Override
  public boolean isTypeVar() {
    return kind == Kind.TYPEVAR;
  }

  @Override
  public boolean isRawType() {
    if (kind != Kind.NAMED || isUnknown()) {
      return false;
    }
    Symbol.TypeSymbol owner = declaredSymbol();
    return owner instanceof JTypeSymbol typeSymbol && typeSymbol.typeParameterCount() > 0 && args.isEmpty();
  }

  @Override
  public Type declaringType() {
    if (isUnknown() || kind != Kind.NAMED) {
      return this;
    }
    if (isRawType()) {
      Symbol.TypeSymbol owner = declaredSymbol();
      int count = owner instanceof JTypeSymbol typeSymbol ? typeSymbol.typeParameterCount() : 0;
      List<JType> variables = new java.util.ArrayList<>();
      for (int i = 0; i < count; i++) {
        variables.add(typeVar(sema, "T" + i, fqn + "<" + i + ">"));
      }
      return named(sema, fqn, variables);
    }
    return sema.getClassType(fqn);
  }

  @Override
  public boolean isUnknown() {
    if (kind == Kind.UNKNOWN) {
      return true;
    }
    if (kind == Kind.NAMED) {
      Symbol.TypeSymbol owner = declaredSymbol();
      return owner.isUnknown();
    }
    return false;
  }

  @Override
  public boolean isNumerical() {
    return NUMERIC.contains(fullyQualifiedName());
  }

  @Override
  public String fullyQualifiedName() {
    return fqn;
  }

  @Override
  public String name() {
    if (kind == Kind.NULL) {
      return "<nulltype>";
    }
    if (kind == Kind.ARRAY) {
      return element.name() + "[]";
    }
    int boundary = Math.max(fqn.lastIndexOf('.'), fqn.lastIndexOf('$'));
    return boundary < 0 ? fqn : fqn.substring(boundary + 1);
  }

  @Override
  public String toString() {
    return name();
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    Symbol.TypeSymbol declared = declaredSymbol();
    return !args.isEmpty() && declared instanceof JTypeSymbol generic ? generic.parameterizedAs(this) : declared;
  }

  private Symbol.TypeSymbol declaredSymbol() {
    return switch (kind) {
      case ARRAY -> sema.arraySymbol(element);
      case PRIMITIVE, VOID -> sema.primitiveSymbol(fqn);
      case TYPEVAR -> sema.resolveTypeSymbol(typeVarId);
      case NULL, UNKNOWN, INTERSECTION -> Symbol.TypeSymbol.UNKNOWN_TYPE;
      case NAMED -> sema.typeSymbol(fqn);
    };
  }

  @Override
  public Type erasure() {
    if (kind == Kind.ARRAY) {
      return JType.array(sema, (JType) element.erasure());
    }
    if (kind == Kind.NAMED && !args.isEmpty()) {
      return sema.getClassType(fqn);
    }
    return this;
  }

  @Override
  public boolean isParameterized() {
    return kind == Kind.NAMED && !args.isEmpty();
  }

  @Override
  public List<Type> typeArguments() {
    return Collections.unmodifiableList(args);
  }

  @Override
  public Type elementType() {
    return element == null ? Type.UNKNOWN : element;
  }

  @Override
  public boolean isIntersectionType() {
    return kind == Kind.INTERSECTION;
  }

  @Override
  public Type[] getIntersectionTypes() {
    if (intersection.isEmpty()) {
      return new Type[] {this};
    }
    return intersection.toArray(Type[]::new);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JType other)) {
      return false;
    }
    return kind == other.kind && fqn.equals(other.fqn) && args.equals(other.args) && Objects.equals(typeVarId, other.typeVarId);
  }

  @Override
  public int hashCode() {
    return fqn.hashCode();
  }
}
