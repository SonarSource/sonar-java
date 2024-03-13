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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.JavaVersionAwareVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6906")
public class VirtualThreadNotSynchronizedCheck extends IssuableSubscriptionVisitor implements JavaVersionAwareVisitor {

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava21Compatible();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    var runnablesCollector = new RunnablesToCheckCollector();
    tree.accept(runnablesCollector);

    var synchonizedTreesCollector = new TreesWithSynchronizedCodeCollector();
    runnablesCollector.invocations.stream()
      .filter(it -> synchonizedTreesCollector.isInvokingSynchronizedCode(it.arguments().get(0)))
      .forEach(it -> reportIssue(it, synchonizedTreesCollector.getSecondaryLocation(it.arguments().get(0))));
  }

  void reportIssue(MethodInvocationTree tree, Tree secondaryLocation) {
    reportIssue(
      tree.methodSelect(),
      "Use a platform thread instead of a virtual thread",
      List.of(new JavaFileScannerContext.Location("synchronized", secondaryLocation)),
      null
    );
  }

  private static class RunnablesToCheckCollector extends BaseTreeVisitor {

    private static final String OF_VIRTUAL = "java.lang.Thread$Builder$OfVirtual";

    private static final MethodMatchers VIRTUAL_THREAD_BUILDER_METHODS = MethodMatchers.or(
      MethodMatchers.create()
        .ofSubTypes("java.lang.Thread$Builder")
        .names("start", "unstarted")
        .addParametersMatcher("java.lang.Runnable").build(),
      MethodMatchers.create()
        .ofTypes("java.lang.Thread")
        .names("startVirtualThread")
        .addParametersMatcher("java.lang.Runnable").build(),
      MethodMatchers.create()
        .ofSubTypes("java.util.concurrent.ExecutorService")
        .names("execute", "submit")
        .withAnyParameters().build()
    );

    private static final MethodMatchers EXECUTOR_BUILDER_SERVICE_WITH_VIRTUAL_TASKS_METHOD = MethodMatchers.create()
      .ofTypes("java.util.concurrent.Executors")
      .names("newVirtualThreadPerTaskExecutor")
      .withAnyParameters().build();

    public final List<MethodInvocationTree> invocations = new ArrayList<>();

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      super.visitMethodInvocation(tree);
      if (VIRTUAL_THREAD_BUILDER_METHODS.matches(tree) && isRunnableInVirtualThread(tree)) {
        invocations.add(tree);
      }
    }

    private static boolean isRunnableInVirtualThread(MethodInvocationTree tree) {
      return switch (tree.methodSymbol().name()) {
        case "start", "unstarted" -> isCallToOfVirtual(tree);
        case "execute", "submit" -> isCallToExecutorServiceWithVirtualTasks(tree);
        default -> true;
      };
    }

    private static boolean isCallToOfVirtual(MethodInvocationTree tree) {
      var callSiteExpression = ((MemberSelectExpressionTree) tree.methodSelect()).expression();
      if (callSiteExpression.symbolType().is(OF_VIRTUAL)) {
        return true;
      }
      // Cover the case that call site is not type OfVirtual, but initialized with OfVirtual
      if (callSiteExpression instanceof IdentifierTree identifier && identifier.symbol().declaration() instanceof VariableTree variableTree) {
        var initializer = variableTree.initializer();
        return initializer != null && initializer.symbolType().is(OF_VIRTUAL);
      }
      return false;
    }

    private static boolean isCallToExecutorServiceWithVirtualTasks(MethodInvocationTree tree) {
      var callSiteExpression = ((MemberSelectExpressionTree) tree.methodSelect()).expression();
      if (isCallToExecutorServiceBuilderWithVirtualTasks(callSiteExpression)) {
        return true;
      }

      if (callSiteExpression instanceof IdentifierTree identifier && identifier.symbol().declaration() instanceof VariableTree variableTree) {
        var initializer = variableTree.initializer();
        return initializer != null && isCallToExecutorServiceBuilderWithVirtualTasks(initializer);
      }
      return false;
    }

    private static boolean isCallToExecutorServiceBuilderWithVirtualTasks(ExpressionTree expression) {
      return expression instanceof MethodInvocationTree mit && EXECUTOR_BUILDER_SERVICE_WITH_VIRTUAL_TASKS_METHOD.matches(mit);
    }
  }

  private static class TreesWithSynchronizedCodeCollector extends BaseTreeVisitor {

    private Tree currentCheckedTree;

    private final Map<Tree, Tree> treesWithSynchronizedCode = new HashMap<>();

    private final Set<Tree> checkedTrees = new HashSet<>();

    private boolean isSynchronizedAttributeFound = false;

    public boolean isInvokingSynchronizedCode(Tree tree) {
      this.currentCheckedTree = tree;
      isSynchronizedAttributeFound = false;
      tree.accept(this);
      return treesWithSynchronizedCode.containsKey(tree);
    }

    public Tree getSecondaryLocation(Tree tree) {
      return treesWithSynchronizedCode.get(tree);
    }

    private void markSynchronizedBy(Tree secondaryLocation) {
      treesWithSynchronizedCode.put(currentCheckedTree, secondaryLocation);
      isSynchronizedAttributeFound = true;
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
      if (!isSynchronizedAttributeFound) {
        markSynchronizedBy(tree.synchronizedKeyword());
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (isSynchronizedAttributeFound) {
        return;
      }
      if (tree.methodSymbol().isSynchronizedMethod()) {
        markSynchronizedBy(tree);
        return;
      }
      // Make sure to visit arguments even for methods without declaration
      super.visitMethodInvocation(tree);

      var declaration = tree.methodSymbol().declaration();
      if (declaration != null) {
        checkNonSynchronizedMethod(declaration);
      }
    }

    void checkNonSynchronizedMethod(MethodTree method) {
      var methodMarkedSynchronizedBy = treesWithSynchronizedCode.get(method);
      if (methodMarkedSynchronizedBy != null) {
        markSynchronizedBy(methodMarkedSynchronizedBy);
        return;
      }

      if (checkedTrees.contains(method)) {
        return;
      }
      checkedTrees.add(method);

      var block = method.block();
      if (block == null) {
        return;
      }
      var restoreCurrentCheckedTree = currentCheckedTree;
      currentCheckedTree = method;
      block.accept(this);
      currentCheckedTree = restoreCurrentCheckedTree;
      if (isSynchronizedAttributeFound) {
        markSynchronizedBy(treesWithSynchronizedCode.get(method));
      }
    }
  }
}
