/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class JavaSymbol implements Symbol {

  public static final int PCK = 1 << 0;
  public static final int TYP = 1 << 1;
  public static final int VAR = 1 << 2;
  public static final int MTH = 1 << 4;

  public static final int ERRONEOUS = 1 << 6;
  public static final int AMBIGUOUS = ERRONEOUS + 1;
  public static final int ABSENT = ERRONEOUS + 2;

  final int kind;
  final SymbolMetadataResolve symbolMetadata;

  int flags;

  String name;

  JavaSymbol owner;

  Completer completer;

  JavaType type;

  private List<IdentifierTree> usages;

  public JavaSymbol(int kind, int flags, @Nullable String name, @Nullable JavaSymbol owner) {
    this.kind = kind;
    this.flags = flags;
    this.name = name;
    this.owner = owner;
    this.symbolMetadata = new SymbolMetadataResolve();
    this.usages = Lists.newArrayList();
  }

  /**
   * @see Flags
   */
  public int flags() {
    return flags;
  }

  @Override
  public JavaSymbol owner() {
    return owner;
  }

  public String getName() {
    return name;
  }

  @Override
  public SymbolMetadataResolve metadata() {
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
  public TypeJavaSymbol outermostClass() {
    JavaSymbol symbol = this;
    JavaSymbol result = null;
    while (symbol.kind != PCK) {
      result = symbol;
      symbol = symbol.owner();
    }
    return (TypeJavaSymbol) result;
  }

  /**
   * The package which indirectly owns this symbol.
   */
  public PackageJavaSymbol packge() {
    JavaSymbol result = this;
    while (result.kind != PCK) {
      result = result.owner();
    }
    return (PackageJavaSymbol) result;
  }

  /**
   * The closest enclosing class.
   */
  @Override
  public TypeJavaSymbol enclosingClass() {
    JavaSymbol result = this;
    while (result != null && result.kind != TYP) {
      result = result.owner;
    }
    return (TypeJavaSymbol) result;
  }

  boolean isKind(int kind) {
    return (this.kind & kind) != 0;
  }

  public JavaType getType() {
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
  public boolean isInterface() {
    return isFlag(Flags.INTERFACE);
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
  public boolean isUnknown() {
    return false;
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

  public void addUsage(IdentifierTree tree) {
    usages.add(tree);
  }

  @Override
  public List<IdentifierTree> usages() {
    return usages;
  }

  @Nullable
  @Override
  public Tree declaration() {
    return null;
  }

  interface Completer {
    void complete(JavaSymbol symbol);
  }

  /**
   * Represents package.
   */
  public static class PackageJavaSymbol extends JavaSymbol {

    Scope members;

    public PackageJavaSymbol(@Nullable String name, @Nullable JavaSymbol owner) {
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
  public static class TypeJavaSymbol extends JavaSymbol implements TypeSymbol {

    Scope members;
    Scope typeParameters;
    List<JavaType.TypeVariableJavaType> typeVariableTypes;
    ClassTree declaration;

    public TypeJavaSymbol(int flags, String name, JavaSymbol owner) {
      super(TYP, flags, name, owner);
      this.type = new JavaType.ClassJavaType(this);
      this.typeVariableTypes = Lists.newArrayList();
    }

    public void addTypeParameter(JavaType.TypeVariableJavaType typeVariableType) {
      typeVariableTypes.add(typeVariableType);
    }

    @Nullable
    public JavaType getSuperclass() {
      complete();
      return ((JavaType.ClassJavaType) type).supertype;
    }

    public List<JavaType> getInterfaces() {
      complete();
      return ((JavaType.ClassJavaType) type).interfaces;
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
    public Set<JavaType.ClassJavaType> superTypes() {
      ImmutableSet.Builder<JavaType.ClassJavaType> types = ImmutableSet.builder();
      JavaType.ClassJavaType superClassType = (JavaType.ClassJavaType) this.superClass();
      types.addAll(this.interfacesOfType());
      while (superClassType != null) {
        types.add(superClassType);
        TypeJavaSymbol superClassSymbol = superClassType.getSymbol();
        types.addAll(superClassSymbol.interfacesOfType());
        superClassType = (JavaType.ClassJavaType) superClassSymbol.superClass();
      }
      return types.build();
    }

    private Set<JavaType.ClassJavaType> interfacesOfType() {
      ImmutableSet.Builder<JavaType.ClassJavaType> builder = ImmutableSet.builder();
      for (JavaType interfaceType : getInterfaces()) {
        JavaType.ClassJavaType classType = (JavaType.ClassJavaType) interfaceType;
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
    public List<Type> interfaces() {
      return Lists.<Type>newArrayList(getInterfaces());
    }

    @Override
    public Collection<org.sonar.plugins.java.api.semantic.Symbol> memberSymbols() {
      return Lists.<org.sonar.plugins.java.api.semantic.Symbol>newArrayList(members().scopeSymbols());
    }

    @Override
    public Collection<org.sonar.plugins.java.api.semantic.Symbol> lookupSymbols(String name) {
      return Lists.<org.sonar.plugins.java.api.semantic.Symbol>newArrayList(members().lookup(name));
    }

    @Override
    public ClassTree declaration() {
      return declaration;
    }
  }

  /**
   * Represents a field, enum constant, method or constructor parameter, local variable, resource variable or exception parameter.
   */
  public static class VariableJavaSymbol extends JavaSymbol implements VariableSymbol {

    VariableTree declaration;

    public VariableJavaSymbol(int flags, String name, JavaSymbol owner) {
      super(VAR, flags, name, owner);
    }

    public VariableJavaSymbol(int flags, String name, JavaType type, JavaSymbol owner) {
      super(VAR, flags, name, owner);
      this.type = type;
    }

    @Override
    public VariableTree declaration() {
      return declaration;
    }
  }

  /**
   * Represents a method, constructor or initializer (static or instance).
   */
  public static class MethodJavaSymbol extends JavaSymbol implements MethodSymbol {

    TypeJavaSymbol returnType;
    OrderedScope parameters;
    Scope typeParameters;
    List<JavaType.TypeVariableJavaType> typeVariableTypes;
    MethodTree declaration;

    public MethodJavaSymbol(int flags, String name, JavaType type, JavaSymbol owner) {
      super(MTH, flags, name, owner);
      super.type = type;
      this.returnType = ((JavaType.MethodJavaType) type).resultType.symbol;
      this.typeVariableTypes = Lists.newArrayList();
    }

    public MethodJavaSymbol(int flags, String name, JavaSymbol owner) {
      super(MTH, flags, name, owner);
      this.typeVariableTypes = Lists.newArrayList();
    }

    public TypeJavaSymbol getReturnType() {
      return returnType;
    }

    public OrderedScope getParameters() {
      return parameters;
    }

    private List<JavaType> getParametersTypes() {
      Preconditions.checkState(super.type != null);
      return ((JavaType.MethodJavaType) super.type).argTypes;
    }

    public Scope typeParameters() {
      return typeParameters;
    }

    public void setMethodType(JavaType.MethodJavaType methodType) {
      super.type = methodType;
      if (methodType.resultType != null) {
        this.returnType = methodType.resultType.symbol;
      }
    }

    public Boolean isOverriden() {
      Boolean result = false;
      TypeJavaSymbol enclosingClass = enclosingClass();
      for (JavaType.ClassJavaType superType : enclosingClass.superTypes()) {
        Boolean overrideFromType = overridesFromSymbol(superType);
        if (overrideFromType == null) {
          result = null;
        } else if (BooleanUtils.isTrue(overrideFromType)) {
          return true;
        }
      }
      return result;
    }

    @Nullable
    private Boolean overridesFromSymbol(JavaType.ClassJavaType classType) {
      Boolean result = false;
      if (classType.isTagged(JavaType.UNKNOWN)) {
        return null;
      }
      List<JavaSymbol> symbols = classType.getSymbol().members().lookup(name);
      for (JavaSymbol overrideSymbol : symbols) {
        if (overrideSymbol.isKind(JavaSymbol.MTH) && canOverride((MethodJavaSymbol) overrideSymbol)) {
          Boolean isOverriding = isOverriding((MethodJavaSymbol) overrideSymbol, classType);
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
    private boolean canOverride(MethodJavaSymbol overridee) {
      if (overridee.isPackageVisibility()) {
        return overridee.outermostClass().owner().equals(outermostClass().owner());
      }
      return !overridee.isPrivate();
    }

    @Nullable
    private Boolean isOverriding(MethodJavaSymbol overridee, JavaType.ClassJavaType classType) {
      // same number and type of formal parameters
      if (getParametersTypes().size() != overridee.getParametersTypes().size()) {
        return false;
      }
      for (int i = 0; i < getParametersTypes().size(); i++) {
        JavaType paramOverrider = getParametersTypes().get(i);
        if (paramOverrider.isTagged(JavaType.UNKNOWN)) {
          // FIXME : complete symbol table should not have unknown types and generics should be handled properly for this.
          return null;
        }
        // Generics type should have same erasure see JLS8 8.4.2

        JavaType overrideeType = overridee.getParametersTypes().get(i);
        if (classType instanceof JavaType.ParametrizedTypeJavaType) {
          overrideeType = ((JavaType.ParametrizedTypeJavaType) classType).typeSubstitution.substitutedType(overrideeType);
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

    public void addTypeParameter(JavaType.TypeVariableJavaType typeVariableType) {
      typeVariableTypes.add(typeVariableType);
    }

    @Override
    public List<org.sonar.plugins.java.api.semantic.Type> parameterTypes() {
      return Lists.<org.sonar.plugins.java.api.semantic.Type>newArrayList(getParametersTypes());
    }

    @Override
    public TypeSymbol returnType() {
      return returnType;
    }

    @Override
    public List<org.sonar.plugins.java.api.semantic.Type> thrownTypes() {
      return Lists.<org.sonar.plugins.java.api.semantic.Type>newArrayList(((JavaType.MethodJavaType) super.type).thrown);
    }

    @Override
    public MethodTree declaration() {
      return declaration;
    }
  }

  /**
   * Represents type variable of a parametrized type ie: T in class Foo<T>{}
   */
  public static class TypeVariableJavaSymbol extends TypeJavaSymbol {
    public TypeVariableJavaSymbol(String name, JavaSymbol owner) {
      super(0, name, owner);
      this.type = new JavaType.TypeVariableJavaType(this);
      this.members = new Scope(this);
    }

    @Override
    @Nullable
    public JavaType getSuperclass() {
      // FIXME : should return upper bound or Object if no bound defined.
      return null;
    }

    @Override
    public List<JavaType> getInterfaces() {
      // FIXME : should return upperbound
      return ImmutableList.of();
    }

    @Override
    public ClassTree declaration() {
      // FIXME: declaration should not be ClassTree: a type Variable is declared by an identifier with bounds
      // This probably implies to refactor this class to not inherit form TypeJavaSymbol and/or to implement its dedicated interface
      return null;
    }
  }

  public static class JavaLabelSymbol extends JavaSymbol implements Symbol.LabelSymbol {

    private LabeledStatementTree declaration;

    public JavaLabelSymbol(LabeledStatementTree tree) {
      super(0, 0, tree.label().name(), null);
      declaration = tree;
    }

    @Override
    public LabeledStatementTree declaration() {
      return declaration;
    }

  }

}
