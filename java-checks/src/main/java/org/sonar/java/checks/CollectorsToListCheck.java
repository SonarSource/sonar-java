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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6204")
public class CollectorsToListCheck extends AbstractMethodDetection implements JavaVersionAwareVisitor {
  private static final String MESSAGE = "Replace this usage of 'Stream.collect(Collectors.%s())' with 'Stream.toList()'";

  private static final MethodMatchers COLLECT = MethodMatchers.create()
    .ofSubTypes("java.util.stream.Stream")
    .names("collect")
    .addParametersMatcher("java.util.stream.Collector")
    .build();

  private static final MethodMatchers COLLECTORS_TO_LIST = MethodMatchers.create()
    .ofSubTypes("java.util.stream.Collectors")
    .names("toList")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers COLLECTORS_TO_UNMODIFIABLE_LIST = MethodMatchers.create()
    .ofSubTypes("java.util.stream.Collectors")
    .names("toUnmodifiableList")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers LIST_MODIFICATION_METHODS = MethodMatchers.create()
    .ofSubTypes("java.util.List")
    .names("add", "addAll", "remove", "removeAll", "retainAll", "replaceAll", "set", "sort", "clear", "removeIf")
    .withAnyParameters()
    .build();

  private static final MethodMatchers COLLECTIONS_METHODS = MethodMatchers.create()
    .ofSubTypes("java.util.Collections")
    .anyName()
    .withAnyParameters()
    .build();

  private static final MethodMatchers COLLECTIONS_MUTATOR_METHODS = MethodMatchers.create()
    .ofSubTypes("java.util.Collections")
    .names("addAll", "copy", "fill", "replaceAll", "reverse", "rotate", "shuffle", "sort", "swap")
    .withAnyParameters()
    .build();

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava16Compatible();
  }

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return COLLECT;
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    boolean mutable;
    if (!mit.arguments().get(0).is(Tree.Kind.METHOD_INVOCATION)) return;
    MethodInvocationTree collector = (MethodInvocationTree) mit.arguments().get(0);
    if (COLLECTORS_TO_LIST.matches(collector)) {
      mutable = true;
    } else if (COLLECTORS_TO_UNMODIFIABLE_LIST.matches(collector)) {
      mutable = false;
    } else {
      return;
    }
    if (isInvariantTypeArgument(mit) && (!mutable || isReturnedListUnmodified(mit))) {
      reportIssue(collector, mutable);
    }
  }

  private static boolean isReturnedListUnmodified(MethodInvocationTree mit) {
    var graylist = new HashSet<Symbol>();
    if (isPossibleMutatorExpression(mit, graylist)) {
      return false;
    }
    if (graylist.isEmpty()) {
      return true;
    }

    var whiteList = new HashSet<Symbol>();
    do {
      final var nextGrayList = new HashSet<Symbol>();
      if (graylist.stream().anyMatch(symbol -> isPossibleMutatorSymbol(symbol, nextGrayList))) {
        return false;
      }
      whiteList.addAll(graylist);
      nextGrayList.removeAll(whiteList);
      graylist = nextGrayList;
    } while (!graylist.isEmpty());
    return true;
  }

  private static boolean isPossibleMutatorSymbol(Symbol symbol, Set<Symbol> graylist) {
    return symbol.usages().stream().anyMatch(it -> isPossibleMutatorExpression(it, graylist));
  }

  private static boolean isPossibleMutatorExpression(ExpressionTree tree, Set<Symbol> graylist) {
    var parent = tree.parent();
    return switch (parent.kind()) {
      case LAMBDA_EXPRESSION ->
        // Don't perform deeper data flow analysis; assume receiver of lambda result might perform mutations
        true;
      case RETURN_STATEMENT ->
        // Don't perform deeper data flow analysis; assume receiver might perform mutations only for non-private methods
        !isReturnFromPrivateMethod(tree);
      case YIELD_STATEMENT -> isPossibleMutatorExpression(
        (ExpressionTree) Objects.requireNonNull(ExpressionUtils.getEnclosingElementAnyType(tree, Tree.Kind.SWITCH_EXPRESSION)),
        graylist
      );
      case VARIABLE -> {
        graylist.add(((VariableTree) parent).symbol());
        yield false;
      }
      case ASSIGNMENT -> !addAssignmentLocalVariableToGraylist((AssignmentExpressionTree) parent, graylist);
      case ARGUMENTS -> isArgumentInPossibleMutatingInvocation(parent);
      case MEMBER_SELECT -> isListTargetOfCallAndBeingModified(tree);
      case EXPRESSION_STATEMENT -> false;
      default ->
        // Don't perform deeper data flow analysis; if semantic of expression is not explicitly handled, assume it might result in a mutation
        true;
    };
  }

  private static boolean isReturnFromPrivateMethod(ExpressionTree tree) {
    Tree enclosing = ExpressionUtils.getEnclosingElementAnyType(tree, Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION);
    return enclosing instanceof MethodTree method && method.symbol().isPrivate();
  }

  private static boolean addAssignmentLocalVariableToGraylist(AssignmentExpressionTree tree, Set<Symbol> graylist) {
    if (tree.variable() instanceof IdentifierTree name && name.symbol().isLocalVariable()) {
      graylist.add(name.symbol());
      return true;
    }
    return false;
  }

  private static boolean isArgumentInPossibleMutatingInvocation(Tree tree) {
    Tree potentialMethodInvocation = tree.parent();
    if (potentialMethodInvocation.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) potentialMethodInvocation;
      return !COLLECTIONS_METHODS.matches(mit) || COLLECTIONS_MUTATOR_METHODS.matches(mit);
    }
    return false;
  }

  private static boolean isListTargetOfCallAndBeingModified(Tree list) {
    while (list.parent().is(Tree.Kind.MEMBER_SELECT)) {
      Tree possibleMethodInvocation = list.parent().parent();
      if (possibleMethodInvocation.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) possibleMethodInvocation;
        ExpressionTree receiver = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
        return receiver == list && LIST_MODIFICATION_METHODS.matches(mit);
      }
      list = list.parent();
    }
    return false;
  }

  private static boolean isInvariantTypeArgument(MethodInvocationTree collectMethodInvocation) {
    Type streamType = collectMethodInvocation.methodSymbol().owner().type();
    if (streamType.isRawType()) {
      return true;
    }
    Type collectArgType = collectMethodInvocation.symbolType().typeArguments().get(0);
    Type streamArgType = streamType.typeArguments().get(0);
    return collectArgType.is(streamArgType.fullyQualifiedName());
  }

  private void reportIssue(MethodInvocationTree collector, boolean mutable) {
    String message = String.format(MESSAGE, mutable ? "toList" : "toUnmodifiableList");
    reportIssue(collector, message);
  }
}
