/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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

import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.Arrays;
import java.util.List;

@Rule(key = "S3052")
public class DefaultInitializedFieldCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Arrays.asList(Kind.CLASS, Kind.ENUM);
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
      initializer = ExpressionUtils.skipParentheses(initializer);
      if (isDefault(initializer, member.type().symbolType().isPrimitive())) {
        reportIssue(initializer, "Remove this initialization to \"" + ((LiteralTree) initializer).value() + "\", the compiler will do that for you.");
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
        return LiteralUtils.isFalse(expression);
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
