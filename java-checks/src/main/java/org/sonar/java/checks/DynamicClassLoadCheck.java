/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2658",
  name = "Classes should not be loaded dynamically",
  priority = Priority.CRITICAL,
  tags = {Tag.CWE, Tag.OWASP_A1, Tag.SECURITY})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.API_ABUSE)
@SqaleConstantRemediation("45min")
public class DynamicClassLoadCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.lang.Class")).name("forName").withNoParameterConstraint(),
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.lang.ClassLoader")).name("loadClass").withNoParameterConstraint()
    );
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    addIssue(mit, "Remove this use of dynamic class loading.");
  }

}
