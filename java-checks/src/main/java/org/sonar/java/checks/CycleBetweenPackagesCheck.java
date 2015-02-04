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

import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

/**
 * Companion of {@link org.sonar.plugins.javang.bridges.DesignBridge} which actually does the job on finding cycles and creation of issues.
 *
 * @since 3.2
 */
@Rule(
  key = CycleBetweenPackagesCheck.KEY,
  name = "Avoid cycle between java packages",
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(value = RulesDefinition.SubCharacteristics.ARCHITECTURE_CHANGEABILITY)
@SqaleConstantRemediation(value = "1d")
public class CycleBetweenPackagesCheck extends BytecodeVisitor {


  public static final String KEY = "CycleBetweenPackages";
  public static final RuleKey RULE_KEY = RuleKey.of(CheckList.REPOSITORY_KEY, KEY);

  @Override
  public String toString() {
    return KEY + " rule";
  }

}
