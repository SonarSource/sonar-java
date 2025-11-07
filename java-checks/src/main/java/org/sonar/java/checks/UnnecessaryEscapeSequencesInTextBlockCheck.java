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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.model.LineUtils;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5665")
public class UnnecessaryEscapeSequencesInTextBlockCheck extends IssuableSubscriptionVisitor {
  private static final String MESSAGE = "Remove this unnecessary escape sequence.";
  private static final String TRIPLE_QUOTE_MESSAGE = "Use '\\\"\"\"' to escape \"\"\".";
  private static final Set<String> ESCAPED = SetUtils.immutableSetOf("\\n", "\\'", "\\\"");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TEXT_BLOCK);
  }

  @Override
  public void visitNode(Tree tree) {
    LiteralTree textBlock = (LiteralTree) tree;
    int startLine = LineUtils.startLine(textBlock.token());
    String value = textBlock.value();
    String[] lines = value.split("\r?\n|\r");
    
    for (int i = 0; i < lines.length; ++i) {
      lines[i] = lines[i].replace("\\\\","");
      if (lines[i].contains("\\\"\\\"\\\"")) {
        addIssue(startLine + i, TRIPLE_QUOTE_MESSAGE);
      } else {
        String replaced = lines[i].replace("\\\"\"\"", "");
        if (ESCAPED.stream().anyMatch(replaced::contains)) {
          addIssue(startLine + i, MESSAGE);
        }
      }
    }
  }
}
