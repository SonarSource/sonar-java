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
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S1185",
  name = "Overriding methods should do more than simply call the same method in the super class ",
  tags = {"clumsy"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("5min")
public class MethodOnlyCallsSuperCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree methodTree = (MethodTree) tree;
    if (isSingleStatementMethod(methodTree) && isUselessSuperCall(methodTree)
      && !hasAnnotationDifferentFromOverride(methodTree.modifiers().annotations()) && !isFinalObjectMethod(methodTree)) {
      addIssue(methodTree, "Remove this method to simply inherit it.");
    }
  }

  private boolean isFinalObjectMethod(MethodTree methodTree) {
    MethodTreeImpl methodTreeImpl = (MethodTreeImpl) methodTree;
    return hasSemantic() && ModifiersUtils.hasModifier(methodTree.modifiers(), Modifier.FINAL) && isObjectMethod(methodTreeImpl);
  }

  private static boolean isObjectMethod(MethodTreeImpl methodTreeImpl) {
    return methodTreeImpl.isEqualsMethod() || methodTreeImpl.isHashCodeMethod() || methodTreeImpl.isToStringMethod();
  }

  private static boolean isSingleStatementMethod(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    return block != null && block.body().size() == 1;
  }

  private static boolean isUselessSuperCall(MethodTree methodTree) {
    ExpressionTree callToSuper = null;
    StatementTree statementTree = methodTree.block().body().get(0);
    if (returnsVoid(methodTree) && statementTree.is(Tree.Kind.EXPRESSION_STATEMENT)) {
      callToSuper = ((ExpressionStatementTree) statementTree).expression();
    } else if (statementTree.is(Tree.Kind.RETURN_STATEMENT)) {
      callToSuper = ((ReturnStatementTree) statementTree).expression();
    }
    return callToSuper != null && isCallToSuper(methodTree, callToSuper);
  }

  private static boolean isCallToSuper(MethodTree methodTree, Tree callToSuper) {
    if (callToSuper.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) callToSuper;
      if (methodInvocationTree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) methodInvocationTree.methodSelect();
        if (callSuperMethodWithSameName(mset, methodTree) && callsWithSameParameters(methodInvocationTree.arguments(), methodTree.parameters())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean callSuperMethodWithSameName(MemberSelectExpressionTree mset, MethodTree methodTree) {
    return mset.expression().is(Tree.Kind.IDENTIFIER)
      && "super".equals(((IdentifierTree) mset.expression()).name())
      && mset.identifier().name().equals(methodTree.simpleName().name());
  }

  private static boolean callsWithSameParameters(List<ExpressionTree> arguments, List<VariableTree> parameters) {
    if (arguments.size() != parameters.size()) {
      return false;
    }
    for (int i = 0; i < arguments.size(); i++) {
      ExpressionTree arg = arguments.get(i);
      VariableTree param = parameters.get(i);
      if (!(arg.is(Tree.Kind.IDENTIFIER) && ((IdentifierTree) arg).name().equals(param.simpleName().name()))) {
        return false;
      }
    }
    return true;
  }

  private static boolean returnsVoid(MethodTree methodTree) {
    Tree returnType = methodTree.returnType();
    return returnType != null && returnType.is(Tree.Kind.PRIMITIVE_TYPE) && "void".equals(((PrimitiveTypeTree) returnType).keyword().text());
  }

  private static boolean hasAnnotationDifferentFromOverride(List<AnnotationTree> annotations) {
    for (AnnotationTree annotation : annotations) {
      if (!(annotation.annotationType().is(Tree.Kind.IDENTIFIER) && "Override".equals(((IdentifierTree) annotation.annotationType()).name()))) {
        return true;
      }
    }
    return false;
  }

}
