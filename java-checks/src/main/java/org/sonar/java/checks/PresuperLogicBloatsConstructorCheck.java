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

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.StatementVisitor;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.StatementTree;

@Rule(key = "S8444")
public class PresuperLogicBloatsConstructorCheck extends FlexibleConstructorCheck {
  private static final int MAX_STATEMENTS_BEFORE_CONSTRUCTOR_CALL = 3;

  @Override
  void validateConstructor(MethodTree constructor, List<StatementTree> body, int constructorCallIndex) {
    if (constructorCallIndex < 0) {
      // No constructor call, nothing to check
      return;
    }
    StatementVisitor statementVisitor = new StatementVisitor();
    int statementsBeforeConstructorCall = body.stream().limit(constructorCallIndex).map(statementVisitor::numberOfStatements).reduce(0, Integer::sum);
    if (statementsBeforeConstructorCall > MAX_STATEMENTS_BEFORE_CONSTRUCTOR_CALL) {
      reportIssue(
        body.get(0),
        body.get(constructorCallIndex - 1),
        "Excessive logic in this \"pre-construction\" phase makes the code harder to read and maintain."
      );
    }
  }
}

