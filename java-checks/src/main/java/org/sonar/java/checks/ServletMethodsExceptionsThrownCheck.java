/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

@Rule(key = "S1989")
public class ServletMethodsExceptionsThrownCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers IS_SERVLET_DO_METHOD = MethodMatchers.create()
    .ofSubTypes("javax.servlet.http.HttpServlet", "jakarta.servlet.http.HttpServlet")
    .names("doGet", "doPost", "doPut", "doDelete", "doHead", "doOptions", "doTrace")
    .withAnyParameters().build();

  private final Deque<Boolean> shouldCheck = new ArrayDeque<>();
  private final Deque<List<Type>> tryCatches = new ArrayDeque<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.THROW_STATEMENT, Tree.Kind.METHOD_INVOCATION, Tree.Kind.TRY_STATEMENT, Tree.Kind.CATCH);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      shouldCheck.push(IS_SERVLET_DO_METHOD.matches((MethodTree) tree));
    } else if (shouldCheck()) {
      if (tree.is(Tree.Kind.TRY_STATEMENT)) {
        tryCatches.push(getCaughtExceptions(((TryStatementTree) tree).catches()));
      } else if (tree.is(Tree.Kind.CATCH)) {
        tryCatches.pop();
        tryCatches.push(Collections.emptyList());
      } else if (tree.is(Tree.Kind.THROW_STATEMENT)) {
        addIssueIfNotCaught(((ThrowStatementTree) tree).expression().symbolType(), tree);
      } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
        checkMethodInvocation((MethodInvocationTree) tree);
      }
    }
  }

  private boolean shouldCheck() {
    return !shouldCheck.isEmpty() && shouldCheck.peek();
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      shouldCheck.pop();
    } else if (shouldCheck() && tree.is(Tree.Kind.TRY_STATEMENT)) {
      tryCatches.pop();
    }
  }

  private static List<Type> getCaughtExceptions(List<CatchTree> catches) {
    List<Type> result = new ArrayList<>();
    for (CatchTree element : catches) {
      result.add(element.parameter().type().symbolType());
    }
    return result;
  }

  private void checkMethodInvocation(MethodInvocationTree node) {
    if (node.methodSelect() instanceof MemberSelectExpressionTree memberSelect && isRunMethod(memberSelect)) {
      Tree parent = ExpressionUtils.getParentOfType(memberSelect, Tree.Kind.MEMBER_SELECT);
      var parentMemberSelect = (MemberSelectExpressionTree) parent;
      if (parentMemberSelect == null || !"onFailure".equals(parentMemberSelect.identifier().name())) {
        reportIssue(memberSelect, "Handle the exception thrown by this method call.");
        return;
      }
    }
    
    Symbol.MethodSymbol symbol = node.methodSymbol();
    if (!symbol.isUnknown()) {
      List<Type> types = symbol.thrownTypes();
      if (!types.isEmpty()) {
        addIssueIfNotCaught(types, ExpressionUtils.methodName(node), symbol.name());
      }
    }
  }

  private static boolean isRunMethod(MemberSelectExpressionTree memberSelect) {
    return memberSelect.expression() instanceof IdentifierTree identifier && "Try".equals(identifier.name())
      && ("run".equals(memberSelect.identifier().name()) || "runRunnable".equals(memberSelect.identifier().name()));
  }

  private void addIssueIfNotCaught(Type thrown, Tree node) {
    if (isNotCaught(thrown)) {
      String message = "Handle the " + "\"" + thrown.name() + "\"" + " thrown here in a \"try/catch\" block.";
      reportIssue(node, message);
    }
  }

  private void addIssueIfNotCaught(Iterable<Type> thrown, Tree node, String methodName) {
    List<Type> uncaughtTypes = new ArrayList<>();
    for (Type type : thrown) {
      if (isNotCaught(type)) {
        uncaughtTypes.add(type);
      }
    }
    if (!uncaughtTypes.isEmpty()) {
      reportIssue(node, buildMessage(methodName, uncaughtTypes));
    }
  }

  private static String buildMessage(String methodName, List<Type> uncaughtTypes) {
    String uncaught = uncaughtTypes.stream().map(Type::name).collect(Collectors.joining(", ")) + ".";
    return String.format("Handle the following exception%s that could be thrown by \"%s\": %s",
      (uncaughtTypes.size() == 1 ? "" : "s"),
      methodName,
      uncaught);
  }

  private boolean isNotCaught(Type type) {
    for (List<Type> tryCatch : tryCatches) {
      for (Type tryCatchType : tryCatch) {
        if (type.isSubtypeOf(tryCatchType)) {
          return false;
        }
      }
    }
    return true;
  }

}
