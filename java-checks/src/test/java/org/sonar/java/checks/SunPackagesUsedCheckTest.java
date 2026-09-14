/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import com.google.common.reflect.ClassPath;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.sonar.check.Rule;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SunPackagesUsedCheckTest {

  @Test
  void rule_metadata_is_available_on_test_classpath() throws IOException {
    var rules = ClassPath.from(getClass().getClassLoader()).getTopLevelClassesRecursive("org.sonar.java.checks").stream()
      .map(ClassPath.ClassInfo::load)
      .filter(check -> check.isAnnotationPresent(Rule.class))
      .toList();
    assertThat(rules).isNotEmpty();
    for (Class<?> rule : rules) {
      String key = rule.getAnnotation(Rule.class).key();
      assertThat(getClass().getResource("/org/sonar/l10n/java/rules/java/" + key + ".json"))
        .as("Remediation metadata for %s (%s)", key, rule.getName()).isNotNull();
    }
  }

  @Test
  void detected() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/SunPackagesUsedCheckSample.java"))
      .withCheck(new SunPackagesUsedCheck())
      .verifyIssues();
  }

  @Test
  void check_with_exclusion() {
    SunPackagesUsedCheck check = new SunPackagesUsedCheck();
    check.exclude = "sun.excluded";
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/SunPackagesUsedCheckCustom.java"))
      .withCheck(check)
      .verifyIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/SunPackagesUsedCheckSample.java"))
      .withCheck(new SunPackagesUsedCheck())
      .withoutSemantic()
      .verifyIssues();
  }
}
