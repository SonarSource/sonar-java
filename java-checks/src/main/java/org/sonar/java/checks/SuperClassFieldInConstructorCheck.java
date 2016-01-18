/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S3399",
  name = "Super class fields should not be assigned from constructors",
  priority = Priority.MAJOR,
  tags = {Tag.SUSPICIOUS})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("5min")
public class SuperClassFieldInConstructorCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    tree.accept(new AssignmentInConstructor(((MethodTree) tree).symbol().owner()));
  }

  private class AssignmentInConstructor extends BaseTreeVisitor {
    private final Symbol owner;

    public AssignmentInConstructor(Symbol owner) {
      this.owner = owner;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      super.visitAssignmentExpression(tree);
      ExpressionTree variable = tree.variable();
      IdentifierTree idTree = null;
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        idTree = (IdentifierTree) variable;
      } else if (variable.is(Tree.Kind.MEMBER_SELECT)) {
        idTree = ((MemberSelectExpressionTree) variable).identifier();
      }

      if (idTree != null && isFieldFromSuperClass(idTree)) {
        reportIssue(idTree, "Invoke the \"super\" constructor that sets this field instead.");
      }
    }

    private boolean isFieldFromSuperClass(IdentifierTree idTree) {
      Type idOwnerType = idTree.symbol().owner().type();
      return owner.type() != idOwnerType && owner.type().isSubtypeOf(idOwnerType);
    }
  }
}
