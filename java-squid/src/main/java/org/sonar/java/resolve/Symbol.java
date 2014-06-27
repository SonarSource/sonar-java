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

import javax.annotation.Nullable;
import java.util.List;

public class Symbol {

  public static final int PCK = 1 << 0;
  public static final int TYP = 1 << 1;
  public static final int VAR = 1 << 2;
  public static final int MTH = 1 << 4;

  public static final int ERRONEOUS = 1 << 6;
  public static final int AMBIGUOUS = ERRONEOUS + 1;
  public static final int ABSENT = ERRONEOUS + 2;

  final int kind;

  int flags;

  String name;

  Symbol owner;

  Completer completer;

  Type type;

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

  public String getName() {
    return name;
  }

  interface Completer {
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

  public boolean isKind(int kind) {
    return (this.kind & kind) != 0;
  }

  public Type getType() {
    return type;
  }

  /**
   * Represents package.
   */
  public static class PackageSymbol extends Symbol {

    Scope members;

    public PackageSymbol(@Nullable String name, @Nullable Symbol owner) {
      super(PCK, 0, name, owner);
    }

    Scope members() {
      complete();
      return members;
    }

  }

  /**
   * Represents a class, interface, enum or annotation type.
   */
  public static class TypeSymbol extends Symbol {

    Scope members;

    public TypeSymbol(int flags, String name, Symbol owner) {
      super(TYP, flags, name, owner);
      this.type = new Type.ClassType(this);
    }

    public Type getSuperclass() {
      complete();
      return ((Type.ClassType) type).supertype;
    }

    public List<Type> getInterfaces() {
      complete();
      return ((Type.ClassType) type).interfaces;
    }

    public Scope members() {
      complete();
      return members;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  /**
   * Represents a field, enum constant, method or constructor parameter, local variable, resource variable or exception parameter.
   */
  public static class VariableSymbol extends Symbol {

    public VariableSymbol(int flags, String name, Symbol owner) {
      super(VAR, flags, name, owner);
    }

    public VariableSymbol(int flags, String name, Type type, Symbol owner) {
      super(VAR, flags, name, owner);
      this.type = type;
    }

    // FIXME(Godin): method "type", which returns a String, looks very strange here:
    public String type() {
      return type.symbol.name;
    }

  }

  /**
   * Represents a method, constructor or initializer (static or instance).
   */
  public static class MethodSymbol extends Symbol {

    TypeSymbol type;
    Scope parameters;
    List<TypeSymbol> thrown;

    public MethodSymbol(int flags, String name, Type type, Symbol owner) {
      super(MTH, flags, name, owner);
      super.type = type;
    }

    public MethodSymbol(int flags, String name, Symbol owner) {
      super(MTH, flags, name, owner);
    }

    public TypeSymbol getReturnType() {
      return type;
    }

    public List<TypeSymbol> getThrownTypes() {
      return thrown;
    }

  }

  public boolean isStatic() {
    return (flags & Flags.STATIC) != 0;
  }

  public boolean isEnum() {
    return (flags & Flags.ENUM) != 0;
  }

  public boolean isAbstract() {
    return (flags & Flags.ABSTRACT) != 0;
  }


}
