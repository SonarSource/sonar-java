/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package checks.tests;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Disabled;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;

public class JUnit4AnnotationsCheckTest {

  @Test // Noncompliant [[sc=3;ec=8]] {{Change this JUnit4 org.junit.Test to the equivalent JUnit5 org.junit.jupiter.api.Test annotation.}}
  private void someTestMethod() {}

  @org.junit.Test // Noncompliant
  private void someTestMethodWithQualifiedAnnotation() {}

  @Test(timeout = 4) // Noncompliant
  void testWithTimeout() {}

  @org.junit.jupiter.api.Test // compliant (JUnit5 annotation)
  void junit5Test() {}

  @Before() // Noncompliant {{Change this JUnit4 org.junit.Before to the equivalent JUnit5 org.junit.jupiter.api.BeforeEach annotation.}}
  void before() {}

  @After // Noncompliant {{Change this JUnit4 org.junit.After to the equivalent JUnit5 org.junit.jupiter.api.AfterEach annotation.}}
  void after() {}

  @BeforeClass // Noncompliant {{Change this JUnit4 org.junit.BeforeClass to the equivalent JUnit5 org.junit.jupiter.api.BeforeAll annotation.}}
  void beforeClass() {}

  @AfterClass // Noncompliant {{Change this JUnit4 org.junit.AfterClass to the equivalent JUnit5 org.junit.jupiter.api.AfterAll annotation.}}
  void afterClass() {}

  @Ignore // Noncompliant {{Change this JUnit4 org.junit.Ignore to the equivalent JUnit5 org.junit.jupiter.api.Disabled annotation.}}
  void ignored() {}

  @Ignore // Noncompliant
  private interface IgnoredInterface {}

  @Ignore // Noncompliant
  protected class IgnoredClass {}

  @Ignore // Noncompliant
  @Test // Noncompliant
  void ignoredTest() {}

  @Test // Noncompliant
  @Disabled // compliant (JUnit5 annotation)
  void disabledTest() {}

  @Category(JUnit4AnnotationsCheckTest.class) // Noncompliant {{Change this JUnit4 org.junit.experimental.categories.Category to the equivalent JUnit5 org.junit.jupiter.api.Tag annotation.}}
  void categorized() {}

  @Rule // Noncompliant {{Change this JUnit4 org.junit.Rule to the equivalent JUnit5 org.junit.jupiter.api.extension.ExtendWith annotation.}}
  void rule() {}

  class RuleClass {
    @Rule // Noncompliant
    String ruleField;
  }

  @ClassRule // Noncompliant {{Change this JUnit4 org.junit.ClassRule to the equivalent JUnit5 org.junit.jupiter.api.extension.RegisterExtension annotation.}}
  void classRule() {}

  @RunWith(Runner.class) // Noncompliant {{Change this JUnit4 org.junit.runner.RunWith to the equivalent JUnit5 org.junit.jupiter.api.extension.ExtendWith annotation.}}
  abstract class RunWithClass {}
}
