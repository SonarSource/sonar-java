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

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.checks.verifier.TestUtils;
import org.sonar.java.test.classpath.TestClasspathUtils;

class UseLombokCheckTest {

  @Test
  void testWithoutLombokClasspath() {
    List<File> classpath = Collections.emptyList();
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/UseLombokCheckSample.java"))
      .withClassPath(classpath)
      .withCheck(new UseLombokCheck())
      .verifyNoIssues();
  }

  @Test
  void testLombokClasspath() {
    var classpath = TestClasspathUtils
      .loadFromFile("../java-checks-test-sources/default/target/test-classpath.txt");
    CheckVerifier.newVerifier()
      .onFile(TestUtils.mainCodeSourcesPath("checks/UseLombokCheckSample.java"))
      .withClassPath(classpath)
      .withCheck(new UseLombokCheck())
      .verifyIssues();
  }

}
