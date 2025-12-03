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
package org.sonar.java.ast.parser;

import org.sonarsource.analyzer.commons.collections.ListUtils;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeParameters;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TypeParameterListTreeImpl extends ListTreeImpl<TypeParameterTree> implements TypeParameters {

  @Nullable
  private final InternalSyntaxToken openBracketToken;
  @Nullable
  private final InternalSyntaxToken closeBracketToken;

  public TypeParameterListTreeImpl(InternalSyntaxToken openBracketToken, InternalSyntaxToken closeBracketToken) {
    super(new ArrayList<>(), new ArrayList<>());

    this.openBracketToken = openBracketToken;
    this.closeBracketToken = closeBracketToken;
  }

  public TypeParameterListTreeImpl() {
    super(Collections.emptyList(), Collections.emptyList());
    this.openBracketToken = null;
    this.closeBracketToken = null;
  }

  @Nullable
  @Override
  public SyntaxToken openBracketToken() {
    return openBracketToken;
  }

  @Nullable
  @Override
  public SyntaxToken closeBracketToken() {
    return closeBracketToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitTypeParameters(this);
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      Collections.singletonList(openBracketToken),
      super.children(),
      Collections.singletonList(closeBracketToken));
  }

  @Override
  public Kind kind() {
    return Kind.TYPE_PARAMETERS;
  }

}
