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
package org.sonar.java.checks.security;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.java.checks.helpers.CredentialMethod;
import org.sonar.java.checks.helpers.CredentialMethodsLoader;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.test.classpath.TestClasspathUtils.AWS_MODULE;

class HardCodedCredentialsShouldNotBeUsedCheckTest {
  @RegisterExtension
  final LogTesterJUnit5 logTester = new LogTesterJUnit5().setLevel(Level.DEBUG);

  @Test
  void uses_empty_collection_when_methods_cannot_be_loaded() {
    var check = new HardCodedCredentialsShouldNotBeUsedCheck("non-existing-file.json");
    assertThat(check.getMethods()).isEmpty();
    List<String> logs = logTester.getLogs(Level.ERROR).stream()
      .map(LogAndArguments::getFormattedMsg)
      .toList();
    assertThat(logs)
      .containsOnly("Could not load methods from \"non-existing-file.json\".");
  }

  private enum ComparisonResult {
    DISTINCT {
      @Override
      public ComparisonResult and(ComparisonResult other) {
        return DISTINCT;
      }
    }, SAME {
      @Override
      public ComparisonResult and(ComparisonResult other) {
        return other;
      }
    }, MAY_INTERSECT {
      @Override
      public ComparisonResult and(ComparisonResult other) {
        if (other == DISTINCT) {
          return DISTINCT;
        }
        return MAY_INTERSECT;
      }
    };

    /**
     * This is kind of a three valued logic, where and describe how the combination of two parts compares to another combination of two parts, given that we know how the small
     * parts compare.
     */
    public abstract ComparisonResult and(ComparisonResult other);
  }

  private ComparisonResult compareMethodDescriptors(CredentialMethod method, CredentialMethod other) {
    if (!method.cls.equals(other.cls) || !method.name.equals(other.name)) {
      return ComparisonResult.DISTINCT;
    }
    if (method.args.size() != other.args.size()) {
      return ComparisonResult.DISTINCT;
    }
    ComparisonResult result = ComparisonResult.SAME;
    for (int i = 0; i < method.args.size(); i++) {
      if (method.args.get(i).equals(other.args.get(i))) {
        result = result.and(ComparisonResult.SAME);
      } else if (method.args.get(i).equals("*") || other.args.get(i).equals("*")) {
        result = result.and(ComparisonResult.MAY_INTERSECT);
      } else {
        return ComparisonResult.DISTINCT;
      }
    }
    return result;
  }

  /**
   * Count the number of method that may intersect. Assert that no two descriptor are exactly the same.
   */
  private int countIntersectingMethodsDescriptors(List<CredentialMethod> methods) {
    int count = 0;
    for (int i = 0; i < methods.size(); i++) {
      for (int j = i + 1; j < methods.size(); j++) {
        var comparisonResult = compareMethodDescriptors(methods.get(i), methods.get(j));
        assertThat(comparisonResult)
          .as("credential method entries " + methods.get(i) + " and " + methods.get(j) + " are not the same")
          .isNotEqualTo(ComparisonResult.SAME);
        if (comparisonResult == ComparisonResult.MAY_INTERSECT) {
          count++;
        }
      }
    }
    return count;
  }

  @Test
  void test_credential_file_content() throws IOException {
    Map<String, List<CredentialMethod>> methods = CredentialMethodsLoader
      .load(HardCodedCredentialsShouldNotBeUsedCheck.CREDENTIALS_METHODS_FILE);
    Integer intersectCount =
      methods.values().stream().map(this::countIntersectingMethodsDescriptors)
        .reduce(Integer::sum)
        .orElse(0);
    // There are three potential intersections we know of. We have checked manually that there are no actual method in the intersection.
    assertThat(intersectCount).isEqualTo(3);
    assertThat(methods).hasSize(2730);
  }

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPathInModule(AWS_MODULE, "checks/security/HardCodedCredentialsShouldNotBeUsedCheckSample.java"))
      .withCheck(new HardCodedCredentialsShouldNotBeUsedCheck())
      .withClassPath(AWS_MODULE.getClassPath())
      .verifyIssues();
  }

  @Test
  void test_non_compiling_code() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/security/HardCodedCredentialsShouldNotBeUsedCheckSample.java"))
      .withCheck(new HardCodedCredentialsShouldNotBeUsedCheck())
      .withClassPath(AWS_MODULE.getClassPath())
      .verifyIssues();
  }

}
