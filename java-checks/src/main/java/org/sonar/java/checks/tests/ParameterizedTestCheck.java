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
package org.sonar.java.checks.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.SyntacticEquivalence;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static java.util.Arrays.asList;

@Rule(key = "S5976")
public class ParameterizedTestCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Replace these %d tests with a single Parameterized one.";

  private static final Set<String> TEST_ANNOTATIONS = new HashSet<>(asList(
    "org.junit.Test",
    "org.junit.jupiter.api.Test",
    "org.testng.annotations.Test"));

  private static final int MIN_SIMILAR_METHODS = 3;
  private static final int MIN_NUMBER_STATEMENTS = 2;
  private static final int MAX_NUMBER_PARAMETER = 3;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    List<MethodTree> methods = classTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .filter(ParameterizedTestCheck::isParametrizedCandidate)
      .toList();
    if (methods.size() < MIN_SIMILAR_METHODS) {
      return;
    }

    Set<MethodTree> handled = new HashSet<>();
    for (int i = 0; i < methods.size(); i++) {
      MethodTree method = methods.get(i);
      if (handled.contains(method)) {
        continue;
      }
      List<StatementTree> methodBody = method.block().body();
      // In addition to filtering literals, we want to count the number of differences since they will represent the number of parameter
      // that would be required to transform the tests to a single parametrized one.
      CollectAndIgnoreLiterals collectAndIgnoreLiterals = new CollectAndIgnoreLiterals();

      List<MethodTree> equivalentMethods = new ArrayList<>();

      for (int j = i + 1; j < methods.size(); j++) {
        MethodTree otherMethod = methods.get(j);
        if (!handled.contains(otherMethod)) {
          boolean areEquivalent = SyntacticEquivalence.areEquivalent(methodBody, otherMethod.block().body(), collectAndIgnoreLiterals);
          if (areEquivalent) {
            // If methods where not equivalent, we don't want to pollute the set of node to parameterize.
            equivalentMethods.add(otherMethod);
            collectAndIgnoreLiterals.finishCollect();
          }
          collectAndIgnoreLiterals.clearCurrentNodes();
        }
      }

      reportIfIssue(handled, method, collectAndIgnoreLiterals, equivalentMethods);
    }
  }

  private void reportIfIssue(Set<MethodTree> handled, MethodTree method, CollectAndIgnoreLiterals collectAndIgnoreLiterals, List<MethodTree> equivalentMethods) {
    if (equivalentMethods.size() + 1 >= MIN_SIMILAR_METHODS) {
      handled.addAll(equivalentMethods);

      int nParameters = collectAndIgnoreLiterals.nodeToParametrize.size();
      if (nParameters <= MAX_NUMBER_PARAMETER
        && method.block().body().size() > nParameters) {
        // We don't report an issue if the change would result in too many parameters.
        // or if no statement would be duplicated.
        // We still add it to "handled" to not report a subset of candidate methods.
        List<JavaFileScannerContext.Location> secondaries = collectAndIgnoreLiterals.nodeToParametrize.stream().map(param ->
          new JavaFileScannerContext.Location("Value to parameterize", param)).collect(Collectors.toCollection(ArrayList::new));

        equivalentMethods.stream().map(equivalentMethod ->
          new JavaFileScannerContext.Location("Related test", equivalentMethod.simpleName()))
          .forEach(secondaries::add);

        reportIssue(method.simpleName(), String.format(MESSAGE, equivalentMethods.size() + 1), secondaries, null);
      }
    }
  }

  private static boolean isParametrizedCandidate(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    SymbolMetadata symbolMetadata = methodTree.symbol().metadata();
    return block != null &&
      block.body().size() >= MIN_NUMBER_STATEMENTS &&
      TEST_ANNOTATIONS.stream().anyMatch(symbolMetadata::isAnnotatedWith);
  }

  static class CollectAndIgnoreLiterals implements BiPredicate<JavaTree, JavaTree> {

    Set<JavaTree> nodeToParametrize = new HashSet<>();
    private final Set<JavaTree> currentNodeToParameterize = new HashSet<>();

    @Override
    public boolean test(JavaTree leftNode, JavaTree rightNode) {
      if (isCompatibleTypes(leftNode, rightNode)) {
        if (!SyntacticEquivalence.areEquivalent(leftNode, rightNode)) {
          // If the two literals are not equivalent, it means that we will have to create a parameter for it.
          currentNodeToParameterize.add(leftNode);
        }
        return true;
      }
      return false;
    }

    public void finishCollect() {
      nodeToParametrize.addAll(currentNodeToParameterize);
    }

    public void clearCurrentNodes() {
      currentNodeToParameterize.clear();
    }

    private static boolean isCompatibleTypes(@Nullable JavaTree leftNode, @Nullable JavaTree rightNode) {
      return leftNode instanceof LiteralTree
        && rightNode instanceof LiteralTree
        && (leftNode.is(rightNode.kind()) ||
        (leftNode.is(Tree.Kind.STRING_LITERAL, Tree.Kind.NULL_LITERAL) &&
          rightNode.is(Tree.Kind.STRING_LITERAL, Tree.Kind.NULL_LITERAL))
      );
    }
  }

}
