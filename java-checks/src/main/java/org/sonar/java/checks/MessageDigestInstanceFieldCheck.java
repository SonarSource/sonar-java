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
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
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

  private boolean hasSemantic;

  @Override
  public void setContext(JavaFileScannerContext context) {
    super.setContext(context);
    hasSemantic = context.getSemanticModel() != null;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic) {
      return;
    }
    VariableTree variable = (VariableTree) tree;
    Symbol symbol = variable.symbol();
    Type type = symbol.type();
    if (symbol.owner().isTypeSymbol() && !type.isUnknown() && type.isSubtypeOf(MESSAGE_DIGEST) && !isOnlyUsedSafely(symbol)) {
      reportIssue(variable.simpleName(), MESSAGE);
    }
  }

  /**
   * A private field is considered safe when none of its usages can alter the digest state concurrently: each usage is either
   * an assignment of the field, a call to a method which does not depend on the digest state (e.g. {@code clone()}),
   * or is located in a {@code synchronized} block or method.
   */
  private static boolean isOnlyUsedSafely(Symbol symbol) {
    return symbol.isPrivate() && symbol.usages().stream().allMatch(MessageDigestInstanceFieldCheck::isSafeUsage);
  }

  private static boolean isSafeUsage(IdentifierTree usage) {
    Tree expression = usage;
    Tree parent = usage.parent();
    if (parent.is(Tree.Kind.MEMBER_SELECT) && ((MemberSelectExpressionTree) parent).identifier() == usage) {
      expression = parent;
      parent = parent.parent();
    }
    return isAssignedTo(expression, parent) || isReceiverOfStatelessCall(expression, parent) || isInSynchronizedContext(usage);
  }

  private static boolean isAssignedTo(Tree expression, Tree parent) {
    return parent.is(Tree.Kind.ASSIGNMENT) && ((AssignmentExpressionTree) parent).variable() == expression;
  }

  private static boolean isReceiverOfStatelessCall(Tree expression, Tree parent) {
    if (!parent.is(Tree.Kind.MEMBER_SELECT) || ((MemberSelectExpressionTree) parent).expression() != expression) {
      return false;
    }
    Tree invocation = parent.parent();
    return invocation.is(Tree.Kind.METHOD_INVOCATION) && STATELESS_METHODS.matches((MethodInvocationTree) invocation);
  }

  private static boolean isInSynchronizedContext(Tree tree) {
    Tree current = tree.parent();
    while (current != null) {
      if (current.is(Tree.Kind.SYNCHRONIZED_STATEMENT)) {
        return true;
      }
      if (current.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR)) {
        return ModifiersUtils.hasModifier(((MethodTree) current).modifiers(), Modifier.SYNCHRONIZED);
      }
      if (current.is(Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.RECORD, Tree.Kind.INTERFACE)) {
        return false;
      }
      current = current.parent();
    }
    return false;
  }
}
