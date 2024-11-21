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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S3011")
public class AccessibilityChangeCheck extends AbstractAccessibilityChangeChecker {
  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (isModifyingFieldFromRecord(mit)) {
      return;
    }
    if (SET_ACCESSIBLE_MATCHER.matches(mit)) {
      if (setsToPubliclyAccessible(mit)) {
        reportIssue(mit, "This accessibility update should be removed.");
      }
    } else {
      reportIssue(mit, "This accessibility bypass should be removed.");
    }
  }
}
