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
package org.sonar.java.model;

import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.sonar.java.resolve.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@MethodsAreNonnullByDefault
public abstract class JSymbol implements Symbol {

  protected final Sema ast;
  protected final IBinding binding;

  JSymbol(Sema ast, IBinding binding) {
    this.ast = Objects.requireNonNull(ast);
    this.binding = Objects.requireNonNull(binding);
  }

  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public final String toString() {
    switch (binding.getKind()) {
      case IBinding.VARIABLE:
        return owner().name() + "#" + name();
      case IBinding.METHOD:
        return owner().name() + "#" + name() + "()";
      case IBinding.TYPE:
        return name();
      default:
        throw new UnsupportedOperationException("kind: " + binding.getKind());
    }
  }

  @Override
  public final String name() {
    if (binding.getKind() == IBinding.METHOD && ((IMethodBinding) binding).isConstructor()) {
      return "<init>";
    }
    if (binding.getKind() == IBinding.TYPE) {
      return ((ITypeBinding) binding).getErasure().getName();
    }
    return binding.getName();
  }

  @Override
  public final Symbol owner() {
    switch (binding.getKind()) {
      case IBinding.VARIABLE: {
        if ("length".equals(name()) && isPublic() && isFinal()) {
          // FIXME array length - supposed to be the array itself
          return Symbols.unknownSymbol;
        }
        IVariableBinding b = (IVariableBinding) binding;
        ITypeBinding declaringClass = b.getDeclaringClass();
        IMethodBinding declaringMethod = b.getDeclaringMethod();
        if (declaringClass == null && declaringMethod == null) {
          // variable in a static or instance initializer or local variable in recovered method
          // See HiddenFieldCheck
          Tree t = declaration();
          assert t != null; // TODO what about access to field in another file?
          while (true) {
            t = t.parent();
            switch (t.kind()) {
              case INITIALIZER:
              case STATIC_INITIALIZER:
                return ((ClassTree) t.parent()).symbol();
              case METHOD:
              case CONSTRUCTOR:
                // local variable
                return ((MethodTree) t).symbol();
            }
          }
        }
        if (declaringMethod != null) {
          // local variable
          return ast.methodSymbol(declaringMethod);
        }
        // field
        return ast.typeSymbol(b.getDeclaringClass());
      }
      case IBinding.METHOD: {
        IMethodBinding b = (IMethodBinding) binding;
        if (b.getDeclaringClass().isAnonymous() && b.isConstructor()) {
          // TODO
          // see constructor in FileHandlingCheck: new FileReader("") {}
          // and method of interface in HostnameVerifierImplementationCheck
          return ast.typeSymbol(b.getDeclaringClass().getSuperclass());
        }
        return ast.typeSymbol(b.getDeclaringClass());
      }
      case IBinding.TYPE: {
        ITypeBinding b = (ITypeBinding) binding;
        IMethodBinding declaringMethod = b.getDeclaringMethod();
        if (declaringMethod != null) {
          // local class
          return ast.methodSymbol(declaringMethod);
        }
        ITypeBinding declaringClass = b.getDeclaringClass();
        if (declaringClass == null) {
          // TODO e.g. owner of top-level classes should be b.getPackage() , otherwise NPE in UtilityClassWithPublicConstructorCheck ?
          return new JSymbol(ast, b.getPackage()) {
          };
        }
        return ast.typeSymbol(declaringClass);
      }
      default:
        throw new NotImplementedException("kind: " + binding.getKind());
    }
  }

  @Override
  public final Type type() {
    switch (binding.getKind()) {
      case IBinding.TYPE:
        return ast.type((ITypeBinding) binding);
      case IBinding.VARIABLE:
        return ast.type(((IVariableBinding) binding).getType());
      case IBinding.PACKAGE:
      case IBinding.METHOD:
//        // TODO METHOD in StandardCharsetsConstantsCheck , RedundantTypeCastCheck and MethodIdenticalImplementationsCheck , PACKAGE in InnerClassTooManyLinesCheck
        return null;
      default:
        throw new NotImplementedException("Kind: " + binding.getKind());
    }
  }

  @Override
  public final boolean isVariableSymbol() {
    return binding.getKind() == IBinding.VARIABLE;
  }

  @Override
  public final boolean isTypeSymbol() {
    if (binding.isRecovered()) {
      // TODO see SAMAnnotatedCheck
      return false;
    }
    return binding.getKind() == IBinding.TYPE;
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
  public boolean isStatic() {
    return Modifier.isStatic(binding.getModifiers());
  }

  @Override
  public final boolean isFinal() {
    return Modifier.isFinal(binding.getModifiers());
  }

  @Override
  public final boolean isEnum() {
    switch (binding.getKind()) {
      case IBinding.VARIABLE:
        return ((IVariableBinding) binding).isEnumConstant();
      case IBinding.TYPE:
        return ((ITypeBinding) binding).isEnum();
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
    return !isPublic() && !isProtected() && !isPrivate();
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

  @Nullable
  @Override
  public final TypeSymbol enclosingClass() {
    // FIXME Godin: can be incorrect
    switch (binding.getKind()) {
      case IBinding.PACKAGE:
        return null;
      case IBinding.TYPE:
        return ast.typeSymbol((ITypeBinding) binding);
      case IBinding.METHOD: {
        ITypeBinding declaringClass = ((IMethodBinding) binding).getDeclaringClass();
        if (declaringClass == null) {
          return null;
        }
        return ast.typeSymbol(declaringClass);
      }
      case IBinding.VARIABLE: {
        IVariableBinding b = (IVariableBinding) binding;
        ITypeBinding declaringClass = b.getDeclaringClass();
        if (declaringClass == null) {
          // local variable
          IMethodBinding declaringMethod = b.getDeclaringMethod();
          if (declaringMethod == null) {
            return Symbols.unknownSymbol;
          }
          return ast.typeSymbol(declaringMethod.getDeclaringClass());
        }
        return ast.typeSymbol(declaringClass);
      }
      default:
        throw new IllegalStateException("Kind: " + binding.getKind());
    }
  }

  @Override
  public final SymbolMetadata metadata() {
    IAnnotationBinding[] annotations;
    if (binding.getKind() == IBinding.PACKAGE) {
      annotations = JWorkarounds.resolvePackageAnnotations(ast.ast, binding.getName());
    } else {
      annotations = binding.getAnnotations();
    }
    return new JSymbolMetadata() {
      @Override
      public List<AnnotationInstance> annotations() {
        return Arrays.stream(annotations)
          .map(ast::annotation)
          .collect(Collectors.toList());
      }
    };
  }

  @Override
  public final List<IdentifierTree> usages() {
    List<IdentifierTree> usages = ast.usages.get(binding);
    return usages == null ? Collections.emptyList() : usages;
  }

  @Nullable
  @Override
  public Tree declaration() {
    return ast.declarations.get(binding);
  }

}
