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
package org.sonar.java.checks.aws;

import com.google.common.reflect.ClassPath;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.sonar.check.Rule;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPathInModule;
import static org.sonar.java.test.classpath.TestClasspathUtils.AWS_MODULE;

class AwsLongTermAccessKeysCheckTest {
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
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPathInModule(AWS_MODULE, "checks/aws/AwsLongTermAccessKeysCheckSample.java"))
      .withCheck(new AwsLongTermAccessKeysCheck())
      .withClassPath(AWS_MODULE.getClassPath())
      .verifyIssues();
  }
}
