/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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

import java.util.List;
import java.util.function.Function;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;


@Rule(key = "S7481")
public class UseOfSequentialForSequentialGathererCheck extends IssuableSubscriptionVisitor {
  /**
   * inner classes
   */

  /**
   * Contains all information necessary to find out if a method invocation should be replaced by another method invocation.
   */
  private record Case(MethodMatchers matcher, ArgumentPredicate pred, String msg) {
  }

  /**
   * @param predicate return locations of the issue if empty no issue must be reported
   */
  private record ArgumentPredicate(int argIdx, Function<ExpressionTree, List<? extends Tree>> predicate) {
  }

  /**
   * constants
   */
  private static final String GATHERER = "java.util.stream.Gatherer";
  private static final String SUPPLIER = "java.util.function.Supplier";
  private static final String INTEGRATOR = "java.util.stream.Gatherer$Integrator";
  private static final String BI_CONSUMER = "java.util.function.BiConsumer";
  private static final String BINARY_OPERATOR = "java.util.function.BinaryOperator";


  private static final MethodMatchers DEFAULT_COMBINER = MethodMatchers.create()
    .ofTypes(GATHERER)
    .names("defaultCombiner")
    .addWithoutParametersMatcher()
    .build();
  private static final MethodMatchers GATHERER_OF = MethodMatchers.create()
    .ofTypes(GATHERER)
    .names("of")
    .addParametersMatcher(SUPPLIER, INTEGRATOR, BINARY_OPERATOR, BI_CONSUMER)
    .build();

  private static final ArgumentPredicate SEQUENTIAL_PREDICATE = new ArgumentPredicate(2, UseOfSequentialForSequentialGathererCheck::isSequentialCombiner);
  private static final List<Case> CASES = List.of(
    new Case(
      GATHERER_OF,
      SEQUENTIAL_PREDICATE,
      "Replace `Gatherer.of(initializer, integrator, combiner, finisher)` with `ofSequential(initializer, integrator, finisher)`")
  );


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;

    for (Case caze : CASES) {
      if (!caze.matcher.matches(mit)) {
        continue;
      }
      var argumentPredicate = caze.pred;
      var issues = argumentPredicate.predicate.apply(mit.arguments().get(argumentPredicate.argIdx));
      if (!issues.isEmpty()) {

        var secondaries = issues.subList(1, issues.size())
          .stream()
          .map(element -> new JavaFileScannerContext.Location("", element))
          .toList();

        context.reportIssue(this, issues.get(0), caze.msg, secondaries, null);
        return;
      }
    }

  }

  private static List<? extends Tree> isSequentialCombiner(ExpressionTree expr) {

    if (expr instanceof LambdaExpressionTree lambda && lambda.body() instanceof BlockTree block) {
      if (block.body().size() == 1 && block.body().get(0) instanceof ThrowStatementTree throwStmt) {
        return List.of(throwStmt);
      }
    } else if (expr instanceof MethodInvocationTree mit && DEFAULT_COMBINER.matches(mit)) {
      return List.of(mit);
    }

    return List.of();
  }
}
