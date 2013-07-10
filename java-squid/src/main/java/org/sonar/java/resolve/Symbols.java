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

import com.google.common.collect.ImmutableList;

import java.util.Arrays;

/**
 * Predefined symbols.
 */
public class Symbols {

  final Symbol.PackageSymbol rootPackage;

  /**
   * Owns all predefined symbols (builtin types, operators).
   */
  final Symbol.TypeSymbol predefClass;

  /**
   * Type, which can't be modelled for the moment.
   */
  final Type.ClassType unknownType;
  final Symbol.TypeSymbol unknownSymbol;

  final Symbol.TypeSymbol arrayClass;

  final Symbol.TypeSymbol methodClass;
  final Symbol.TypeSymbol noSymbol;

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

    // TODO should have type "noType":
    noSymbol = new Symbol.TypeSymbol(0, "", rootPackage);

    methodClass = new Symbol.TypeSymbol(Flags.PUBLIC, "", noSymbol);

    // builtin types
    byteType = initType(Type.BYTE, "byte");
    charType = initType(Type.CHAR, "char");
    shortType = initType(Type.SHORT, "short");
    intType = initType(Type.INT, "int");
    longType = initType(Type.LONG, "long");
    floatType = initType(Type.FLOAT, "float");
    doubleType = initType(Type.DOUBLE, "double");
    booleanType = initType(Type.BOOLEAN, "boolean");
    nullType = initType(Type.BOT, "<nulltype>");

    // predefined types
    objectType = enterClass("java.lang.Object");
    classType = enterClass("java.lang.Class");
    stringType = enterClass("java.lang.String");
    cloneableType = enterClass("java.lang.Cloneable");
    serializableType = enterClass("java.io.Serializable");

    // TODO comment me
    arrayClass = new Symbol.TypeSymbol(Flags.PUBLIC, "Array", noSymbol);
    Type.ClassType arrayClassType = (Type.ClassType) arrayClass.type;
    arrayClassType.supertype = objectType;
    arrayClassType.interfaces = ImmutableList.of(cloneableType, serializableType);
    arrayClass.members = new Scope(arrayClass);
    arrayClass.members().enter(new Symbol.VariableSymbol(Flags.PUBLIC | Flags.FINAL, "length", intType, arrayClass));
    // TODO arrayClass implements clone() method

    enterOperators();
  }

  private Type enterClass(String name) {
    // TODO use BytecodeCompleter
    Symbol.TypeSymbol symbol = new Symbol.TypeSymbol(Flags.PUBLIC, name.substring(name.lastIndexOf('.') + 1, name.length()), /* FIXME */ rootPackage);
    symbol.members = new Scope(symbol);
    ((Type.ClassType) symbol.type).interfaces = ImmutableList.of();
    return symbol.type;
  }

  /**
   * Registers builtin types as symbols, so that they can be found as an usual identifiers.
   */
  private Type initType(int tag, String name) {
    Symbol.TypeSymbol symbol = new Symbol.TypeSymbol(Flags.PUBLIC, name, rootPackage);
    symbol.members = new Scope(symbol);
    predefClass.members.enter(symbol);
    ((Type.ClassType) symbol.type).interfaces = ImmutableList.of();
    symbol.type.tag = tag;
    return symbol.type;
  }

  /**
   * Registers operators as methods, so that they can be found as an usual methods.
   */
  private void enterOperators() {
    for (String op : new String[]{"+", "-", "*", "/", "%"}) {
      for (Type type : Arrays.asList(doubleType, floatType, longType, intType)) {
        enterBinop(op, type, type, type);
      }
    }
    for (String op : new String[]{"&", "|", "^"}) {
      for (Type type : Arrays.asList(booleanType, longType, intType)) {
        enterBinop(op, type, type, type);
      }
    }
    for (String op : new String[]{"<<", ">>", ">>>"}) {
      enterBinop(op, longType, longType, longType);
      enterBinop(op, intType, longType, intType);
      enterBinop(op, longType, intType, longType);
      enterBinop(op, intType, intType, intType);
    }
    for (String op : new String[]{"<", ">", ">=", "<="}) {
      for (Type type : Arrays.asList(doubleType, floatType, longType, intType)) {
        enterBinop(op, type, type, booleanType);
      }
    }
    for (String op : new String[]{"==", "!="}) {
      for (Type type : Arrays.asList(objectType, booleanType, doubleType, floatType, longType, intType)) {
        enterBinop(op, type, type, booleanType);
      }
    }
    enterBinop("&&", booleanType, booleanType, booleanType);
    enterBinop("||", booleanType, booleanType, booleanType);

    // string concatenation
    for (Type type : Arrays.asList(nullType, objectType, booleanType, doubleType, floatType, longType, intType)) {
      enterBinop("+", stringType, type, stringType);
      enterBinop("+", type, stringType, stringType);
    }
    enterBinop("+", stringType, stringType, stringType);
  }

  private void enterBinop(String name, Type left, Type right, Type result) {
    Type type = new Type.MethodType(ImmutableList.of(left, right), result, ImmutableList.<Type>of(), methodClass);
    Symbol symbol = new Symbol.MethodSymbol(Flags.PUBLIC | Flags.STATIC, name, type, predefClass);
    predefClass.members.enter(symbol);
  }

}
