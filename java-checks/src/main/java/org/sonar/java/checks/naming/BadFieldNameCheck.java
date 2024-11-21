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
package org.sonar.java.checks.naming;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.AbstractBadFieldNameChecker;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "S00116", repositoryKey = "squid")
@Rule(key = "S116")
public class BadFieldNameCheck extends AbstractBadFieldNameChecker {

  @RuleProperty(
    key = DEFAULT_FORMAT_KEY,
    description = DEFAULT_FORMAT_DESCRIPTION,
    defaultValue = DEFAULT_FORMAT_VALUE)
  public String format = DEFAULT_FORMAT_VALUE;

  @Override
  protected String getFormat() {
    return format;
  }

  @Override
  protected boolean isFieldModifierConcernedByRule(ModifiersTree modifier) {
    return !ModifiersUtils.hasModifier(modifier, Modifier.STATIC);
  }

}
