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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.commons.lang.BooleanUtils;
import org.sonar.java.resolve.Scope.OrderedScope;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

//FIXME rename this class to avoid clash of name with interface.
public class Symbol implements org.sonar.plugins.java.api.semantic.Symbol {

  public static final int PCK = 1 << 0;
  public static final int TYP = 1 << 1;
  public static final int VAR = 1 << 2;
  public static final int MTH = 1 << 4;

  public static final int ERRONEOUS = 1 << 6;
  public static final int AMBIGUOUS = ERRONEOUS + 1;
  public static final int ABSENT = ERRONEOUS + 2;

  final int kind;
  final SymbolMetadata symbolMetadata;

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
    this.symbolMetadata = new SymbolMetadata();
  }

  /**
   * @see Flags
   */
  public int flags() {
    return flags;
  }

  @Override
  public Symbol owner() {
    return owner;
  }

  public String getName() {
    return name;
  }

  public SymbolMetadata metadata() {
    complete();
    return symbolMetadata;
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
  @Override
  public TypeSymbol enclosingClass() {
    Symbol result = this;
    while (result != null && result.kind != TYP) {
      result = result.owner;
    }
    return (TypeSymbol) result;
  }

  boolean isKind(int kind) {
    return (this.kind & kind) != 0;
  }

  public Type getType() {
    return type;
  }

  @Override
  public org.sonar.plugins.java.api.semantic.Type type() {
    return type;
  }

  @Override
  public boolean isVariableSymbol() {
    return isKind(VAR);
  }

  @Override
  public boolean isTypeSymbol() {
    return isKind(TYP);
  }

  @Override
  public boolean isMethodSymbol() {
    return isKind(MTH);
  }

  @Override
  public boolean isStatic() {
    return isFlag(Flags.STATIC);
  }

  @Override
  public boolean isFinal() {
    return isFlag(Flags.FINAL);
  }

  @Override
  public boolean isEnum() {
    return isFlag(Flags.ENUM);
  }

  @Override
  public boolean isAbstract() {
    return isFlag(Flags.ABSTRACT);
  }

  @Override
  public boolean isPublic() {
    return isFlag(Flags.PUBLIC);
  }

  @Override
  public boolean isPrivate() {
    return isFlag(Flags.PRIVATE);
  }

  @Override
  public boolean isProtected() {
    return isFlag(Flags.PROTECTED);
  }

  @Override
  public boolean isDeprecated() {
    return isFlag(Flags.DEPRECATED);
  }

  @Override
  public boolean isVolatile() {
    return isFlag(Flags.VOLATILE);
  }

  @Override
  public String name() {
    return name;
  }

  protected boolean isFlag(int flag) {
    complete();
    return (flags & flag) != 0;
  }

  @Override
  public boolean isPackageVisibility() {
    complete();
    return (flags & (Flags.PROTECTED | Flags.PRIVATE | Flags.PUBLIC)) == 0;
  }

  interface Completer {
    void complete(Symbol symbol);
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
  public static class TypeSymbol extends Symbol implements TypeSymbolSemantic {

    Scope members;
    Scope typeParameters;
    List<Type.TypeVariableType> typeVariableTypes;

    public TypeSymbol(int flags, String name, Symbol owner) {
      super(TYP, flags, name, owner);
      this.type = new Type.ClassType(this);
      this.typeVariableTypes = Lists.newArrayList();
    }

    public void addTypeParameter(Type.TypeVariableType typeVariableType) {
      typeVariableTypes.add(typeVariableType);
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

    public Scope typeParameters() {
      complete();
      return typeParameters;
    }

    public String getFullyQualifiedName() {
      String ownerName = "";
      if (!owner.name.isEmpty()) {
        ownerName = owner.name + ".";
      }
      return ownerName + name;
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

    @Override
    public org.sonar.plugins.java.api.semantic.Type superClass() {
      return getSuperclass();
    }

    @Override
    public List<org.sonar.plugins.java.api.semantic.Type> interfaces() {
      return Lists.<org.sonar.plugins.java.api.semantic.Type>newArrayList(getInterfaces());
    }

    @Override
    public Collection<org.sonar.plugins.java.api.semantic.Symbol> memberSymbols() {
      return Lists.<org.sonar.plugins.java.api.semantic.Symbol>newArrayList(members().scopeSymbols());
    }
  }

  /**
   * Represents a field, enum constant, method or constructor parameter, local variable, resource variable or exception parameter.
   */
  public static class VariableSymbol extends Symbol implements VariableSymbolSemantic {

    public VariableSymbol(int flags, String name, Symbol owner) {
      super(VAR, flags, name, owner);
    }

    public VariableSymbol(int flags, String name, Type type, Symbol owner) {
      super(VAR, flags, name, owner);
      this.type = type;
    }

  }

  /**
   * Represents a method, constructor or initializer (static or instance).
   */
  public static class MethodSymbol extends Symbol implements MethodSymbolSemantic {

    TypeSymbol returnType;
    OrderedScope parameters;
    Scope typeParameters;
    List<TypeSymbol> thrown;
    List<Type.TypeVariableType> typeVariableTypes;

    public MethodSymbol(int flags, String name, Type type, Symbol owner) {
      super(MTH, flags, name, owner);
      super.type = type;
      this.returnType = ((Type.MethodType) type).resultType.symbol;
      this.typeVariableTypes = Lists.newArrayList();
    }

    public MethodSymbol(int flags, String name, Symbol owner) {
      super(MTH, flags, name, owner);
      this.typeVariableTypes = Lists.newArrayList();
    }

    public TypeSymbol getReturnType() {
      return returnType;
    }

    public OrderedScope getParameters() {
      return parameters;
    }

    public List<TypeSymbol> getThrownTypes() {
      return thrown;
    }

    public List<Type> getParametersTypes() {
      Preconditions.checkState(super.type != null);
      return ((Type.MethodType) super.type).argTypes;
    }

    public Scope typeParameters() {
      return typeParameters;
    }

    public void setMethodType(Type.MethodType methodType) {
      super.type = methodType;
      if (methodType.resultType != null) {
        this.returnType = methodType.resultType.symbol;
      }
    }

    public Boolean isOverriden() {
      Boolean result = false;
      Symbol.TypeSymbol enclosingClass = enclosingClass();
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
          Boolean isOverriding = isOverriding((Symbol.MethodSymbol) overrideSymbol, classType);
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

    private Boolean isOverriding(Symbol.MethodSymbol overridee, Type.ClassType classType) {
      // same number and type of formal parameters
      if (getParametersTypes().size() != overridee.getParametersTypes().size()) {
        return false;
      }
      for (int i = 0; i < getParametersTypes().size(); i++) {
        Type paramOverrider = getParametersTypes().get(i);
        if (paramOverrider.isTagged(Type.UNKNOWN)) {
          // FIXME : complete symbol table should not have unknown types and generics should be handled properly for this.
          return null;
        }
        // Generics type should have same erasure see JLS8 8.4.2

        Type overrideeType = overridee.getParametersTypes().get(i);
        if (classType instanceof Type.ParametrizedTypeType) {
          overrideeType = ((Type.ParametrizedTypeType) classType).typeSubstitution.get(overrideeType);
          if (overrideeType == null) {
            overrideeType = overridee.getParametersTypes().get(i);
          }
        }
        if (!paramOverrider.erasure().equals(overrideeType.erasure())) {
          return false;
        }
      }
      // we assume code is compiling so no need to check return type at this point.
      return true;
    }

    public boolean isVarArgs() {
      return isFlag(Flags.VARARGS);
    }

    public void addTypeParameter(Type.TypeVariableType typeVariableType) {
      typeVariableTypes.add(typeVariableType);
    }

  }

  /**
   * Represents type variable of a parametrized type ie: T in class Foo<T>{}
   */
  public static class TypeVariableSymbol extends TypeSymbol {
    public TypeVariableSymbol(String name, Symbol owner) {
      super(0, name, owner);
      this.type = new Type.TypeVariableType(this);
      this.members = new Scope(this);
    }

    @Override
    public Type getSuperclass() {
      // FIXME : should return upper bound or Object if no bound defined.
      return null;
    }

    @Override
    public List<Type> getInterfaces() {
      // FIXME : should return upperbound
      return ImmutableList.of();
    }
  }

}
