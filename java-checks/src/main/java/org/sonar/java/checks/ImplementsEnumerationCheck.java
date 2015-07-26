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
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;
import java.util.List;

@Rule(
  key = "S1150",
  name = "Enumeration should not be implemented",
  tags = {"obsolete"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("30min")
public class ImplementsEnumerationCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    for (Tree superInterface : classTree.superInterfaces()) {
      IdentifierTree identifierTree = null;
      if (superInterface.is(Tree.Kind.IDENTIFIER)) {
        identifierTree = (IdentifierTree) superInterface;
      } else if (superInterface.is(Tree.Kind.PARAMETERIZED_TYPE) && ((ParameterizedTypeTree) superInterface).type().is(Tree.Kind.IDENTIFIER)) {
        identifierTree = (IdentifierTree) ((ParameterizedTypeTree) superInterface).type();
      }
      if (isEnumeration(identifierTree)) {
        addIssue(superInterface, "Implement Iterator rather than Enumeration.");
      }
    }
  }

  private static boolean isEnumeration(@Nullable IdentifierTree tree) {
    return tree != null && "Enumeration".equals(tree.name());
  }
}
