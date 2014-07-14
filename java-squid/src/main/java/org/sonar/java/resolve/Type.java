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

import java.util.List;

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

    public Symbol.TypeSymbol getSymbol() {
      return symbol;
    }
  }

  public static class ArrayType extends Type {

    /**
     * Type of elements of this array.
     */
    Type elementType;

    /**
     * @param arrayClass {@link Symbols#arrayClass}
     */
    public ArrayType(Type elementType, Symbol.TypeSymbol arrayClass) {
      super(ARRAY, arrayClass);
      this.elementType = elementType;
    }

    @Override
    public String toString() {
      return elementType.toString() + "[]";
    }
  }

  public static class MethodType extends Type {

    List<Type> argTypes;
    Type resultType;
    List<Type> thrown;

    public MethodType(List<Type> argTypes, Type resultType, List<Type> thrown, Symbol.TypeSymbol symbol) {
      super(METHOD, symbol);
      this.argTypes = argTypes;
      this.resultType = resultType;
      this.thrown = thrown;
    }

    @Override
    public String toString() {
      return "returns " + resultType.toString();
    }
  }

  @Override
  public String toString() {
    return symbol == null ? "" : symbol.toString();
  }
}
