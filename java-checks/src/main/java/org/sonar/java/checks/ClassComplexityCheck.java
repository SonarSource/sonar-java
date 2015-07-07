/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleLinearWithOffsetRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.text.MessageFormat;
import java.util.List;

@Rule(
  key = "ClassCyclomaticComplexity",
  name = "Classes should not be too complex",
  tags = {"brain-overload"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleLinearWithOffsetRemediation(coeff = "1min", offset = "10min", effortToFixDescription = "per complexity point over the threshold" )
public class ClassComplexityCheck extends SubscriptionBaseVisitor {

  private static final int DEFAULT_MAX = 200;

  @RuleProperty(defaultValue = "" + DEFAULT_MAX,
    description = "Maximum complexity allowed.")
  private int max = DEFAULT_MAX;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    int complexity = context.getComplexity(tree);
    if (complexity > max) {
      addIssue(tree,
        MessageFormat.format("The Cyclomatic Complexity of this class is {0,number,integer} which is greater than {1,number,integer} authorized.", complexity, max),
          (double) complexity - max);
    }
  }

  public void setMax(int max) {
    this.max = max;
  }

}
