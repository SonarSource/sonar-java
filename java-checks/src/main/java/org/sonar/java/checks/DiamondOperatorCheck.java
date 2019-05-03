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

import org.apache.commons.lang.ArrayUtils;
import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaVersion;
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
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Rule(key = "S2293")
public class DiamondOperatorCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  private static final Tree.Kind[] JAVA_7_KINDS = new Tree.Kind[] {
    Tree.Kind.VARIABLE,
    Tree.Kind.TYPE_CAST,
    Tree.Kind.RETURN_STATEMENT,
    Tree.Kind.ASSIGNMENT};
  private static final Tree.Kind[] JAVA_8_KINDS = (Tree.Kind[]) ArrayUtils.addAll(JAVA_7_KINDS, new Tree.Kind[] {
    Tree.Kind.CONDITIONAL_EXPRESSION});
  private Tree.Kind[] expressionKindsToCheck = JAVA_7_KINDS;

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    if (version.isJava8Compatible()) {
      expressionKindsToCheck = JAVA_8_KINDS;
    }
    return version.isJava7Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    NewClassTree newClassTree = (NewClassTree) tree;
    TypeTree newTypeTree = newClassTree.identifier();
    if (newClassTree.classBody() == null && isParameterizedType(newTypeTree)) {
      TypeTree type = getTypeFromExpression(tree.parent(), expressionKindsToCheck);
      if (type != null && isParameterizedType(type)) {
        reportIssue(
          ((ParameterizedTypeTree) newTypeTree).typeArguments(),
          "Replace the type specification in this constructor call with the diamond operator (\"<>\")." +
            context.getJavaVersion().java7CompatibilityMessage());
      }
    }
  }

  @CheckForNull
  private static TypeTree getTypeFromExpression(Tree expression, Tree.Kind[] kinds) {
    if (expression.is(kinds)) {
      TypeTreeLocator visitor = new TypeTreeLocator(kinds);
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

    private final Tree.Kind[] kinds;

    @Nullable
    private TypeTree type = null;

    public TypeTreeLocator(Kind[] kinds) {
      this.kinds = kinds;
    }

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
        type = getTypeFromExpression(assignedVariable, kinds);
      }
    }

    @Override
    public void visitVariable(VariableTree tree) {
      type = tree.type();
    }

    @Override
    public void visitConditionalExpression(ConditionalExpressionTree tree) {
      type = getTypeFromExpression(tree.parent(), kinds);
    }

    @CheckForNull
    private static TypeTree getMethodReturnType(ReturnStatementTree returnStatementTree) {
      MethodTree methodTree = getParentMethod(returnStatementTree);
      if (methodTree != null) {
        return methodTree.returnType();
      }
      return null;
    }

    @CheckForNull
    private static MethodTree getParentMethod(Tree tree) {
      Tree result = tree;
      while (result != null && !result.is(Tree.Kind.METHOD)) {
        result = result.parent();
      }
      return (MethodTree) result;
    }

    @CheckForNull
    private static Tree getAssignedVariable(ExpressionTree expression) {
      IdentifierTree identifier;
      switch (expression.kind()) {
        case ARRAY_ACCESS_EXPRESSION:
          return getAssignedVariable(((ArrayAccessExpressionTree) expression).expression());
        case TYPE_CAST:
          return getAssignedVariable(((TypeCastTree) expression).expression());
        case PARENTHESIZED_EXPRESSION:
          return getAssignedVariable(((ParenthesizedTree) expression).expression());
        case IDENTIFIER:
          identifier = (IdentifierTree) expression;
          break;
        case MEMBER_SELECT:
          identifier = ((MemberSelectExpressionTree) expression).identifier();
          break;
        default:
          throw new IllegalStateException("Unexpected expression " + expression.kind().name() + " at: " + ((JavaTree) expression).getLine());
      }
      return identifier.symbol().declaration();
    }
  }
}
