/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.checks.synchronization;

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.sonar.java.model.ModifiersUtils.findModifier;
import static org.sonar.plugins.java.api.tree.Modifier.SYNCHRONIZED;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CONSTRUCTOR;
import static org.sonar.plugins.java.api.tree.Tree.Kind.METHOD;

@Rule(key = "S3046")
public class TwoLocksWaitCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatcher WAIT_MATCHER = MethodMatcher.create().name("wait").withoutParameter();

  private Deque<Counter> synchronizedStack = new LinkedList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(METHOD, CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(METHOD, CONSTRUCTOR)) {
      MethodTree methodTree = (MethodTree) tree;
      int initialCounter = findModifier(methodTree.modifiers(), SYNCHRONIZED).map(m -> 1).orElse(0);
      synchronizedStack.push(new Counter(initialCounter));
      findWaitInvocation(methodTree);
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }
    if (tree.is(METHOD, CONSTRUCTOR)) {
      synchronizedStack.pop();
    }
  }

  private void findWaitInvocation(MethodTree tree) {
    findMethodCall(tree, WAIT_MATCHER)
      .ifPresent(wait -> reportIssue(wait, "Don't use \"wait()\" here; multiple locks are held.", flowFromTree(tree), null));
  }

  private Optional<MethodInvocationTree> findMethodCall(Tree tree, MethodMatcher methodMatcher) {
    MethodInvocationVisitor visitor = new MethodInvocationVisitor(methodMatcher);
    tree.accept(visitor);
    return visitor.matchedMethods().findAny();
  }

  private static List<JavaFileScannerContext.Location> flowFromTree(Tree tree) {
    SynchronizedKeywordVisitor synchronizedKeywordVisitor = new SynchronizedKeywordVisitor();
    tree.accept(synchronizedKeywordVisitor);
    return synchronizedKeywordVisitor.stream()
      .map(t -> new JavaFileScannerContext.Location("locking", t))
      .collect(Collectors.toList());
  }

  private class MethodInvocationVisitor extends BaseTreeVisitor {

    private final MethodMatcher methodMatcher;
    private Stream.Builder<MethodInvocationTree> matchedMethods = Stream.builder();

    private MethodInvocationVisitor(MethodMatcher methodMatcher) {
      this.methodMatcher = methodMatcher;
    }

    @Override
    public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
      synchronizedStack.peek().increment();
      super.visitSynchronizedStatement(tree);
      synchronizedStack.peek().decrement();
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (methodMatcher.matches(tree) && synchronizedStack.peek().value >= 2) {
        matchedMethods.add(tree);
      }
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // cut visit on lambdas, calls are likely not invoked in this method scope
    }

    @Override
    public void visitClass(ClassTree tree) {
      // cut visit on anonymous and local classes, calls are likely not invoked in this method scope
    }

    private Stream<MethodInvocationTree> matchedMethods() {
      return matchedMethods.build();
    }
  }

  private static class SynchronizedKeywordVisitor extends BaseTreeVisitor {

    private Stream.Builder<SyntaxToken> synchronizedKeywords = Stream.builder();

    @Override
    public void visitSynchronizedStatement(SynchronizedStatementTree tree) {
      synchronizedKeywords.add(tree.synchronizedKeyword());
      super.visitSynchronizedStatement(tree);
    }

    @Override
    public void visitMethod(MethodTree tree) {
      findModifier(tree.modifiers(), SYNCHRONIZED)
        .ifPresent(s -> synchronizedKeywords.add(s.keyword()));
      super.visitMethod(tree);
    }

    Stream<SyntaxToken> stream() {
      return synchronizedKeywords.build();
    }
  }

  private static class Counter {
    int value;

    private Counter(int initialValue) {
      this.value = initialValue;
    }

    void increment() {
      value++;
    }

    void decrement() {
      value--;
    }
  }

}
