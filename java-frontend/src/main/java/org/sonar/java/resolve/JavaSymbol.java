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

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

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

  boolean completing = false;
  private ImmutableList.Builder<IdentifierTree> usagesBuilder;
  private List<IdentifierTree> usages;
  private List<Runnable> callbacks = new ArrayList<>();

  public JavaSymbol(int kind, int flags, @Nullable String name, @Nullable JavaSymbol owner) {
    this.kind = kind;
    this.flags = flags;
    this.name = name;
    this.owner = owner;
    this.symbolMetadata = new SymbolMetadataResolve();
    this.usagesBuilder = ImmutableList.builder();
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
      completing = true;
      c.complete(this);
      completing = false;
      callbacks.forEach(Runnable::run);
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
  public Type type() {
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
  public boolean isPackageSymbol() {
    return isKind(PCK);
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

  public boolean isAnnotation() {
    return isFlag(Flags.ANNOTATION);
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

  public boolean isDefault() {
    return isFlag(Flags.DEFAULT);
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
    return Flags.isFlagged(flags, flag);
  }

  @Override
  public boolean isPackageVisibility() {
    complete();
    return Flags.isNotFlagged(flags, Flags.PROTECTED | Flags.PRIVATE | Flags.PUBLIC);
  }

  public void addUsage(IdentifierTree tree) {
    usagesBuilder.add(tree);
  }

  @Override
  public List<IdentifierTree> usages() {
    if (usages == null) {
      usages = ImmutableList.<IdentifierTree>builder().addAll(usagesBuilder.build().stream().distinct().collect(Collectors.toList())).build();
    }
    return usages;
  }

  @Nullable
  @Override
  public Tree declaration() {
    return null;
  }

  public void callbackOnceComplete(Runnable callback) {
    callbacks.add(callback);
  }

  interface Completer {
    void complete(JavaSymbol symbol);
  }

  /**
   * Represents package.
   */
  public static class PackageJavaSymbol extends JavaSymbol {

    Scope members;
    TypeJavaSymbol packageInfo;

    public PackageJavaSymbol(@Nullable String name, @Nullable JavaSymbol owner) {
      super(PCK, 0, name, owner);
    }

    Scope completedMembers() {
      complete();
      return members;
    }

    @Override
    public SymbolMetadataResolve metadata() {
      complete();
      return packageInfo == null ? super.metadata() : packageInfo.metadata();
    }
  }

  /**
   * Represents a class, interface, enum or annotation type.
   */
  public static class TypeJavaSymbol extends JavaSymbol implements TypeSymbol {

    private String bytecodeName = null;
    private String fullyQualifiedName;
    Scope members;
    Scope typeParameters;
    List<TypeVariableJavaType> typeVariableTypes;
    ClassTree declaration;
    private final String internalName;
    private final Multiset<String> internalNames = HashMultiset.create();
    private Set<ClassJavaType> superTypes;
    private Set<ClassJavaType> interfaces;

    public TypeJavaSymbol(int flags, String name, JavaSymbol owner) {
      super(TYP, flags, name, owner);
      this.type = new ClassJavaType(this);
      this.typeVariableTypes = new ArrayList<>();
      if (owner.isMethodSymbol()) {
        // declaration of a class or an anonymous class in a method
        internalName = ((TypeJavaSymbol) owner.owner).registerClassInternalName(name);
      } else if (owner.isTypeSymbol() && name.isEmpty()) {
        // anonymous class in a field
        internalName = ((TypeJavaSymbol) owner).registerClassInternalName("");
      } else {
        internalName = name;
      }
    }

    public TypeJavaSymbol(int flags, String name, TypeJavaSymbol owner, String bytecodeName) {
      this(flags, name, owner);
      this.bytecodeName = bytecodeName;
    }

    private String registerClassInternalName(String name) {
      internalNames.add(name);
      return internalNames.count(name) + name;
    }

    String getInternalName() {
      return internalName;
    }

    public void addTypeParameter(TypeVariableJavaType typeVariableType) {
      typeVariableTypes.add(typeVariableType);
    }

    @Nullable
    public JavaType getSuperclass() {
      complete();
      return ((ClassJavaType) type).supertype;
    }

    public List<JavaType> getInterfaces() {
      complete();
      return ((ClassJavaType) type).interfaces;
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
      if (bytecodeName != null) {
        return bytecodeName;
      }
      if(fullyQualifiedName == null) {
        String newQualification = "";
        if (owner.isPackageSymbol()) {
          if (!owner.name.isEmpty()) {
            newQualification = owner.name + ".";
          }
        } else if (owner.isTypeSymbol()) {
          newQualification = owner.type.fullyQualifiedName() + "$";
        } else if (owner.isMethodSymbol()) {
          newQualification = owner.owner.type().fullyQualifiedName() + "$";
        } else {
          throw new IllegalStateException("" + owner);
        }
        fullyQualifiedName = newQualification + getInternalName();
      }
      return fullyQualifiedName;
    }

    public Set<ClassJavaType> directSuperTypes() {
      ImmutableSet.Builder<ClassJavaType> types = ImmutableSet.builder();
      ClassJavaType superClassType = (ClassJavaType) this.superClass();
      if(superClassType != null) {
        types.add(superClassType);
      }
      for (JavaType interfaceType : getInterfaces()) {
        ClassJavaType classType = (ClassJavaType) interfaceType;
        types.add(classType);
      }
      return types.build();
    }

    /**
     * Includes superclass and super interface hierarchy.
     * @return list of classTypes.
     */
    public Set<ClassJavaType> superTypes() {
      if (superTypes == null) {
        ImmutableSet.Builder<ClassJavaType> types = ImmutableSet.builder();
        ClassJavaType superClassType = (ClassJavaType) this.superClass();
        types.addAll(this.interfacesOfType());
        while (superClassType != null) {
          types.add(superClassType);
          TypeJavaSymbol superClassSymbol = superClassType.getSymbol();
          types.addAll(superClassSymbol.interfacesOfType());
          superClassType = (ClassJavaType) superClassSymbol.superClass();
        }
        superTypes = types.build();
      }
      return superTypes;
    }

    private Set<ClassJavaType> interfacesOfType() {
      if (interfaces == null) {
        Deque<ClassJavaType> todo = getInterfaces().stream().map(ClassJavaType.class::cast).distinct().collect(Collectors.toCollection(LinkedList::new));
        Set<ClassJavaType> builder = new LinkedHashSet<>();
        while (!todo.isEmpty()) {
          ClassJavaType classType = todo.pop();
          if(classType == type) {
            continue;
          }
          if (builder.add(classType)) {
            classType.symbol.getInterfaces().forEach(t -> todo.addLast((ClassJavaType) t));
          }
        }
        interfaces = builder;
      }
      return interfaces;
    }

    @Override
    public String toString() {
      return name;
    }

    @Override
    public Type superClass() {
      return getSuperclass();
    }

    @Override
    public List<Type> interfaces() {
      return Lists.<Type>newArrayList(getInterfaces());
    }

    @Override
    public Collection<Symbol> memberSymbols() {
      return Lists.<Symbol>newArrayList(members().scopeSymbols());
    }

    @Override
    public Collection<Symbol> lookupSymbols(String name) {
      return Lists.<Symbol>newArrayList(members().lookup(name));
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

    @Nullable
    private final Object value;

    public VariableJavaSymbol(int flags, String name, JavaSymbol owner) {
      this(flags, name, owner, null);
    }

    public VariableJavaSymbol(int flags, String name, JavaSymbol owner, @Nullable Object value) {
      super(VAR, flags, name, owner);
      this.value = value;
    }

    public VariableJavaSymbol(int flags, String name, JavaType type, JavaSymbol owner) {
      this(flags, name, type, owner, null);
    }

    public VariableJavaSymbol(int flags, String name, JavaType type, JavaSymbol owner, @Nullable Object value) {
      super(VAR, flags, name, owner);
      this.type = type;
      this.value = value;
    }

    @Override
    public VariableTree declaration() {
      return declaration;
    }

    public Optional<Object> constantValue() {
      if (Flags.isFlagged(flags, Flags.STATIC) && Flags.isFlagged(flags, Flags.FINAL)) {
        if (value != null && type.is("boolean")) {
          return Optional.of(Integer.valueOf(1).equals(value) ? Boolean.TRUE : Boolean.FALSE);
        }
        return Optional.ofNullable(value);
      }
      return Optional.empty();
    }

    @Override
    public String toString() {
      return String.format("%s#%s", owner().name(), name());
    }
  }

  /**
   * Represents a method, constructor or initializer (static or instance).
   */
  public static class MethodJavaSymbol extends JavaSymbol implements MethodSymbol {

    TypeJavaSymbol returnType;
    Scope parameters;
    Scope typeParameters;
    List<TypeVariableJavaType> typeVariableTypes;
    MethodTree declaration;
    Object defaultValue;
    String desc;
    String signature;

    public MethodJavaSymbol(int flags, String name, JavaType type, JavaSymbol owner) {
      super(MTH, flags, name, owner);
      super.type = type;
      this.returnType = ((MethodJavaType) type).resultType.symbol;
      this.typeVariableTypes = new ArrayList<>();
    }

    public MethodJavaSymbol(int flags, String name, JavaSymbol owner) {
      super(MTH, flags, name, owner);
      this.typeVariableTypes = new ArrayList<>();
    }

    @Override
    public String signature() {
      if (signature == null) {
        signature = "";
        if (owner != null) {
          signature += owner.getType().fullyQualifiedName();
        }
        signature += "#" + name + desc();
      }
      return signature;
    }

    private String desc() {
      if(desc == null) {
        org.objectweb.asm.Type ret = returnType == null ? org.objectweb.asm.Type.VOID_TYPE : toAsmType(((MethodJavaType) super.type).resultType);
        org.objectweb.asm.Type[] argTypes = new org.objectweb.asm.Type[0];
        if(super.type != null) {
          argTypes = getParametersTypes().stream().map(MethodJavaSymbol::toAsmType).toArray(org.objectweb.asm.Type[]::new);
        }
        desc = org.objectweb.asm.Type.getMethodDescriptor(ret, argTypes);
      }
      return desc;
    }

    private static org.objectweb.asm.Type toAsmType(JavaType javaType) {
      switch (javaType.tag) {
        case JavaType.BYTE:
          return org.objectweb.asm.Type.BYTE_TYPE;
        case JavaType.CHAR:
          return org.objectweb.asm.Type.CHAR_TYPE;
        case JavaType.SHORT:
          return org.objectweb.asm.Type.SHORT_TYPE;
        case JavaType.INT:
          return org.objectweb.asm.Type.INT_TYPE;
        case JavaType.LONG:
          return org.objectweb.asm.Type.LONG_TYPE;
        case JavaType.FLOAT:
          return org.objectweb.asm.Type.FLOAT_TYPE;
        case JavaType.DOUBLE:
          return org.objectweb.asm.Type.DOUBLE_TYPE;
        case JavaType.BOOLEAN:
          return org.objectweb.asm.Type.BOOLEAN_TYPE;
        case JavaType.VOID:
          return org.objectweb.asm.Type.VOID_TYPE;
        case JavaType.CLASS:
        case JavaType.UNKNOWN:
          return org.objectweb.asm.Type.getObjectType(Convert.bytecodeName(javaType.fullyQualifiedName()));
        case JavaType.ARRAY:
          JavaType element = ((ArrayJavaType) javaType).elementType;
          return org.objectweb.asm.Type.getObjectType("["+toAsmType(element).getDescriptor());
        case JavaType.PARAMETERIZED:
        case JavaType.TYPEVAR:
          return toAsmType(javaType.erasure());
        default:
          throw new IllegalStateException("Unexpected java type tag "+javaType.tag);
      }
    }

    public TypeJavaSymbol getReturnType() {
      return returnType;
    }

    public Scope getParameters() {
      return parameters;
    }

    private List<JavaType> getParametersTypes() {
      Preconditions.checkState(super.type != null);
      return ((MethodJavaType) super.type).argTypes;
    }

    public Scope typeParameters() {
      return typeParameters;
    }

    public void setMethodType(MethodJavaType methodType) {
      super.type = methodType;
      if (methodType.resultType != null) {
        this.returnType = methodType.resultType.symbol;
      }
    }

    @Override
    @Nullable
    public MethodJavaSymbol overriddenSymbol() {
      if (isStatic()) {
        return null;
      }
      TypeJavaSymbol enclosingClass = enclosingClass();
      boolean unknownFound = false;
      for (ClassJavaType superType : enclosingClass.superTypes()) {
        MethodJavaSymbol overridden = overriddenSymbolFrom(superType);
        if (overridden != null) {
          if (!overridden.isUnknown()) {
            return overridden;
          } else {
            unknownFound = true;
          }
        }
      }
      if (unknownFound) {
        return Symbols.unknownMethodSymbol;
      }
      return null;
    }

    @Nullable
    private MethodJavaSymbol overriddenSymbolFrom(ClassJavaType classType) {
      if (classType.isUnknown()) {
        return Symbols.unknownMethodSymbol;
      }
      boolean unknownFound = false;
      List<JavaSymbol> symbols = classType.getSymbol().members().lookup(name);
      for (JavaSymbol overrideSymbol : symbols) {
        if (overrideSymbol.isKind(JavaSymbol.MTH) && !overrideSymbol.isStatic()) {
          MethodJavaSymbol methodJavaSymbol = (MethodJavaSymbol) overrideSymbol;
          if (canOverride(methodJavaSymbol)) {
            Boolean overriding = checkOverridingParameters(methodJavaSymbol, classType);
            if (overriding == null) {
              if (!unknownFound) {
                unknownFound = true;
              }
            } else if (overriding) {
              return methodJavaSymbol;
            }
          }
        }
      }
      if (unknownFound) {
        return Symbols.unknownMethodSymbol;
      }
      return null;
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

    @CheckForNull
    public Boolean checkOverridingParameters(MethodJavaSymbol overridee, ClassJavaType classType) {
      // same number and type of formal parameters
      if (getParametersTypes().size() != overridee.getParametersTypes().size()) {
        return false;
      }
      for (int i = 0; i < getParametersTypes().size(); i++) {
        JavaType paramOverrider = getParametersTypes().get(i);
        if (paramOverrider.isUnknown()) {
          // FIXME : complete symbol table should not have unknown types and generics should be handled properly for this.
          return null;
        }
        // Generics type should have same erasure see JLS8 8.4.2

        JavaType overrideeType = overridee.getParametersTypes().get(i);
        if (classType.isParameterized()) {
          overrideeType = ((ParametrizedTypeJavaType) classType).typeSubstitution.substitutedType(overrideeType);
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

    public boolean isConstructor() {
      return "<init>".equals(name);
    }

    public void addTypeParameter(TypeVariableJavaType typeVariableType) {
      typeVariableTypes.add(typeVariableType);
    }

    @Override
    public List<Type> parameterTypes() {
      return Lists.<Type>newArrayList(getParametersTypes());
    }

    @Override
    public TypeSymbol returnType() {
      return returnType;
    }

    @Override
    public List<Type> thrownTypes() {
      return Lists.<Type>newArrayList(((MethodJavaType) super.type).thrown);
    }

    @Override
    public MethodTree declaration() {
      return declaration;
    }

    public boolean isOverridable() {
      return !(isPrivate() || isStatic() || isFinal() || owner().isFinal());
    }

    public boolean isParametrized() {
      return !typeVariableTypes.isEmpty();
    }

    @Override
    public String toString() {
      if (isUnknown()) {
        return "!unknownOwner!#!unknownMethod!()";
      } else if (owner.isUnknown()) {
        return String.format("!unknownOwner!#%s()", name);
      }
      return String.format("%s#%s()", owner.name, name);
    }

    @CheckForNull
    public Object defaultValue() {
      return defaultValue;
    }
  }

  /**
   * Represents type variable of a parametrized type ie: T in class Foo<T>{}
   */
  public static class TypeVariableJavaSymbol extends TypeJavaSymbol {
    public TypeVariableJavaSymbol(String name, JavaSymbol owner) {
      super(0, name, owner);
      this.type = new TypeVariableJavaType(this);
      this.members = new Scope(this);
    }

    @Override
    @Nullable
    public JavaType getSuperclass() {
      JavaType firstBound = bounds().get(0);
      if (!firstBound.symbol().isInterface()) {
        return firstBound;
      }
      return getObjectType(firstBound);
    }

    private static JavaType getObjectType(JavaType type) {
      JavaType superClass = (JavaType) type.symbol().superClass();
      if (superClass == null) {
        return type;
      }
      return getObjectType(superClass);
    }

    private List<JavaType> bounds() {
      return new ArrayList<>(((TypeVariableJavaType) type).bounds);
    }

    @Override
    public List<JavaType> getInterfaces() {
      List<JavaType> bounds = bounds();
      if (bounds.get(0).symbol().isInterface()) {
        return bounds;
      }
      return bounds.subList(1, bounds.size());
    }

    @Override
    public ClassTree declaration() {
      // FIXME: declaration should not be ClassTree: a type Variable is declared by an identifier with bounds
      // This probably implies to refactor this class to not inherit form TypeJavaSymbol and/or to implement its dedicated interface
      return null;
    }

    @Override
    public String getFullyQualifiedName() {
      return name;
    }

    @Override
    String getInternalName() {
      throw new UnsupportedOperationException();
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

  public static class WildcardSymbol extends TypeJavaSymbol {

    public WildcardSymbol(String name) {
      super(0, name, Symbols.unknownSymbol);
      this.members = new Scope(this);
    }

    @Override
    @Nullable
    public ClassTree declaration() {
      return null;
    }

    @Override
    @Nullable
    public JavaType getSuperclass() {
      return null;
    }

    @Override
    public List<JavaType> getInterfaces() {
      return Collections.emptyList();
    }

    @Override
    public String getFullyQualifiedName() {
      return name;
    }

    @Override
    String getInternalName() {
      throw new UnsupportedOperationException();
    }
  }

}
