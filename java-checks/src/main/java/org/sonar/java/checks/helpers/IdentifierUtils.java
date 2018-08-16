/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java.checks.helpers;

import java.util.function.Function;
import javax.annotation.CheckForNull;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

public class IdentifierUtils {
  private IdentifierUtils() {
    // This class only contains static methods
  }

  @CheckForNull
  public static <T> T getValue(ExpressionTree expression, Function<ExpressionTree,T> resolver) {
    T value = resolver.apply(expression);
    if (value == null && expression.is(Tree.Kind.IDENTIFIER)) {
      ExpressionTree last = ReassignmentFinder.getClosestReassignmentOrDeclarationExpression(expression, ((IdentifierTree) expression).symbol());
      if (last == null || !isStrictAssignmentOrDeclaration(last) || last == expression) {
        value = null;
      } else {
        value = getValue(last, resolver);
      }
    }
    return value;
  }

  private static boolean isStrictAssignmentOrDeclaration(ExpressionTree expression) {
    if (expression.parent() instanceof AssignmentExpressionTree) {
      return expression.parent().is(Tree.Kind.ASSIGNMENT);
    }
    return true;
  }
}
