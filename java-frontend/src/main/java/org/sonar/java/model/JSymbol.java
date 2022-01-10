/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.model;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

abstract class JSymbol implements Symbol {

  protected final JSema sema;
  protected final IBinding binding;

  /**
   * Cache for {@link #hashCode()}.
   */
  private int hashCode;

  /**
   * Cache for {@link #owner()}.
   */
  private Symbol owner;

  /**
   * Cache for {@link #metadata()}.
   */
  private SymbolMetadata metadata;

  JSymbol(JSema sema, IBinding binding) {
    this.sema = Objects.requireNonNull(sema);
    this.binding = Objects.requireNonNull(binding);
  }

  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JSymbol)) {
      return false;
    }
    JSymbol other = (JSymbol) obj;
    if (this.binding.getKind() != other.binding.getKind()) {
      return false;
    }
    switch (this.binding.getKind()) {
      case IBinding.TYPE:
        return JType.areEqual(
          (ITypeBinding) this.binding,
          (ITypeBinding) other.binding
        );
      case IBinding.VARIABLE:
        return areEqualVariables(
          this,
          other
        );
      case IBinding.METHOD:
        return areEqualMethods(
          this,
          other
        );
      default:
        return super.equals(obj);
    }
  }

  private static boolean areEqualVariables(JSymbol thisVariableSymbol, JSymbol otherVariableSymbol) {
    IVariableBinding thisBinding = (IVariableBinding) thisVariableSymbol.binding;
    IVariableBinding otherBinding = (IVariableBinding) otherVariableSymbol.binding;
    return thisBinding.getVariableId() == otherBinding.getVariableId()
      && thisVariableSymbol.owner().equals(otherVariableSymbol.owner());
  }

  private static boolean areEqualMethods(JSymbol thisMethodSymbol, JSymbol otherMethodSymbol) {
    IMethodBinding thisBinding = (IMethodBinding) thisMethodSymbol.binding;
    IMethodBinding otherBinding = (IMethodBinding) otherMethodSymbol.binding;
    return thisMethodSymbol.name().equals(otherMethodSymbol.name())
      && thisMethodSymbol.owner().equals(otherMethodSymbol.owner())
      && Arrays.equals(thisBinding.getParameterTypes(), otherBinding.getParameterTypes())
      && Arrays.equals(thisBinding.getTypeParameters(), otherBinding.getTypeParameters())
      && Arrays.equals(thisBinding.getTypeArguments(), otherBinding.getTypeArguments());
  }

  @Override
  public final int hashCode() {
    if (hashCode == 0) {
      Symbol owner = owner();
      hashCode = owner == null ? 0 : (owner.hashCode() * 31);
      hashCode += name().hashCode();
    }
    return hashCode;
  }

  /**
   * @see JType#name()
   */
  @Override
  public final String name() {
    if (binding.getKind() == IBinding.METHOD && ((IMethodBinding) binding).isConstructor()) {
      return "<init>";
    }
    if (binding.getKind() == IBinding.TYPE && ((ITypeBinding) binding).isParameterizedType()) {
      // without names of parameters
      return ((ITypeBinding) binding).getErasure().getName();
    }
    return binding.getName();
  }

  /**
   * @see #enclosingClass()
   */
  @Override
  public final Symbol owner() {
    if (isUnknown()) {
      return Symbols.unknownSymbol;
    }
    if (owner == null) {
      owner = convertOwner();
    }
    return owner;
  }

  private Symbol convertOwner() {
    switch (binding.getKind()) {
      case IBinding.PACKAGE:
        return Symbols.rootPackage;
      case IBinding.TYPE:
        return typeOwner((ITypeBinding) binding);
      case IBinding.METHOD:
        return methodOwner((IMethodBinding) binding);
      case IBinding.VARIABLE:
        return variableOwner((IVariableBinding) binding);
      default:
        throw new IllegalStateException(unexpectedBinding());
    }
  }

  private String unexpectedBinding() {
    return "Unexpected binding Kind: " + binding.getKind();
  }

  private Symbol typeOwner(ITypeBinding typeBinding) {
    IMethodBinding declaringMethod = typeBinding.getDeclaringMethod();
    if (declaringMethod != null) {
      // local type
      return sema.methodSymbol(declaringMethod);
    }
    ITypeBinding declaringClass = typeBinding.getDeclaringClass();
    if (declaringClass != null) {
      // member type
      return sema.typeSymbol(declaringClass);
    }
    // top-level type
    return sema.packageSymbol(typeBinding.getPackage());
  }

  private Symbol methodOwner(IMethodBinding methodBinding) {
    return sema.typeSymbol(methodBinding.getDeclaringClass());
  }

  private Symbol variableOwner(IVariableBinding variableBinding) {
    IMethodBinding declaringMethod = variableBinding.getDeclaringMethod();
    if (declaringMethod != null) {
      // local variable
      return sema.methodSymbol(declaringMethod);
    }
    ITypeBinding declaringClass = variableBinding.getDeclaringClass();
    if (declaringClass != null) {
      // field
      return sema.typeSymbol(declaringClass);
    }
    Tree node = sema.declarations.get(variableBinding);
    if (node == null) {
      // array.length
      return Symbols.unknownSymbol;
    }
    boolean initializerBlock = false;
    boolean staticInitializerBlock = false;
    while (true) {
      node = node.parent();
      switch (node.kind()) {
        case CLASS:
        case RECORD:
        case ENUM:
          JTypeSymbol typeSymbol = sema.typeSymbol(((ClassTreeImpl) node).typeBinding);
          if (initializerBlock) {
            return sema.initializerBlockSymbol(typeSymbol);
          }
          if (staticInitializerBlock) {
            return sema.staticInitializerBlockSymbol(typeSymbol);
          }
          // Record parameters
          return typeSymbol;
        case INITIALIZER:
          initializerBlock = true;
          break;
        case STATIC_INITIALIZER:
          staticInitializerBlock = true;
          break;
        case METHOD:
        case CONSTRUCTOR:
          // local variable declaration in recovered method
          // and recovered methods do not have bindings
          return Symbols.unknownMethodSymbol;
        default:
          // continue exploring parent
          break;
      }
    }
  }

  @Override
  public final Type type() {
    switch (binding.getKind()) {
      case IBinding.TYPE:
        return sema.type((ITypeBinding) binding);
      case IBinding.VARIABLE:
        ITypeBinding variableType = ((IVariableBinding) binding).getType();
        return variableType != null ? sema.type(variableType) : Symbols.unknownType;
      case IBinding.PACKAGE:
      case IBinding.METHOD:
        return Symbols.unknownType;
      default:
        throw new IllegalStateException(unexpectedBinding());
    }
  }

  @Override
  public final boolean isVariableSymbol() {
    return binding.getKind() == IBinding.VARIABLE;
  }

  @Override
  public final boolean isTypeSymbol() {
    return !isUnknown() && binding.getKind() == IBinding.TYPE;
  }

  @Override
  public final boolean isMethodSymbol() {
    return binding.getKind() == IBinding.METHOD;
  }

  @Override
  public final boolean isPackageSymbol() {
    return binding.getKind() == IBinding.PACKAGE;
  }

  @Override
  public final boolean isStatic() {
    return Modifier.isStatic(binding.getModifiers());
  }

  @Override
  public final boolean isFinal() {
    return Modifier.isFinal(binding.getModifiers());
  }

  @Override
  public final boolean isEnum() {
    switch (binding.getKind()) {
      case IBinding.TYPE:
        return ((ITypeBinding) binding).isEnum();
      case IBinding.VARIABLE:
        return ((IVariableBinding) binding).isEnumConstant();
      default:
        return false;
    }
  }

  @Override
  public final boolean isInterface() {
    return binding.getKind() == IBinding.TYPE
      && ((ITypeBinding) binding).isInterface();
  }

  @Override
  public final boolean isAbstract() {
    return Modifier.isAbstract(binding.getModifiers());
  }

  @Override
  public final boolean isPublic() {
    return Modifier.isPublic(binding.getModifiers());
  }

  @Override
  public final boolean isPrivate() {
    return Modifier.isPrivate(binding.getModifiers());
  }

  @Override
  public final boolean isProtected() {
    return Modifier.isProtected(binding.getModifiers());
  }

  @Override
  public final boolean isPackageVisibility() {
    return !isPublic() && !isPrivate() && !isProtected();
  }

  @Override
  public final boolean isDeprecated() {
    return binding.isDeprecated();
  }

  @Override
  public final boolean isVolatile() {
    return Modifier.isVolatile(binding.getModifiers());
  }

  @Override
  public final boolean isUnknown() {
    return binding.isRecovered();
  }

  @Override
  public final SymbolMetadata metadata() {
    if (metadata == null) {
      try {
        metadata = convertMetadata();
      } catch (RuntimeException e) {
        // ECJ raises exception in rare occasions, when it is the case, we don't want to prevent the whole analysis of the file
        metadata = Symbols.EMPTY_METADATA;
      }
    }
    return metadata;
  }

  private SymbolMetadata convertMetadata() {
    switch (binding.getKind()) {
      case IBinding.PACKAGE:
        return new JSymbolMetadata(sema, this, sema.resolvePackageAnnotations(binding.getName()));
      case IBinding.VARIABLE:
        ITypeBinding type = ((IVariableBinding) binding).getType();
        return new JSymbolMetadata(
          sema,
          this,
          type == null ? new IAnnotationBinding[0] : type.getTypeAnnotations(),
          binding.getAnnotations());
      case IBinding.METHOD:
        ITypeBinding returnType = ((IMethodBinding) binding).getReturnType();
        return new JSymbolMetadata(
          sema,
          this,
          returnType.getTypeAnnotations(),
          binding.getAnnotations());
      default:
        return new JSymbolMetadata(sema, this, binding.getAnnotations());
    }
  }

  /**
   * @see #owner()
   */
  @Nullable
  @Override
  public final TypeSymbol enclosingClass() {
    switch (binding.getKind()) {
      case IBinding.PACKAGE:
        return null;
      case IBinding.TYPE:
        return typeEnclosingClass((ITypeBinding) binding);
      case IBinding.METHOD:
        return methodEnclosingClass((IMethodBinding) binding);
      case IBinding.VARIABLE:
        return variableEnclosingClass((IVariableBinding) binding);
      default:
        throw new IllegalStateException(unexpectedBinding());
    }
  }

  private TypeSymbol typeEnclosingClass(ITypeBinding typeBinding) {
    ITypeBinding declaringClass = typeBinding.getDeclaringClass();
    if (declaringClass != null) {
      // nested (member or local) type
      return sema.typeSymbol(declaringClass);
    }
    // top-level type
    return (TypeSymbol) this;
  }

  private TypeSymbol methodEnclosingClass(IMethodBinding methodBinding) {
    ITypeBinding declaringClass = methodBinding.getDeclaringClass();
    return sema.typeSymbol(declaringClass);
  }

  private TypeSymbol variableEnclosingClass(IVariableBinding variableBinding) {
    ITypeBinding declaringClass = variableBinding.getDeclaringClass();
    if (declaringClass != null) {
      // field
      return sema.typeSymbol(declaringClass);
    }
    IMethodBinding declaringMethod = variableBinding.getDeclaringMethod();
    if (declaringMethod != null) {
      // local variable
      return sema.typeSymbol(declaringMethod.getDeclaringClass());
    }
    Tree node = sema.declarations.get(variableBinding);
    if (node == null) {
      // array.length
      return Symbols.unknownTypeSymbol;
    }
    do {
      node = node.parent();
      switch (node.kind()) {
        case CLASS:
        case RECORD:
        case ENUM:
          // variable declaration in a static or instance initializer
          // or local variable declaration in recovered method
          return sema.typeSymbol(((ClassTreeImpl) node).typeBinding);
        default:
          // continue exploring parent
          break;
      }
    } while (true);
  }

  @Override
  public final List<IdentifierTree> usages() {
    List<IdentifierTree> usages = sema.usages.get(JSema.declarationBinding(binding));
    return usages != null ? usages : Collections.emptyList();
  }

  @Nullable
  @Override
  public Tree declaration() {
    return sema.declarations.get(JSema.declarationBinding(binding));
  }

}
