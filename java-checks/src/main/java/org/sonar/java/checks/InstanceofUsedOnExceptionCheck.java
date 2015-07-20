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
import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Set;

@Rule(
  key = "S1193",
  name = "Exception types should not be tested using \"instanceof\" in catch blocks",
  tags = {"error-handling", "security"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("10min")
public class InstanceofUsedOnExceptionCheck extends SubscriptionBaseVisitor {

  private final Set<String> caughtVariables = Sets.newHashSet();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CATCH, Tree.Kind.INSTANCE_OF);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    caughtVariables.clear();
    super.scanFile(context);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.CATCH)) {
      CatchTree catchTree = (CatchTree) tree;
      caughtVariables.add(catchTree.parameter().simpleName().name());
    } else if (isLeftOperandAnException((InstanceOfTree) tree)) {
      addIssue(((InstanceOfTree) tree).instanceofKeyword(), "Replace the usage of the \"instanceof\" operator by a catch block.");
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if(tree.is(Tree.Kind.CATCH)) {
      CatchTree catchTree = (CatchTree) tree;
      caughtVariables.remove(catchTree.parameter().simpleName().name());
    }
  }

  private boolean isLeftOperandAnException(InstanceOfTree tree) {
    return tree.expression().is(Tree.Kind.IDENTIFIER) && caughtVariables.contains(((IdentifierTree) tree.expression()).name());
  }
}
