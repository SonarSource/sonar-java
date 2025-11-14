/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.junit.jupiter.api.Test;
import org.sonar.java.AnalysisException;
import org.sonar.java.checks.verifier.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DisallowedClassCheckTest {

  @Test
  void check() {
    DisallowedClassCheck visitor = new DisallowedClassCheck();
    visitor.disallowedClass = "java.lang.String";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DisallowedClassCheck.java")
      .withCheck(visitor)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DisallowedClassCheck.java")
      .withCheck(visitor)
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void check_annotation() {
    DisallowedClassCheck visitor = new DisallowedClassCheck();
    visitor.disallowedClass = "org.foo.MyAnnotation";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DisallowedClassCheckAnnotation.java")
      .withCheck(visitor)
      .verifyIssues();
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DisallowedClassCheckAnnotation.java")
      .withCheck(visitor)
      .withoutSemantic()
      .verifyNoIssues();
  }

  @Test
  void checkRegex() {
    DisallowedClassCheck visitor = new DisallowedClassCheck();
    visitor.disallowedClass = "java.lang\\..*";
    CheckVerifier.newVerifier()
      .onFile("src/test/files/checks/DisallowedClassCheckRegex.java")
      .withCheck(visitor)
      .verifyIssues();
  }

  @Test
  void checkBadRegex() {
    DisallowedClassCheck visitor = new DisallowedClassCheck();
    // bad regex
    visitor.disallowedClass = "java.lang(";
    CheckVerifier verifier = CheckVerifier.newVerifier().onFile("src/test/files/checks/DisallowedClassCheckRegex.java").withCheck(visitor);

    AnalysisException e = assertThrows(AnalysisException.class, verifier::verifyIssues);
    assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
  }
}
