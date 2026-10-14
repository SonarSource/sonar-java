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
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9428")
public class MessageDigestInstanceFieldCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE_DIGEST = "java.security.MessageDigest";
  private static final String MESSAGE = "Make this MessageDigest a local variable.";

  private static final MethodMatchers STATELESS_METHODS = MethodMatchers.create()
    .ofSubTypes(MESSAGE_DIGEST)
    .names("clone", "getAlgorithm", "getDigestLength", "getProvider", "toString")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    VariableTree variable = (VariableTree) tree;
    Symbol symbol = variable.symbol();
    if (symbol.type().isSubtypeOf(MESSAGE_DIGEST) && symbol.owner().isTypeSymbol() && !isOnlyUsedAsPrototype(symbol)) {
      reportIssue(variable, MESSAGE);
    }
  }

  /**
   * A private field is only used as a prototype when, apart from being assigned, it is only the receiver of calls which do not
   * depend on or alter the digest state, such as {@code clone()} or {@code getAlgorithm()}.
   */
  private static boolean isOnlyUsedAsPrototype(Symbol symbol) {
    return symbol.isPrivate() && symbol.usages().stream()
      .map(MessageDigestInstanceFieldCheck::fieldExpression)
      .allMatch(expression -> isAssignedTo(expression) || isReceiverOfStatelessCall(expression));
  }

  private static Tree fieldExpression(IdentifierTree usage) {
    Tree parent = usage.parent();
    if (parent.is(Tree.Kind.MEMBER_SELECT) && ((MemberSelectExpressionTree) parent).identifier() == usage) {
      return parent;
    }
    return usage;
  }

  private static boolean isAssignedTo(Tree expression) {
    Tree parent = expression.parent();
    return parent.is(Tree.Kind.ASSIGNMENT) && ((AssignmentExpressionTree) parent).variable() == expression;
  }

  private static boolean isReceiverOfStatelessCall(Tree expression) {
    Tree parent = expression.parent();
    if (!parent.is(Tree.Kind.MEMBER_SELECT) || ((MemberSelectExpressionTree) parent).expression() != expression) {
      return false;
    }
    Tree invocation = parent.parent();
    return invocation.is(Tree.Kind.METHOD_INVOCATION) && STATELESS_METHODS.matches((MethodInvocationTree) invocation);
  }
}
