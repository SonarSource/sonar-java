/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks;

import org.assertj.core.api.Fail;
import org.junit.Test;
import org.sonar.java.AnalysisException;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

public class DisallowedClassCheckTest {

  @Test
  public void check() {
    DisallowedClassCheck visitor = new DisallowedClassCheck();
    visitor.disallowedClass = "java.lang.String";
    JavaCheckVerifier.verify("src/test/files/checks/DisallowedClassCheck.java", visitor);
    JavaCheckVerifier.verifyNoIssueWithoutSemantic("src/test/files/checks/DisallowedClassCheck.java", visitor);
  }

  @Test
  public void check_annotation() {
    DisallowedClassCheck visitor = new DisallowedClassCheck();
    visitor.disallowedClass = "org.foo.MyAnnotation";
    JavaCheckVerifier.verify("src/test/files/checks/DisallowedClassCheckAnnotation.java", visitor);
    JavaCheckVerifier.verifyNoIssueWithoutSemantic("src/test/files/checks/DisallowedClassCheckAnnotation.java", visitor);
  }

  @Test
  public void checkRegex() {
    DisallowedClassCheck visitor = new DisallowedClassCheck();
    visitor.disallowedClass = "java.lang\\..*";
    JavaCheckVerifier.verify("src/test/files/checks/DisallowedClassCheckRegex.java", visitor);
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkBadRegex() throws Throwable {
    DisallowedClassCheck visitor = new DisallowedClassCheck();
    // bad regex
    visitor.disallowedClass = "java.lang(";
    try {
      JavaCheckVerifier.verify("src/test/files/checks/DisallowedClassCheckRegex.java", visitor);
      Fail.fail("Should have failed");
    } catch (AnalysisException e) {
      throw e.getCause();
    }
  }
}
