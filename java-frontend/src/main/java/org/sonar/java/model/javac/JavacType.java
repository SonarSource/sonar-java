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
package org.sonar.java.model.javac;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

/** Adapts a javac type from the running JDK to the SonarJava semantic API. */
public final class JavacType implements Type.ArrayType {

  private final TypeMirror mirror;
  private final Types types;
  private final Elements elements;
  @Nullable
  private final JavacSymbols symbols;

  public JavacType(TypeMirror mirror, Types types, Elements elements) {
    this(mirror, types, elements, null);
  }

  JavacType(TypeMirror mirror, Types types, Elements elements, @Nullable JavacSymbols symbols) {
    this.mirror = Objects.requireNonNull(mirror);
    this.types = Objects.requireNonNull(types);
    this.elements = Objects.requireNonNull(elements);
    this.symbols = symbols;
  }

  public TypeMirror mirror() {
    return mirror;
  }

  public Set<Type> directSupertypes() {
    if (isUnknown()) {
      return Set.of();
    }
    Set<Type> result = new LinkedHashSet<>();
    try {
      types.directSupertypes(mirror).stream().map(this::wrap).forEach(result::add);
    } catch (IllegalArgumentException e) {
      return Set.of();
    }
    if (mirror instanceof DeclaredType declared && declared.asElement() instanceof TypeElement element) {
      TypeMirror superclass = element.getSuperclass();
      if (superclass.getKind() == TypeKind.ERROR) {
        result.add(wrap(superclass));
      }
      element.getInterfaces().stream()
        .filter(type -> type.getKind() == TypeKind.ERROR)
        .map(this::wrap).forEach(result::add);
    }
    return result;
  }

  @Override
  public boolean is(String fullyQualifiedName) {
    return mirror.getKind() != TypeKind.ERROR && fullyQualifiedName().equals(fullyQualifiedName);
  }

  @Override
  public boolean isSubtypeOf(String fullyQualifiedName) {
    if (isUnknown()) {
      return false;
    }
    if (is(fullyQualifiedName)) {
      return true;
    }
    TypeElement superElement = elements.getTypeElement(fullyQualifiedName.replace('$', '.'));
    if (superElement != null) {
      return isSubtype(mirror, superElement.asType());
    }
    if (fullyQualifiedName.endsWith("[]") && isArray()) {
      String elementName = fullyQualifiedName.substring(0, fullyQualifiedName.length() - 2);
      return elementType().isSubtypeOf(elementName);
    }
    return false;
  }

  @Override
  public boolean isSubtypeOf(Type superType) {
    if (isUnknown() || superType.isUnknown()) {
      return false;
    }
    if (superType instanceof JavacType javacType) {
      return isSubtype(mirror, javacType.mirror);
    }
    return isSubtypeOf(superType.fullyQualifiedName());
  }

  private boolean isSubtype(TypeMirror subtype, TypeMirror supertype) {
    try {
      return types.isSameType(subtype, supertype) || types.isSubtype(subtype, supertype)
        || types.isSubtype(types.erasure(subtype), types.erasure(supertype));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  public boolean isArray() {
    return mirror.getKind() == TypeKind.ARRAY;
  }

  @Override
  public boolean isClass() {
    return mirror.getKind() == TypeKind.DECLARED;
  }

  @Override
  public boolean isVoid() {
    return mirror.getKind() == TypeKind.VOID;
  }

  @Override
  public boolean isPrimitive() {
    return mirror.getKind().isPrimitive();
  }

  @Override
  public boolean isPrimitive(Primitives primitive) {
    return is(primitive.name().toLowerCase(Locale.ROOT));
  }

  @Override
  public boolean isPrimitiveWrapper() {
    try {
      types.unboxedType(mirror);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Nullable
  @Override
  public Type primitiveWrapperType() {
    if (!isPrimitive()) {
      return null;
    }
    return wrap(types.boxedClass((javax.lang.model.type.PrimitiveType) mirror).asType());
  }

  @Nullable
  @Override
  public Type primitiveType() {
    if (!isPrimitiveWrapper()) {
      return null;
    }
    return wrap(types.unboxedType(mirror));
  }

  @Override
  public boolean isNullType() {
    return mirror.getKind() == TypeKind.NULL;
  }

  @Override
  public boolean isTypeVar() {
    return mirror.getKind() == TypeKind.TYPEVAR;
  }

  @Override
  public boolean isRawType() {
    if (!(mirror instanceof DeclaredType declared)) {
      return false;
    }
    Element element = declared.asElement();
    return element instanceof TypeElement typeElement
      && !typeElement.getTypeParameters().isEmpty()
      && declared.getTypeArguments().isEmpty();
  }

  @Override
  public Type declaringType() {
    if (mirror instanceof DeclaredType declared && declared.asElement() instanceof TypeElement element) {
      return wrap(element.asType());
    }
    return this;
  }

  @Override
  public boolean isUnknown() {
    if (mirror.getKind() == TypeKind.ERROR || mirror.getKind() == TypeKind.NONE || mirror.getKind() == TypeKind.OTHER) {
      return true;
    }
    if (mirror instanceof DeclaredType declared) {
      return declared.getTypeArguments().stream().anyMatch(argument -> wrap(argument).isUnknown());
    }
    if (mirror instanceof javax.lang.model.type.ArrayType array) {
      return wrap(array.getComponentType()).isUnknown();
    }
    return false;
  }

  @Override
  public boolean isNumerical() {
    return isPrimitive() && mirror.getKind() != TypeKind.BOOLEAN;
  }

  @Override
  public String fullyQualifiedName() {
    if (isArray()) {
      return elementType().fullyQualifiedName() + "[]";
    }
    if (isNullType()) {
      return "<nulltype>";
    }
    if (mirror instanceof DeclaredType declared && declared.asElement() instanceof TypeElement element) {
      return elements.getBinaryName(element).toString();
    }
    return mirror.toString();
  }

  @Override
  public String name() {
    if (isArray()) {
      return elementType().name() + "[]";
    }
    if (mirror instanceof DeclaredType declared && declared.asElement() instanceof TypeElement element) {
      return element.getSimpleName().toString();
    }
    return fullyQualifiedName();
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    if (isUnknown()) {
      return Symbol.TypeSymbol.UNKNOWN_TYPE;
    }
    if (symbols != null && mirror instanceof DeclaredType declared && declared.asElement() instanceof TypeElement typeElement) {
      if (!declared.getTypeArguments().isEmpty() || !typeElement.getTypeParameters().isEmpty()) {
        return symbols.instantiatedType(declared);
      }
      return (Symbol.TypeSymbol) symbols.symbol(typeElement);
    }
    if (symbols != null && mirror.getKind() == TypeKind.TYPEVAR) {
      return (Symbol.TypeSymbol) symbols.symbol(types.asElement(mirror));
    }
    if (symbols != null && !isUnknown()) {
      return symbols.syntheticType(mirror);
    }
    return Symbol.TypeSymbol.UNKNOWN_TYPE;
  }

  @Override
  public Type erasure() {
    return wrap(types.erasure(mirror));
  }

  @Override
  public boolean isParameterized() {
    return mirror instanceof DeclaredType declared && !declared.getTypeArguments().isEmpty();
  }

  @Override
  public List<Type> typeArguments() {
    if (!(mirror instanceof DeclaredType declared)) {
      return List.of();
    }
    return declared.getTypeArguments().stream().<Type>map(this::wrap).toList();
  }

  @Override
  public Type elementType() {
    return isArray() ? wrap(((javax.lang.model.type.ArrayType) mirror).getComponentType()) : Type.UNKNOWN;
  }

  @Override
  public boolean isIntersectionType() {
    return mirror.getKind() == TypeKind.INTERSECTION;
  }

  @Override
  public Type[] getIntersectionTypes() {
    if (!(mirror instanceof IntersectionType intersection)) {
      return new Type[] {this};
    }
    return intersection.getBounds().stream().map(this::wrap).toArray(Type[]::new);
  }

  private JavacType wrap(TypeMirror value) {
    return symbols == null ? new JavacType(value, types, elements) : (JavacType) symbols.type(value);
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof JavacType other)) {
      return false;
    }
    if (types != other.types) {
      return fullyQualifiedName().equals(other.fullyQualifiedName()) && mirror.toString().equals(other.mirror.toString());
    }
    try {
      return types.isSameType(mirror, other.mirror);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return Objects.hash(fullyQualifiedName(), mirror.toString());
  }

  @Override
  public String toString() {
    return name();
  }
}
