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
package org.sonar.java.checks.security;


import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.log.LogTesterJUnit5;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class HardCodedCredentialsShouldNotBeUsedCheckTest {
  @RegisterExtension
  final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  @Test
  void uses_empty_collection_when_methods_cannot_be_loaded() {
    var check = new HardCodedCredentialsShouldNotBeUsedCheck("non-existing-file.json");
    assertThat(check.getMethods()).isEmpty();
    List<String> logs = logTester.getLogs(LoggerLevel.WARN).stream()
      .map(logAndArguments -> logAndArguments.getFormattedMsg())
      .collect(Collectors.toList());
    assertThat(logs)
      .containsOnly("Could not load methods from \"non-existing-file.json\".");
  }


  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/security/HardCodedCredentialsShouldNotBeUsedCheck.java"))
      .withCheck(new HardCodedCredentialsShouldNotBeUsedCheck())
      .verifyIssues();
  }
}
