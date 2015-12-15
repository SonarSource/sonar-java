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
import org.sonar.check.RuleProperty;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleLinearWithOffsetRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayList;
import java.util.List;

@Rule(
  key = "MethodCyclomaticComplexity",
  name = "Methods should not be too complex",
  priority = Priority.MAJOR,
  tags = {Tag.BRAIN_OVERLOAD})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNIT_TESTABILITY)
@SqaleLinearWithOffsetRemediation(coeff = "1min", offset = "10min", effortToFixDescription = "per complexity point above the threshold")
public class MethodComplexityCheck extends SubscriptionBaseVisitor {

  private static final int DEFAULT_MAX = 10;

  @RuleProperty(
    key = "Threshold",
    description = "The maximum authorized complexity.",
    defaultValue = "" + DEFAULT_MAX)
  private int max = DEFAULT_MAX;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (isExcluded(methodTree)) {
      return;
    }
    List<Tree> complexity = context.getComplexityNodes(methodTree);
    int size = complexity.size();
    if (size > max) {
      List<JavaFileScannerContext.Location> flow = new ArrayList<>();
      for (Tree element : complexity) {
        flow.add(new JavaFileScannerContext.Location("+1", element));
      }
      reportIssue(
        methodTree.simpleName(),
        "The Cyclomatic Complexity of this method \"" + methodTree.simpleName().name() + "\" is " + size + " which is greater than " + max + " authorized.",
        flow,
        size - max);
    }
  }

  private static boolean isExcluded(MethodTree methodTree) {
    String name = methodTree.simpleName().name();
    if ("equals".equals(name)) {
      return methodTree.parameters().size() == 1;
    } else if ("hashCode".equals(name)) {
      return methodTree.parameters().isEmpty();
    }
    return false;
  }

  public void setMax(int max) {
    this.max = max;
  }
}
