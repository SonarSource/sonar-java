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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static java.lang.String.format;

@Rule(key = "S3252")
public class StaticMemberAccessCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcherCollection LIST_SET_OF = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition("java.util.List").name("of").withAnyParameters(),
    MethodMatcher.create().typeDefinition("java.util.Set").name("of").withAnyParameters()
  );

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.MEMBER_SELECT);
  }


  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    MemberSelectExpressionTree mse = (MemberSelectExpressionTree) tree;
    Symbol symbol = mse.identifier().symbol();
    if (symbol.isStatic()  && !isListOrSetOf(mse)) {
      ExpressionTree expression = mse.expression();
      Type staticType = symbol.owner().type();
      if (!expression.symbolType().equals(staticType)) {
        reportIssue(mse.identifier(),
          format("Use static access with \"%s\" for \"%s\".", staticType.fullyQualifiedName(), symbol.name()));
      }
    }
  }

  private static boolean isListOrSetOf(MemberSelectExpressionTree mse) {
    // this is necessary because we incorrectly resolve to Set#of List#of methods on JDK11
    // see SONARJAVA-3095
    Tree parent = mse.parent();
    return parent.is(Tree.Kind.METHOD_INVOCATION) && LIST_SET_OF.anyMatch((MethodInvocationTree) parent);
  }
}
