/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
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

  public static final Type unknownType = new UnknownType();

  public static final Symbol rootPackage = new RootPackageSymbol();
  public static final Symbol unknownSymbol = new UnknownSymbol();
  public static final Symbol.TypeSymbol unknownTypeSymbol = new UnkownTypeSymbol();
  public static final Symbol.MethodSymbol unknownMethodSymbol = new UnknownMethodSymbol();

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

  private static class UnknownSymbol extends DefaultSymbol {
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
      return rootPackage;
    }

    @Override
    public final Type type() {
      return unknownType;
    }

    @Override
    public final Symbol.TypeSymbol enclosingClass() {
      return unknownTypeSymbol;
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

  private static final class UnkownTypeSymbol extends UnknownSymbol implements Symbol.TypeSymbol {
    @Override
    public ClassTree declaration() {
      return null;
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

  private static final class RootPackageSymbol extends UnknownSymbol {
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

  private static final class UnknownMethodSymbol extends UnknownSymbol implements Symbol.MethodSymbol {
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
      return unknownTypeSymbol;
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
      return unknownTypeSymbol;
    }

    @Override
    public String name() {
      return "!unknownMethod!";
    }

    @Override
    public String signature() {
      return "!unknownMethod!";
    }
  }

  private static final class UnknownType implements Type {
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
      return unknownTypeSymbol;
    }

    @Override
    public Type erasure() {
      return unknownType;
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
