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
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(
  key = "S2165",
  priority = Priority.MAJOR,
  tags = {"clumsy", "performance"})
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class FinalizeFieldsSetCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (isFinalizeDeclaration(methodTree)) {
      methodTree.accept(new AssignmentVisitor());
    }
  }

  private boolean isFinalizeDeclaration(MethodTree tree) {
    return hasSemantic() && isMethodNamedFinalize(tree) && isOverriding(tree) && hasNoParameters(tree);
  }

  private boolean isMethodNamedFinalize(MethodTree tree) {
    return "finalize".equals(tree.simpleName().name());
  }

  private boolean isOverriding(MethodTree tree) {
    return Boolean.TRUE.equals(((MethodTreeImpl) tree).isOverriding());
  }

  private boolean hasNoParameters(MethodTree tree) {
    return tree.parameters().isEmpty();
  }

  private class AssignmentVisitor extends BaseTreeVisitor {
    @Override
    public void visitClass(ClassTree tree) {
      // Do not visit inner classes as their methods will be visited by main visitor
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (isFieldAssignment(tree) && isNullAssignment(tree)) {
        addIssue(tree, "Remove this nullification of \"" + getFieldName(tree) + "\".");
      }
    }

    private boolean isFieldAssignment(AssignmentExpressionTree tree) {
      ExpressionTree variableTree = tree.variable();
      if (variableTree.is(Kind.IDENTIFIER)) {
        Symbol variableSymbol = getSemanticModel().getReference((IdentifierTree) variableTree);
        return variableSymbol.owner().isKind(Symbol.TYP);
      }
      return false;
    }

    private boolean isNullAssignment(AssignmentExpressionTree assignmentTree) {
      return assignmentTree.expression().is(Kind.NULL_LITERAL);
    }

    private String getFieldName(AssignmentExpressionTree tree) {
      return ((IdentifierTree) tree.variable()).name();
    }
  }
}
