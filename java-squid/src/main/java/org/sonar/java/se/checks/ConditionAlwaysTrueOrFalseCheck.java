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
package org.sonar.java.se.checks;

import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.se.CheckerContext;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Set;

@Rule(
  key = "S2583",
  name = "Conditions should not unconditionally evaluate to \"TRUE\" or to \"FALSE\"",
  priority = Priority.BLOCKER,
  tags = {"bug", "cwe", "misra"})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("15min")
public class ConditionAlwaysTrueOrFalseCheck extends SECheck {

  private final Set<Tree> evaluatedToFalse = Sets.newHashSet();
  private final Set<Tree> evaluatedToTrue = Sets.newHashSet();

  @Override
  public void init() {
    evaluatedToFalse.clear();
    evaluatedToTrue.clear();
  }

  @Override
  public void checkEndOfExecution(CheckerContext context) {
    for (Tree condition : Sets.difference(evaluatedToFalse, evaluatedToTrue)) {
      context.reportIssue(condition, this, "Change this condition so that it does not always evaluate to \"false\"");
    }
    for (Tree condition : Sets.difference(evaluatedToTrue, evaluatedToFalse)) {
      context.reportIssue(condition, this, "Change this condition so that it does not always evaluate to \"true\"");
    }
  }

  public void evaluatedToFalse(Tree condition) {
    evaluatedToFalse.add(condition);
  }

  public void evaluatedToTrue(Tree condition) {
    evaluatedToTrue.add(condition);
  }
}
