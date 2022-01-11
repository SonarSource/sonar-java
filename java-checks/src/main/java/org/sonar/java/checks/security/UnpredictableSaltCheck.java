/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.Preconditions;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.ExpressionsHelper.getSingleWriteUsage;

@Rule(key = "S2053")
public class UnpredictableSaltCheck extends IssuableSubscriptionVisitor {

  private static final String UNPREDICTABLE_SALT = "Make this salt unpredictable.";

  private static final String BYTE_ARRAY = "byte[]";
  private static final MethodMatchers NEW_PBE_KEY_SPEC = MethodMatchers.create()
    .ofSubTypes("javax.crypto.spec.PBEKeySpec")
    .constructor()
    .addParametersMatcher("char[]", BYTE_ARRAY, "int", "int")
    .addParametersMatcher("char[]", BYTE_ARRAY, "int")
    .build();

  private static final MethodMatchers NEW_PBE_PARAM_SPEC = MethodMatchers.create()
    .ofSubTypes("javax.crypto.spec.PBEParameterSpec")
    .constructor()
    .addParametersMatcher(BYTE_ARRAY, "int")
    .addParametersMatcher(BYTE_ARRAY, "int", "java.security.spec.AlgorithmParameterSpec")
    .build();

  private static final MethodMatchers GET_BYTES = MethodMatchers.create()
    .ofTypes("java.lang.String")
    .names("getBytes")
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    NewClassTree newClassTree = (NewClassTree) tree;
    saltExpression(((NewClassTree) tree))
    .map(ExpressionUtils::skipParentheses)
    .ifPresent(salt -> {
      List<JavaFileScannerContext.Location> locations = new ArrayList<>();
      if (isPredictable(salt, locations)) {
        reportIssue(newClassTree, UNPREDICTABLE_SALT, locations, null);
      }
    });
  }
  
  private static Optional<ExpressionTree> saltExpression(NewClassTree tree) {
    if (NEW_PBE_KEY_SPEC.matches(tree)) {
      return Optional.of(tree.arguments().get(1));
    } else if (NEW_PBE_PARAM_SPEC.matches(tree)) {
      return Optional.of(tree.arguments().get(0));
    }
    return Optional.empty();
  }

  private static boolean isPredictable(ExpressionTree saltExpression, List<JavaFileScannerContext.Location> locations) {
    return (saltExpression.is(Tree.Kind.METHOD_INVOCATION) && isInitializedWithGetBytes((MethodInvocationTree) saltExpression)) ||
      (saltExpression.is(Tree.Kind.IDENTIFIER) && isInitializedWithLiteral((IdentifierTree) saltExpression, locations));
  }

  private static boolean isInitializedWithLiteral(IdentifierTree identifier, List<JavaFileScannerContext.Location> locations) {
    Symbol symbol = identifier.symbol();
    return Optional.ofNullable(getSingleWriteUsage(symbol))
      .filter(expressionTree -> expressionTree.is(Tree.Kind.METHOD_INVOCATION))
      .map(MethodInvocationTree.class::cast)
      .map(mit -> {
        locations.add(new JavaFileScannerContext.Location("Salt initialized with a constant.", mit));
        return isInitializedWithGetBytes(mit);
      })
      .orElse(false);
  }

  private static boolean isInitializedWithGetBytes(MethodInvocationTree mit) {
    if (!GET_BYTES.matches(mit)) {
      return false;
    }
    ExpressionTree methodSelect = (mit).methodSelect();
    Preconditions.checkState(methodSelect.is(Tree.Kind.MEMBER_SELECT),
      "'getBytes' method invocation should have a MEMBER_SELECT kind as expression.");
    ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
    return expression.asConstant().isPresent();
  }
}
