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

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

final class JType implements Type, Type.ArrayType {

  private static final String UNKNOWN_NAME = "!Unknown!";

  @Override
  public boolean is(String fullyQualifiedName) {
    return false;
  }

  @Override
  public boolean isSubtypeOf(String fullyQualifiedName) {
    return false;
  }

  @Override
  public boolean isSubtypeOf(Type superType) {
    return false;
  }

  @Override
  public boolean isArray() {
    return false;
  }

  @Override
  public boolean isClass() {
    return false;
  }

  @Override
  public boolean isVoid() {
    return false;
  }

  @Override
  public boolean isPrimitive() {
    return false;
  }

  @Override
  public boolean isPrimitive(Primitives primitive) {
    return false;
  }

  @Override
  public boolean isPrimitiveWrapper() {
    return false;
  }

  @Nullable
  @Override
  public Type primitiveWrapperType() {
    return null;
  }

  @Nullable
  @Override
  public Type primitiveType() {
    return null;
  }

  @Override
  public boolean isNullType() {
    return false;
  }

  @Override
  public boolean isTypeVar() {
    return false;
  }

  @Override
  public boolean isRawType() {
    return false;
  }

  @Override
  public Type declaringType() {
    return this;
  }

  @Override
  public boolean isUnknown() {
    return true;
  }

  @Override
  public boolean isNumerical() {
    return false;
  }

  @Override
  public String fullyQualifiedName() {
    return UNKNOWN_NAME;
  }

  @Override
  public String name() {
    return UNKNOWN_NAME;
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    return Symbol.TypeSymbol.UNKNOWN_TYPE;
  }

  @Override
  public Type erasure() {
    return this;
  }

  @Override
  public boolean isParameterized() {
    return false;
  }

  @Override
  public List<Type> typeArguments() {
    return Collections.emptyList();
  }

  @Override
  public Type elementType() {
    return Type.UNKNOWN;
  }

  @Override
  public boolean isIntersectionType() {
    return false;
  }

  @Override
  public Type[] getIntersectionTypes() {
    return new Type[0];
  }

}
