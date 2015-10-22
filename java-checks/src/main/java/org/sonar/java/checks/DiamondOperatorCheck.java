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
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.checks.helpers.JavaVersionHelper;
import org.sonar.java.tag.Tag;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;

import java.util.List;

@Rule(
  key = "S2293",
  name = "The diamond operator (\"<>\") should be used",
  priority = Priority.MAJOR,
  tags = {Tag.JAVA_7, Tag.CLUMSY})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("5min")
public class DiamondOperatorCheck extends SubscriptionBaseVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(Integer version) {
    return JavaVersionHelper.java7Guaranteed(version);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    NewClassTree newClassTree = (NewClassTree) tree;
    TypeTree newTypeTree = newClassTree.identifier();
    if (newClassTree.classBody() == null && isParameterizedType(newTypeTree)) {
      TypeTree type = getTypeFromExpression(tree.parent());
      if (type != null && isParameterizedType(type)) {
        reportIssue(((ParameterizedTypeTree) newTypeTree).typeArguments(), "Replace the type specification in this constructor call.");
      }
    }
  }

  @CheckForNull
  private static TypeTree getTypeFromExpression(Tree expression) {
    if (expression.is(Tree.Kind.VARIABLE, Tree.Kind.TYPE_CAST, Tree.Kind.RETURN_STATEMENT, Tree.Kind.ASSIGNMENT, Tree.Kind.CONDITIONAL_EXPRESSION)) {
      TypeTreeLocator visitor = new TypeTreeLocator();
      expression.accept(visitor);
      return visitor.type;
    }
    return null;
  }

  private static boolean isParameterizedType(TypeTree type) {
    if (type.is(Tree.Kind.ARRAY_TYPE)) {
      return isParameterizedType(((ArrayTypeTree) type).type());
    }
    return type.is(Tree.Kind.PARAMETERIZED_TYPE) && !((ParameterizedTypeTree) type).typeArguments().isEmpty();
  }

  private static class TypeTreeLocator extends BaseTreeVisitor {

    private TypeTree type = null;

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      type = getMethodReturnType(tree);
    }

    @Override
    public void visitTypeCast(TypeCastTree tree) {
      type = tree.type();
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      Tree assignedVariable = getAssignedVariable(tree.variable());
      if (assignedVariable != null) {
        type = getTypeFromExpression(assignedVariable);
      }
    }

    @Override
    public void visitVariable(VariableTree tree) {
      type = tree.type();
    }

    @Override
    public void visitConditionalExpression(ConditionalExpressionTree tree) {
      type = getTypeFromExpression(tree.parent());
    }

    private static TypeTree getMethodReturnType(ReturnStatementTree returnStatementTree) {
      MethodTree methodTree = getParentMethod(returnStatementTree);
      return methodTree.returnType();
    }

    private static MethodTree getParentMethod(Tree tree) {
      Tree result = tree;
      while (!result.is(Tree.Kind.METHOD)) {
        result = result.parent();
      }
      return (MethodTree) result;
    }

    @CheckForNull
    private static Tree getAssignedVariable(ExpressionTree expression) {
      if (expression.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
        return getAssignedVariable(((ArrayAccessExpressionTree) expression).expression());
      }
      IdentifierTree identifier;
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        identifier = (IdentifierTree) expression;
      } else {
        identifier = ((MemberSelectExpressionTree) expression).identifier();
      }
      return identifier.symbol().declaration();
    }
  }

}
