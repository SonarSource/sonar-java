/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.NameCriteria;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

@Rule(key = "S1989")
public class ServletMethodsExceptionsThrownCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher IS_SERVLET_DO_METHOD = MethodMatcher.create()
    .typeDefinition(TypeCriteria.subtypeOf("javax.servlet.http.HttpServlet")).name(NameCriteria.startsWith("do")).withAnyParameters();

  private final Deque<Boolean> shouldCheck = new ArrayDeque<>();
  private final Deque<List<Type>> tryCatches = new ArrayDeque<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.THROW_STATEMENT, Tree.Kind.METHOD_INVOCATION, Tree.Kind.TRY_STATEMENT, Tree.Kind.CATCH);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(Tree.Kind.METHOD)) {
      shouldCheck.push(IS_SERVLET_DO_METHOD.matches((MethodTree) tree));
    } else if (shouldCheck()) {
      if (tree.is(Tree.Kind.TRY_STATEMENT)) {
        tryCatches.add(getCaughtExceptions(((TryStatementTree) tree).catches()));
      } else if (tree.is(Tree.Kind.CATCH)) {
        tryCatches.pop();
        tryCatches.add(Collections.emptyList());
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
    if (!hasSemantic()) {
      return;
    }
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
    Symbol symbol = node.symbol();
    if (symbol.isMethodSymbol()) {
      List<Type> types = ((Symbol.MethodSymbol) symbol).thrownTypes();
      if (!types.isEmpty()) {
        addIssueIfNotCaught(types, ExpressionUtils.methodName(node), symbol.name());
      }
    }
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
