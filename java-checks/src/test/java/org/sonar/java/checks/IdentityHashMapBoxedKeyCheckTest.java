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

import com.google.common.collect.Maps;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.java.checks.verifier.CheckVerifier;
import org.sonar.java.test.classpath.TestClasspathUtils;

import static org.sonar.java.checks.verifier.TestUtils.mainCodeSourcesPath;

class IdentityHashMapBoxedKeyCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/IdentityHashMapBoxedKeyCheckSample.java"))
      .withCheck(new IdentityHashMapBoxedKeyCheck())
      .verifyIssues();
  }

  @Test
  void test_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/IdentityHashMapBoxedKeyCheckSample.java"))
      .withCheck(new IdentityHashMapBoxedKeyCheck())
      .withoutSemantic()
      .verifyIssues();
  }

  @Test
  void test_guava() throws URISyntaxException {
    List<File> classPath = new ArrayList<>(TestClasspathUtils.DEFAULT_MODULE.getClassPath());
    classPath.add(new File(Maps.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/IdentityHashMapBoxedKeyCheckGuavaSample.java"))
      .withCheck(new IdentityHashMapBoxedKeyCheck())
      .withClassPath(classPath)
      .verifyIssues();
  }

  @Test
  void test_guava_without_semantic() {
    CheckVerifier.newVerifier()
      .onFile(mainCodeSourcesPath("checks/IdentityHashMapBoxedKeyCheckGuavaSample.java"))
      .withCheck(new IdentityHashMapBoxedKeyCheck())
      .withoutSemantic()
      .verifyNoIssues();
  }
}
