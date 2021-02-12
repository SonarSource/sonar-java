/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.Preconditions;
import org.sonar.java.collections.SetUtils;
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

import static org.sonar.java.checks.helpers.ExpressionsHelper.getInvokedSymbol;
import static org.sonar.java.checks.helpers.ExpressionsHelper.getSingleWriteUsage;

@Rule(key = "S2053")
public class UnpredictableSaltCheck extends IssuableSubscriptionVisitor {

  private static final String ADD_SALT = "Add an unpredictable salt value to this hash.";
  private static final String UNPREDICTABLE_SALT = "Make this salt unpredictable.";
  private static final String MESSAGE_DIGEST = "java.security.MessageDigest";
  private static final Set<String> WEAK_HASH_ALGORITHMS = SetUtils.immutableSetOf(
    "MD2", "MD4", "MD5", "SHA-1", "RIPEMD160", "HMACRIPEMD160");
  
  private static final MethodMatchers UPDATE_MESSAGE_DIGEST = MethodMatchers.create()
    .ofSubTypes(MESSAGE_DIGEST)
    .names("update")
    .withAnyParameters()
    .build();

  private static final MethodMatchers DIGEST_METHOD = MethodMatchers.create()
    .ofSubTypes(MESSAGE_DIGEST)
    .names("digest")
    .withAnyParameters()
    .build();

  private static final MethodMatchers CREATE_DIGEST_METHOD = MethodMatchers.create()
    .ofSubTypes(MESSAGE_DIGEST)
    .names("getInstance")
    .addParametersMatcher("java.lang.String")
    .build();

  private static final MethodMatchers NEW_PBE_KEY_SPEC = MethodMatchers.create()
    .ofSubTypes("javax.crypto.spec.PBEKeySpec")
    .constructor()
    .withAnyParameters()
    .build();

  private static final MethodMatchers GET_BYTES = MethodMatchers.create()
    .ofTypes("java.lang.String")
    .names("getBytes")
    .withAnyParameters()
    .build();

  private final Map<Symbol, Integer> updateCalls = new HashMap<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      NewClassTree newClassTree = (NewClassTree) tree;
      checkPBEKeySpec(newClassTree);
      return;
    }
    checkDigestMethod((MethodInvocationTree) tree);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    updateCalls.clear();
    super.setContext(context);
  }

  private void checkDigestMethod(MethodInvocationTree methodInvocationTree) { 
    if (UPDATE_MESSAGE_DIGEST.matches(methodInvocationTree)) {
      getInvokedSymbol(methodInvocationTree)
        .ifPresent(symbol ->  
          updateCalls.compute(symbol, (k, v) -> (v == null) ? 1 : (v + 1)));

    } else if (DIGEST_METHOD.matches(methodInvocationTree)) {
      getInvokedSymbol(methodInvocationTree)
        .filter(UnpredictableSaltCheck::isStrongDigestAlgorithm)
        .ifPresent(symbol -> {
          Integer count = updateCalls.get(symbol);
          if (count == null || (count == 1 && methodInvocationTree.arguments().isEmpty())) {
            reportIssue(methodInvocationTree, ADD_SALT);
          }
        });
    }
  }

  private void checkPBEKeySpec(NewClassTree newClassTree) {
    if (NEW_PBE_KEY_SPEC.matches(newClassTree)) {
      if (newClassTree.arguments().size() <= 1) {
        reportIssue(newClassTree, ADD_SALT);
      } else {
        ExpressionTree saltExpression = ExpressionUtils.skipParentheses(newClassTree.arguments().get(1));
        List<JavaFileScannerContext.Location> locations = new ArrayList<>();
        if (isPredictable(saltExpression, locations)) {
          reportIssue(newClassTree, UNPREDICTABLE_SALT, locations, null);
        }
      }
    }
  }

  private static boolean isStrongDigestAlgorithm(Symbol symbol) {
    return Optional.ofNullable(getSingleWriteUsage(symbol))
      .filter(expressionTree -> expressionTree.is(Tree.Kind.METHOD_INVOCATION))
      .map(MethodInvocationTree.class::cast)
      .filter(CREATE_DIGEST_METHOD::matches)
      .flatMap(mit -> mit.arguments().get(0).asConstant(String.class))
      .filter(arg -> !WEAK_HASH_ALGORITHMS.contains(arg.toUpperCase(Locale.ROOT)))
      .isPresent();
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
