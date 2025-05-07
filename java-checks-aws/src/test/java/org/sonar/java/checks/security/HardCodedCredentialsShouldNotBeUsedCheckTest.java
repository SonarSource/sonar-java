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
import static org.sonar.java.checks.CommonConstants.AWS_CLASSPATH;
import static org.sonar.java.checks.CommonConstants.AWS_MODULE;

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

  private boolean areMethodsDistinct(CredentialMethod method, CredentialMethod other) {
    if (!method.cls.equals(other.cls) || !method.name.equals(other.name)) {
      return true;
    }
    if (method.args.size() != other.args.size()) {
      return true;
    }
    for (int i = 0; i < method.args.size(); i++) {
      if (!method.args.get(i).equals(other.args.get(i)) &&
        !method.args.get(i).equals("*") && !other.args.get(i).equals("*")) {
        return true;
      }
    }
    return false;
  }

  private void checkMethodsDistinct(List<CredentialMethod> methods) {
    for (int i = 0; i < methods.size(); i++) {
      for (int j = i + 1; j < methods.size(); j++) {
        assertThat(areMethodsDistinct(methods.get(i), methods.get(j)))
        .as("credential method entries " + methods.get(i) + " and " + methods.get(j) + " are distinct")
        .isTrue();
      }
    }
  }

  @Test
  void test_credential_file_content() throws IOException {
    Map<String, List<CredentialMethod>> methods = CredentialMethodsLoader
      .load(HardCodedCredentialsShouldNotBeUsedCheck.CREDENTIALS_METHODS_FILE);

    for (Map.Entry<String, List<CredentialMethod>> entry : methods.entrySet()) {
      checkMethodsDistinct(entry.getValue());
    }
    assertThat(methods).hasSize(2730);
  }

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPathInModule(AWS_MODULE, "checks/security/HardCodedCredentialsShouldNotBeUsedCheckSample.java"))
      .withCheck(new HardCodedCredentialsShouldNotBeUsedCheck())
      .withClassPath(AWS_CLASSPATH)
      .verifyIssues();
  }

  @Test
  void test_non_compiling_code() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.nonCompilingTestSourcesPath("checks/security/HardCodedCredentialsShouldNotBeUsedCheckSample.java"))
      .withCheck(new HardCodedCredentialsShouldNotBeUsedCheck())
      .withClassPath(AWS_CLASSPATH)
      .verifyIssues();
  }

}
