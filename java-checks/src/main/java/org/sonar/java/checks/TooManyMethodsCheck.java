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
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayList;
import java.util.List;

@Rule(
  key = "S1448",
  name = "Classes should not have too many methods",
  priority = Priority.MAJOR,
  tags = {Tag.BRAIN_OVERLOAD})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_CHANGEABILITY)
@SqaleConstantRemediation("1h")
public class TooManyMethodsCheck extends IssuableSubscriptionVisitor {

  private static final int DEFAULT_MAXIMUM = 35;

  @RuleProperty(
    key = "maximumMethodThreshold",
    description = "The maximum number of methods authorized in a class.",
    defaultValue = "" + DEFAULT_MAXIMUM
  )
  public int maximumMethodThreshold = DEFAULT_MAXIMUM;

  @RuleProperty(
    key = "countNonpublicMethods",
    description = "Whether or not to include non-public methods in the count.",
    defaultValue = "true"
  )
  public boolean countNonPublic = true;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE);
  }

  @Override
  public void visitNode(Tree tree) {
    List<Tree> count = new ArrayList<>();
    ClassTree classTree = (ClassTree) tree;
    for (Tree member : classTree.members()) {
      if (member.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR) && (countNonPublic || ((MethodTree) member).symbol().isPublic())) {
        count.add(member);
      }
    }
    if (count.size() > maximumMethodThreshold) {
      List<JavaFileScannerContext.Location> secondary = new ArrayList<>();
      for (Tree element : count) {
        secondary.add(new JavaFileScannerContext.Location("Method + 1", element));
      }

      String classDescription;
      if (classTree.simpleName() == null) {
        classDescription = "Anonymous class \"" + ((NewClassTree) classTree.parent()).identifier().symbolType().name() + "\"";
      } else {
        classDescription = classTree.declarationKeyword().text() + " \"" + classTree.simpleName() + "\"";
      }
      reportIssue(
        ExpressionsHelper.reportOnClassTree(classTree),
        String.format("%s has %d%s methods, which is greater than the %d authorized. Split it into smaller classes.",
          classDescription, count.size(), countNonPublic ? "" : " public", maximumMethodThreshold),
        secondary,
        null);
    }
  }

}
