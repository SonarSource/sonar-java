/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;

import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Predefined symbols.
 */
public class Symbols {

  private Symbols() {
    // Utility class
  }

  public static final SymbolMetadata EMPTY_METADATA = new SymbolMetadata() {

    @Override
    public boolean isAnnotatedWith(String fullyQualifiedName) {
      return false;
    }

    @Override
    @CheckForNull
    public List<AnnotationValue> valuesForAnnotation(String fullyQualifiedNameOfAnnotation) {
      return null;
    }

    @Override
    public List<AnnotationInstance> annotations() {
      return Collections.emptyList();
    }

    @Override
    public NullabilityData nullabilityData() {
      return JSymbolMetadata.unknownNullabilityAt(NullabilityLevel.UNKNOWN);
    }

    @Override
    public NullabilityData nullabilityData(NullabilityTarget level) {
      return JSymbolMetadata.unknownNullabilityAt(NullabilityLevel.UNKNOWN);
    }

    @Nullable
    @Override
    public AnnotationTree findAnnotationTree(AnnotationInstance annotationInstance) {
      return null;
    }
  };

  public abstract static class DefaultSymbol implements Symbol {

    @Override
    public boolean isVariableSymbol() {
      return false;
    }

    @Override
    public final boolean isTypeSymbol() {
      return false;
    }

    @Override
    public final boolean isMethodSymbol() {
      return false;
    }

    @Override
    public boolean isPackageSymbol() {
      return false;
    }

    @Override
    public final boolean isStatic() {
      return false;
    }

    @Override
    public boolean isFinal() {
      return false;
    }

    @Override
    public final boolean isEnum() {
      return false;
    }

    @Override
    public final boolean isInterface() {
      return false;
    }

    @Override
    public final boolean isAbstract() {
      return false;
    }

    @Override
    public final boolean isPublic() {
      return false;
    }

    @Override
    public final boolean isPrivate() {
      return false;
    }

    @Override
    public final boolean isProtected() {
      return false;
    }

    @Override
    public final boolean isPackageVisibility() {
      return false;
    }

    @Override
    public final boolean isDeprecated() {
      return false;
    }

    @Override
    public final boolean isVolatile() {
      return false;
    }

    @Override
    public SymbolMetadata metadata() {
      return EMPTY_METADATA;
    }
  }

  public static class UnknownSymbol extends DefaultSymbol {
    @Override
    public boolean isUnknown() {
      return true;
    }

    @Override
    public String name() {
      return "!unknown!";
    }

    @Override
    public Symbol owner() {
      return ROOT_PACKAGE;
    }

    @Override
    public final Type type() {
      return Type.UNKNOWN;
    }

    @Override
    public final Symbol.TypeSymbol enclosingClass() {
      return TypeSymbol.UNKNOWN_TYPE;
    }

    @Override
    public Tree declaration() {
      return null;
    }

    @Override
    public final List<IdentifierTree> usages() {
      return Collections.emptyList();
    }
  }

  public static final class UnkownTypeSymbol extends UnknownSymbol implements Symbol.TypeSymbol {
    @Override
    public ClassTree declaration() {
      return null;
    }

    @Override
    public Set<Type> superTypes() {
      return Collections.emptySet();
    }

    @Override
    public TypeSymbol outermostClass() {
      return TypeSymbol.UNKNOWN_TYPE;
    }

    @Override
    public boolean isAnnotation() {
      return false;
    }

    @Override
    public Type superClass() {
      return null;
    }

    @Override
    public List<Type> interfaces() {
      return Collections.emptyList();
    }

    @Override
    public Collection<Symbol> memberSymbols() {
      return Collections.emptyList();
    }

    @Override
    public Collection<Symbol> lookupSymbols(String name) {
      return Collections.emptyList();
    }
  }

  public static final class RootPackageSymbol extends UnknownSymbol {
    @Override
    public boolean isPackageSymbol() {
      return true;
    }

    @Override
    public String name() {
      return "";
    }

    @Override
    public Symbol owner() {
      return null;
    }
  }

  public static final class UnknownMethodSymbol extends UnknownSymbol implements Symbol.MethodSymbol {
    @Override
    public MethodTree declaration() {
      return null;
    }

    @Override
    public List<Type> parameterTypes() {
      return Collections.emptyList();
    }

    @Override
    public List<Symbol> declarationParameters() {
      return Collections.emptyList();
    }

    @Override
    public Symbol.TypeSymbol returnType() {
      return TypeSymbol.UNKNOWN_TYPE;
    }

    @Override
    public List<Type> thrownTypes() {
      return Collections.emptyList();
    }

    @Override
    public List<Symbol.MethodSymbol> overriddenSymbols() {
      return Collections.emptyList();
    }

    @Override
    public Symbol owner() {
      return TypeSymbol.UNKNOWN_TYPE;
    }

    @Override
    public String name() {
      return "!unknownMethod!";
    }

    @Override
    public String signature() {
      return "!unknownMethod!";
    }

    @Override
    public boolean isOverridable() {
      return false;
    }

    @Override
    public boolean isParametrizedMethod() {
      return false;
    }

    @Override
    public boolean isDefaultMethod() {
      return false;
    }

    @Override
    public boolean isSynchronizedMethod() {
      return false;
    }

    @Override
    public boolean isVarArgsMethod() {
      return false;
    }

    @Override
    public boolean isNativeMethod() {
      return false;
    }
  }

  public static final class UnknownType implements Type {
    @Override
    public boolean is(String fullyQualifiedName) {
      return false;
    }

    @Override
    public boolean isSubtypeOf(String fullyQualifiedName) {
      return false;
    }

    @Override
    public boolean isSubtypeOf(Type superType) {
      return false;
    }

    @Override
    public boolean isArray() {
      return false;
    }

    @Override
    public boolean isClass() {
      return false;
    }

    @Override
    public boolean isVoid() {
      return false;
    }

    @Override
    public boolean isPrimitive() {
      return false;
    }

    @Override
    public boolean isPrimitive(Primitives primitive) {
      return false;
    }

    @Override
    public boolean isPrimitiveWrapper() {
      return false;
    }

    @Nullable
    @Override
    public Type primitiveWrapperType() {
      return null;
    }

    @Nullable
    @Override
    public Type primitiveType() {
      return null;
    }

    @Override
    public boolean isNullType() {
      return false;
    }

    @Override
    public boolean isTypeVar() {
      return false;
    }

    @Override
    public boolean isRawType() {
      return false;
    }

    @Override
    public Type declaringType() {
      return this;
    }

    @Override
    public boolean isUnknown() {
      return true;
    }

    @Override
    public boolean isNumerical() {
      return false;
    }

    @Override
    public String fullyQualifiedName() {
      return "!Unknown!";
    }

    @Override
    public String name() {
      return "!Unknown!";
    }

    @Override
    public Symbol.TypeSymbol symbol() {
      return Symbol.TypeSymbol.UNKNOWN_TYPE;
    }

    @Override
    public Type erasure() {
      return UNKNOWN;
    }

    @Override
    public boolean isParameterized() {
      return false;
    }

    @Override
    public List<Type> typeArguments() {
      return Collections.emptyList();
    }
  }
}
