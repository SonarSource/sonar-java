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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

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

  boolean isParametrized = false;

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

    /**
     * Includes superclass and super interface hierarchy.
     * @return list of classTypes.
     */
    public Set<Type.ClassType> superTypes() {
      ImmutableSet.Builder<Type.ClassType> types = ImmutableSet.builder();
      Type.ClassType superClassType = (Type.ClassType) this.getSuperclass();
      types.addAll(this.interfacesOfType());
      while (superClassType != null) {
        types.add(superClassType);
        Symbol.TypeSymbol superClassSymbol = superClassType.getSymbol();
        types.addAll(superClassSymbol.interfacesOfType());
        superClassType = (Type.ClassType) superClassSymbol.getSuperclass();
      }
      return types.build();
    }

    private Set<Type.ClassType> interfacesOfType() {
      ImmutableSet.Builder<Type.ClassType> builder = ImmutableSet.builder();
      for (Type interfaceType : getInterfaces()) {
        Type.ClassType classType = (Type.ClassType) interfaceType;
        builder.add(classType);
        builder.addAll(classType.getSymbol().interfacesOfType());
      }
      return builder.build();
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
      this.type = ((Type.MethodType)type).resultType.symbol;
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

    public List<Type> getParametersTypes() {
      Preconditions.checkState(super.type != null);
      return ((Type.MethodType) super.type).argTypes;
    }

    public void setMethodType(Type.MethodType methodType) {
      super.type = methodType;
      if(methodType.resultType != null) {
        this.type = methodType.resultType.symbol;
      }
    }

    public Boolean isOverriden() {
      Boolean result = false;
      Symbol.TypeSymbol enclosingClass = enclosingClass();
      if (StringUtils.isEmpty(enclosingClass.getName())) {
        //FIXME : SONARJAVA-645 : exclude methods within anonymous classes
        return null;
      }
      for (Type.ClassType superType : enclosingClass.superTypes()) {
        Boolean overrideFromType = overridesFromSymbol(superType);
        if (overrideFromType == null) {
          result = null;
        } else if (BooleanUtils.isTrue(overrideFromType)) {
          return true;
        }
      }
      return result;
    }

    private Boolean overridesFromSymbol(Type.ClassType classType) {
      Boolean result = false;
      if (classType.isTagged(Type.UNKNOWN)) {
        return null;
      }
      List<Symbol> symbols = classType.getSymbol().members().lookup(name);
      for (Symbol overrideSymbol : symbols) {
        if (overrideSymbol.isKind(Symbol.MTH) && canOverride((Symbol.MethodSymbol) overrideSymbol)) {
          Boolean isOverriding = isOverriding((Symbol.MethodSymbol) overrideSymbol);
          if (isOverriding == null) {
            result = null;
          } else if (BooleanUtils.isTrue(isOverriding)) {
            return true;
          }
        }
      }
      return result;
    }

    /**
     * Check accessibility of parent method.
     */
    private boolean canOverride(Symbol.MethodSymbol overridee) {
      if (overridee.isPackageVisibility()) {
        return overridee.outermostClass().owner().equals(outermostClass().owner());
      }
      return !overridee.isPrivate();
    }

    private Boolean isOverriding(Symbol.MethodSymbol overridee) {
      //same number and type of formal parameters
      if (getParametersTypes().size() != overridee.getParametersTypes().size()) {
        return false;
      }
      for (int i = 0; i < getParametersTypes().size(); i++) {
        Type paramOverrider = getParametersTypes().get(i);
        if (paramOverrider.isTagged(Type.UNKNOWN)) {
          //FIXME : complete symbol table should not have unknown types.
          return null;
        }
        if (!paramOverrider.equals(overridee.getParametersTypes().get(i))) {
          return false;
        }
      }
      //we assume code is compiling so no need to check return type at this point.
      return true;
    }

    public boolean isVarArgs() {
      return isFlag(Flags.VARARGS);
    }
  }

  public boolean isStatic() {
    return isFlag(Flags.STATIC);
  }

  public boolean isFinal() {
    return isFlag(Flags.FINAL);
  }

  public boolean isEnum() {
    return isFlag(Flags.ENUM);
  }

  public boolean isAbstract() {
    return isFlag(Flags.ABSTRACT);
  }

  public boolean isPublic() {
    return isFlag(Flags.PUBLIC);
  }

  public boolean isPrivate() {
    return isFlag(Flags.PRIVATE);
  }

  public boolean isDeprecated() {
    return isFlag(Flags.DEPRECATED);
  }

  protected boolean isFlag(int flag) {
    complete();
    return (flags & flag) != 0;
  }

  public boolean isPackageVisibility() {
    complete();
    return (flags & (Flags.PROTECTED | Flags.PRIVATE | Flags.PUBLIC)) == 0;
  }

}
