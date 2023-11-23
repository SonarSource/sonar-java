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

import java.io.File;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityTarget;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.semantic.Type.Primitives;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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
    assertThat(unknownType.primitiveWrapperType()).isNull();
    assertThat(unknownType.isPrimitiveWrapper()).isFalse();
    assertThat(unknownType.primitiveType()).isNull();
    assertThat(unknownType.isNullType()).isFalse();
    assertThat(unknownType.isTypeVar()).isFalse();
    assertThat(unknownType.isRawType()).isFalse();
    assertThat(unknownType.declaringType()).isEqualTo(unknownType);
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

    // since SonarJava 7.6
    assertThat(metadata.nullabilityData().type()).isEqualTo(NullabilityType.UNKNOWN);
    assertThat(metadata.nullabilityData().level()).isEqualTo(NullabilityLevel.UNKNOWN);
    assertThat(metadata.nullabilityData(NullabilityTarget.METHOD).type()).isEqualTo(NullabilityType.UNKNOWN);
    assertThat(metadata.findAnnotationTree(mock(AnnotationInstance.class))).isNull();
  }

  @Test
  void package_metadata_nullability_is_not_supported() {
    File file = new File("src/main/java/org/sonar/java/model/package-info.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);
    SymbolMetadata metadata = tree.packageDeclaration().packageName().symbolType().symbol().metadata();
    assertThat(metadata.nullabilityData().type()).isEqualTo(NullabilityType.UNKNOWN);
    assertThat(metadata.nullabilityData(NullabilityTarget.METHOD).type()).isEqualTo(NullabilityType.UNKNOWN);
  }

  @Test
  void metadata_with_unknown_symbol_and_unsupported_declaration() {
    Tree unsupportedDeclaration = mock(Tree.class);
    Symbol symbol = spy(Symbols.unknownSymbol);
    when(symbol.declaration()).thenReturn(unsupportedDeclaration);
    SymbolMetadata metadata = new JSymbolMetadata(JUtilsTest.SEMA, symbol, new IAnnotationBinding[0]);
    assertThat(metadata.findAnnotationTree(mock(AnnotationInstance.class))).isNull();
    assertThat(metadata.nullabilityData().type()).isEqualTo(NullabilityType.UNKNOWN);
    assertThat(metadata.nullabilityData(NullabilityTarget.METHOD).type()).isEqualTo(NullabilityType.UNKNOWN);
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
    SymbolMetadata metadata = unknownSymbol.metadata();
    assertThat(metadata.nullabilityData().type()).isEqualTo(NullabilityType.UNKNOWN);
    assertThat(metadata.nullabilityData(NullabilityTarget.METHOD).type()).isEqualTo(NullabilityType.UNKNOWN);
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
    assertThat(unknownTypeSymbol.isAnnotation()).isFalse();
    assertThat(unknownTypeSymbol.outermostClass()).isEqualTo(Symbols.unknownTypeSymbol);
    assertThat(unknownTypeSymbol.superTypes()).isEmpty();
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
    assertThat(unknownMethodSymbol.declarationParameters()).isEmpty();
    assertThat(unknownMethodSymbol.thrownTypes()).isEmpty();
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
