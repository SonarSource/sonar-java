/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LabeledStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.plugins.java.api.tree.Tree.Kind.BLOCK;
import static org.sonar.plugins.java.api.tree.Tree.Kind.CASE_GROUP;
import static org.sonar.plugins.java.api.tree.Tree.Kind.LABELED_STATEMENT;

@Rule(key = "S1219")
public class SwitchWithLabelsCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(CASE_GROUP);
  }

  @Override
  public void visitNode(Tree tree) {
    CaseGroupTree cgt = (CaseGroupTree) tree;
    cgt.body().stream()
      .flatMap(SwitchWithLabelsCheck::getStatementTreeStream)
      .map(LabeledStatementTree.class::cast)
      .forEach(this::reportLabeledStatement);
  }

  private static Stream<StatementTree> getStatementTreeStream(StatementTree statementTree) {
    if (statementTree.is(LABELED_STATEMENT)) {
      return Stream.of(statementTree);
    }
    if (statementTree.is(BLOCK)) {
      return ((BlockTree) statementTree).body().stream()
        .filter(st -> st.is(LABELED_STATEMENT));
    }
    return Stream.empty();
  }

  private void reportLabeledStatement(LabeledStatementTree statementTree) {
    IdentifierTree label = statementTree.label();
    reportIssue(label, "Remove this misleading \"" + label.name() + "\" label.");
  }
}
