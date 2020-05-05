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
package org.sonar.java.se.checks;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.sonar.java.se.AlwaysTrueOrFalseExpressionCollector;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.SETestUtils;
import org.sonar.java.testing.CheckVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionAlwaysTrueOrFalseCheckTest {

  @Test
  void test() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/se/ConditionAlwaysTrueOrFalseCheck.java")
      .withChecks(new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_without_jsr305() {
    List<File> classpath = FileUtils.listFiles(new File("target/test-jars"), new String[] {"jar"}, true).stream()
      .filter(file -> file.getName().startsWith("spring-core-") || file.getName().startsWith("spring-web-"))
      .collect(Collectors.toList());
    classpath.add(new File("target/test-classes"));
    CheckVerifier.newVerifier()
      .onFile("src/test/files/se/SpringNullableAndNonNullAnnotationsWithoutJSR305.java")
      .withCheck(new BooleanGratuitousExpressionsCheck())
      .withClassPath(classpath)
      .verifyIssues();
  }

  @Test
  void test_unreachable_vs_gratuitous() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/se/UnreachableOrGratuitous.java")
      .withCheck(new ConditionalUnreachableCodeCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void whole_stack_required_for_ps_equality() throws Exception {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/se/PsEqualityRequiresFullStack.java")
      .withCheck(new AssertNoAlwaysTrueOrFalseExpression())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }

  @Test
  void condition_always_true_with_optional() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/se/ConditionAlwaysTrueWithOptional.java")
      .withCheck(new AssertNoAlwaysTrueOrFalseExpression())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }

  @Test
  void resetFields_ThreadSleepCalls() throws Exception {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/se/ThreadSleepCall.java")
      .withCheck(new AssertNoAlwaysTrueOrFalseExpression())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyNoIssues();
  }

  @Test
  void reporting() {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/se/ConditionAlwaysTrueOrFalseCheckReporting.java")
      .withChecks(new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void reporting_getting_wrong_parent() {
    // Checks flow iterating through the correct parent
    CheckVerifier.newVerifier()
      .onFile("src/test/files/se/ConditionAlwaysTrueOrFalseCheckParentLoop.java")
      .withChecks(new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_transitivity() throws Exception {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/se/Transitivity.java")
      .withChecks(new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  private static class AssertNoAlwaysTrueOrFalseExpression extends SECheck {
    @Override
    public void checkEndOfExecution(CheckerContext context) {
      AlwaysTrueOrFalseExpressionCollector atof = context.alwaysTrueOrFalseExpressions();
      assertThat(atof.alwaysFalse()).isEmpty();
      assertThat(atof.alwaysTrue()).isEmpty();
    }
  }

  @Test
  void test_constraint_is_not_lost_after_copying() throws Exception {
    // see also SONARJAVA-2351
    CheckVerifier.newVerifier()
      .onFile("src/test/files/se/ConstraintCopy.java")
      .withChecks(new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }

  @Test
  void test_binary_expressions_always_not_null() throws Exception {
    CheckVerifier.newVerifier()
      .onFile("src/test/files/se/BinaryExpressionNotNull.java")
      .withChecks(new ConditionalUnreachableCodeCheck(), new BooleanGratuitousExpressionsCheck())
      .withClassPath(SETestUtils.CLASS_PATH)
      .verifyIssues();
  }
}
