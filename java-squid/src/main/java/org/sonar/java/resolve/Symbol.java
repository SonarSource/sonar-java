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

import javax.annotation.Nullable;
import java.util.List;

public class Symbol {

  public static int PCK = 1 << 0;
  public static int TYP = 1 << 1;
  public static int VAR = 1 << 2;
  public static int MTH = 1 << 4;

  public static int ERRONEOUS = 1 << 6;
  public static int ABSENT = ERRONEOUS + 1;

  final int kind;

  private final int flags;

  final String name;

  private final Symbol owner;

  Completer completer;

  public Symbol(int kind, int flags, @Nullable String name, @Nullable Symbol owner) {
    this.kind = kind;
    this.flags = flags;
    this.name = name;
    this.owner = owner;
  }

  /**
   * @see Flags
   */
  public int flags() {
    return flags;
  }

  /**
   * The owner of this symbol.
   */
  public Symbol owner() {
    return owner;
  }

  static interface Completer {
    void complete(Symbol symbol);
  }

  public void complete() {
    if (completer != null) {
      Completer c = completer;
      completer = null;
      c.complete(this);
    }
  }

  /**
   * The outermost class which indirectly owns this symbol.
   */
  public TypeSymbol outermostClass() {
    Symbol symbol = this;
    Symbol result = null;
    while (symbol.kind != PCK) {
      result = symbol;
      symbol = symbol.owner();
    }
    return (TypeSymbol) result;
  }

  /**
   * The package which indirectly owns this symbol.
   */
  public PackageSymbol packge() {
    Symbol result = this;
    while (result.kind != PCK) {
      result = result.owner();
    }
    return (PackageSymbol) result;
  }

  /**
   * The closest enclosing class.
   */
  public TypeSymbol enclosingClass() {
    Symbol result = this;
    while (result != null && result.kind != TYP) {
      result = result.owner;
    }
    return (TypeSymbol) result;
  }

  /**
   * Represents package.
   */
  public static class PackageSymbol extends Symbol {

    Scope members;

    public PackageSymbol(String name, Symbol owner) {
      super(PCK, 0, name, owner);
    }

  }

  /**
   * Represents a class, interface, enum or annotation type.
   */
  public static class TypeSymbol extends Symbol {

    Scope members;
    TypeSymbol superclass;
    ImmutableList<TypeSymbol> interfaces;

    public TypeSymbol(int flags, String name, Symbol owner) {
      super(TYP, flags, name, owner);
    }

    public TypeSymbol getSuperclass() {
      complete();
      return superclass;
    }

    public List<TypeSymbol> getInterfaces() {
      complete();
      return interfaces;
    }

    Scope members() {
      complete();
      return members;
    }

  }

  /**
   * Represents a field, enum constant, method or constructor parameter, local variable, resource variable or exception parameter.
   */
  public static class VariableSymbol extends Symbol {

    TypeSymbol type;

    public VariableSymbol(int flags, String name, Symbol owner) {
      super(VAR, flags, name, owner);
    }

  }

  /**
   * Represents a method, constructor or initializer (static or instance).
   */
  public static class MethodSymbol extends Symbol {

    TypeSymbol type;
    Scope parameters;

    public MethodSymbol(int flags, String name, Symbol owner) {
      super(MTH, flags, name, owner);
    }

  }

}
