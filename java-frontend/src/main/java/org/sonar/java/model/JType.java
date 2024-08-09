/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

final class JType implements Type, Type.ArrayType {

//  private static final Logger LOG = LoggerFactory.getLogger(JType.class);

  final JSema sema;
  final ITypeBinding typeBinding;

  private final String fullyQualifiedName;

  /**
   * Cache for {@link #primitiveWrapperType()}.
   */
  private Type primitiveWrapperType;

  /**
   * Cache for {@link #primitiveType()}.
   */
  private Type primitiveType;

  /**
   * Cache for {@link #declaringType()}.
   */
  private Type declaringType;

  /**
   * Cache for {@link #typeArguments()}.
   */
  private List<Type> typeArguments;

  JType(JSema sema, ITypeBinding typeBinding) {
    this.sema = Objects.requireNonNull(sema);
    this.typeBinding = Objects.requireNonNull(typeBinding);
    this.fullyQualifiedName = fullyQualifiedName(typeBinding);
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
    return !superType.isUnknown()
      && isSubtype(this.typeBinding, ((JType) superType).typeBinding);
  }

  private static boolean isSubtype(ITypeBinding left, ITypeBinding right) {
    if (left.isRecovered()) {
      return false;
    }
    if (left.isNullType()) {
      return !right.isPrimitive();
    }
    try {
      return left.isSubTypeCompatible(right);
    } catch (NullPointerException ex) {
      // In rare circumstances, ECJ may produce a NPE while calling isSubTypeCompatible(), see SONARJAVA-4390
//      LOG.debug("NullPointerException while resolving isSubTypeCompatible()", ex);
      return false;
    }
  }

  @Override
  public boolean isArray() {
    return typeBinding.isArray();
  }

  @Override
  public boolean isClass() {
    return typeBinding.isClass()
      || typeBinding.isInterface()
      || typeBinding.isEnum();
  }

  @Override
  public boolean isVoid() {
    return "void".equals(fullyQualifiedName());
  }

  @Override
  public boolean isPrimitive() {
    return typeBinding.isPrimitive()
      && !isVoid();
  }

  @Override
  public boolean isPrimitive(Primitives primitive) {
    // TODO suboptimal
    return primitive.name().toLowerCase(Locale.ROOT).equals(fullyQualifiedName());
  }

  @Override
  public boolean isPrimitiveWrapper() {
    return isClass() && JUtils.WRAPPER_TO_PRIMITIVE.containsKey(fullyQualifiedName());
  }

  @Nullable
  @Override
  public Type primitiveWrapperType() {
    if (primitiveWrapperType == null) {
      String name = JUtils.PRIMITIVE_TO_WRAPPER.get(fullyQualifiedName());
      if (name == null) {
        return null;
      }
      primitiveWrapperType = sema.type(sema.resolveType(name));
    }
    return primitiveWrapperType;
  }

  @Nullable
  @Override
  public Type primitiveType() {
    if (primitiveType == null) {
      String name = JUtils.WRAPPER_TO_PRIMITIVE.get(fullyQualifiedName());
      if (name == null) {
        return null;
      }
      primitiveType = sema.type(sema.resolveType(name));
    }
    return primitiveType;
  }

  @Override
  public boolean isNullType() {
    return !isUnknown() && typeBinding.isNullType();
  }

  @Override
  public boolean isTypeVar() {
    return !isUnknown() && typeBinding.isTypeVariable();
  }

  @Override
  public boolean isRawType() {
    if (isUnknown()) {
      return false;
    }
    return typeBinding.isRawType();
  }

  @Override
  public Type declaringType() {
    if (declaringType == null) {
      if (isUnknown()) {
        return this;
      }
      declaringType = sema.type(typeBinding.getTypeDeclaration());
    }
    return declaringType;
  }

  @Override
  public boolean isUnknown() {
    return typeBinding.isRecovered();
  }

  @Override
  public boolean isNumerical() {
    switch (fullyQualifiedName()) {
      case "byte":
      case "char":
      case "short":
      case "int":
      case "long":
      case "float":
      case "double":
        return true;
      default:
        return false;
    }
  }

  @Override
  public String fullyQualifiedName() {
    return fullyQualifiedName;
  }

  private static String fullyQualifiedName(ITypeBinding typeBinding) {
    if (typeBinding.isNullType()) {
      return "<nulltype>";
    } else if (typeBinding.isPrimitive()) {
      return typeBinding.getName();
    } else if (typeBinding.isArray()) {
      return fullyQualifiedName(typeBinding.getComponentType()) + "[]";
    } else if (typeBinding.isCapture()) {
      return "!capture!";
    } else if (typeBinding.isTypeVariable()) {
      return typeBinding.getName();
    } else {
      String binaryName = typeBinding.getBinaryName();
      if (binaryName == null) {
        // e.g. anonymous class in unreachable code
        return typeBinding.getKey();
      }
      return binaryName;
    }
  }

  /**
   * @see JSymbol#name()
   */
  @Override
  public String name() {
    if (typeBinding.isNullType()) {
      return "<nulltype>";
    } else if (typeBinding.isParameterizedType()) {
      // without names of parameters
      return typeBinding.getErasure().getName();
    }
    return typeBinding.getName();
  }

  @Override
  public String toString() {
    return name();
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    return sema.typeSymbol(typeBinding);
  }

  @Override
  public Type erasure() {
    return sema.type(typeBinding.getErasure());
  }

  @Override
  public Type elementType() {
    return sema.type(typeBinding.getComponentType());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof JType other) {
      return areEqual(this.typeBinding, other.typeBinding);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return fullyQualifiedName().hashCode();
  }

  static boolean areEqual(@Nullable ITypeBinding binding1, @Nullable ITypeBinding binding2) {
    if (binding1 == null || binding2 == null) {
      return binding1 == binding2;
    }
    if (binding1.isWildcardType()) {
      return binding2.isWildcardType()
        && binding1.isUpperbound() == binding2.isUpperbound()
        && areEqual(binding1.getBound(), binding2.getBound());
    }
    // TODO compare declaring class, method, member
    return binding1.getTypeDeclaration().equals(binding2.getTypeDeclaration())
      && isParameterizedOrGeneric(binding1) == isParameterizedOrGeneric(binding2)
      && binding1.isRawType() == binding2.isRawType()
      && Arrays.equals(binding1.getTypeParameters(), binding2.getTypeParameters())
      && Arrays.equals(binding1.getTypeArguments(), binding2.getTypeArguments());
  }

  static ITypeBinding normalize(ITypeBinding typeBinding) {
    ITypeBinding typeDeclaration = typeBinding.getTypeDeclaration();
    if (typeBinding.isLocal()) {
      return typeDeclaration;
    }
    return typeBinding.isParameterizedType() && Arrays.equals(typeDeclaration.getTypeParameters(), typeBinding.getTypeArguments())
      ? typeDeclaration
      : typeBinding;
  }

  private static boolean isParameterizedOrGeneric(ITypeBinding typeBinding) {
    return typeBinding.isParameterizedType() || typeBinding.isGenericType();
  }

  @Override
  public boolean isParameterized() {
    return typeBinding.isParameterizedType()
      // when diamond operator is not fully resolved by ECJ, there is 0 typeArguments, while ECJ
      // knows it is a Parameterized Type
      && !typeArguments().isEmpty();
  }

  @Override
  public List<Type> typeArguments() {
    if (typeArguments == null) {
      typeArguments = sema.types(typeBinding.getTypeArguments());
    }
    return typeArguments;
  }

}
