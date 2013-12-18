/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import org.sonar.api.checks.CheckFactory;
import org.sonar.api.rules.ActiveRule;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;

import javax.annotation.CheckForNull;

/**
 * Companion of {@link org.sonar.plugins.javang.bridges.DesignBridge} which actually does the job on finding cycles and creation of violations.
 *
 * @since 3.2
 */
@Rule(key = CycleBetweenPackagesCheck.RULE_KEY, priority = Priority.MAJOR)
public class CycleBetweenPackagesCheck extends BytecodeVisitor {

  public static final String RULE_KEY = "CycleBetweenPackages";

  /**
   * @return null, if this check is inactive
   */
  @CheckForNull
  public static ActiveRule getActiveRule(CheckFactory checkFactory) {
    for (Object check : checkFactory.getChecks()) {
      if (CycleBetweenPackagesCheck.class.equals(check.getClass())) {
        return checkFactory.getActiveRule(check);
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return RULE_KEY + " rule";
  }

}
