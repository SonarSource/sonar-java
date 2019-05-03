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

import com.google.common.collect.ImmutableSet;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Rule(key = "S1158")
public class ToStringUsingBoxingCheck extends IssuableSubscriptionVisitor {

  private static final Set<String> PRIMITIVE_WRAPPERS = ImmutableSet.of(
    "Byte",
    "Short",
    "Integer",
    "Long",
    "Float",
    "Double",
    "Character",
    "Boolean");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    String callingToStringOrCompareTo = isCallingToStringOrCompareTo(mit.methodSelect());
    if (callingToStringOrCompareTo != null) {
      String newlyCreatedClassName = getNewlyCreatedClassName(mit);
      if (PRIMITIVE_WRAPPERS.contains(newlyCreatedClassName)) {
        reportIssue(((MemberSelectExpressionTree) mit.methodSelect()).expression(),
          "Call the static method " + newlyCreatedClassName + "." + callingToStringOrCompareTo +
            "(...) instead of instantiating a temporary object to perform this to string conversion.");
      }
    }
  }

  private static String getNewlyCreatedClassName(MethodInvocationTree mit) {
    MemberSelectExpressionTree mset = (MemberSelectExpressionTree) mit.methodSelect();
    if (mset.expression().is(Tree.Kind.NEW_CLASS)) {
      Tree classId = ((NewClassTree) mset.expression()).identifier();
      if (classId.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) classId).name();
      } else if (classId.is(Tree.Kind.MEMBER_SELECT)) {
        return ((MemberSelectExpressionTree) classId).identifier().name();
      }
    }
    return "";
  }

  @CheckForNull
  private static String isCallingToStringOrCompareTo(ExpressionTree methodSelect) {
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      String name = ((MemberSelectExpressionTree) methodSelect).identifier().name();
      if ("toString".equals(name)) {
        return name;
      } else if ("compareTo".equals(name)) {
        return "compare";
      }
    }
    return null;
  }
}
