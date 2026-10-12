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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
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
   * A private field is considered safe when none of its usages can alter the digest state concurrently. Assignments of the field
   * are ignored, and the remaining usages must either all be calls which do not alter the digest state (e.g. {@code clone()},
   * when the field is only used as a prototype), or all be guarded by the same known monitor. For a static field, this monitor
   * must be shared by all instances: a static final lock field, a class literal or a static synchronized method.
   */
  private static boolean isOnlyUsedSafely(Symbol symbol) {
    if (!symbol.isPrivate()) {
      return false;
    }
    List<IdentifierTree> readUsages = symbol.usages().stream()
      .filter(usage -> !isAssignedTo(usage))
      .toList();
    if (readUsages.stream().allMatch(MessageDigestInstanceFieldCheck::isReceiverOfStatelessCall)) {
      return true;
    }
    Set<Optional<Monitor>> monitors = readUsages.stream()
      .map(MessageDigestInstanceFieldCheck::monitorOf)
      .collect(Collectors.toSet());
    return monitors.size() == 1 && monitors.iterator().next().filter(monitor -> monitor.canGuard(symbol)).isPresent();
  }

  private static Tree fieldExpression(IdentifierTree usage) {
    Tree parent = usage.parent();
    if (parent.is(Tree.Kind.MEMBER_SELECT) && ((MemberSelectExpressionTree) parent).identifier() == usage) {
      return parent;
    }
    return usage;
  }

  private static boolean isAssignedTo(IdentifierTree usage) {
    Tree expression = fieldExpression(usage);
    Tree parent = expression.parent();
    return parent.is(Tree.Kind.ASSIGNMENT) && ((AssignmentExpressionTree) parent).variable() == expression;
  }

  private static boolean isReceiverOfStatelessCall(IdentifierTree usage) {
    Tree expression = fieldExpression(usage);
    Tree parent = expression.parent();
    if (!parent.is(Tree.Kind.MEMBER_SELECT) || ((MemberSelectExpressionTree) parent).expression() != expression) {
      return false;
    }
    Tree invocation = parent.parent();
    return invocation.is(Tree.Kind.METHOD_INVOCATION) && STATELESS_METHODS.matches((MethodInvocationTree) invocation);
  }

  private static Optional<Monitor> monitorOf(Tree tree) {
    Tree current = tree.parent();
    while (!current.is(Tree.Kind.CONSTRUCTOR, Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.RECORD, Tree.Kind.INTERFACE)) {
      if (current.is(Tree.Kind.SYNCHRONIZED_STATEMENT)) {
        return lockMonitor(((SynchronizedStatementTree) current).expression());
      }
      if (current.is(Tree.Kind.METHOD)) {
        return methodMonitor((MethodTree) current);
      }
      current = current.parent();
    }
    return Optional.empty();
  }

  private static Optional<Monitor> methodMonitor(MethodTree method) {
    if (!ModifiersUtils.hasModifier(method.modifiers(), Modifier.SYNCHRONIZED)) {
      return Optional.empty();
    }
    Symbol owner = method.symbol().owner();
    return Optional.of(new Monitor(owner, method.symbol().isStatic() ? null : owner));
  }

  private static Optional<Monitor> lockMonitor(ExpressionTree lock) {
    ExpressionTree expression = ExpressionUtils.skipParentheses(lock);
    if (expression.is(Tree.Kind.IDENTIFIER) && "this".equals(((IdentifierTree) expression).name())) {
      Symbol owner = expression.symbolType().symbol();
      return Optional.of(new Monitor(owner, owner));
    }
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) expression;
      if ("class".equals(memberSelect.identifier().name())) {
        return Optional.of(new Monitor(memberSelect.expression().symbolType().symbol(), null));
      }
      if (!ExpressionUtils.isSelectOnThisOrSuper(memberSelect)) {
        return Optional.empty();
      }
      expression = memberSelect.identifier();
    }
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      Symbol lockSymbol = ((IdentifierTree) expression).symbol();
      if (lockSymbol.isVariableSymbol() && lockSymbol.owner().isTypeSymbol() && lockSymbol.isFinal()) {
        return Optional.of(new Monitor(lockSymbol, lockSymbol.isStatic() ? null : lockSymbol.owner()));
      }
    }
    return Optional.empty();
  }

  /**
   * Object whose lock guards a usage. {@code instanceOwner} is the class whose instances each hold their own monitor, or
   * {@code null} when the monitor is shared by all instances.
   */
  private record Monitor(Symbol symbol, @Nullable Symbol instanceOwner) {
    boolean canGuard(Symbol field) {
      return instanceOwner == null || (!field.isStatic() && instanceOwner.equals(field.owner()));
    }
  }
}
