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
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.java.model.ExpressionUtils;

@Rule(key = "S9437")
public class GetInstanceReturningNewInstanceCheck extends IssuableSubscriptionVisitor {

  private static final String MESSAGE = "Rename this method, as \"getInstance\" is conventionally reserved for singletons,"
    + " or make it return a cached instance instead of a new one.";
  private static final String SECONDARY_MESSAGE = "This name promises a single shared instance.";

  private static final MethodMatchers GET_INSTANCE = MethodMatchers.create()
    .ofAnyType()
    .names("getInstance")
    .addWithoutParametersMatcher()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;
    if (!method.symbol().isStatic() || !GET_INSTANCE.matches(method)) {
      return;
    }
    NewClassTree instantiation = onlyStatementInstantiation(method);
    if (instantiation != null && instantiatesOwnClass(instantiation, method)) {
      List<JavaFileScannerContext.Location> secondary =
        List.of(new JavaFileScannerContext.Location(SECONDARY_MESSAGE, method.simpleName()));
      reportIssue(instantiation, MESSAGE, secondary, null);
    }
  }

  /**
   * Returns the instance creation of a method whose body is nothing but {@code return new Something(…);}, or {@code null}
   * when the body does anything else. Anything else includes reading or assigning a field, so a lazily initialised
   * accessor is never returned here.
   */
  @CheckForNull
  private static NewClassTree onlyStatementInstantiation(MethodTree method) {
    BlockTree body = method.block();
    if (body == null || body.body().size() != 1) {
      return null;
    }
    StatementTree statement = body.body().get(0);
    if (!statement.is(Tree.Kind.RETURN_STATEMENT)) {
      return null;
    }
    ExpressionTree returned = ((ReturnStatementTree) statement).expression();
    if (returned == null) {
      return null;
    }
    ExpressionTree expression = ExpressionUtils.skipParentheses(returned);
    if (!expression.is(Tree.Kind.NEW_CLASS)) {
      return null;
    }
    NewClassTree instantiation = (NewClassTree) expression;
    return instantiation.classBody() == null ? instantiation : null;
  }

  private static boolean instantiatesOwnClass(NewClassTree instantiation, MethodTree method) {
    Symbol createdType = instantiation.symbolType().symbol();
    return !createdType.isUnknown() && createdType.equals(method.symbol().owner());
  }
}
