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
package org.sonar.java.resolve;

import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Set;

public class JavaType implements Type {

  public static final int BYTE = 1;
  public static final int CHAR = 2;
  public static final int SHORT = 3;
  public static final int INT = 4;
  public static final int LONG = 5;
  public static final int FLOAT = 6;
  public static final int DOUBLE = 7;
  public static final int BOOLEAN = 8;
  public static final int VOID = 9;
  public static final int CLASS = 10;
  public static final int ARRAY = 11;
  public static final int METHOD = 12;
  public static final int BOT = 13;
  public static final int UNKNOWN = 14;
  public static final int TYPEVAR = 15;
  public static final int WILDCARD = 16;
  public static final int DEFERRED = 17;
  public static final int PARAMETERIZED = 18;
  public static final int INTERSECTION = 19;

  int tag;

  JavaType primitiveType = null;

  JavaType primitiveWrapperType = null;

  /**
   * Symbol, which defines this type.
   */
  JavaSymbol.TypeJavaSymbol symbol;

  public JavaType(int tag, JavaSymbol.TypeJavaSymbol symbol) {
    this.tag = tag;
    this.symbol = symbol;
  }

  public boolean isTagged(int tag) {
    return tag == this.tag;
  }

  @Override
  public boolean isNumerical() {
    // JLS8 4.2
    return tag <= DOUBLE;
  }

  public JavaSymbol.TypeJavaSymbol getSymbol() {
    symbol.complete();
    return symbol;
  }

  @CheckForNull
  public JavaType getSuperType() {
    return getSymbol().getSuperclass();
  }

  public Set<ClassJavaType> directSuperTypes() {
    return getSymbol().directSuperTypes();
  }

  @Override
  public boolean is(String fullyQualifiedName) {
    if (tag < CLASS) {
      // primitive type
      return fullyQualifiedName.equals(symbol.name);
    }
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

  /**
   * JLS8 4.6
   */
  @Override
  public JavaType erasure() {
    return this;
  }

  @Override
  public boolean isPrimitive() {
    return tag <= BOOLEAN;
  }

  @Override
  public boolean isPrimitive(Primitives primitive) {
    return tag == primitive.ordinal() + 1;
  }

  @Override
  public boolean isUnknown() {
    return false;
  }

  public boolean isPrimitiveWrapper() {
    if (!isTagged(CLASS)) {
      return false;
    }
    return is("java.lang.Byte") ||
      is("java.lang.Character") ||
      is("java.lang.Short") ||
      is("java.lang.Integer") ||
      is("java.lang.Long") ||
      is("java.lang.Float") ||
      is("java.lang.Double") ||
      is("java.lang.Boolean");
  }

  @Override
  public String toString() {
    return symbol == null ? "" : symbol.toString();
  }

  @Nullable
  public JavaType primitiveType() {
    return primitiveType;
  }

  @Nullable
  public JavaType primitiveWrapperType() {
    return primitiveWrapperType;
  }

  @Override
  public boolean isArray() {
    return isTagged(ARRAY);
  }

  public boolean isParameterized() {
    return isTagged(PARAMETERIZED);
  }

  @Override
  public boolean isClass() {
    return isTagged(CLASS) || isTagged(PARAMETERIZED);
  }

  @Override
  public boolean isVoid() {
    return isTagged(VOID);
  }

  @Override
  public String fullyQualifiedName() {
    return symbol.getFullyQualifiedName();
  }

  @Override
  public String name() {
    return symbol.name;
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    return getSymbol();
  }

}
