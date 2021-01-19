/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.checks;

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
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
      if (!line.isEmpty() && isIndentation(line.charAt(0))) {
        return line.charAt(0);
      }
    }
    // If the string doesn't contain any indentation characters, it doesn't matter what we return
    return '\0';
  }

  private static boolean isIndentation(char c) {
    return c == ' ' || c == '\t';
  }

  private static boolean containsWrongIndentation(String[] lines) {
    char indentationCharacter = indentationCharacter(lines);
    for (String line : lines) {
      for (int i = 0; i < line.length() && isIndentation(line.charAt(i)); i++) {
        if (line.charAt(i) != indentationCharacter) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void visitNode(Tree tree) {
    LiteralTree textBlock = (LiteralTree) tree;
    String[] lines = textBlock.value().split("\r?\n");
    if (containsWrongIndentation(lines)) {
      reportIssue(tree, "Use only spaces or only tabs for indentation");
    }
  }
}
