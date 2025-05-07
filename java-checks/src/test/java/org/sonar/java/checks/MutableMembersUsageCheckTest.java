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
package org.sonar.java.checks;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;
import static org.sonar.java.checks.verifier.TestUtils.nonCompilingTestSourcesPath;

class MutableMembersUsageCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/MutableMembersUsageCheckSample.java"))
      .withCheck(new MutableMembersUsageCheck())
      .verifyIssues();
  }

  @Test
  void test_non_compiling() {
    CheckVerifier.newVerifier()
      .onFile(nonCompilingTestSourcesPath("checks/MutableMembersUsageCheckSample.java"))
      .withCheck(new MutableMembersUsageCheck())
      .verifyNoIssues();
  }

  /** The toString method is just used for debugging, so correctness doesn't matter, we just want it not to crash. */
  @Test
  void test_toString() {
    var callSite = new MutableMembersUsageCheck.CallSite(
      "foo()V",
      Arrays.asList(
        new MutableMembersUsageCheck.ArgumentParameterMapping(1, 0),
        new MutableMembersUsageCheck.ArgumentParameterMapping(0, 2)));
    assertThat(callSite.toString()).isNotEmpty();
  }
}
