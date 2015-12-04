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
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S3052",
  name = "Fields should not be initialized to default values",
  priority = Priority.MINOR,
  tags = {Tag.CONVENTION})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
public class DefaultInitializedFieldCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.CLASS, Kind.ENUM);
  }

  @Override
  public void visitNode(Tree tree) {
    List<Tree> members = ((ClassTree) tree).members();
    for (Tree member : members) {
      if (member.is(Kind.VARIABLE)) {
        checkVariable((VariableTree) member);
      }
    }
  }

  private void checkVariable(VariableTree member) {
    if (ModifiersUtils.hasModifier(member.modifiers(), Modifier.FINAL)) {
      return;
    }
    ExpressionTree initializer = member.initializer();
    if (initializer != null) {
      initializer = ExpressionsHelper.skipParentheses(initializer);
      if (isDefault(initializer, member.type().symbolType().isPrimitive())) {
        addIssue(member, "Remove this initialization to \"" + ((LiteralTree) initializer).value() + "\", the compiler will do that for you.");
      }
    }
  }

  private static boolean isDefault(ExpressionTree expression, boolean isPrimitive) {
    if(!isPrimitive) {
      return expression.is(Kind.NULL_LITERAL);
    }
    switch (expression.kind()) {
      case CHAR_LITERAL:
        String charValue = ((LiteralTree) expression).value();
        return "'\\u0000'".equals(charValue) || "'\\0'".equals(charValue);
      case BOOLEAN_LITERAL:
        return "false".equals(((LiteralTree) expression).value());
      case INT_LITERAL:
      case LONG_LITERAL:
        Long value = LiteralUtils.longLiteralValue(expression);
        return value != null && value == 0;
      case FLOAT_LITERAL:
      case DOUBLE_LITERAL:
        return Double.doubleToLongBits(Double.valueOf(((LiteralTree) expression).value())) == 0;
      default:
        return false;
    }
  }

}
