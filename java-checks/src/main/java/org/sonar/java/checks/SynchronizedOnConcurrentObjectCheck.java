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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S9141")
public class SynchronizedOnConcurrentObjectCheck extends IssuableSubscriptionVisitor {

  private static final String CONCURRENT_LOCKS_PREFIX = "java.util.concurrent.locks.";
  private static final String CONCURRENT_ATOMIC_PREFIX = "java.util.concurrent.atomic.";
  private static final Set<String> CONCURRENT_SYNC_TYPES = Set.of(
    "java.util.concurrent.Semaphore",
    "java.util.concurrent.CountDownLatch",
    "java.util.concurrent.CyclicBarrier",
    "java.util.concurrent.Exchanger",
    "java.util.concurrent.Phaser",
    "java.util.concurrent.BlockingQueue",
    "java.util.concurrent.BlockingDeque",
    "java.util.concurrent.TransferQueue");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.SYNCHRONIZED_STATEMENT);
  }

  @Override
  public void visitNode(Tree tree) {
    ExpressionTree expression = ((SynchronizedStatementTree) tree).expression();
    Type expressionType = expression.symbolType();
    if (isConcurrentSyncPrimitive(expressionType)) {
      reportIssue(expression, String.format(
        "Use the \"%s\" API for synchronization instead of a \"synchronized\" block.", expressionType.name()));
    }
  }

  private static boolean isConcurrentSyncPrimitive(Type type) {
    return isKnownSyncPrimitive(type) || type.symbol().superTypes().stream().anyMatch(SynchronizedOnConcurrentObjectCheck::isKnownSyncPrimitive);
  }

  private static boolean isKnownSyncPrimitive(Type type) {
    String fqn = type.fullyQualifiedName();
    return fqn.startsWith(CONCURRENT_LOCKS_PREFIX)
      || fqn.startsWith(CONCURRENT_ATOMIC_PREFIX)
      || CONCURRENT_SYNC_TYPES.contains(fqn);
  }

}
