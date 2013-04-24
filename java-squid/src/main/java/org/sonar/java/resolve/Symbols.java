/*
 * Sonar Java
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

import com.google.common.collect.ImmutableList;

/**
 * Predefined symbols.
 */
public class Symbols {

  final Symbol.PackageSymbol rootPackage;

  final Symbol.TypeSymbol predefClass;

  /**
   * Type, which can't be modelled for the moment.
   */
  final Type.ClassType unknownType;
  final Symbol.TypeSymbol unknownSymbol;

  final Symbol.TypeSymbol arrayClass;

  // builtin types
  final Type byteType;
  final Type charType;
  final Type shortType;
  final Type intType;
  final Type longType;
  final Type floatType;
  final Type doubleType;
  final Type booleanType;
  final Type nullType;

  // predefined types
  final Type objectType;
  final Type cloneableType;
  final Type serializableType;
  final Type classType;
  final Type stringType;

  public Symbols() {
    rootPackage = new Symbol.PackageSymbol("", null);

    predefClass = new Symbol.TypeSymbol(Flags.PUBLIC, "", rootPackage);
    predefClass.members = new Scope(predefClass);
    ((Type.ClassType) predefClass.type).interfaces = ImmutableList.of();

    unknownSymbol = new Symbol.TypeSymbol(Flags.PUBLIC, /* TODO name */"", rootPackage);
    unknownSymbol.members = new Scope(unknownSymbol);
    unknownType = new Type.ClassType(unknownSymbol) {
      @Override
      public String toString() {
        return "!unknown!";
      }
    };
    unknownType.interfaces = ImmutableList.of();
    unknownSymbol.type = unknownType;

    // builtin types
    byteType = initType("byte");
    charType = initType("char");
    shortType = initType("short");
    intType = initType("int");
    longType = initType("long");
    floatType = initType("float");
    doubleType = initType("double");
    booleanType = initType("boolean");

    nullType = new Type(null);

    // predefined types
    objectType = enterClass("java.lang.Object");
    classType = enterClass("java.lang.Class");
    stringType = enterClass("java.lang.String");
    cloneableType = enterClass("java.lang.Cloneable");
    serializableType = enterClass("java.io.Serializable");

    // TODO comment me
    arrayClass = new Symbol.TypeSymbol(Flags.PUBLIC, "Array", null);
    Type.ClassType arrayClassType = (Type.ClassType) arrayClass.type;
    arrayClassType.supertype = objectType;
    arrayClassType.interfaces = ImmutableList.of(cloneableType, serializableType);
    arrayClass.members = new Scope(arrayClass);
    arrayClass.members().enter(new Symbol.VariableSymbol(Flags.PUBLIC | Flags.FINAL, "length", intType, arrayClass));
    // TODO arrayClass implements clone() method
  }

  /**
   * Registers builtin types as symbols, so that they can be found as an usual identifiers.
   */
  private Type initType(String name) {
    Symbol.TypeSymbol symbol = new Symbol.TypeSymbol(Flags.PUBLIC, name, rootPackage);
    symbol.members = new Scope(symbol);
    predefClass.members.enter(symbol);
    ((Type.ClassType) symbol.type).interfaces = ImmutableList.of();
    return symbol.type;
  }

  private Type enterClass(String name) {
    // TODO use BytecodeCompleter
    Symbol.TypeSymbol symbol = new Symbol.TypeSymbol(Flags.PUBLIC, name.substring(name.lastIndexOf('.') + 1, name.length()), /* FIXME */ rootPackage);
    symbol.members = new Scope(symbol);
    ((Type.ClassType) symbol.type).interfaces = ImmutableList.of();
    return symbol.type;
  }

}
