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
package org.sonar.java.filters;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.CallToDeprecatedCodeMarkedForRemovalCheck;
import org.sonar.java.checks.CallToDeprecatedMethodCheck;
import org.sonar.java.checks.EmptyBlockCheck;
import org.sonar.java.checks.EmptyStatementUsageCheck;
import org.sonar.java.checks.EqualsOverridenWithHashCodeCheck;
import org.sonar.java.checks.MissingDeprecatedCheck;
import org.sonar.java.checks.ObjectFinalizeCheck;
import org.sonar.java.checks.RedundantTypeCastCheck;
import org.sonar.java.checks.ReturnInFinallyCheck;
import org.sonar.java.checks.StaticMembersAccessCheck;
import org.sonar.java.checks.SuppressWarningsCheck;
import org.sonar.java.checks.SwitchCaseWithoutBreakCheck;
import org.sonar.java.checks.TodoTagPresenceCheck;
import org.sonar.java.checks.naming.BadConstantNameCheck;
import org.sonar.java.checks.serialization.SerialVersionUidCheck;
import org.sonar.java.checks.unused.UnusedPrivateFieldCheck;
import org.sonar.java.se.checks.DivisionByZeroCheck;

public class SuppressWarningFilterTest {
  /**
   * Constant used in test for rule key.
   */
  public static final String CONSTANT_RULE_KEY = "java:S115";
  @Test
  public void verify() {
    FilterVerifier.verify("src/test/files/filters/SuppressWarningFilter.java", new SuppressWarningFilter(),
      // activated rules
      new UnusedPrivateFieldCheck(),
      new BadConstantNameCheck(),
      new SuppressWarningsCheck(),
      new TodoTagPresenceCheck(),
      new ObjectFinalizeCheck(),
      new SwitchCaseWithoutBreakCheck(),
      new RedundantTypeCastCheck(),
      new CallToDeprecatedMethodCheck(),
      new CallToDeprecatedCodeMarkedForRemovalCheck(),
      new MissingDeprecatedCheck(),
      new DivisionByZeroCheck(),
      new EmptyBlockCheck(),
      new EmptyStatementUsageCheck(),
      new ReturnInFinallyCheck(),
      new EqualsOverridenWithHashCodeCheck(),
      new StaticMembersAccessCheck(),
      new SerialVersionUidCheck()
    );
  }

}
