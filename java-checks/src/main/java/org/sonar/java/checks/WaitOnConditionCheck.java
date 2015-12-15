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
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1844",
  name = "\"Object.wait(...)\" should never be called on objects that implement \"java.util.concurrent.locks.Condition\"",
  priority = Priority.BLOCKER,
  tags = {Tag.BUG, Tag.PITFALL})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SYNCHRONIZATION_RELIABILITY)
@SqaleConstantRemediation("20min")
public class WaitOnConditionCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    TypeCriteria conditionSubType = TypeCriteria.subtypeOf("java.util.concurrent.locks.Condition");
    return ImmutableList.<MethodMatcher>builder()
        .add(MethodMatcher.create().callSite(conditionSubType).name("wait"))
        .add(MethodMatcher.create().callSite(conditionSubType).name("wait").addParameter("long"))
        .add(MethodMatcher.create().callSite(conditionSubType).name("wait").addParameter("long").addParameter("int"))
        .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    addIssue(mit, "The \"Condition.await(...)\" method should be used instead of \"Object.wait(...)\"");
  }
}
