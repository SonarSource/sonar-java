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
package org.sonar.java.checks.naming;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.AbstractBadFieldNameChecker;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;

@Rule(key = "S3008")
public class BadFieldNameStaticNonFinalCheck extends AbstractBadFieldNameChecker {

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
    return ModifiersUtils.hasModifier(modifier, Modifier.STATIC) && !ModifiersUtils.hasModifier(modifier, Modifier.FINAL);
  }

}
