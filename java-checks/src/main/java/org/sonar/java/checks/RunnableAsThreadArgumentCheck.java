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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.List;

@Rule(
  key = "S2438",
  name = "\"Threads\" should not be used where \"Runnables\" are expected",
  tags = {"multi-threading", "pitfall"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("15min")
public class RunnableAsThreadArgumentCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    NewClassTree newClassTree = (NewClassTree) tree;
    List<ExpressionTree> arguments = newClassTree.arguments();
    if (arguments.size() > 0 && isOfTypeThread(newClassTree) && isSubTypeOfThread(arguments.get(0))) {
      String firstArgName = getArgName(arguments.get(0));
      addIssue(tree, getMessage(firstArgName));
    }
  }

  @Nullable
  private String getArgName(ExpressionTree tree) {
    if (tree.is(Kind.IDENTIFIER)) {
      return ((IdentifierTree) tree).name();
    }
    return null;
  }

  private String getMessage(@Nullable String variableName) {
    String start = "First argument";
    if (variableName != null) {
      start = "\"" + variableName + "\"";
    }
    return start + " is a \"Thread\".";
  }

  private boolean isOfTypeThread(Tree tree) {
    return ((AbstractTypedTree) tree).getSymbolType().is("java.lang.Thread");
  }

  private boolean isSubTypeOfThread(Tree tree) {
    return ((AbstractTypedTree) tree).getSymbolType().isSubtypeOf("java.lang.Thread");
  }
}
