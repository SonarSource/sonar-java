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

import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
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
    Symbol assignedVariable = findAssignedVariable(mit.parent());
    return assignedVariable == null || assignedVariable.usages().stream().noneMatch(CollectorsToListCheck::isListBeingModified);
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

  private static boolean isListBeingModified(Tree list) {
    return isListTargetOfCallAndBeingModified(list) || isListArgumentToCollectionsMutatorMethod(list);
  }

  private static boolean isListArgumentToCollectionsMutatorMethod(Tree list) {
    if (list.parent().is(Tree.Kind.ARGUMENTS)) {
      Tree potentialMethodInvocation = list.parent().parent();
      if (potentialMethodInvocation.is(Tree.Kind.METHOD_INVOCATION)) {
        MethodInvocationTree mit = (MethodInvocationTree) potentialMethodInvocation;
        return COLLECTIONS_MUTATOR_METHODS.matches(mit);
      }
    }
    return false;
  }

  private static boolean isListTargetOfCallAndBeingModified(Tree list) {
    while (list.parent().is(Tree.Kind.ARRAY_ACCESS_EXPRESSION, Tree.Kind.MEMBER_SELECT)) {
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

  @CheckForNull
  private static Symbol findAssignedVariable(Tree tree) {
    return switch (tree.kind()) {
      case ASSIGNMENT -> findAssignedVariable(((AssignmentExpressionTree) tree).variable());
      case VARIABLE -> ((VariableTree) tree).symbol();
      case IDENTIFIER -> ((IdentifierTree) tree).symbol();
      case MEMBER_SELECT -> ((MemberSelectExpressionTree) tree).identifier().symbol();
      case ARRAY_ACCESS_EXPRESSION -> findAssignedVariable(((ArrayAccessExpressionTree) tree).expression());
      default -> null;
    };
  }
}
