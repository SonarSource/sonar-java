/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
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

import java.io.File;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
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
    Type unknownType = Type.UNKNOWN;

    assertThat(unknownType.isUnknown()).isTrue();
    assertThat(unknownType).isEqualTo(Type.UNKNOWN);

    assertThat(unknownType.is("!Unknown!")).isFalse();
    assertThat(unknownType.isSubtypeOf("!Unknown!")).isFalse();
    assertThat(unknownType.isSubtypeOf(Type.UNKNOWN)).isFalse();

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

    assertThat(unknownType.symbol()).isEqualTo(Symbol.TypeSymbol.UNKNOWN_TYPE);
    assertThat(unknownType.erasure()).isEqualTo(Type.UNKNOWN);

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
    Symbol symbol = spy(Symbol.UNKNOWN_SYMBOL);
    when(symbol.declaration()).thenReturn(unsupportedDeclaration);
    SymbolMetadata metadata = new JSymbolMetadata(JUtilsTest.SEMA, symbol, new IAnnotationBinding[0]);
    assertThat(metadata.findAnnotationTree(mock(AnnotationInstance.class))).isNull();
    assertThat(metadata.nullabilityData().type()).isEqualTo(NullabilityType.UNKNOWN);
    assertThat(metadata.nullabilityData(NullabilityTarget.METHOD).type()).isEqualTo(NullabilityType.UNKNOWN);
  }

  @Test
  void root_package_symbol() {
    Symbol rootPackage = Symbol.ROOT_PACKAGE;

    assertThat(rootPackage.isUnknown()).isTrue();
    assertThat(rootPackage.name()).isEmpty();
    assertThat(rootPackage.owner()).isNull();
    assertThat(rootPackage.isPackageSymbol()).isTrue();
  }

  @Test
  void unknown_symbol() {
    Symbol unknownSymbol = Symbol.UNKNOWN_SYMBOL;

    assertCommonProperties(unknownSymbol);
    assertThat(unknownSymbol.isMethodSymbol()).isEqualTo(false);
    assertThat(unknownSymbol.name()).isEqualTo("!unknown!");
    assertThat(unknownSymbol.owner()).isEqualTo(Symbol.ROOT_PACKAGE);
    SymbolMetadata metadata = unknownSymbol.metadata();
    assertThat(metadata.nullabilityData().type()).isEqualTo(NullabilityType.UNKNOWN);
    assertThat(metadata.nullabilityData(NullabilityTarget.METHOD).type()).isEqualTo(NullabilityType.UNKNOWN);
  }

  @Test
  void unknown_type_symbol() {
    Symbol.TypeSymbol unknownTypeSymbol = Symbol.TypeSymbol.UNKNOWN_TYPE;

    assertCommonProperties(unknownTypeSymbol);
    assertThat(unknownTypeSymbol.isMethodSymbol()).isEqualTo(false);
    assertThat(unknownTypeSymbol.name()).isEqualTo("!unknown!");
    assertThat(unknownTypeSymbol.owner()).isEqualTo(Symbol.ROOT_PACKAGE);

    assertThat(unknownTypeSymbol.superClass()).isNull();
    assertThat(unknownTypeSymbol.interfaces()).isEmpty();
    assertThat(unknownTypeSymbol.memberSymbols()).isEmpty();
    assertThat(unknownTypeSymbol.lookupSymbols("whatever")).isEmpty();
    assertThat(unknownTypeSymbol.isAnnotation()).isFalse();
    assertThat(unknownTypeSymbol.outermostClass()).isEqualTo(Symbol.TypeSymbol.UNKNOWN_TYPE);
    assertThat(unknownTypeSymbol.superTypes()).isEmpty();
  }

  @Test
  void unknown_method_symbol() {
    Symbol.MethodSymbol unknownMethodSymbol = Symbol.MethodSymbol.UNKNOWN_METHOD;

    assertCommonProperties(unknownMethodSymbol);
    assertThat(unknownMethodSymbol.isMethodSymbol()).isEqualTo(true);
    assertThat(unknownMethodSymbol.name()).isEqualTo("!unknownMethod!");
    assertThat(unknownMethodSymbol.owner()).isEqualTo(Symbol.TypeSymbol.UNKNOWN_TYPE);

    assertThat(unknownMethodSymbol.signature()).isEqualTo("!unknownMethod!");
    assertThat(unknownMethodSymbol.returnType()).isEqualTo(Symbol.TypeSymbol.UNKNOWN_TYPE);
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
    assertThat(unknownSymbol.enclosingClass()).isEqualTo(Symbol.TypeSymbol.UNKNOWN_TYPE);
    Type unknowType = unknownSymbol.type();
    assertThat(unknowType).isEqualTo(Type.UNKNOWN);
    assertThat(unknowType.isIntersectionType()).isFalse();
    assertThat(unknowType.getIntersectionTypes()).extracting(Type::fullyQualifiedName).containsExactly("!Unknown!");

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
