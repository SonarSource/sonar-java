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
import java.util.Objects;
import org.sonar.check.Rule;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S2654")
public class JEEThreadCheck extends IssuableSubscriptionVisitor {

  private static final String RUNNABLE_ISSUE_MESSAGE = "Remove this use of \"Runnable\".";
  private static final String SYNCHRONIZED_ISSUE_MESSAGE = "Remove this use of the \"synchronized\" keyword.";

  private static final MethodMatchers JEE_SERVLET_MATCHER = MethodMatchers.create()
    .ofSubTypes("javax.servlet.Servlet", "jakarta.servlet.Servlet")
    .anyName()
    .withAnyParameters()
    .build();

  private static final List<String> EJB_ANNOTATIONS = List.of(
    "javax.ejb.Stateless", "jakarta.ejb.Stateless",
    "javax.ejb.Stateful", "jakarta.ejb.Stateful",
    "javax.ejb.Singleton", "jakarta.ejb.Singleton",
    "javax.ejb.MessageDriven", "jakarta.ejb.MessageDriven");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.SYNCHRONIZED_STATEMENT, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case CLASS -> handleClass((ClassTree) tree);
      case LAMBDA_EXPRESSION -> handleLambda((LambdaExpressionTree) tree);
      case SYNCHRONIZED_STATEMENT -> handleSynchronizedStatement((SynchronizedStatementTree) tree);
      case METHOD -> handleMethod((MethodTree) tree);
      default -> {/* ignored */ }
    }
  }

  private void handleClass(ClassTree classTree) {
    if (!classTree.symbol().type().isSubtypeOf("java.lang.Runnable")) {
      return;
    }
    if (!isJeeClass(classTree) && !isInJeeClass(classTree)) {
      return;
    }
    if (classTree.parent() instanceof NewClassTree newClassTree) {
      reportIssue(newClassTree.identifier(), RUNNABLE_ISSUE_MESSAGE);
    } else {
      reportIssue(Objects.requireNonNull(classTree.simpleName()), RUNNABLE_ISSUE_MESSAGE);
    }
  }

  private void handleLambda(LambdaExpressionTree lambda) {
    if (lambda.symbolType().isSubtypeOf("java.lang.Runnable") && isInJeeClass(lambda)) {
      reportIssue(lambda.arrowToken(), RUNNABLE_ISSUE_MESSAGE);
    }
  }

  private void handleSynchronizedStatement(SynchronizedStatementTree synchronizedStatement) {
    if (isInJeeClass(synchronizedStatement)) {
      reportIssue(synchronizedStatement.synchronizedKeyword(), SYNCHRONIZED_ISSUE_MESSAGE);
    }
  }

  private void handleMethod(MethodTree method) {
    ModifierKeywordTree syncModifier = ModifiersUtils.getModifier(method.modifiers(), Modifier.SYNCHRONIZED);
    if (syncModifier != null && (JEE_SERVLET_MATCHER.matches(method) || isEjbAnnotated(method))) {
      reportIssue(syncModifier, SYNCHRONIZED_ISSUE_MESSAGE);
    }
  }

  private static boolean isEjbAnnotated(MethodTree method) {
    SymbolMetadata metadata = Objects.requireNonNull(method.symbol().enclosingClass()).metadata();
    return EJB_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
  }

  private static boolean isInJeeClass(Tree tree) {
    Tree current = tree.is(Tree.Kind.CLASS) ? tree.parent() : tree;
    while (current != null) {
      if (current.is(Tree.Kind.CLASS) && isJeeClass((ClassTree) current)) {
        return true;
      }
      current = current.parent();
    }
    return false;
  }

  private static boolean isJeeClass(ClassTree classTree) {
    Type classType = classTree.symbol().type();
    if (classType.isSubtypeOf("javax.servlet.Servlet") || classType.isSubtypeOf("jakarta.servlet.Servlet")) {
      return true;
    }
    SymbolMetadata metadata = classTree.symbol().metadata();
    return EJB_ANNOTATIONS.stream().anyMatch(metadata::isAnnotatedWith);
  }

}
