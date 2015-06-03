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
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;

import java.util.List;

@Rule(
  key = "S2131",
  name = "Primitives should not be boxed just for \"String\" conversion",
  tags = {"performance"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.MEMORY_EFFICIENCY)
@SqaleConstantRemediation("5min")
public class PrimitiveTypeBoxingWithToStringCheck extends AbstractMethodDetection {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.<Kind>builder().addAll(super.nodesToVisit()).add(Kind.PLUS).build();
  }

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return getToStringMatchers(
      "java.lang.Byte",
      "java.lang.Character",
      "java.lang.Short",
      "java.lang.Integer",
      "java.lang.Long",
      "java.lang.Float",
      "java.lang.Double",
      "java.lang.Boolean");
  }

  private static List<MethodInvocationMatcher> getToStringMatchers(String... typeFullyQualifiedNames) {
    List<MethodInvocationMatcher> matchers = Lists.newArrayList();
    for (String fullyQualifiedName : typeFullyQualifiedNames) {
      matchers.add(MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf(fullyQualifiedName))
        .name("toString"));
    }
    return matchers;
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Kind.PLUS)) {
      checkPrimitiveConcatenationWithEmptyString((BinaryExpressionTree) tree);
    } else {
      super.visitNode(tree);
    }
  }

  @CheckForNull
  private void checkPrimitiveConcatenationWithEmptyString(BinaryExpressionTree concatenation) {
    Type wrapper = null;
    if (isEmptyString(concatenation.leftOperand())) {
      wrapper = ((JavaType) concatenation.rightOperand().symbolType()).primitiveWrapperType();
    } else if (isEmptyString(concatenation.rightOperand())) {
      wrapper = ((JavaType) concatenation.leftOperand().symbolType()).primitiveWrapperType();
    }
    if (wrapper != null) {
      addIssue(concatenation, "Use \"" + wrapper.name() + ".toString\" instead.");
    }
  }

  private static boolean isEmptyString(ExpressionTree expressionTree) {
    return expressionTree.is(Kind.STRING_LITERAL) && LiteralUtils.trimQuotes(((LiteralTree) expressionTree).value()).isEmpty();
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    ExpressionTree abstractTypedTree = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
    if (abstractTypedTree.is(Kind.NEW_CLASS) || isValueOfInvocation(abstractTypedTree)) {
      String typeName = abstractTypedTree.symbolType().toString();
      addIssue(mit, "Use \"" + typeName + ".toString\" instead.");
    }
  }

  private static boolean isValueOfInvocation(ExpressionTree abstractTypedTree) {
    if (!abstractTypedTree.is(Kind.METHOD_INVOCATION)) {
      return false;
    }
    Type type = abstractTypedTree.symbolType();
    MethodInvocationMatcher valueOfMatcher = MethodInvocationMatcher.create()
      .typeDefinition(type.fullyQualifiedName())
      .name("valueOf")
      .addParameter(((JavaType) type).primitiveType().fullyQualifiedName());
    return valueOfMatcher.matches((MethodInvocationTree) abstractTypedTree);
  }
}
