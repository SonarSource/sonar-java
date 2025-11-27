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
package org.sonar.java.model.statement;

import java.util.ArrayList;
import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.StaticInitializerTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public class StaticInitializerTreeImpl extends BlockTreeImpl implements StaticInitializerTree {

  private SyntaxToken staticKeyword;

  public StaticInitializerTreeImpl(InternalSyntaxToken staticKeyword, InternalSyntaxToken openBraceToken, List<StatementTree> body, InternalSyntaxToken closeBraceToken) {
    super(Tree.Kind.STATIC_INITIALIZER, openBraceToken, body, closeBraceToken);
    this.staticKeyword = staticKeyword;
  }

  @Override
  public SyntaxToken staticKeyword() {
    return staticKeyword;
  }

  @Override
  public List<Tree> children() {
    List<Tree> list = new ArrayList<>();
    list.add(staticKeyword);
    super.children().forEach(list::add);
    return list;
  }

}
