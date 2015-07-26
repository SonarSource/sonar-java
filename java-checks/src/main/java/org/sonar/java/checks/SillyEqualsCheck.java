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
import com.google.common.collect.Iterables;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
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
public class SillyEqualsCheck extends AbstractMethodDetection {

  private static final String JAVA_LANG_OBJECT = "java.lang.Object";

  private static final String MESSAGE = "Remove this call to \"equals\"; comparisons between unrelated types always return false.";

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(MethodMatcher.create()
      .name("equals")
      .addParameter(JAVA_LANG_OBJECT));
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree tree) {
    ExpressionTree firstArgument = Iterables.getOnlyElement(tree.arguments());
    Type argumentType = firstArgument.symbolType().erasure();
    if (argumentType.isPrimitive()) {
      argumentType = ((JavaType) argumentType).primitiveWrapperType();
    }
    Type ownerType = getMethodOwnerType(tree).erasure();
    if (isLiteralNull(firstArgument)) {
      addIssue(tree, "Remove this call to \"equals\"; comparisons against null always return false; consider using '== null' to check for nullity.");
    } else if (ownerType.isArray()) {
      checkWhenOwnerIsArray(tree, (Type.ArrayType) ownerType, argumentType);
    } else {
      checkWhenOwnerIsNotArray(tree, ownerType, argumentType);
    }
  }

  private void checkWhenOwnerIsArray(Tree tree, Type.ArrayType ownerType, Type argumentType) {
    if (argumentType.isArray()) {
      if (areNotRelated(ownerType.elementType(), ((Type.ArrayType) argumentType).elementType())) {
        addIssue(tree, "Remove this call to \"equals\"; comparisons between unrelated arrays always return false.");
      } else {
        addIssue(tree, "Use \"Arrays.equals(array1, array2)\" or the \"==\" operator instead of using the \"Object.equals(Object obj)\" method.");
      }
    } else if (!argumentType.is(JAVA_LANG_OBJECT)) {
      addIssue(tree, "Remove this call to \"equals\"; comparisons between an array and a type always return false.");
    }
  }

  private void checkWhenOwnerIsNotArray(Tree tree, Type ownerType, Type argumentType) {
    if (argumentType.isArray() && !ownerType.is(JAVA_LANG_OBJECT)) {
      addIssue(tree, "Remove this call to \"equals\"; comparisons between a type and an array always return false.");
    } else if (argumentType.isClass() && areNotRelated(ownerType, argumentType)
      && (areTypesFinalClassAndInterface(ownerType, argumentType) || areNeitherInterfaces(ownerType, argumentType))) {
      addIssue(tree, MESSAGE);
    }
  }

  private static boolean areNeitherInterfaces(Type ownerType, Type argumentType) {
    return !ownerType.symbol().isInterface() && !argumentType.symbol().isInterface();
  }

  private static boolean areTypesFinalClassAndInterface(Type ownerType, Type argumentType) {
    return (ownerType.symbol().isInterface() && argumentType.symbol().isFinal()) || (argumentType.symbol().isInterface() && ownerType.symbol().isFinal());
  }

  private static boolean isLiteralNull(Tree tree) {
    return tree.is(Tree.Kind.NULL_LITERAL);
  }

  private static Type getMethodOwnerType(MethodInvocationTree methodSelectTree) {
    if (methodSelectTree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) methodSelectTree.methodSelect()).expression().symbolType();
    } else {
      return methodSelectTree.symbol().owner().type();
    }
  }

  private static boolean areNotRelated(Type type1, Type type2) {
    return !type1.isSubtypeOf(type2) && !type2.isSubtypeOf(type1);
  }

}
