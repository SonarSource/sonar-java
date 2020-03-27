/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1641")
public class EnumSetCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers COLLECTIONS_UNMODIFIABLE = MethodMatchers.create()
    .ofTypes("java.util.Collections")
    .names("unmodifiableSet")
    .withAnyParameters()
    .build();

  private static final MethodMatchers SET_CREATION_METHODS = MethodMatchers.or(
    // Java 9 factory methods
    MethodMatchers.create().ofTypes("java.util.Set").names("of").withAnyParameters().build(),
    // guava
    MethodMatchers.create().ofTypes("com.google.common.collect.ImmutableSet").names("of").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("com.google.common.collect.Sets").anyName().withAnyParameters().build());

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
      } else if (!SET_CREATION_METHODS.matches(mit) || "immutableEnumSet".equals(mit.symbol().name())) {
        // Methods from Guava 'Sets' except 'immutableEnumSet' should be checked,
        // but discard any other method invocations (killing the noise)
        return;
      }
    }
    checkIssue(initializer.symbolType(), initializer);
  }

  private void checkIssue(Type type, Tree reportTree) {
    if (type.isSubtypeOf("java.util.Set") && !type.isSubtypeOf("java.util.EnumSet") && JUtils.isParametrized(type)) {
      Type typeArgument = JUtils.typeArguments(type).get(0);
      if (typeArgument != null && typeArgument.symbol().isEnum()) {
        reportIssue(reportTree, "Convert this Set to an EnumSet.");
      }
    }
  }

}
