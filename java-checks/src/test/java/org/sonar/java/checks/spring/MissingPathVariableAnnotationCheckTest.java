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
package org.sonar.java.checks.spring;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class MissingPathVariableAnnotationCheckTest {
  private static final String TEST_SOURCE_FILE = mainCodeSourcesPath("checks/spring/s6856/MissingPathVariableAnnotationCheck_PathVariable.java");
  private static final String TEST_SOURCE_FILE_MODEL_ATTRIBUTE = mainCodeSourcesPath("checks/spring/s6856/MissingPathVariableAnnotationCheck_ModelAttribute.java");
  private static final String SETTER_PROPERTIES_TEST_FILE = mainCodeSourcesPath("checks/spring/s6856/ExtractSetterPropertiesTestData.java");

  private static final JavaFileScanner check = new MissingPathVariableAnnotationCheck();
  private static final Map<String, Type> testTypes = new HashMap<>();

  @BeforeAll
  static void scanTestFile() {
    IssuableSubscriptionVisitor typeCollector = new IssuableSubscriptionVisitor() {
      @Override
      public java.util.List<Tree.Kind> nodesToVisit() {
        return java.util.List.of(Tree.Kind.CLASS);
      }

      @Override
      public void visitNode(Tree tree) {
        ClassTree classTree = (ClassTree) tree;
        Symbol.TypeSymbol symbol = classTree.symbol();
        testTypes.put(symbol.name(), symbol.type());
      }
    };

    CheckVerifier.newVerifier()
      .onFile(SETTER_PROPERTIES_TEST_FILE)
      .withCheck(typeCollector)
      .verifyNoIssues();
  }

  @Test
  void test_compiling() {
    CheckVerifier.newVerifier()
      .onFile(TEST_SOURCE_FILE)
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void test_model_attribute() {
    CheckVerifier.newVerifier()
      .onFile(TEST_SOURCE_FILE_MODEL_ATTRIBUTE)
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(TEST_SOURCE_FILE)
      .withCheck(check)
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void test_composed_request_mapping() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/spring/SpringComposedRequestMappingCheck.java")
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void test_pattern_parser_simple_pattern() {
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/my/path/"))
      .isEmpty();
  }

  @Test
  void test_pattern_parser_documentation_pattern() {
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/pages/t?st.html"))
      .isEmpty();
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/resources/*.png"))
      .isEmpty();
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/resources/**"))
      .isEmpty();
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/resources/{**}"))
      .isEmpty();
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/resources/{*path}"))
      .containsExactly("path");
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/resources/{filename:\\\\w+}.dat"))
      .containsExactly("filename");
  }

  @Test
  void test_pattern_parser_template_variables() {
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/{page}/{age}"))
      .containsExactly("page", "age");
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/{page}{age}"))
      .containsExactly("page", "age");
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/{a}{b}-{c}{d:...}${e}\\{f}}"))
      .containsExactly("a", "b", "c", "d", "e", "f");
  }

  @Test
  void test_pattern_parser_regexes() {
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("{page:[a-z]*}"))
      .containsExactly("page");
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("{page:${}}"))
      .containsExactly("page");
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("{page:{{}}}"))
      .containsExactly("page");
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("{page:{{}}{}}"))
      .containsExactly("page");
  }

  @Test
  void test_pattern_parser_errors() {
    assertThatThrownBy(() -> MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("{}"))
      .isInstanceOf(MissingPathVariableAnnotationCheck.DoNotReport.class);
    assertThatThrownBy(() -> MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("{*aaa"))
      .isInstanceOf(MissingPathVariableAnnotationCheck.DoNotReport.class);
    assertThatThrownBy(() -> MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("{xxx:"))
      .isInstanceOf(MissingPathVariableAnnotationCheck.DoNotReport.class);
    assertThatThrownBy(() -> MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/url/{"))
      .isInstanceOf(MissingPathVariableAnnotationCheck.DoNotReport.class);
    assertThatThrownBy(() -> MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("{x:{}"))
      .isInstanceOf(MissingPathVariableAnnotationCheck.DoNotReport.class);
    assertThatThrownBy(() -> MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("{x:\\{}"))
      .isInstanceOf(MissingPathVariableAnnotationCheck.DoNotReport.class);
    assertThatThrownBy(() -> MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("{x:{{{}}}"))
      .isInstanceOf(MissingPathVariableAnnotationCheck.DoNotReport.class);
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("provideExtractSetterPropertiesTestCases")
  void test_extractSetterProperties(String typeName, Set<String> expectedProperties, Set<String> unexpectedProperties) {
    Type type = testTypes.get(typeName);
    Set<String> properties = MissingPathVariableAnnotationCheck.extractSetterProperties(type);

    if (!expectedProperties.isEmpty()) {
      assertThat(properties).containsExactlyInAnyOrderElementsOf(expectedProperties);
    } else {
      assertThat(properties).isEmpty();
    }

    if (!unexpectedProperties.isEmpty()) {
      assertThat(properties).doesNotContainAnyElementsOf(unexpectedProperties);
    }
  }

  private static Stream<Arguments> provideExtractSetterPropertiesTestCases() {
    return Stream.of(
      Arguments.of(
        "ExplicitSetters",
        Set.of("name", "age", "active"),
        Set.of("invalid", "empty", "multiple", "private", "static")
      ),
      Arguments.of(
        "LombokData",
        Set.of("project", "year", "month"),
        Set.of("constant", "staticField")
      ),
      Arguments.of(
        "LombokClassLevelSetter",
        Set.of("firstName", "lastName"),
        Set.of("id", "count")
      ),
      Arguments.of(
        "LombokFieldLevelSetter",
        Set.of("email", "score"),
        Set.of("noSetter", "staticField")
      ),
      Arguments.of(
        "MixedSetters",
        Set.of("lombokField", "explicitField"),
        Set.of()
      ),
      Arguments.of(
        "NoSetters",
        Set.of(),
        Set.of("field")
      ),
      Arguments.of(
        "EmptyClass",
        Set.of(),
        Set.of()
      ),
      Arguments.of(
        "OnlyGetters",
        Set.of(),
        Set.of("name", "age")
      )
    );
  }

}
