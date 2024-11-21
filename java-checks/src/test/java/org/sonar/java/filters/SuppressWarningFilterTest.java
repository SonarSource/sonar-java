/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.filters;

import org.junit.jupiter.api.Test;
import org.sonar.java.checks.BoxedBooleanExpressionsCheck;
import org.sonar.java.checks.CallToDeprecatedCodeMarkedForRemovalCheck;
import org.sonar.java.checks.CallToDeprecatedMethodCheck;
import org.sonar.java.checks.DeadStoreCheck;
import org.sonar.java.checks.EmptyBlockCheck;
import org.sonar.java.checks.EmptyStatementUsageCheck;
import org.sonar.java.checks.EqualsOverriddenWithHashCodeCheck;
import org.sonar.java.checks.ImmediateReverseBoxingCheck;
import org.sonar.java.checks.MissingDeprecatedCheck;
import org.sonar.java.checks.ObjectFinalizeCheck;
import org.sonar.java.checks.RawTypeCheck;
import org.sonar.java.checks.RedundantTypeCastCheck;
import org.sonar.java.checks.ReturnInFinallyCheck;
import org.sonar.java.checks.StaticFieldUpateCheck;
import org.sonar.java.checks.StaticMembersAccessCheck;
import org.sonar.java.checks.StaticMethodCheck;
import org.sonar.java.checks.SuppressWarningsCheck;
import org.sonar.java.checks.SwitchCaseWithoutBreakCheck;
import org.sonar.java.checks.SynchronizedOverrideCheck;
import org.sonar.java.checks.TodoTagPresenceCheck;
import org.sonar.java.checks.TryWithResourcesCheck;
import org.sonar.java.checks.TypeParametersShadowingCheck;
import org.sonar.java.checks.UndocumentedApiCheck;
import org.sonar.java.checks.naming.BadConstantNameCheck;
import org.sonar.java.checks.serialization.SerialVersionUidCheck;
import org.sonar.java.checks.unused.UnusedLabelCheck;
import org.sonar.java.checks.unused.UnusedLocalVariableCheck;
import org.sonar.java.checks.unused.UnusedPrivateClassCheck;
import org.sonar.java.checks.unused.UnusedPrivateFieldCheck;
import org.sonar.java.checks.unused.UnusedPrivateMethodCheck;
import org.sonar.java.checks.unused.UnusedTypeParameterCheck;

class SuppressWarningFilterTest {
  /**
   * Constant used in test for rule key.
   */
  public static final String CONSTANT_RULE_KEY = "java:S115";
  @Test
  void verify() {
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
      new EmptyBlockCheck(),
      new EmptyStatementUsageCheck(),
      new ReturnInFinallyCheck(),
      new EqualsOverriddenWithHashCodeCheck(),
      new StaticMembersAccessCheck(),
      new SerialVersionUidCheck(),
      new RawTypeCheck()
    );
  }

  @Test
  void verify_2() {
    FilterVerifier.verify("src/test/files/filters/SuppressWarningFilter_2.java", new SuppressWarningFilter(),
      // activated rules
      new BoxedBooleanExpressionsCheck(),
      new ImmediateReverseBoxingCheck(),
      new TypeParametersShadowingCheck(),
      new TryWithResourcesCheck(),
      new SerialVersionUidCheck(),
      new StaticFieldUpateCheck(),
      new StaticMethodCheck(),
      new SynchronizedOverrideCheck()
    );
  }

  @Test
  void verify_unused() {
    FilterVerifier.verify("src/test/files/filters/SuppressWarningFilter_unused.java", new SuppressWarningFilter(),
      // activated rules
      new UnusedLocalVariableCheck(),
      new UnusedLabelCheck(),
      new UnusedPrivateFieldCheck(),
      new UnusedPrivateClassCheck(),
      new UnusedTypeParameterCheck(),
      new UnusedPrivateMethodCheck(),
      new DeadStoreCheck()
    );
  }

  @Test
  void verify_javadoc() {
    FilterVerifier.verify("src/test/files/filters/SuppressWarningFilter_javadoc.java", new SuppressWarningFilter(),
      // activated rules
      new UndocumentedApiCheck()
    );
  }

}
