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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(
  key = "S1848",
  priority = Priority.CRITICAL,
  tags = {"bug"})
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class UselessObjectCreationCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.EXPRESSION_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionStatementTree expressionStatement = (ExpressionStatementTree) tree;
    ExpressionTree expression = expressionStatement.expression();
    if (expression.is(Tree.Kind.NEW_CLASS)) {
      String className = getClassName(((NewClassTree) expression).identifier());
      addIssue(tree, "Either remove this useless object instantiation of class \"" + className + "\" or use it");
    }
  }

  private String getClassName(Tree identifier) {
    String name = "";
    if (identifier.is(Tree.Kind.IDENTIFIER)) {
      name = ((IdentifierTree) identifier).name();
    } else if (identifier.is(Tree.Kind.MEMBER_SELECT)) {
      name = ((MemberSelectExpressionTree) identifier).identifier().name();
    } else if (identifier.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      name = getClassName(((ParameterizedTypeTree) identifier).type());
    }
    return name;
  }

}
