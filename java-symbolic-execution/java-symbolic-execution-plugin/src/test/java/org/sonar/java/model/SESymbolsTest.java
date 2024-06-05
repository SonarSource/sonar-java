/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import org.junit.jupiter.api.Test;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.se.utils.JParserTestUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SESymbolsTest {

  @Test
  void unknown_type() {
    Type unknownType = SESymbols.unknownType;

    assertThat(unknownType.isUnknown()).isTrue();
    assertThat(unknownType).isEqualTo(SESymbols.unknownType);

    assertThat(unknownType.is("!Unknown!")).isFalse();
    assertThat(unknownType.isSubtypeOf("!Unknown!")).isFalse();
    assertThat(unknownType.isSubtypeOf(SESymbols.unknownType)).isFalse();

    assertThat(unknownType.isArray()).isFalse();
    assertThat(unknownType.isClass()).isFalse();
    assertThat(unknownType.isVoid()).isFalse();
    assertThat(unknownType.isPrimitive()).isFalse();
    assertThat(unknownType.isPrimitive(Type.Primitives.BOOLEAN)).isFalse();
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

    assertThat(unknownType.symbol()).isEqualTo(SESymbols.unknownTypeSymbol);
    assertThat(unknownType.erasure()).isEqualTo(SESymbols.unknownType);

    // since SonarJava 6.3
    assertThat(unknownType.typeArguments()).isEmpty();
    assertThat(unknownType.isParameterized()).isFalse();
  }

  @Test
  void empty_metadata() {
    SymbolMetadata metadata = SESymbols.EMPTY_METADATA;

    assertThat(metadata.annotations()).isEmpty();
    assertThat(metadata.isAnnotatedWith("whatever")).isFalse();
    assertThat(metadata.valuesForAnnotation("whatever")).isNull();

    // since SonarJava 7.6
    assertThat(metadata.nullabilityData().type()).isEqualTo(SymbolMetadata.NullabilityType.UNKNOWN);
    assertThat(metadata.nullabilityData().level()).isEqualTo(SymbolMetadata.NullabilityLevel.UNKNOWN);
    assertThat(metadata.nullabilityData(SymbolMetadata.NullabilityTarget.METHOD).type()).isEqualTo(SymbolMetadata.NullabilityType.UNKNOWN);
    assertThat(metadata.findAnnotationTree(mock(SymbolMetadata.AnnotationInstance.class))).isNull();
  }

  @Test
  void package_metadata_nullability_is_not_supported() {
    File file = new File("src/main/java/org/sonar/java/model/package-info.java");
    CompilationUnitTree tree = JParserTestUtils.parse(file);
    SymbolMetadata metadata = tree.packageDeclaration().packageName().symbolType().symbol().metadata();
    assertThat(metadata.nullabilityData().type()).isEqualTo(SymbolMetadata.NullabilityType.UNKNOWN);
    assertThat(metadata.nullabilityData(SymbolMetadata.NullabilityTarget.METHOD).type()).isEqualTo(SymbolMetadata.NullabilityType.UNKNOWN);
  }

  @Test
  void root_package_symbol() {
    Symbol rootPackage = SESymbols.rootPackage;

    assertThat(rootPackage.isUnknown()).isTrue();
    assertThat(rootPackage.name()).isEmpty();
    assertThat(rootPackage.owner()).isNull();
    assertThat(rootPackage.isPackageSymbol()).isTrue();
  }

  @Test
  void unknown_type_symbol() {
    Symbol.TypeSymbol unknownTypeSymbol = SESymbols.unknownTypeSymbol;

    assertCommonProperties(unknownTypeSymbol);
    assertThat(unknownTypeSymbol.name()).isEqualTo("!unknown!");
    assertThat(unknownTypeSymbol.owner()).isEqualTo(SESymbols.rootPackage);

    assertThat(unknownTypeSymbol.superClass()).isNull();
    assertThat(unknownTypeSymbol.interfaces()).isEmpty();
    assertThat(unknownTypeSymbol.memberSymbols()).isEmpty();
    assertThat(unknownTypeSymbol.lookupSymbols("whatever")).isEmpty();
    assertThat(unknownTypeSymbol.isAnnotation()).isFalse();
    assertThat(unknownTypeSymbol.outermostClass()).isEqualTo(SESymbols.unknownTypeSymbol);
    assertThat(unknownTypeSymbol.superTypes()).isEmpty();
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

    assertThat(unknownSymbol.metadata()).isEqualTo(SESymbols.EMPTY_METADATA);
    assertThat(unknownSymbol.type()).isEqualTo(SESymbols.unknownType);
    assertThat(unknownSymbol.enclosingClass()).isEqualTo(SESymbols.unknownTypeSymbol);
  }

  @Test
  void testIsAnnotation() {
    JSema sema = ((JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse("")).sema;
    Type objectType = sema.type(sema.resolveType("java.lang.Object"));
    JavaTree.CompilationUnitTreeImpl cu = (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse("@interface Anno { Unknown u; }");
    ClassTreeImpl anno = (ClassTreeImpl) cu.types().get(0);

    assertThat(anno.symbol().isAnnotation()).isTrue();

    assertThat(objectType.symbol().isAnnotation()).isFalse();

    VariableTreeImpl u = (VariableTreeImpl) anno.members().get(0);
    assertThat(u.type().symbolType().symbol().isAnnotation()).isFalse();
  }

}
