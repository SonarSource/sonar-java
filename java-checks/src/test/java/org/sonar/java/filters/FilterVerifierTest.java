/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.filters;

import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.sonar.plugins.java.api.JavaCheck;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class FilterVerifierTest {

  @Test
  void filterVerifierShouldFailInCaseOfInvalidFile() {
    String parseErrorFile = "src/test/files/filters/FilterParseError.java";
    assertThrows(AssertionError.class, () -> FilterVerifier.verify(parseErrorFile, new TestIssueFilter()));
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
