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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.CLASS;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.METHOD;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.PACKAGE;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel.VARIABLE;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType.NON_NULL;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType.STRONG_NULLABLE;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType.UNKNOWN;
import static org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType.WEAK_NULLABLE;

class JSymbolMetadataTest {

  private static final Path NULLABILITY_SOURCE_DIR = JParserTestUtils.CHECKS_TEST_DIR
    .resolve(Paths.get("src", "main", "java", "annotations", "nullability"));

  private static final Pattern NULLABILITY_ID_PATTERN = Pattern.compile("id\\d++" +
    "_type_(?<type>NO_ANNOTATION|UNKNOWN|STRONG_NULLABLE|WEAK_NULLABLE|NON_NULL)" +
    "(?:_level_(?<level>UNKNOWN|PACKAGE|CLASS|METHOD|VARIABLE))?" +
    "(?<meta>_meta)?" +
    "(?:_line_(?<line>empty|\\d++))?");

  @Test
  void should_convert_annotation_values() {
    JavaTree.CompilationUnitTreeImpl cu = test("import java.lang.annotation.*; @Target({ElementType.TYPE, ElementType.METHOD}) @interface A { }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    Object[] value = (Object[]) cu.sema.typeSymbol(c.typeBinding).metadata().annotations().get(0).values().get(0).value();
    assertThat(value).hasSize(2);
    assertThat(((JVariableSymbol) value[0]).isEnum()).isTrue();
    assertThat(((JVariableSymbol) value[1]).isEnum()).isTrue();
  }

  @Test
  void unknown_nullability() {
    JavaTree.CompilationUnitTreeImpl cu = test("class A {}");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    SymbolMetadata.NullabilityData nullabilityData = c.symbol().metadata().nullabilityData();
    assertThat(nullabilityData.isNonNull(NullabilityLevel.PACKAGE, false, false)).isFalse();
    assertThat(nullabilityData.annotation()).isNull();
    assertThat(nullabilityData.level()).isEqualTo(NullabilityLevel.UNKNOWN);
    assertThat(nullabilityData.declaration()).isNull();
  }

  @Test
  void parameter_unknown_annotation() {
    JavaTree.CompilationUnitTreeImpl cu = test("@Unknown class A {}");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    SymbolMetadata.NullabilityData nullabilityData = c.symbol().metadata().nullabilityData();
    assertThat(nullabilityData.isNonNull(NullabilityLevel.PACKAGE, false, false)).isFalse();
    assertThat(nullabilityData.annotation()).isNull();
    assertThat(nullabilityData.level()).isEqualTo(NullabilityLevel.UNKNOWN);
    assertThat(nullabilityData.declaration()).isNull();
  }

  @Test
  void placeholder_nullability() {
    JavaTree.CompilationUnitTreeImpl cu = test("class A { void m() {\"\".substring(1); } }");
    ClassTreeImpl c = (ClassTreeImpl) cu.types().get(0);
    MethodTreeImpl m = (MethodTreeImpl) c.members().get(0);
    ExpressionStatementTreeImpl es = (ExpressionStatementTreeImpl) m.block().body().get(0);
    MethodInvocationTreeImpl mit = (MethodInvocationTreeImpl) es.expression();
    Symbol.MethodSymbol methodSymbol = mit.symbol();
    Symbol symbol = methodSymbol.declarationParameters().get(0);

    SymbolMetadata.NullabilityData nullabilityData = symbol.metadata().nullabilityData();
    assertThat(nullabilityData.isNonNull(NullabilityLevel.PACKAGE, false, false)).isFalse();
    assertThat(nullabilityData.annotation()).isNull();
    assertThat(nullabilityData.type()).isEqualTo(NullabilityType.NO_ANNOTATION);
    assertThat(nullabilityData.level()).isEqualTo(NullabilityLevel.PACKAGE);
    assertThat(nullabilityData.declaration()).isNull();
  }

  @Test
  void non_compiling_annotations() throws IOException {
    assertNullability(
      JParserTestUtils.CHECKS_TEST_DIR
        .resolve(Paths.get("src","main","files","non-compiling","annotations","nullability","NullabilityAtClassLevel.java"))
    );
  }

  @Test
  void package_level_eclipse_jdt_non_null_by_default() throws IOException {
    assertNullability(
      NULLABILITY_SOURCE_DIR.resolve(Paths.get("eclipse_jdt_non_null_by_default", "NullabilityAtPackageLevel.java"))
    );
  }

  @Test
  void package_level_javax_non_null_by_default() throws IOException {
    assertNullability(
      NULLABILITY_SOURCE_DIR.resolve(Paths.get("javax_non_null_by_default", "NullabilityAtPackageLevel.java"))
    );
  }

  @Test
  void package_level_javax_nullable_by_default() throws IOException {
    assertNullability(
      NULLABILITY_SOURCE_DIR.resolve(Paths.get("javax_nullable_by_default", "NullabilityAtPackageLevel.java"))
    );
  }

  @Test
  void package_level_mongo_db_non_null_api() throws IOException {
    assertNullability(
      NULLABILITY_SOURCE_DIR.resolve(Paths.get("mongo_db_non_null_api", "NullabilityAtPackageLevel.java"))
    );
  }

  @Test
  void package_level_spring_non_null_api() throws IOException {
    assertNullability(
      NULLABILITY_SOURCE_DIR.resolve(Paths.get("spring_non_null_api", "NullabilityAtPackageLevel.java"))
    );
  }

  @Test
  void package_level_spring_non_null_fields() throws IOException {
    assertNullability(
      NULLABILITY_SOURCE_DIR.resolve(Paths.get("spring_non_null_fields", "NullabilityAtPackageLevel.java"))
    );
  }

  @Test
  void class_level_nullability() throws IOException {
    assertNullability(
      NULLABILITY_SOURCE_DIR.resolve(Paths.get("no_default", "NullabilityAtClassLevel.java"))
    );
  }

  @Test
  void method_level_nullability() throws IOException {
    assertNullability(
      NULLABILITY_SOURCE_DIR.resolve(Paths.get("no_default", "NullabilityAtMethodLevel.java"))
    );
  }

  @Test
  void variable_level_nullability() throws IOException {
    assertNullability(
      NULLABILITY_SOURCE_DIR.resolve(Paths.get("no_default", "NullabilityAtVariableLevel.java"))
    );
  }

  @Test
  void unsupported_nullability() throws IOException {
    assertNullability(
      NULLABILITY_SOURCE_DIR.resolve(Paths.get("no_default", "UnsupportedNullability.java"))
    );
  }

  @Test
  void meta_annotation_nullability() throws IOException {
    assertNullability(
      NULLABILITY_SOURCE_DIR.resolve(Paths.get("no_default", "NullabilityWithMetaAnnotation.java"))
    );
  }

  @Test
  void generics_nullability() throws IOException {
    Path sourceFile = NULLABILITY_SOURCE_DIR.resolve(Paths.get("no_default", "NullabilityWithGenerics.java"));
    CompilationUnitTree cut = JParserTestUtils.parse(sourceFile.toRealPath().toFile(), JParserTestUtils.checksTestClassPath());
    ClassTree classTree = (ClassTree) cut.types().get(0);
    MethodTree declaration = (MethodTree) classTree.members().get(0);

    SymbolMetadata.NullabilityData declarationData = declaration.symbol().declarationParameters().get(0).metadata().nullabilityData();
    assertThat(declarationData.isNullable(VARIABLE, false, false)).isTrue();

    MethodTree callDeclaration = (MethodTree) classTree.members().get(1);
    MethodInvocationTree invocationWithString = (MethodInvocationTree) ((ExpressionStatementTree) callDeclaration.block().body().get(0)).expression();
    MethodInvocationTree invocationWithInteger = (MethodInvocationTree) ((ExpressionStatementTree) callDeclaration.block().body().get(1)).expression();

    SymbolMetadata.NullabilityData invocation1ParamData = invocationWithString.symbol()
      .declarationParameters().get(0).metadata().nullabilityData();
    SymbolMetadata.NullabilityData invocation2ParamData = invocationWithInteger.symbol()
      .declarationParameters().get(0).metadata().nullabilityData();

    assertThat(declarationData)
      .isSameAs(invocation1ParamData)
      .isSameAs(invocation2ParamData);
  }

  @Nested
  class NullabilityDataTest {

    @Test
    void nullability_data_non_null() {
      SymbolMetadata.NullabilityData nonNullAtVariable =
        new JSymbolMetadata.JNullabilityData(NON_NULL, VARIABLE, null, null, false);

      assertThat(nonNullAtVariable.isNonNull(VARIABLE, true, false)).isTrue();
      assertThat(nonNullAtVariable.isStrongNullable(VARIABLE, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNullable(VARIABLE, true, false)).isFalse();
    }

    @Test
    void nullability_data_weak_nullable() {
      SymbolMetadata.NullabilityData nonNullAtVariable =
        new JSymbolMetadata.JNullabilityData(WEAK_NULLABLE, VARIABLE, null, null, false);

      assertThat(nonNullAtVariable.isNonNull(VARIABLE, true, false)).isFalse();
      assertThat(nonNullAtVariable.isStrongNullable(VARIABLE, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNullable(VARIABLE, true, false)).isTrue();
    }

    @Test
    void nullability_data_strong_nullable() {
      SymbolMetadata.NullabilityData nonNullAtVariable =
        new JSymbolMetadata.JNullabilityData(STRONG_NULLABLE, VARIABLE, null, null, false);

      assertThat(nonNullAtVariable.isNonNull(VARIABLE, true, false)).isFalse();
      assertThat(nonNullAtVariable.isStrongNullable(VARIABLE, true, false)).isTrue();
      // Strong nullable is a subset of nullable
      assertThat(nonNullAtVariable.isNullable(VARIABLE, true, false)).isTrue();
    }

    @Test
    void nullability_data_level_variable() {
      SymbolMetadata.NullabilityData nonNullAtVariable =
        new JSymbolMetadata.JNullabilityData(NON_NULL, VARIABLE, null, null, false);

      assertThat(nonNullAtVariable.isNonNull(VARIABLE, true, false)).isTrue();
      assertThat(nonNullAtVariable.isNonNull(METHOD, true, false)).isTrue();
      assertThat(nonNullAtVariable.isNonNull(CLASS, true, false)).isTrue();
      assertThat(nonNullAtVariable.isNonNull(PACKAGE, true, false)).isTrue();
    }

    @Test
    void nullability_data_level_method() {
      SymbolMetadata.NullabilityData nonNullAtVariable =
        new JSymbolMetadata.JNullabilityData(NON_NULL, METHOD, null, null, false);

      assertThat(nonNullAtVariable.isNonNull(VARIABLE, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(METHOD, true, false)).isTrue();
      assertThat(nonNullAtVariable.isNonNull(CLASS, true, false)).isTrue();
      assertThat(nonNullAtVariable.isNonNull(PACKAGE, true, false)).isTrue();
    }

    @Test
    void nullability_data_level_class() {
      SymbolMetadata.NullabilityData nonNullAtVariable =
        new JSymbolMetadata.JNullabilityData(NON_NULL, CLASS, null, null, false);

      assertThat(nonNullAtVariable.isNonNull(VARIABLE, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(METHOD, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(CLASS, true, false)).isTrue();
      assertThat(nonNullAtVariable.isNonNull(PACKAGE, true, false)).isTrue();
    }

    @Test
    void nullability_data_level_package() {
      SymbolMetadata.NullabilityData nonNullAtVariable =
        new JSymbolMetadata.JNullabilityData(NON_NULL, PACKAGE, null, null, false);

      assertThat(nonNullAtVariable.isNonNull(VARIABLE, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(METHOD, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(CLASS, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(PACKAGE, true, false)).isTrue();
    }

    @Test
    void nullability_data_ignore_meta_annotation() {
      SymbolMetadata.NullabilityData nonNullAtVariable =
        new JSymbolMetadata.JNullabilityData(NON_NULL, VARIABLE, null, null, true);

      assertThat(nonNullAtVariable.isNonNull(VARIABLE, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(METHOD, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(CLASS, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(PACKAGE, true, false)).isFalse();
    }

    @Test
    void nullability_data_do_not_ignore_meta_annotation() {
      SymbolMetadata.NullabilityData nonNullAtVariable =
        new JSymbolMetadata.JNullabilityData(NON_NULL, CLASS, null, null, true);

      assertThat(nonNullAtVariable.isNonNull(VARIABLE, false, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(METHOD, false, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(CLASS, false, false)).isTrue();
      assertThat(nonNullAtVariable.isNonNull(PACKAGE, false, false)).isTrue();
    }

    @Test
    void nullability_unknown_type() {
      SymbolMetadata.NullabilityData nonNullAtVariable =
        new JSymbolMetadata.JNullabilityData(UNKNOWN, VARIABLE, null, null, false);

      assertThat(nonNullAtVariable.isStrongNullable(VARIABLE, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNullable(VARIABLE, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(VARIABLE, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(METHOD, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(CLASS, true, false)).isFalse();
      assertThat(nonNullAtVariable.isNonNull(PACKAGE, true, false)).isFalse();
    }
  }

  void assertNullability(Path sourceFile) throws IOException {
    CompilationUnitTree cut = JParserTestUtils.parse(sourceFile.toRealPath().toFile(), JParserTestUtils.checksTestClassPath());
    List<Symbol> idSymbols = collectIdentifiers(cut).stream()
      .filter(identifier -> identifier.name().startsWith("id"))
      .map(IdentifierTree::symbol)
      .collect(Collectors.toList());
    assertThat(idSymbols).isNotEmpty();

    for (Symbol symbol : idSymbols) {
      Matcher matcher = NULLABILITY_ID_PATTERN.matcher(symbol.name());
      if (!matcher.matches()) {
        throw new IllegalArgumentException("Invalid identifier format: " + symbol.name());
      }
      NullabilityType expectedType = NullabilityType.valueOf(matcher.group("type"));
      String levelValue = matcher.group("level");
      NullabilityLevel expectedLevel = levelValue != null ? NullabilityLevel.valueOf(levelValue) : NullabilityLevel.UNKNOWN;
      String expectedLine = matcher.group("line");
      boolean metaAnnotation = matcher.group("meta") != null;

      assertNullability(symbol, expectedType, expectedLevel, expectedLine, metaAnnotation, "\nFile: " + sourceFile.toRealPath() + "\n");
    }
  }

  private static void assertNullability(Symbol symbol, NullabilityType expectedType,
                                        @Nullable NullabilityLevel expectedLevel, @Nullable String expectedLine,
                                        boolean metaAnnotation, String context) {

    String symbolContext = "for symbol: " + symbol.name() + " in " + context;
    SymbolMetadata.NullabilityData nullabilityData = symbol.metadata().nullabilityData();
    assertThat(nullabilityData.type()).describedAs(symbolContext).isEqualTo(expectedType);
    if (expectedLevel != null) {
      assertThat(nullabilityData.level()).describedAs(symbolContext).isEqualTo(expectedLevel);
    }
    if (expectedLine != null) {
      Tree declaration = nullabilityData.declaration();
      String actualLine = declaration != null ? Integer.toString(declaration.firstToken().range().start().line()) : "empty";
      assertThat(actualLine).describedAs(symbolContext).isEqualTo(expectedLine);
    }
    assertThat(nullabilityData.metaAnnotation()).describedAs(symbolContext + "Meta annotation data is incorrect").isEqualTo(metaAnnotation);
  }

  private static List<IdentifierTree> collectIdentifiers(Tree tree) {
    List<IdentifierTree> identifiers = new ArrayList<>();
    tree.accept(new BaseTreeVisitor() {
      @Override
      public void visitIdentifier(IdentifierTree tree) {
        identifiers.add(tree);
        super.visitIdentifier(tree);
      }
    });
    return identifiers;
  }

  private static JavaTree.CompilationUnitTreeImpl test(String source) {
    return (JavaTree.CompilationUnitTreeImpl) JParserTestUtils.parse(source);
  }

}
