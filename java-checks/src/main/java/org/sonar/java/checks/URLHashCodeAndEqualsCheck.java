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
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.MethodInvocationMatcherCollection;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;

import java.util.List;

@Rule(
  key = "S2112",
  name = "\"URL.hashCode\" and \"URL.equals\" should be avoided",
  tags = {"performance"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ARCHITECTURE_RELIABILITY)
@SqaleConstantRemediation("20min")
public class URLHashCodeAndEqualsCheck extends SubscriptionBaseVisitor {

  private static final String JAVA_NET_URL = "java.net.URL";

  private static final MethodInvocationMatcherCollection URL_MATCHERS = MethodInvocationMatcherCollection.create(
    MethodMatcher.create().typeDefinition(JAVA_NET_URL).name("equals").addParameter("java.lang.Object"),
    MethodMatcher.create().typeDefinition(JAVA_NET_URL).name("hashCode"));

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.VARIABLE, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) tree;
      if (variableTree.initializer() != null) {
        Type variableType = variableTree.type().symbolType();
        if (isSubTypeOfSetOrMap(variableType) && usesURLAsTypeParameter(variableType)) {
          insertIssue(tree);
        }
      }
    } else {
      if (hasSemantic() && URL_MATCHERS.anyMatch((MethodInvocationTree) tree)) {
        insertIssue(tree);
      }
    }
  }

  private static boolean isSubTypeOfSetOrMap(Type type) {
    return type.isSubtypeOf("java.util.Set") || type.isSubtypeOf("java.util.Map");
  }

  private static boolean usesURLAsTypeParameter(Type type) {
    Type firstTypeParameter = getFirstTypeParameter(type);
    return firstTypeParameter != null && firstTypeParameter.is(JAVA_NET_URL);
  }

  @CheckForNull
  private static Type getFirstTypeParameter(Type type) {
    if (type instanceof JavaType.ParametrizedTypeJavaType) {
      JavaType.ParametrizedTypeJavaType parametrizedTypeType = (JavaType.ParametrizedTypeJavaType) type;
      for (JavaType.TypeVariableJavaType variableType : parametrizedTypeType.typeParameters()) {
        return parametrizedTypeType.substitution(variableType);
      }
    }
    return null;
  }

  private void insertIssue(Tree tree) {
    addIssue(tree, "Use the URI class instead.");
  }

}
