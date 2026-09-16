/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9401")
public class IdentityHashMapBoxedKeyCheck extends IssuableSubscriptionVisitor {

  private static final String IDENTITY_HASH_MAP = "java.util.IdentityHashMap";
  private static final String MESSAGE = "Use a map that compares keys by value because IdentityHashMap compares keys by reference.";
  private static final Set<String> BOXED_TYPES = Set.of(
    "java.lang.Boolean",
    "java.lang.Byte",
    "java.lang.Character",
    "java.lang.Short",
    "java.lang.Integer",
    "java.lang.Long",
    "java.lang.Float",
    "java.lang.Double");
  private static final MethodMatchers GUAVA_IDENTITY_HASH_MAP_FACTORY = MethodMatchers.create()
    .ofTypes("com.google.common.collect.Maps")
    .names("newIdentityHashMap")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionTree expression = (ExpressionTree) tree;
    if (isIdentityHashMapCreation(tree, expression.symbolType()) && hasBoxedKey(expression.symbolType())) {
      reportIssue(tree, MESSAGE);
    }
  }

  private static boolean isIdentityHashMapCreation(Tree tree, Type resultType) {
    return tree.is(Tree.Kind.NEW_CLASS)
      ? resultType.is(IDENTITY_HASH_MAP)
      : GUAVA_IDENTITY_HASH_MAP_FACTORY.matches((MethodInvocationTree) tree);
  }

  private static boolean hasBoxedKey(Type resultType) {
    List<Type> typeArguments = resultType.typeArguments();
    return typeArguments.size() == 2 && BOXED_TYPES.contains(typeArguments.get(0).fullyQualifiedName());
  }
}
