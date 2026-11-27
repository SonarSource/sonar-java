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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

/**
 * A type recovered from a SemSitter symbol fact or a JVM descriptor.
 * Fully-qualified names are erased binary names ({@code java.util.Map$Entry}).
 */
final class JType implements Type.ArrayType {

  enum Kind {
    NAMED, PRIMITIVE, VOID, NULL, ARRAY, TYPEVAR, UNKNOWN, INTERSECTION, CAPTURE, WILDCARD
  }

  /** Marks a parameterization the compiler builds itself; compared by identity. */
  private static final List<JType> INFERRED = Collections.unmodifiableList(new ArrayList<>());

  private static final Set<String> NUMERIC = Set.of("byte", "char", "short", "int", "long", "float", "double");

  final JSema sema;
  final Kind kind;
  final String fqn;
  final List<JType> args;
  @Nullable
  final JType element;
  @Nullable
  final String typeVarId;
  final List<JType> intersectionBounds;
  @Nullable
  final JType lower;
  /** The parameterized enclosing type of an inner class of a generic class, which gives the inner class's members the outer type arguments. */
  @Nullable
  private JType outer;

  private record Parts(@Nullable JType element, @Nullable String typeVarId, List<JType> intersectionBounds, @Nullable JType lower) {
  }

  private JType(JSema sema, Kind kind, String fqn, List<JType> args, @Nullable JType element, @Nullable String typeVarId, List<JType> intersection) {
    this(sema, kind, fqn, args, new Parts(element, typeVarId, intersection, null));
  }

  private JType(JSema sema, Kind kind, String fqn, List<JType> args, Parts parts) {
    this.sema = sema;
    this.kind = kind;
    this.fqn = fqn;
    this.args = args;
    this.element = parts.element();
    this.typeVarId = parts.typeVarId();
    this.intersectionBounds = parts.intersectionBounds();
    this.lower = parts.lower();
  }

  @Nullable
  JType outer() {
    return outer;
  }

  /** A copy of this class type that is an inner class of the parameterized {@code enclosing} type. */
  JType withOuter(JType enclosing) {
    JType copy = new JType(sema, kind, fqn, args, new Parts(element, typeVarId, intersectionBounds, lower));
    copy.outer = enclosing;
    return copy;
  }

  static JType named(JSema sema, String fqn, List<JType> args) {
    return new JType(sema, Kind.NAMED, fqn, List.copyOf(args), null, null, List.of());
  }

  /** A parameterization the compiler builds itself, like the type of a class literal: its arguments are not checked. */
  static JType inferred(JSema sema, String fqn, List<JType> args) {
    return new JType(sema, Kind.NAMED, fqn, List.copyOf(args), null, null, INFERRED);
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
    if (kind == Kind.TYPEVAR) {
      JType replacement = typeVarReplacement(substitution);
      if (replacement != null) {
        return replacement;
      }
    }
    if (kind == Kind.CAPTURE) {
      JType replaced = element.substitute(substitution);
      return replaced == element ? this : capture(sema, replaced);
    }
    if (kind == Kind.WILDCARD) {
      return substituteWildcard(substitution);
    }
    if (kind == Kind.ARRAY) {
      JType replaced = element.substitute(substitution);
      return replaced == element ? this : array(sema, replaced);
    }
    if (kind == Kind.NAMED && !args.isEmpty()) {
      return substituteArguments(substitution);
    }
    return this;
  }

  @Nullable
  private JType typeVarReplacement(Map<String, Type> substitution) {
    Type found = typeVarId == null ? null : substitution.get(typeVarId);
    if (found == null) {
      found = substitution.get(fqn);
    }
    return found instanceof JType replaced ? replaced : null;
  }

  private JType substituteWildcard(Map<String, Type> substitution) {
    if (lower != null) {
      JType replaced = lower.substitute(substitution);
      return replaced == lower ? this : superWildcard(sema, replaced);
    }
    JType replaced = element.substitute(substitution);
    return replaced == element ? this : wildcard(sema, replaced);
  }

  private JType substituteArguments(Map<String, Type> substitution) {
    List<JType> replaced = new ArrayList<>();
    boolean changed = false;
    for (JType argument : args) {
      JType next = argument.substitute(substitution);
      changed |= next != argument;
      replaced.add(next);
    }
    return changed ? named(sema, fqn, replaced) : this;
  }

  static JType capture(JSema sema, JType upper) {
    return new JType(sema, Kind.CAPTURE, "!capture!", List.of(), upper, null, List.of());
  }

  /** A wildcard type argument, named after the erasure of its upper bound as ECJ names it. */
  static JType wildcard(JSema sema, JType upper) {
    if (upper.isUnknown()) {
      return upper;
    }
    return new JType(sema, Kind.WILDCARD, upper.erasure().fullyQualifiedName(), List.of(), upper, null, List.of());
  }

  /** A {@code ? super lower} type argument, which ECJ names after its implicit upper bound {@code Object}. */
  static JType superWildcard(JSema sema, JType lower) {
    JType object = (JType) sema.getClassType("java.lang.Object");
    return new JType(sema, Kind.WILDCARD, object.fullyQualifiedName(), List.of(), new Parts(object, null, List.of(), lower));
  }

  boolean isWildcard() {
    return kind == Kind.WILDCARD;
  }

  /** The type a lambda sees for this type argument: the bound a wildcard is written with, or the type itself. */
  JType groundType() {
    if (kind != Kind.WILDCARD) {
      return this;
    }
    return lower != null ? lower : element;
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
    return Subtyping.isSubtype(this, superType, new HashSet<>());
  }

  @Override
  public boolean isArray() {
    return kind == Kind.ARRAY;
  }

  boolean isCapture() {
    return kind == Kind.CAPTURE;
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
    return kind == Kind.NAMED && !isUnknown() && args.isEmpty() && sema.typeSymbol(fqn).typeParameterCount() > 0;
  }

  @Override
  public Type declaringType() {
    if (isUnknown() || kind != Kind.NAMED) {
      return this;
    }
    if (isRawType()) {
      return sema.typeSymbol(fqn).declaringType(this);
    }
    return sema.getClassType(fqn);
  }

  @Override
  public boolean isUnknown() {
    if (kind == Kind.UNKNOWN) {
      return true;
    }
    if (kind == Kind.ARRAY) {
      return element.isUnknown();
    }
    if (kind == Kind.NAMED) {
      Symbol.TypeSymbol owner = declaredSymbol();
      // the compiler cannot bind a parameterization with an argument nothing declares
      return owner.isUnknown() || (intersectionBounds != INFERRED && args.stream().anyMatch(JType::isUnknown));
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
    if (kind == Kind.WILDCARD) {
      if (lower != null) {
        return "? super " + lower.name();
      }
      return element.is("java.lang.Object") ? "?" : ("? extends " + element.name());
    }
    int boundary = Math.max(fqn.lastIndexOf('.'), fqn.lastIndexOf('$'));
    String simple = boundary < 0 ? fqn : fqn.substring(boundary + 1);
    return SemaFacts.isAnonymousName(simple) ? "" : simple;
  }

  @Override
  public String toString() {
    return name();
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    return args.isEmpty() ? declaredSymbol() : sema.typeSymbol(fqn).parameterizedAs(this);
  }

  Symbol.TypeSymbol declaredSymbol() {
    return switch (kind) {
      case ARRAY -> sema.memberFactory().arraySymbol(element);
      case PRIMITIVE, VOID -> sema.memberFactory().primitiveSymbol(fqn);
      case TYPEVAR -> sema.resolveTypeSymbol(typeVarId);
      case NULL, UNKNOWN, INTERSECTION -> Symbol.TypeSymbol.UNKNOWN_TYPE;
      case CAPTURE, WILDCARD -> element.symbol();
      case NAMED -> sema.typeSymbol(fqn);
    };
  }

  @Override
  public Type erasure() {
    if (kind == Kind.CAPTURE || kind == Kind.WILDCARD) {
      return element.erasure();
    }
    if (kind == Kind.ARRAY) {
      return JType.array(sema, (JType) element.erasure());
    }
    if (kind == Kind.NAMED && !args.isEmpty()) {
      return sema.getClassType(fqn);
    }
    if (kind == Kind.TYPEVAR && typeVarId != null) {
      Type bound = sema.memberFactory().typeVariableBound(typeVarId);
      return bound == this ? this : bound.erasure();
    }
    return this;
  }

  @Override
  public boolean isParameterized() {
    return kind == Kind.NAMED && !args.isEmpty() && !isGenericSelf();
  }

  /** The type written with its own type parameters is the generic declaration itself, not a parameterization. */
  private boolean isGenericSelf() {
    String owner = fqn + "`" + args.size() + "<";
    return args.stream().allMatch(argument -> argument.kind == Kind.TYPEVAR && argument.typeVarId != null && argument.typeVarId.startsWith(owner));
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
    if (intersectionBounds.isEmpty()) {
      return new Type[] {this};
    }
    return intersectionBounds.toArray(Type[]::new);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JType other) || kind == Kind.CAPTURE) {
      return false;
    }
    if (kind == Kind.WILDCARD) {
      return other.kind == Kind.WILDCARD && Objects.equals(element, other.element) && Objects.equals(lower, other.lower);
    }
    return kind == other.kind && fqn.equals(other.fqn) && args.equals(other.args) && (kind != Kind.ARRAY || Objects.equals(element, other.element)) && sameTypeVariable(other);
  }

  /** A type variable a signature names by its simple name is the one of that name the declaration spells with its full id. */
  private boolean sameTypeVariable(JType other) {
    if (Objects.equals(typeVarId, other.typeVarId)) {
      return true;
    }
    return typeVarId != null && other.typeVarId != null && (typeVarId.indexOf('<') < 0 || other.typeVarId.indexOf('<') < 0);
  }

  @Override
  public int hashCode() {
    return fqn.hashCode();
  }
}
