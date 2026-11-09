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
package org.sonar.java.psi;

import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiCapturedWildcardType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiIntersectionType;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.PsiWildcardType;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.TypeConversionUtil;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

final class PsiType implements Type, Type.ArrayType {
  final PsiSema sema;
  final com.intellij.psi.PsiType psi;
  final String key;
  private final String recoveredName;

  PsiType(PsiSema sema, com.intellij.psi.PsiType psi, String key) {
    this(sema, psi, key, null);
  }

  PsiType(PsiSema sema, com.intellij.psi.PsiType psi, String key, String recoveredName) {
    this.sema = sema;
    this.psi = psi;
    this.key = key;
    this.recoveredName = recoveredName;
  }

  PsiClass resolvedClass() {
    return psi instanceof PsiClassType cls ? cls.resolve() : null;
  }

  public Set<Type> directSuperTypes() {
    return sema.read(() -> {
      Set<Type> result = new HashSet<>();
      if (!isUnknown()) {
        for (var parent : psi.getSuperTypes()) {
          if (!(resolvedClass() != null && resolvedClass().isInterface() && parent.equalsToText("java.lang.Object"))) {
            result.add(sema.type(parent));
          }
        }
      }
      return result;
    });
  }

  @Override
  public boolean is(String name) {
    return fullyQualifiedName().equals(name);
  }

  @Override
  public boolean isSubtypeOf(String name) {
    return isSubtypeOf(sema.getClassType(name).erasure());
  }

  @Override
  public boolean isSubtypeOf(Type other) {
    return sema.read(() -> {
      if (!(other instanceof PsiType target) || isUnknown() || target.isUnknown()) {
        return false;
      }
      if (isNullType()) {
        return !target.isPrimitive() && !target.isVoid();
      }
      if (isPrimitive() || target.isPrimitive() || isVoid() || target.isVoid()) {
        return equals(target);
      }
      try {
        var expected = !target.isParameterized() && target.psi instanceof PsiClassType
          ? TypeConversionUtil.erasure(target.psi) : target.psi;
        return TypeConversionUtil.isAssignable(expected, psi, false);
      } catch (NullPointerException e) {
        org.slf4j.LoggerFactory.getLogger(PsiType.class).debug("NullPointerException while resolving isSubTypeCompatible()");
        return false;
      }
    });
  }

  @Override
  public boolean isArray() {
    return psi instanceof PsiArrayType;
  }

  @Override
  public boolean isVoid() {
    return PsiTypes.voidType().equals(psi);
  }

  @Override
  public boolean isPrimitive() {
    return psi instanceof PsiPrimitiveType && !isVoid() && !isNullType();
  }

  @Override
  public boolean isPrimitive(Primitives primitive) {
    return is(primitive.name().toLowerCase(Locale.ROOT));
  }

  @Override
  public boolean isNullType() {
    return PsiTypes.nullType().equals(psi);
  }

  @Override
  public boolean isPrimitiveWrapper() {
    return JUtils.WRAPPER_TO_PRIMITIVE.containsKey(fullyQualifiedName());
  }

  @Override
  public boolean isNumerical() {
    return Set.of("byte", "char", "short", "int", "long", "float", "double").contains(fullyQualifiedName());
  }

  @Override
  public boolean isIntersectionType() {
    return psi instanceof PsiIntersectionType;
  }

  @Override
  public boolean isClass() {
    return sema.read(() -> psi instanceof PsiClassType && !(resolvedClass() instanceof PsiTypeParameter));
  }

  @Override
  public boolean isTypeVar() {
    return sema.read(() -> resolvedClass() instanceof PsiTypeParameter);
  }

  @Override
  public boolean isRawType() {
    return sema.read(() -> psi instanceof PsiClassType cls && cls.isRaw());
  }

  @Override
  public boolean isUnknown() {
    return sema.read(() -> unresolved(psi));
  }

  private static boolean unresolved(com.intellij.psi.PsiType type) {
    if (type instanceof PsiArrayType array) {
      return unresolved(array.getComponentType());
    }
    if (type instanceof PsiWildcardType wildcard && wildcard.getBound() != null) {
      return unresolved(wildcard.getBound());
    }
    return type instanceof PsiClassType cls && (cls.resolve() == null || Arrays.stream(cls.getParameters()).anyMatch(PsiType::unresolved));
  }

  @Override
  public Type primitiveWrapperType() {
    String name = JUtils.PRIMITIVE_TO_WRAPPER.get(fullyQualifiedName());
    Type value = name == null ? null : sema.getClassType(name);
    return value == Type.UNKNOWN ? null : value;
  }

  @Override
  public Type primitiveType() {
    String name = JUtils.WRAPPER_TO_PRIMITIVE.get(fullyQualifiedName());
    Type value = name == null ? null : sema.getClassType(name);
    return value == Type.UNKNOWN ? null : value;
  }

  @Override
  public Type declaringType() {
    return sema.read(() -> {
      var cls = resolvedClass();
      return cls == null ? this : sema.type(sema.declarationType(cls));
    });
  }

  @Override
  public String fullyQualifiedName() {
    return recoveredName == null ? sema.read(() -> qualifiedName(psi)) : recoveredName;
  }

  private String qualifiedName(com.intellij.psi.PsiType type) {
    if (type instanceof PsiArrayType array) {
      return qualifiedName(array.getComponentType()) + "[]";
    }
    if (type instanceof PsiCapturedWildcardType) {
      return "!capture!";
    }
    if (type instanceof PsiWildcardType wildcard) {
      return wildcard.getCanonicalText(false);
    }
    if (type instanceof PsiIntersectionType intersection) {
      Set<String> names = new TreeSet<>();
      Arrays.stream(intersection.getConjuncts()).map(this::qualifiedName).forEach(names::add);
      return String.join(" & ", names);
    }
    if (type instanceof PsiClassType cls) {
      var resolved = cls.resolve();
      if (resolved instanceof PsiTypeParameter parameter) {
        return parameter.getName();
      }
      if (resolved != null) {
        return sema.binaryName(resolved);
      }
      String name = cls.rawType().getCanonicalText(false);
      return (cls instanceof PsiClassReferenceType reference && reference.getReference().getParent() instanceof PsiReferenceList)
        || sema.explicitRuntime || sema.getClassType("java.lang.Object").isUnknown() ? "Recovered#typeBindingL" + name.replace('.', '/') + ";0" : name;
    }
    return PsiTypes.nullType().equals(type) ? "<nulltype>" : type.getCanonicalText(false);
  }

  @Override
  public String name() {
    return sema.read(() -> {
      if (psi instanceof PsiArrayType array) {
        return sema.type(array.getComponentType()).name() + "[]";
      }
      var cls = resolvedClass();
      if (cls != null) {
        return cls.getName() == null ? "" : cls.getName();
      }
      if (psi instanceof PsiClassType classType) {
        return classType.getClassName();
      }
      return fullyQualifiedName();
    });
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    return sema.read(() -> sema.typeSymbol(this));
  }

  @Override
  public Type erasure() {
    return sema.read(() -> sema.type(erased(psi)));
  }

  private com.intellij.psi.PsiType erased(com.intellij.psi.PsiType value) {
    if (value instanceof PsiArrayType array) {
      return erased(array.getComponentType()).createArrayType();
    }
    var erased = TypeConversionUtil.erasure(value);
    return erased instanceof PsiClassType cls && cls.resolve() != null ? sema.declarationType(cls.resolve()) : erased;
  }

  @Override
  public Type elementType() {
    return sema.read(() -> psi instanceof PsiArrayType array ? sema.type(array.getComponentType()) : Type.UNKNOWN);
  }

  @Override
  public boolean isParameterized() {
    return sema.read(() -> psi instanceof PsiClassType cls && !cls.isRaw() && !typeArguments().isEmpty()
      && !typeArguments().equals(declaringType().typeArguments()));
  }

  @Override
  public List<Type> typeArguments() {
    return sema.read(() -> {
      if (!(psi instanceof PsiClassType cls) || cls.isRaw()) {
        return List.of();
      }
      var resolved = cls.resolveGenerics();
      if (resolved.getElement() == null) {
        return List.of();
      }
      var arguments = Arrays.stream(resolved.getElement().getTypeParameters()).map(p -> resolved.getSubstitutor().substitute(p)).toArray(com.intellij.psi.PsiType[]::new);
      if (IntStream.range(0, arguments.length).allMatch(i -> arguments[i] instanceof PsiClassType argument
        && argument.resolve() == resolved.getElement().getTypeParameters()[i])) {
        return List.of();
      }
      return sema.types(arguments);
    });
  }

  @Override
  public Type[] getIntersectionTypes() {
    return sema.read(() -> psi instanceof PsiIntersectionType intersection
      ? sema.types(intersection.getConjuncts()).toArray(Type[]::new) : new Type[] {this});
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof PsiType type && key.equals(type.key);
  }

  @Override
  public int hashCode() {
    return fullyQualifiedName().hashCode();
  }

  @Override
  public String toString() {
    return name();
  }
}
