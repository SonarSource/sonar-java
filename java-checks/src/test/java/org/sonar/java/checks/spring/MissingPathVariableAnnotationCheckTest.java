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
package org.sonar.java.checks.spring;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.plugins.java.api.JavaFileScanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class MissingPathVariableAnnotationCheckTest {
  private static final String TEST_SOURCE_FILE = mainCodeSourcesPath("checks/spring/MissingPathVariableAnnotationCheckSample.java");
  private static final JavaFileScanner check = new MissingPathVariableAnnotationCheck();

  @Test
  void test_compiling() {
    CheckVerifier.newVerifier()
      .onFile(TEST_SOURCE_FILE)
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
  void expr() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/spring/SpringComposedRequestMappingCheck.java")
      .withCheck(check)
      .verifyNoIssues();
  }

  @Test
  void test_pattern_parser_simple_pattern(){
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/my/path/"))
      .isEmpty();
  }

  @Test
  void test_pattern_parser_documentation_pattern(){
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
  void test_pattern_parser_template_variables(){
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/{page}/{age}"))
      .containsExactly("page","age");
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/{page}{age}"))
      .containsExactly("page","age");
    assertThat(MissingPathVariableAnnotationCheck.PathPatternParser.parsePathVariables("/{a}{b}-{c}{d:...}${e}\\{f}}"))
      .containsExactly("a","b","c","d","e","f");
  }

  @Test
  void test_pattern_parser_regexes(){
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
  void test_pattern_parser_errors(){
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

}
