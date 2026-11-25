/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.TypeTree;

/** Finds the methods a method overrides, walking the supertypes of its class. */
final class OverriddenMethods {

  private final JMethodSymbol symbol;
  private final JSema sema;

  OverriddenMethods(JMethodSymbol symbol) {
    this.symbol = symbol;
    this.sema = symbol.sema;
  }

  List<MethodSymbol> find() {
    if (!(symbol.owner() instanceof JTypeSymbol typeSymbol) || symbol.isUnknown() || symbol.isStatic() || symbol.isPrivate()) {
      return List.of();
    }
    List<MethodSymbol> found = new ArrayList<>();
    Set<String> visited = new HashSet<>();
    if (typeSymbol.isInterface()) {
      match(found, sema.typeSymbol("java.lang.Object"), null);
      visited.add("java.lang.Object");
    }
    walk(found, visited, typeSymbol.superClass());
    for (Type iface : typeSymbol.interfaces()) {
      walk(found, visited, iface);
    }
    return found;
  }

  private void walk(List<MethodSymbol> found, Set<String> visited, @Nullable Type type) {
    if (type == null || type.isUnknown() || !visited.add(type.fullyQualifiedName())) {
      return;
    }
    if (type.symbol() instanceof JTypeSymbol parent) {
      match(found, parent, type);
      walk(found, visited, parent.superClass());
      for (Type iface : parent.interfaces()) {
        walk(found, visited, iface);
      }
    }
  }

  private void match(List<MethodSymbol> found, JTypeSymbol parent, @Nullable Type declaring) {
    for (Symbol member : parent.membersNamed(symbol.name())) {
      if (member instanceof JMethodSymbol method && !method.isStatic() && !method.isPrivate() && sameSignature(method, declaring)) {
        found.add(withWrittenTypeArgument(method, declaring));
        return;
      }
    }
  }

  /**
   * A method returning a class type variable, seen through a supertype written with an annotated type argument
   * ({@code Converter<String, @Nullable Boolean>}), carries that annotation as the nullability of its result.
   */
  private MethodSymbol withWrittenTypeArgument(JMethodSymbol library, @Nullable Type declaring) {
    if (declaring == null) {
      return library;
    }
    Integer position = returnedClassVariable(library, declaring);
    SymbolMetadata metadata = position == null ? null : writtenArgumentMetadata(declaring, position);
    if (metadata == null) {
      return library;
    }
    return (MethodSymbol) Proxy.newProxyInstance(MethodSymbol.class.getClassLoader(), new Class<?>[] {MethodSymbol.class}, (proxy, method, arguments) -> {
      if ("metadata".equals(method.getName()) && method.getParameterCount() == 0) {
        return metadata;
      }
      if ("equals".equals(method.getName()) && method.getParameterCount() == 1) {
        return proxy == arguments[0] || library.equals(arguments[0]);
      }
      try {
        return method.invoke(library, arguments);
      } catch (InvocationTargetException e) {
        throw e.getCause();
      }
    });
  }

  /** The annotations of the type argument at {@code position} of {@code declaring}: as written in this class, or as a supertype states it. */
  @Nullable
  private SymbolMetadata writtenArgumentMetadata(Type declaring, int position) {
    JTypeSymbol owner = (JTypeSymbol) symbol.owner();
    if (owner.declaration() instanceof ClassTree classTree) {
      List<TypeTree> supertypes = new ArrayList<>(classTree.superInterfaces());
      if (classTree.superClass() != null) {
        supertypes.add(classTree.superClass());
      }
      for (TypeTree supertype : supertypes) {
        if (supertype instanceof ParameterizedTypeTree parameterized && parameterized.symbolType().erasure().fullyQualifiedName().equals(declaring.erasure().fullyQualifiedName())
          && position < parameterized.typeArguments().size()
          && JSymbolMetadata.isAnnotatedType(parameterized.typeArguments().get(position))) {
          return JSymbolMetadata.ofTypeTree(sema, parameterized.typeArguments().get(position));
        }
      }
    }
    List<DeclaredAnnotation> declared = sema.annotationFacts().supertypeArgumentAnnotations(owner.type(), declaring, position);
    return declared.isEmpty() ? null : JSymbolMetadata.ofDeclared(sema, declared);
  }

  /** The position, among the type variables of its class, of the one the method returns. */
  @Nullable
  private Integer returnedClassVariable(JMethodSymbol method, Type declaring) {
    if (method.returnTypeParameter() != null) {
      return method.returnTypeParameter();
    }
    if (method.declaration() == null) {
      return sema.substitution().returnedClassVariable(method, declaring);
    }
    if (method.declaration() instanceof MethodTree tree && tree.returnType() instanceof IdentifierTree returned
      && method.owner().declaration() instanceof ClassTree parentClass) {
      for (int i = 0; i < parentClass.typeParameters().size(); i++) {
        if (parentClass.typeParameters().get(i).identifier().name().equals(returned.name())) {
          return i;
        }
      }
    }
    return null;
  }

  private boolean viaClassVariable(JMethodSymbol library, int index, Type declaring, String mine) {
    Type argument = sema.substitution().classVariableArgument(library, index, declaring);
    return argument != null && mine.equals(argument.erasure().fullyQualifiedName());
  }

  private boolean sameSignature(JMethodSymbol other, @Nullable Type declaring) {
    List<Type> theirs = other.parameterTypes();
    if (symbol.parameterTypes().size() != theirs.size()) {
      return false;
    }
    for (int i = 0; i < symbol.parameterTypes().size(); i++) {
      String mine = symbol.parameterTypes().get(i).erasure().fullyQualifiedName();
      boolean same = mine.equals(theirs.get(i).erasure().fullyQualifiedName())
        || (declaring != null && (mine.equals(sema.substitution().asMemberOf(theirs.get(i), declaring).erasure().fullyQualifiedName())
          || viaClassVariable(other, i, declaring, mine)));
      if (!same) {
        return false;
      }
    }
    return true;
  }
}
