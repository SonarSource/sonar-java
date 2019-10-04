/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import java.util.Objects;

final class JType implements Type, Type.ArrayType {

  final JSema sema;
  final ITypeBinding typeBinding;

  JType(JSema sema, ITypeBinding typeBinding) {
    this.sema = Objects.requireNonNull(sema);
    this.typeBinding = Objects.requireNonNull(typeBinding);
  }

  @Override
  public boolean is(String fullyQualifiedName) {
    return fullyQualifiedName.equals(this.fullyQualifiedName());
  }

  @Override
  public boolean isSubtypeOf(String fullyQualifiedName) {
    ITypeBinding otherTypeBinding = sema.resolveType(fullyQualifiedName);
    return otherTypeBinding != null
      && isSubtype(this.typeBinding, otherTypeBinding);
  }

  @Override
  public boolean isSubtypeOf(Type superType) {
    return !superType.isUnknown()
      && isSubtype(this.typeBinding, ((JType) superType).typeBinding);
  }

  private static boolean isSubtype(ITypeBinding left, ITypeBinding right) {
    if (left.isNullType()) {
      return !right.isPrimitive();
    }
    return left.isSubTypeCompatible(right);
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
    return "void".equals(typeBinding.getName());
  }

  @Override
  public boolean isPrimitive() {
    return typeBinding.isPrimitive()
      && !isVoid();
  }

  @Override
  public boolean isPrimitive(Primitives primitive) {
    // TODO suboptimal
    return primitive.name().toLowerCase().equals(typeBinding.getName());
  }

  @Override
  public boolean isUnknown() {
    return typeBinding.isRecovered();
  }

  @Override
  public boolean isNumerical() {
    switch (typeBinding.getName()) {
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
    return fullyQualifiedName(typeBinding);
  }

  private static String fullyQualifiedName(ITypeBinding typeBinding) {
    if (typeBinding.isNullType()) {
      return "<nulltype>";
    } else if (typeBinding.isPrimitive()) {
      return typeBinding.getName();
    } else if (typeBinding.isArray()) {
      return fullyQualifiedName(typeBinding.getComponentType()) + "[]";
    } else {
      return typeBinding.getBinaryName();
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

}
