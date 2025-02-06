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
package org.sonar.java.checks.tests;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;

@Rule(key = "S5786")
public class JUnit5DefaultPackageClassAndMethodCheck extends AbstractJUnit5NotCompliantModifierChecker {

  @Override
  protected boolean isNonCompliantModifier(Modifier modifier, ModifierScope modifierScope) {
    // All visibility modifiers except 'private' handled by S5810
    return modifier == Modifier.PUBLIC || modifier == Modifier.PROTECTED;
  }

  @Override
  protected void raiseIssueOnNonCompliantReturnType(MethodTree methodTree) {
    // Handled by S5810
  }

}
