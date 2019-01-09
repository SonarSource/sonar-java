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

import org.junit.Test;
import org.sonar.java.checks.verifier.JavaCheckVerifier;

public class ClassFieldCountCheckTest {

  private static final String TEST_FILES_DIR = "src/test/files/checks/ClassFieldCountCheck/";

  @Test
  public void simple_case() {
    JavaCheckVerifier.verify(TEST_FILES_DIR + "SimpleDefaultCase.java", new ClassFieldCountCheck());
    JavaCheckVerifier.verifyNoIssueWithoutSemantic(TEST_FILES_DIR + "SimpleDefaultCase.java", new ClassFieldCountCheck());
  }

  @Test
  public void static_final() {
    JavaCheckVerifier.verify(TEST_FILES_DIR + "ClassFieldCountCheck.java", new ClassFieldCountCheck());
  }

  @Test
  public void enums_interfaces_and_anonymous_trees() {
    ClassFieldCountCheck check = new ClassFieldCountCheck();
    check.setThreshold(2);
    JavaCheckVerifier.verify(TEST_FILES_DIR + "UnusualTrees.java", check);
  }

  @Test
  public void count_only_public_fields() {
    ClassFieldCountCheck check = new ClassFieldCountCheck();
    check.setCountNonPublicFields(false);
    JavaCheckVerifier.verify(TEST_FILES_DIR + "CountOnlyPublicFields.java", check);
  }

}
