/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.filters;

import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.JavaCheck;

import static org.junit.jupiter.api.Assertions.assertThrows;

class FilterVerifierTest {

  @Test
  void filterVerifierShouldFailInCaseOfInvalidFile() {
    String parseErrorFile = "src/test/files/filters/FilterParseError.java";
    JavaIssueFilter testIssueFilter = new TestIssueFilter();
    assertThrows(
      AssertionError.class,
      () -> FilterVerifier.newInstance().verify(parseErrorFile, testIssueFilter));
  }

  @Test
  void filterVerifierShouldFailInCaseOfInvalidFileWithoutSemantic() {
    String parseErrorFile = "src/test/files/filters/FilterParseError.java";
    JavaIssueFilter testIssueFilter = new TestIssueFilter();
    assertThrows(
      AssertionError.class,
      () -> FilterVerifier.newInstance().withoutSemantic().verify(parseErrorFile, testIssueFilter));
  }

  /**
   * IssueFilter which filter nothing
   */
  static class TestIssueFilter extends BaseTreeVisitorIssueFilter {

    @Override
    public Set<Class<? extends JavaCheck>> filteredRules() {
      return Collections.emptySet();
    }
  }
}
