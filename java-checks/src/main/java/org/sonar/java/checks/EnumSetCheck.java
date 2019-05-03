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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.NameCriteria;
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

@Rule(key = "S1641")
public class EnumSetCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher COLLECTIONS_UNMODIFIABLE = MethodMatcher.create().typeDefinition("java.util.Collections").name("unmodifiableSet").withAnyParameters();
  private static final MethodMatcherCollection SET_CREATION_METHODS = MethodMatcherCollection.create(
    // Java 9 factory methods
    MethodMatcher.create().typeDefinition("java.util.Set").name("of").withAnyParameters(),
    // guava
    MethodMatcher.create().typeDefinition("com.google.common.collect.ImmutableSet").name("of").withAnyParameters(),
    MethodMatcher.create().typeDefinition("com.google.common.collect.Sets").name(NameCriteria.any()).withAnyParameters());

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    VariableTree variableTree = (VariableTree) tree;
    ExpressionTree initializer = variableTree.initializer();
    if (initializer == null) {
      return;
    }
    if (initializer.is(Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) initializer;
      if (COLLECTIONS_UNMODIFIABLE.matches(mit)) {
        // check the collection used as parameter
        initializer = mit.arguments().get(0);
      } else if (!SET_CREATION_METHODS.anyMatch(mit) || "immutableEnumSet".equals(mit.symbol().name())) {
        // Methods from Guava 'Sets' except 'immutableEnumSet' should be checked,
        // but discard any other method invocations (killing the noise)
        return;
      }
    }
    checkIssue(initializer.symbolType(), initializer, variableTree.type());
  }

  private void checkIssue(Type type, Tree reportTree, TypeTree typeTree) {
    if (type.isSubtypeOf("java.util.Set") && !type.isSubtypeOf("java.util.EnumSet") && type instanceof ParametrizedTypeJavaType) {
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

}
