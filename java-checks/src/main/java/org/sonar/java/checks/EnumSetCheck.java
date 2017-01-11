/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.ImmutableList;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
import org.sonar.java.resolve.TypeVariableJavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

@Rule(key = "S1641")
public class EnumSetCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher COLLECTIONS_UNMODIFIABLE = MethodMatcher.create().typeDefinition("java.util.Collections").name("unmodifiableSet").withAnyParameters();
  private static final MethodMatcher GUAVA_IMMUTABLE_ENUM_SET = MethodMatcher.create().typeDefinition("com.google.common.collect.Sets").name("immutableEnumSet")
    .withAnyParameters();

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    VariableTree variableTree = (VariableTree) tree;
    ExpressionTree initializer = variableTree.initializer();
    if (initializer != null) {
      if(initializer.is(Kind.METHOD_INVOCATION) && COLLECTIONS_UNMODIFIABLE.matches((MethodInvocationTree) initializer)) {
        initializer = ((MethodInvocationTree) initializer).arguments().get(0);
      }
      checkIssue(initializer.symbolType(), initializer, variableTree.type());
    }
  }

  private void checkIssue(Type type, Tree reportTree, TypeTree typeTree) {
    if (type.isSubtypeOf("java.util.Set") && !callToImmutableEnumSet(reportTree) && !type.isSubtypeOf("java.util.EnumSet") && type instanceof ParametrizedTypeJavaType) {
      ParametrizedTypeJavaType parametrizedType = (ParametrizedTypeJavaType) type;
      List<TypeVariableJavaType> typeParameters = parametrizedType.typeParameters();
      Type variableType = typeTree.symbolType();
      if(typeParameters.isEmpty() && variableType instanceof ParametrizedTypeJavaType) {
        // for java 7 diamond operator lookup declaration.
        parametrizedType = (ParametrizedTypeJavaType) variableType;
        typeParameters = parametrizedType.typeParameters();
      }
      if(!typeParameters.isEmpty()) {
        Type typeParameter = parametrizedType.substitution(typeParameters.get(0));
        if (typeParameter != null && typeParameter.symbol().isEnum()) {
          reportIssue(reportTree, "Convert this Set to an EnumSet.");
        }
      }
    }
  }

  private static boolean callToImmutableEnumSet(Tree tree) {
    return tree.is(Tree.Kind.METHOD_INVOCATION) && GUAVA_IMMUTABLE_ENUM_SET.matches((MethodInvocationTree) tree);
  }

}
