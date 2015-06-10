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
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S1659",
  name = "Multiple variables should not be declared on the same line",
  tags = {"convention"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
public class OneDeclarationPerLineCheck extends SubscriptionBaseVisitor {

  private int varLineLast;
  private List<String> varsOnSameLine = new ArrayList<>();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    varLineLast = -1;
    varsOnSameLine.clear();
    super.scanFile(context);
  }

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree varTree = (VariableTree) tree;
    int varLineCurrent = varTree.endToken().line();
    if (varLineCurrent == varLineLast) {
      varsOnSameLine.add(varTree.symbol().name());
    } else {
      if (!varsOnSameLine.isEmpty()) {
        addIssue(varLineLast, String.format("Declare \"%s\" on a separate line.", StringUtils.join(varsOnSameLine, "\", \"")));
      }
      varLineLast = varLineCurrent;
      varsOnSameLine.clear();
    }
  }
}
