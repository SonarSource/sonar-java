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
package org.sonar.java.ast.visitors;

import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LinesOfCodeVisitor extends SubscriptionVisitor{

  private Set<Integer> lines = new HashSet<>();

  public int linesOfCode(Tree tree) {
    lines.clear();
    scanTree(tree);
    return lines.size();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TOKEN);
  }

  @Override
  public void visitToken(SyntaxToken syntaxToken) {
    if (!((InternalSyntaxToken) syntaxToken).isEOF()) {
      lines.add(Position.startOf(syntaxToken).line());
    }
  }
}
