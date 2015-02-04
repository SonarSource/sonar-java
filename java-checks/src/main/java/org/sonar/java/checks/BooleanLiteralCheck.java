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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1125",
  name = "Literal boolean values should not be used in condition expressions",
  priority = Priority.MINOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MINOR)
@SqaleSubCharacteristic(value = RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation(value = "10min")
public class BooleanLiteralCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.EQUAL_TO, Kind.NOT_EQUAL_TO, Kind.CONDITIONAL_AND, Kind.CONDITIONAL_OR, Kind.LOGICAL_COMPLEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    String literal;
    if(tree.is(Kind.LOGICAL_COMPLEMENT)) {
      literal = getBooleanLiteral(((UnaryExpressionTree)tree).expression());
    } else {
      literal = getBooleanLiteralOperands((BinaryExpressionTree)tree);
    }
    if(literal != null) {
      addIssue(tree, "Remove the literal \"" + literal + "\" boolean value.");
    }
  }

  private String getBooleanLiteral(Tree tree) {
    String result = null;
    if (tree.is(Kind.BOOLEAN_LITERAL)) {
      result = ((LiteralTree) tree).value();
    }
    return result;
  }

  private String getBooleanLiteralOperands(BinaryExpressionTree tree) {
    String result = getBooleanLiteral(tree.leftOperand());
    if (result == null) {
      result = getBooleanLiteral(tree.rightOperand());
    }
    return result;
  }
}
