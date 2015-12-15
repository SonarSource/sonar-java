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
  key = "S2236",
  name = "Methods \"wait(...)\", \"notify()\" and \"notifyAll()\" should never be called on Thread instances",
  priority = Priority.BLOCKER,
  tags = {Tag.BUG, Tag.MULTI_THREADING})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SYNCHRONIZATION_RELIABILITY)
@SqaleConstantRemediation("30min")
public class ThreadWaitCallCheck extends AbstractMethodDetection {

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    addIssue(mit, "Refactor the synchronisation mechanism to not use a Thread instance as a monitor");
  }

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    TypeCriteria subtypeOfThread = TypeCriteria.subtypeOf("java.lang.Thread");
    return ImmutableList.<MethodMatcher>builder()
        .add(MethodMatcher.create().callSite(subtypeOfThread).name("wait"))
        .add(MethodMatcher.create().callSite(subtypeOfThread).name("wait").addParameter("long"))
        .add(MethodMatcher.create().callSite(subtypeOfThread).name("wait").addParameter("long").addParameter("int"))
        .add(MethodMatcher.create().callSite(subtypeOfThread).name("notify"))
        .add(MethodMatcher.create().callSite(subtypeOfThread).name("notifyAll")).build();
  }
}
