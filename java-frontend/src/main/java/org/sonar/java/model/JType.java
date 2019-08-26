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
import org.sonar.java.resolve.ArrayJavaType;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.MethodJavaType;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;

import java.util.Objects;

@MethodsAreNonnullByDefault
final class JType implements Type, Type.ArrayType {

  private final Sema ast;
  private final ITypeBinding typeBinding;

  JType(Sema ast, ITypeBinding typeBinding) {
    this.ast = Objects.requireNonNull(ast);
    this.typeBinding = Objects.requireNonNull(typeBinding);
  }

  /**
   * TODO check {@link JavaType#toString()}, {@link ArrayJavaType#toString()}, {@link MethodJavaType#toString()}
   */
  @Override
  public String toString() {
    return symbol().toString();
  }

  @Override
  public boolean is(String fullyQualifiedName) {
    if (typeBinding.isNullType()) {
      // as in out implementation
      return true;
    }
    return fullyQualifiedName.equals(fullyQualifiedName());
  }

  @Override
  public boolean isSubtypeOf(String fullyQualifiedName) {
    if (isArray() && "java.io.Serializable".equals(fullyQualifiedName)) {
      // as in our implementation
      return false;
    }
    return typeBinding.isSubTypeCompatible(ast.resolveType(fullyQualifiedName));
  }

  @Override
  public boolean isSubtypeOf(Type superType) {
    if (superType == Symbols.unknownType) {
      return false;
    }

    return typeBinding.isSubTypeCompatible(
      ((JType) superType).typeBinding
    );
  }

  @Override
  public boolean isArray() {
    return typeBinding.isArray();
  }

  @Override
  public boolean isClass() {
    return typeBinding.isClass()
      // in our implementation also
      || typeBinding.isInterface()
      || typeBinding.isEnum()
      || typeBinding.isAnnotation(); // TODO isAnnotation probably redundant with isInterface
  }

  @Override
  public boolean isVoid() {
    return "void".equals(typeBinding.getName());
  }

  @Override
  public boolean isPrimitive() {
    return typeBinding.isPrimitive()
      // in our implementation also
      && !isVoid();
  }

  @Override
  public boolean isPrimitive(Primitives primitive) {
    return isPrimitive() && primitive.name().toLowerCase().equals(typeBinding.getName());
  }

  @Override
  public boolean isUnknown() {
    return typeBinding.isRecovered();
  }

  @Override
  public boolean isNumerical() {
    // TODO suboptimal
    return isPrimitive(Primitives.BYTE)
      || isPrimitive(Primitives.CHAR)
      || isPrimitive(Primitives.SHORT)
      || isPrimitive(Primitives.INT)
      || isPrimitive(Primitives.LONG)
      || isPrimitive(Primitives.FLOAT)
      || isPrimitive(Primitives.DOUBLE);
  }

  @Override
  public String fullyQualifiedName() {
    if (typeBinding.isNullType()) {
      return "<nulltype>";
    }
    if (typeBinding.isTypeVariable()) {
      return typeBinding.getQualifiedName();
    }
    if (typeBinding.isMember()) {
      // TODO helped for ThrowsSeveralCheckedExceptionCheck and others, add test
      return typeBinding.getBinaryName();
//      return typeBinding.getDeclaringClass().getErasure().getQualifiedName() + "$" + typeBinding.getErasure().getName();
    }
    return typeBinding.getErasure().getQualifiedName();
  }

  @Override
  public String name() {
    if (typeBinding.isNullType()) {
      return "<nulltype>";
    }
    if (typeBinding.isTypeVariable()) {
      return typeBinding.getName();
    }
    return typeBinding.getErasure().getName();
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    return ast.typeSymbol(typeBinding);
  }

  @Override
  public Type erasure() {
    return ast.type(typeBinding.getErasure());
  }

  @Override
  public Type elementType() {
    if (!isArray()) {
      // in our implementation only ArrayJavaType implements this method
      throw new IllegalStateException();
    }
    return ast.type(typeBinding.getComponentType());
  }
}
