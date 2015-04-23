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
import com.google.common.collect.Iterables;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2159",
  name = "Silly equality checks should not be made",
  tags = {"bug", "unused"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("15min")
public class SillyEqualsCheck extends SubscriptionBaseVisitor {

  private static final String JAVA_LANG_OBJECT = "java.lang.Object";

  private static final String MESSAGE = "Remove this call to \"equals\"; comparisons between unrelated types always return false.";

  private static final MethodInvocationMatcher EQUALS_METHOD = MethodInvocationMatcher.create()
    .name("equals")
    .addParameter(JAVA_LANG_OBJECT);

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree methodTree = (MethodInvocationTree) tree;
    if (EQUALS_METHOD.matches(methodTree)) {
      ExpressionTree firstArgument = Iterables.getOnlyElement(methodTree.arguments());
      Type argumentType = firstArgument.symbolType().erasure();
      Type ownerType = getMethodOwnerType(methodTree).erasure();
      if (isLiteralNull(firstArgument)) {
        addIssue(tree, "Remove this call to \"equals\"; comparisons against null always return false; consider using '== null' to check for nullity.");
      } else if (ownerType.isArray()) {
        checkWhenOwnerIsArray(tree, ownerType, argumentType);
      } else {
        checkWhenOwnerIsNotArray(tree, ownerType, argumentType);
      }
    }
  }

  private void checkWhenOwnerIsArray(Tree tree, Type ownerType, Type argumentType) {
    if (!argumentType.isArray()) {
      if (!argumentType.is(JAVA_LANG_OBJECT)) {
        addIssue(tree, "Remove this call to \"equals\"; comparisons between an array and a type always return false.");
      }
    } else if (!areRelated(((Type.ArrayType) ownerType).elementType(), ((Type.ArrayType) argumentType).elementType())) {
      addIssue(tree, "Remove this call to \"equals\"; comparisons between unrelated arrays always return false.");
    } else {
      addIssue(tree, "Use \"Arrays.equals(array1, array2)\" or the \"==\" operator instead of using the \"Object.equals(Object obj)\" method.");
    }
  }

  private void checkWhenOwnerIsNotArray(Tree tree, Type ownerType, Type argumentType) {
    if (argumentType.isArray() && !ownerType.is(JAVA_LANG_OBJECT)) {
      addIssue(tree, "Remove this call to \"equals\"; comparisons between a type and an array always return false.");
    } else if (argumentType.isClass() && !areRelated(ownerType, argumentType)) {
      if (isFinalClassWithInterface(ownerType, argumentType)) {
        addIssue(tree, MESSAGE);
      } else if (!ownerType.symbol().isInterface() && !argumentType.symbol().isInterface()) {
        addIssue(tree, MESSAGE);
      }
    }
  }

  private boolean isFinalClassWithInterface(Type type1, Type type2) {
    return (type1.symbol().isInterface() && type2.symbol().isFinal()) || (type2.symbol().isInterface() && type1.symbol().isFinal());
  }

  private boolean isLiteralNull(Tree tree) {
    return tree.is(Tree.Kind.NULL_LITERAL);
  }

  private Type getMethodOwnerType(MethodInvocationTree methodSelectTree) {
    if (methodSelectTree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) methodSelectTree.methodSelect()).expression().symbolType();
    } else {
      return methodSelectTree.symbol().owner().type();
    }
  }

  private boolean areRelated(Type type1, Type type2) {
    return type1.isSubtypeOf(type2) || type2.isSubtypeOf(type1);
  }

}
