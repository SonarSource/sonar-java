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
package org.sonar.plugins.java.api.tree;

import java.util.List;
import java.util.regex.Pattern;
import org.sonar.java.ast.visitors.SubscriptionVisitor;

public class TreeUnparser extends SubscriptionVisitor {
  // Tokens that should not be surrounded by spaces
  private static final Pattern NO_SPACE_TOKENS = Pattern.compile("[!().\\[\\]]|\\+\\+|--");

  private final StringBuilder stringBuilder = new StringBuilder();
  private boolean previousTokenNeedsSpace = false;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.TOKEN);
  }

  @Override
  public void visitToken(SyntaxToken token) {
    boolean needsSpace = !NO_SPACE_TOKENS.matcher(token.text()).matches();
    if (previousTokenNeedsSpace && needsSpace) {
      stringBuilder.append(' ');
    }
    stringBuilder.append(token.text());
    previousTokenNeedsSpace = needsSpace;
  }

  public static String unparse(Tree tree) {
    var unparser = new TreeUnparser();
    unparser.scanTree(tree);
    return unparser.stringBuilder.toString();
  }
}
