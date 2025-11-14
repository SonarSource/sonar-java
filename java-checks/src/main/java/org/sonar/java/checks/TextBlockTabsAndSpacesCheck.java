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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5664")
public class TextBlockTabsAndSpacesCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TEXT_BLOCK);
  }

  private static char indentationCharacter(String[] lines) {
    for (String line : lines) {
      if (!line.isEmpty() && line.charAt(0) != '"') {
        return line.charAt(0);
      }
    }
    // If the string doesn't contain any indentation characters, it doesn't matter what we return
    return '\0';
  }

  private static boolean containsWrongIndentation(String line, int indent, char indentationCharacter) {
    for (int i = 0; i < line.length() && i < indent; i++) {
      if (line.charAt(i) != indentationCharacter) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsWrongIndentation(LiteralTree textBlock) {
    String[] lines = textBlock.value().split("\r?\n|\r");
    int indent = LiteralUtils.indentationOfTextBlock(lines);
    char indentationCharacter = indentationCharacter(lines);
    return indent> 0 && Arrays.stream(lines).skip(1)
      .anyMatch(line -> containsWrongIndentation(line, indent, indentationCharacter));
  }

  @Override
  public void visitNode(Tree tree) {
    LiteralTree textBlock = (LiteralTree) tree;
    if (containsWrongIndentation(textBlock)) {
      reportIssue(tree, "Use only spaces or only tabs for indentation");
    }
  }
}
