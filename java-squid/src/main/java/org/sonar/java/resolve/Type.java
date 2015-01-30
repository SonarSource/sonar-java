/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.resolve;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class Type {

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

  int tag;

  /**
   * Symbol, which defines this type.
   */
  Symbol.TypeSymbol symbol;

  public Type(int tag, Symbol.TypeSymbol symbol) {
    this.tag = tag;
    this.symbol = symbol;
  }

  public boolean isTagged(int tag) {
    return tag == this.tag;
  }

  public boolean isNumerical() {
    //JLS8 4.2
    return tag <= DOUBLE;
  }

  public Symbol.TypeSymbol getSymbol() {
    symbol.complete();
    return symbol;
  }

  public boolean is(String fullyQualifiedName) {
    if (isTagged(CLASS)) {
      return fullyQualifiedName.equals(symbol.getFullyQualifiedName());
    } else if (tag < CLASS) {
      //primitive type
      return fullyQualifiedName.equals(symbol.name);
    } else if (isTagged(ARRAY)) {
      return fullyQualifiedName.endsWith("[]") && ((ArrayType) this).elementType.is(fullyQualifiedName.substring(0, fullyQualifiedName.length() - 2));
    } else if (isTagged(TYPEVAR)) {
      return false;
    }
    return isTagged(BOT) || !isTagged(UNKNOWN);
  }

  public boolean isSubtypeOf(String fullyQualifiedName) {
    if (isTagged(CLASS)) {
      if (is(fullyQualifiedName)) {
        return true;
      }
      for (ClassType classType : symbol.superTypes()) {
        if (classType.is(fullyQualifiedName)) {
          return true;
        }
      }
    }
    if(isTagged(TYPEVAR)) {
      return erasure().isSubtypeOf(fullyQualifiedName);
    }
    return false;
  }

  public boolean isParametrized() {
    symbol.complete();
    return symbol.isParametrized;
  }

  /**
   * JLS8 4.6
   */
  public Type erasure() {
    return this;
  }

  public boolean isPrimitive() {
    return tag <= BOOLEAN;
  }

  public boolean isPrimitiveWrapper() {
    if(!isTagged(CLASS)) {
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

  public static class ClassType extends Type {

    /**
     * Supertype of this class.
     */
    Type supertype;

    /**
     * Interfaces of this class.
     */
    List<Type> interfaces;

    public ClassType(Symbol.TypeSymbol symbol) {
      super(CLASS, symbol);
    }
  }

  public static class ArrayType extends Type {

    /**
     * Type of elements of this array.
     */
    Type elementType;
    private final ArrayType erasure;
    /**
     * @param arrayClass {@link Symbols#arrayClass}
     */
    public ArrayType(Type elementType, Symbol.TypeSymbol arrayClass) {
      super(ARRAY, arrayClass);
      this.elementType = elementType;
      //element
      this.erasure = new ArrayType(arrayClass);
    }

    private ArrayType(Symbol.TypeSymbol arrayClass) {
      super(ARRAY, arrayClass);
      this.erasure = this;
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(31, 37).append(elementType).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof ArrayType)) {
        return false;
      }
      ArrayType rhs = (ArrayType) obj;
      return new EqualsBuilder()
          .append(elementType, rhs.elementType)
          .isEquals();
    }

    @Override
    public String toString() {
      return elementType.toString() + "[]";
    }

    public Type elementType() {
      return elementType;
    }

    @Override
    public Type erasure() {
      if(erasure.elementType == null) {
        erasure.elementType = elementType.erasure();
      }
      return erasure;
    }
  }

  public static class MethodType extends Type {

    List<Type> argTypes;
    //Return type of constructor is null.
    @Nullable
    Type resultType;
    List<Type> thrown;

    public MethodType(List<Type> argTypes, @Nullable Type resultType, List<Type> thrown, Symbol.TypeSymbol symbol) {
      super(METHOD, symbol);
      this.argTypes = argTypes;
      this.resultType = resultType;
      this.thrown = thrown;
    }

    @Override
    public String toString() {
      return resultType == null ? "constructor" : "returns " + resultType.toString();
    }
  }

  public static class TypeVariableType extends Type {

    List<Type> bounds;

    public TypeVariableType(Symbol.TypeVariableSymbol symbol) {
      super(TYPEVAR, symbol);
    }

    /**
     * Erasure of a type variable is the erasure of its leftmost bound.
     */
    @Override
    public Type erasure() {
      return bounds.get(0);
    }
  }

  public static class ParametrizedTypeType extends ClassType {

    final Map<TypeVariableType, Type> typeSubstitution;
    final Type rawType;

    ParametrizedTypeType(Symbol.TypeSymbol symbol, Map<TypeVariableType, Type> typeSubstitution) {
      super(symbol);
      this.rawType = symbol.getType();
      this.typeSubstitution = typeSubstitution;
    }

    @Override
    public Type erasure() {
      return rawType.erasure();
    }
  }
}
