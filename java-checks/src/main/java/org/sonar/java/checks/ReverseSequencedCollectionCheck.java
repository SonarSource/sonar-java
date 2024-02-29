/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.EnumSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.VariableSymbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6877")
public class ReverseSequencedCollectionCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {

  private static final String MESSAGE = "Remove this \"reverse\" statement and replace \"%s\" with \"%s.reversed()\" after.";

  private static final String ADD_ALL = "addAll";

  private static final MethodMatchers LIST_CONSTRUCTORS = MethodMatchers.create()
    .ofTypes(
      "java.util.ArrayList",
      "java.util.LinkedList",
      "java.util.Vector",
      "java.util.Stack",
      "java.util.concurrent.CopyOnWriteArrayList",
      "javax.management.AttributeList",
      "javax.management.relation.RoleList",
      "javax.management.relation.RoleUnresolvedList")
    .constructor()
    .withAnyParameters()
    .build();

  private static final MethodMatchers LIST_READONLY_CONSUMERS = MethodMatchers.create()
    .ofSubTypes("java.util.List")
    .names(ADD_ALL, "copyOf")
    .addParametersMatcher("java.util.Collection")
    .build();

  private static final MethodMatchers LIST_READ_ACCESSORS = MethodMatchers.or(
    MethodMatchers.create()
      .ofAnyType()
      .names("clone", "getClass", "getFirst", "getLast", "hashCode", "isEmpty", "listIterator", "parallelStream",
        "size", "spliterator", "stream", "toArray", "toString", "wait", "notify", "notifyAll")
      .addWithoutParametersMatcher()
      .build(),
    MethodMatchers.create()
      .ofAnyType()
      .names("contains", "containsAll", "equals", "forEach", "get", "indexOf", "lastIndexOf", "listIterator", "toArray")
      .addParametersMatcher(MethodMatchers.ANY)
      .build());

  private static final MethodMatchers LIST_WRITE_ACCESSORS = MethodMatchers.or(
    MethodMatchers.create()
      .ofAnyType()
      .names("clear", "removeFirst", "removeLast")
      .addWithoutParametersMatcher()
      .build(),
    MethodMatchers.create()
      .ofAnyType()
      .names("add", ADD_ALL, "addFirst", "addLast", "remove", "removeAll", "replaceAll", "retainAll", "sort")
      .addParametersMatcher(MethodMatchers.ANY)
      .build(),
    MethodMatchers.create()
      .ofAnyType()
      .names("add", ADD_ALL, "set")
      .addParametersMatcher("int", MethodMatchers.ANY)
      .build());

  private static final Set<Tree.Kind> SUPPORTED_REVERSE_SCOPE_CHILD_KINDS = EnumSet.of(
    Tree.Kind.IDENTIFIER,
    Tree.Kind.ARGUMENTS,
    Tree.Kind.METHOD_INVOCATION,
    Tree.Kind.EXPRESSION_STATEMENT,
    Tree.Kind.MEMBER_SELECT,
    Tree.Kind.BLOCK,
    Tree.Kind.IF_STATEMENT);

  private static final Set<Tree.Kind> SUPPORTED_USAGE_SCOPE_CHILD_KINDS = EnumSet.of(
    Tree.Kind.IDENTIFIER,
    Tree.Kind.MEMBER_SELECT,
    Tree.Kind.METHOD_INVOCATION,
    Tree.Kind.ASSIGNMENT,
    Tree.Kind.EXPRESSION_STATEMENT,
    Tree.Kind.BLOCK,
    Tree.Kind.IF_STATEMENT);

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create()
      .ofTypes("java.util.Collections")
      .names("reverse")
      .addParametersMatcher("java.util.List")
      .build();
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree methodInvocation) {
    Arguments reverseMethodArguments = methodInvocation.arguments();
    if (reverseMethodArguments.isEmpty() || !reverseMethodArguments.get(0).is(Tree.Kind.IDENTIFIER)) {
      return;
    }
    IdentifierTree reverseMethodArgument = (IdentifierTree) reverseMethodArguments.get(0);
    Symbol symbol = reverseMethodArgument.symbol();
    // Only support local variables. "Collections.reverse(list)" mutates "list" and return void. If "list" was a
    // field or a method parameter, we don't know if the mutation of the "list" is used outside of this method body. So
    // we don't raise issue.
    if (!symbol.isLocalVariable() || symbol.isParameter()) {
      return;
    }
    VariableSymbol reverseMethodArgumentSymbol = (VariableSymbol) symbol;
    if (areUsagesCompatibleWithReversed(reverseMethodArgument, reverseMethodArgumentSymbol)) {
      String message = String.format(MESSAGE, reverseMethodArgumentSymbol.name(), reverseMethodArgumentSymbol.name());
      reportIssue(ExpressionUtils.methodName(methodInvocation), message);
    }
  }

  private static boolean areUsagesCompatibleWithReversed(IdentifierTree reverseArgument, VariableSymbol listSymbol) {
    VariableTree declaration = listSymbol.declaration();
    // We are checking that there are no write modifiers of "list" after "Collections.reverse(list)".
    // Because we don't use symbolic execution, we will know that a usage is after by using its position in the file.
    // This strategy only works if there are no loops or lambdas. To ensure this, we will check that "Collections.reverse(list)"
    // and its write usages share the same "reverseParentScope".
    Tree reverseParentScope = findParentScope(reverseArgument, SUPPORTED_REVERSE_SCOPE_CHILD_KINDS);
    if (declaration == null || reverseParentScope == null ||
      !isInitializerCompatibleWithReversed(declaration, reverseParentScope)) {
      return false;
    }
    for (IdentifierTree usage : listSymbol.usages()) {
      if (!isUsageCompatibleWithReversed(usage, reverseArgument, reverseParentScope)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isInitializerCompatibleWithReversed(VariableTree declaration, Tree reverseParentScope) {
    if (declaration.parent() instanceof ForEachStatement) {
      return false;
    }
    ExpressionTree initializer = declaration.initializer();
    if (initializer == null) {
      return true;
    }
    return isNullOrListConstructor(initializer) &&
      matchReverseParentSafeScope(declaration.parent(), reverseParentScope);
  }

  private static boolean isUsageCompatibleWithReversed(IdentifierTree usage, IdentifierTree reverseArgument, Tree reverseParentScope) {
    if (usage == reverseArgument || isCompatibleReadUsage(usage)) {
      return true;
    }
    if (isAfter(usage, reverseArgument)) {
      return false;
    }
    return isCompatibleWriteUsage(usage) && matchReverseParentSafeScope(usage, reverseParentScope);
  }

  private static boolean isCompatibleWriteUsage(IdentifierTree usage) {
    if (matchAccessor(usage, LIST_WRITE_ACCESSORS)) {
      return true;
    }
    return usage.parent() instanceof AssignmentExpressionTree assignmentExpression &&
      isNullOrListConstructor(assignmentExpression.expression());
  }

  private static boolean isCompatibleReadUsage(IdentifierTree usage) {
    if (matchAccessor(usage, LIST_READ_ACCESSORS)) {
      return true;
    }
    Tree parent = usage.parent();
    if (parent instanceof Arguments arguments) {
      return isListReadOnlyConsumer(arguments.parent());
    } else {
      return parent instanceof ForEachStatement forEachStatement && forEachStatement.expression() == usage;
    }
  }

  /**
   * @return true if there is a method call on the given "usage" identifier, e.g. "list.getFirst()", and if the
   * method call matches the given "methodMatchers".
   */
  private static boolean matchAccessor(IdentifierTree usage, MethodMatchers methodMatchers) {
    Tree parent = usage.parent();
    if (parent instanceof MemberSelectExpressionTree memberSelect && memberSelect.expression() == usage) {
      Tree grandParent = parent.parent();
      if (grandParent instanceof MethodInvocationTree methodInvocation && methodInvocation.methodSelect() == parent) {
        return methodMatchers.matches(methodInvocation);
      }
    }
    return false;
  }

  private static boolean isAfter(IdentifierTree a, IdentifierTree b) {
    return a.identifierToken().range().start().isAfter(b.identifierToken().range().start());
  }

  private static boolean isNullOrListConstructor(ExpressionTree expression) {
    if (expression.is(Tree.Kind.NULL_LITERAL)) {
      return true;
    }
    return isListConstructor(expression);
  }

  private static boolean isListReadOnlyConsumer(@Nullable Tree tree) {
    if (tree instanceof MethodInvocationTree methodInvocation) {
      return LIST_READONLY_CONSUMERS.matches(methodInvocation);
    }
    return isListConstructor(tree);
  }

  private static boolean isListConstructor(@Nullable Tree tree) {
    return tree instanceof NewClassTree newClassTree && LIST_CONSTRUCTORS.matches(newClassTree);
  }

  private static Tree findParentScope(@Nullable Tree tree, Set<Tree.Kind> supportedChildKinds) {
    while (tree != null && supportedChildKinds.contains(tree.kind())) {
      tree = tree.parent();
    }
    return tree;
  }

  /**
   * @return true if we can reach "reverseParentScope" following "tree" parents and continuing only for
   * the given Tree.Kind list. This will ensure that between the given tree and the "Collections.reverse(list)" call,
   * there are no loops or lambdas.
   */
  private static boolean matchReverseParentSafeScope(@Nullable Tree tree, Tree reverseParentScope) {
    return findParentScope(tree, SUPPORTED_USAGE_SCOPE_CHILD_KINDS) == reverseParentScope;
  }

}
