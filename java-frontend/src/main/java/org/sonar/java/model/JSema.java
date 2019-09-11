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

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryType;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.lookup.AnnotationBinding;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JSema {

  private final AST ast;
  final Map<IBinding, Tree> declarations = new HashMap<>();
  final Map<IBinding, List<IdentifierTree>> usages = new HashMap<>();
  private final Map<ITypeBinding, JType> types = new HashMap<>();
  private final Map<IBinding, JSymbol> symbols = new HashMap<>();
  private final Map<IAnnotationBinding, JSymbolMetadata.JAnnotationInstance> annotations = new HashMap<>();

  JSema(AST ast) {
    this.ast = ast;
  }

  public JType type(ITypeBinding typeBinding) {
    return types.computeIfAbsent(typeBinding, k -> new JType(this, k));
  }

  JPackageSymbol packageSymbol(IPackageBinding packageBinding) {
    return (JPackageSymbol) symbols.computeIfAbsent(packageBinding, k -> new JPackageSymbol(this, (IPackageBinding) k));
  }

  public JTypeSymbol typeSymbol(ITypeBinding typeBinding) {
    return (JTypeSymbol) symbols.computeIfAbsent(typeBinding, k -> new JTypeSymbol(this, (ITypeBinding) k));
  }

  public JMethodSymbol methodSymbol(IMethodBinding methodBinding) {
    return (JMethodSymbol) symbols.computeIfAbsent(methodBinding, k -> new JMethodSymbol(this, (IMethodBinding) k));
  }

  public JVariableSymbol variableSymbol(IVariableBinding variableBinding) {
    return (JVariableSymbol) symbols.computeIfAbsent(variableBinding, k -> new JVariableSymbol(this, (IVariableBinding) k));
  }

  JSymbolMetadata.JAnnotationInstance annotation(IAnnotationBinding annotationBinding) {
    return annotations.computeIfAbsent(annotationBinding, k -> new JSymbolMetadata.JAnnotationInstance(this, k));
  }

  IBinding declarationBinding(IBinding binding) {
    switch (binding.getKind()) {
      case IBinding.TYPE:
        return ((ITypeBinding) binding).getTypeDeclaration();
      case IBinding.METHOD:
        return ((IMethodBinding) binding).getMethodDeclaration();
      case IBinding.VARIABLE:
        return ((IVariableBinding) binding).getVariableDeclaration();
      default:
        return binding;
    }
  }

  @Nullable
  ITypeBinding resolveType(String name) {
    int dimensions = 0;
    int end = name.length() - 1;
    while (name.charAt(end) == ']') {
      end -= 2;
      dimensions++;
    }
    name = name.substring(0, end + 1);

    ITypeBinding typeBinding = ast.resolveWellKnownType(name);
    if (typeBinding == null) {
      typeBinding = resolveType(ast, name);
      if (typeBinding == null) {
        return null;
      }
    }
    return dimensions == 0 ? typeBinding : typeBinding.createArrayType(dimensions);
  }

  @Nullable
  private static ITypeBinding resolveType(AST ast, String name) {
    // BindingResolver bindingResolver = ast.getBindingResolver();
    // ReferenceBinding referenceBinding = bindingResolver
    //   .lookupEnvironment()
    //   .getType(CharOperation.splitOn('.', fqn.toCharArray()));
    // return bindingResolver.getTypeBinding(referenceBinding);
    try {
      Method methodGetBindingResolver = ast.getClass()
        .getDeclaredMethod("getBindingResolver");
      methodGetBindingResolver.setAccessible(true);
      Object bindingResolver = methodGetBindingResolver.invoke(ast);

      Method methodLookupEnvironment = bindingResolver.getClass()
        .getDeclaredMethod("lookupEnvironment");
      methodLookupEnvironment.setAccessible(true);
      LookupEnvironment lookupEnvironment = (LookupEnvironment) methodLookupEnvironment.invoke(bindingResolver);

      ReferenceBinding referenceBinding = lookupEnvironment.getType(
        CharOperation.splitOn('.', name.toCharArray())
      );

      Method methodGetTypeBinding = bindingResolver.getClass()
        .getDeclaredMethod("getTypeBinding", TypeBinding.class);
      methodGetTypeBinding.setAccessible(true);
      return (ITypeBinding) methodGetTypeBinding.invoke(bindingResolver, referenceBinding);

    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  IAnnotationBinding[] resolvePackageAnnotations(String packageName) {
    // See org.eclipse.jdt.core.dom.PackageBinding#getAnnotations()
    try {
      Method methodGetBindingResolver = ast.getClass()
        .getDeclaredMethod("getBindingResolver");
      methodGetBindingResolver.setAccessible(true);
      Object bindingResolver = methodGetBindingResolver.invoke(ast);

      Method methodLookupEnvironment = bindingResolver.getClass()
        .getDeclaredMethod("lookupEnvironment");
      methodLookupEnvironment.setAccessible(true);
      LookupEnvironment lookupEnvironment = (LookupEnvironment) methodLookupEnvironment.invoke(bindingResolver);

      NameEnvironmentAnswer answer = lookupEnvironment.nameEnvironment.findType(
        TypeConstants.PACKAGE_INFO_NAME,
        CharOperation.splitOn('.', packageName.toCharArray())
      );
      if (answer == null) {
        return new IAnnotationBinding[0];
      }

      IBinaryType type = answer.getBinaryType();
      IBinaryAnnotation[] binaryAnnotations = type.getAnnotations();
      AnnotationBinding[] binaryInstances =
        BinaryTypeBinding.createAnnotations(binaryAnnotations, lookupEnvironment, type.getMissingTypeNames());
      AnnotationBinding[] allInstances =
        AnnotationBinding.addStandardAnnotations(binaryInstances, type.getTagBits(), lookupEnvironment);

      Method methodGetAnnotationInstance = bindingResolver.getClass()
        .getDeclaredMethod("getAnnotationInstance", AnnotationBinding.class);
      methodGetAnnotationInstance.setAccessible(true);

      IAnnotationBinding[] domInstances = new IAnnotationBinding[allInstances.length];
      for (int i = 0; i < allInstances.length; i++) {
        // FIXME can be null if annotation can not be resolved e.g. due to incomplete classpath
        domInstances[i] = (IAnnotationBinding) methodGetAnnotationInstance.invoke(bindingResolver, allInstances[i]);
      }
      return domInstances;

    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

}
