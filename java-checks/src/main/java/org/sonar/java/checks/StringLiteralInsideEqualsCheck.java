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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.List;

@Rule(key = "S1132")
public class StringLiteralInsideEqualsCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    check((MethodInvocationTree) tree);
  }

  private void check(MethodInvocationTree tree) {
    if (isEquals(tree.methodSelect()) && tree.arguments().size() == 1 && tree.arguments().get(0).is(Kind.STRING_LITERAL)) {
      LiteralTree stringLiteral = (LiteralTree) tree.arguments().get(0);
      reportIssue(stringLiteral, "Move the " + stringLiteral.value() + " string literal on the left side of this string comparison.");
    }
  }

  private static boolean isEquals(ExpressionTree tree) {
    if (tree.is(Kind.IDENTIFIER)) {
      return isEquals((IdentifierTree) tree);
    } else if (tree.is(Kind.MEMBER_SELECT)) {
      return isEquals(((MemberSelectExpressionTree) tree).identifier());
    } else {
      return false;
    }
  }

  private static boolean isEquals(IdentifierTree tree) {
    return "equals".equals(tree.name()) ||
      "equalsIgnoreCase".equals(tree.name());
  }

}
