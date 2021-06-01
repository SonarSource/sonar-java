/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.semantic.Type.Primitives;

import static org.assertj.core.api.Assertions.assertThat;

class SymbolsTest {

  @Test
  void unknown_type() {
    Type unknownType = Symbols.unknownType;

    assertThat(unknownType.isUnknown()).isTrue();
    assertThat(unknownType).isEqualTo(Symbols.unknownType);

    assertThat(unknownType.is("!Unknown!")).isFalse();
    assertThat(unknownType.isSubtypeOf("!Unknown!")).isFalse();
    assertThat(unknownType.isSubtypeOf(Symbols.unknownType)).isFalse();

    assertThat(unknownType.isArray()).isFalse();
    assertThat(unknownType.isClass()).isFalse();
    assertThat(unknownType.isVoid()).isFalse();
    assertThat(unknownType.isPrimitive()).isFalse();
    assertThat(unknownType.isPrimitive(Primitives.BOOLEAN)).isFalse();
    assertThat(unknownType.isNumerical()).isFalse();

    assertThat(unknownType.fullyQualifiedName()).isEqualTo("!Unknown!");
    assertThat(unknownType.name()).isEqualTo("!Unknown!");

    assertThat(unknownType.symbol()).isEqualTo(Symbols.unknownTypeSymbol);
    assertThat(unknownType.erasure()).isEqualTo(Symbols.unknownType);

    // since SonarJava 6.3
    assertThat(unknownType.typeArguments()).isEmpty();
    assertThat(unknownType.isParameterized()).isFalse();
  }

  @Test
  void empty_metadata() {
    SymbolMetadata metadata = Symbols.EMPTY_METADATA;

    assertThat(metadata.annotations()).isEmpty();
    assertThat(metadata.isAnnotatedWith("whatever")).isFalse();
    assertThat(metadata.valuesForAnnotation("whatever")).isNull();
  }

  @Test
  void root_package_symbol() {
    Symbol rootPackage = Symbols.rootPackage;

    assertThat(rootPackage.isUnknown()).isTrue();
    assertThat(rootPackage.name()).isEmpty();
    assertThat(rootPackage.owner()).isNull();
    assertThat(rootPackage.isPackageSymbol()).isTrue();
  }

  @Test
  void unknown_symbol() {
    Symbol unknownSymbol = Symbols.unknownSymbol;

    assertCommonProperties(unknownSymbol);
    assertThat(unknownSymbol.name()).isEqualTo("!unknown!");
    assertThat(unknownSymbol.owner()).isEqualTo(Symbols.rootPackage);
  }

  @Test
  void unknown_type_symbol() {
    Symbol.TypeSymbol unknownTypeSymbol = Symbols.unknownTypeSymbol;

    assertCommonProperties(unknownTypeSymbol);
    assertThat(unknownTypeSymbol.name()).isEqualTo("!unknown!");
    assertThat(unknownTypeSymbol.owner()).isEqualTo(Symbols.rootPackage);

    assertThat(unknownTypeSymbol.superClass()).isNull();
    assertThat(unknownTypeSymbol.interfaces()).isEmpty();
    assertThat(unknownTypeSymbol.memberSymbols()).isEmpty();
    assertThat(unknownTypeSymbol.lookupSymbols("whatever")).isEmpty();
  }

  @Test
  void unknown_method_symbol() {
    Symbol.MethodSymbol unknownMethodSymbol = Symbols.unknownMethodSymbol;

    assertCommonProperties(unknownMethodSymbol);
    assertThat(unknownMethodSymbol.name()).isEqualTo("!unknownMethod!");
    assertThat(unknownMethodSymbol.owner()).isEqualTo(Symbols.unknownTypeSymbol);

    assertThat(unknownMethodSymbol.signature()).isEqualTo("!unknownMethod!");
    assertThat(unknownMethodSymbol.returnType()).isEqualTo(Symbols.unknownTypeSymbol);
    assertThat(unknownMethodSymbol.parameterTypes()).isEmpty();
    assertThat(unknownMethodSymbol.thrownTypes()).isEmpty();
    assertThat(unknownMethodSymbol.overriddenSymbol()).isNull();
    assertThat(unknownMethodSymbol.overriddenSymbols()).isEmpty();
  }

  private static void assertCommonProperties(Symbol unknownSymbol) {
    assertThat(unknownSymbol.isUnknown()).isTrue();

    assertThat(unknownSymbol.isPackageSymbol()).isFalse();
    assertThat(unknownSymbol.isTypeSymbol()).isFalse();
    assertThat(unknownSymbol.isVariableSymbol()).isFalse();
    assertThat(unknownSymbol.isMethodSymbol()).isFalse();

    assertThat(unknownSymbol.isStatic()).isFalse();
    assertThat(unknownSymbol.isFinal()).isFalse();
    assertThat(unknownSymbol.isAbstract()).isFalse();
    assertThat(unknownSymbol.isDeprecated()).isFalse();
    assertThat(unknownSymbol.isVolatile()).isFalse();

    assertThat(unknownSymbol.isEnum()).isFalse();
    assertThat(unknownSymbol.isInterface()).isFalse();

    assertThat(unknownSymbol.isPublic()).isFalse();
    assertThat(unknownSymbol.isPrivate()).isFalse();
    assertThat(unknownSymbol.isProtected()).isFalse();
    assertThat(unknownSymbol.isPackageVisibility()).isFalse();

    assertThat(unknownSymbol.declaration()).isNull();
    assertThat(unknownSymbol.usages()).isEmpty();

    assertThat(unknownSymbol.metadata()).isEqualTo(Symbols.EMPTY_METADATA);
    assertThat(unknownSymbol.type()).isEqualTo(Symbols.unknownType);
    assertThat(unknownSymbol.enclosingClass()).isEqualTo(Symbols.unknownTypeSymbol);
  }
}
