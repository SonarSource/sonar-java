/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.Collections;
import java.util.List;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

@DeprecatedRuleKey(ruleKey = "UselessParenthesesCheck", repositoryKey = "squid")
@Rule(key = "S1110")
public class UselessParenthesesCheck extends IssuableSubscriptionVisitor {

  @Override
  public void visitNode(Tree tree) {
    ParenthesizedTree parenthesizedTree = (ParenthesizedTree) tree;
    if (parenthesizedTree.expression().is(Kind.PARENTHESIZED_EXPRESSION)) {
      reportIssue(((ParenthesizedTree) parenthesizedTree.expression()).openParenToken(),
          "Remove these useless parentheses.",
          Collections.singletonList(new JavaFileScannerContext.Location("", parenthesizedTree.closeParenToken())), null);
    }
  }

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.PARENTHESIZED_EXPRESSION);
  }
}
