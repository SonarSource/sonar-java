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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;


@Rule(key = "S7482")
public class ForStatelessGatherersOmitInitializerCheck extends IssuableSubscriptionVisitor {
  private static final String GATHERER = "java.util.stream.Gatherer";
  private static final String SUPPLIER = "java.util.function.Supplier";
  private static final String INTEGRATOR = "java.util.stream.Gatherer$Integrator";
  private static final String BI_CONSUMER = "java.util.function.BiConsumer";

  private static final MethodMatchers EMPTY = MethodMatchers.create()
    .ofTypes("java.util.Optional")
    .names("empty")
    .addWithoutParametersMatcher()
    .build();
  private static final MethodMatchers TRIVIAL = MethodMatchers.create()
    .ofTypes(GATHERER)
    .names("ofSequential")
    .addParametersMatcher(SUPPLIER, INTEGRATOR)
    .build();
  private static final MethodMatchers WITH_FINISHER = MethodMatchers.create()
    .ofTypes(GATHERER)
    .names("ofSequential")
    .addParametersMatcher(SUPPLIER, INTEGRATOR, BI_CONSUMER)
    .build();


  private static final Predicate<Tree> STATELESS = tree -> tree.is(Tree.Kind.NULL_LITERAL)
    || (tree instanceof MethodInvocationTree mit && EMPTY.matches(mit));
  private static final Function<ExpressionTree, List<? extends Tree>> STATELESS_INITIALIZER = tree -> {
    if (tree instanceof LambdaExpressionTree lambda) {
      var body = lambda.body();
      if (STATELESS.test(body)) {
        return List.of(body);
      }


      if (body instanceof BlockTree block) {
        List<ReturnStatementTree> all = ReturnStatementCollector.collect(block);
        List<ReturnStatementTree> stateless = all
          .stream()
          .filter(rs -> STATELESS.test(rs.expression()))
          .toList();

        if (stateless.size() == all.size()) {
          return all;
        } else {
          return List.of();
        }
      }
    }
    return List.of();
  };


  private static final ArgumentPredicate INITIALIZER_PREDICATE = new ArgumentPredicate(0, STATELESS_INITIALIZER);
  private static final List<Case> CASES = List.of(
    new Case(TRIVIAL, INITIALIZER_PREDICATE, "Replace of `Gatherer.ofSequential(initializer, integrator)` with `Gatherer.ofSequential(integrator)`"),
    new Case(WITH_FINISHER, INITIALIZER_PREDICATE, "Replace of `Gatherer.ofSequential(initializer, integrator, finisher)` with `Gatherer.ofSequential(integrator, finisher)`")
  );


  private record Case(MethodMatchers matcher, ArgumentPredicate pred, String msg) {}
  private record ArgumentPredicate(int argIdx, Function<ExpressionTree, List<? extends Tree>> predicate) {
  }


  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;

    for(Case c : CASES) {
      if (!c.matcher.matches(mit)) {
        continue;
      }
      var issues = INITIALIZER_PREDICATE.predicate.apply(mit.arguments().get(INITIALIZER_PREDICATE.argIdx));
      if (!issues.isEmpty()) {

        var secondaries = issues.subList(1, issues.size())
          .stream()
          .map(element -> new JavaFileScannerContext.Location("", element))
          .toList();

        context.reportIssue(this, issues.get(0), c.msg, secondaries, null);
        return;
      }
    }
  }

  /**
   * Utility visitor that collects all return statements from a given tree.
   */
  private static class ReturnStatementCollector extends BaseTreeVisitor {
    private final List<ReturnStatementTree> returnStatements = new ArrayList<>();

    public List<ReturnStatementTree> getReturnStatements() {
      return returnStatements;
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      returnStatements.add(tree);
      super.visitReturnStatement(tree);
    }


    public static List<ReturnStatementTree> collect(Tree tree) {
      ReturnStatementCollector collector = new ReturnStatementCollector();
      tree.accept(collector);
      return collector.getReturnStatements();
    }
  }
}
