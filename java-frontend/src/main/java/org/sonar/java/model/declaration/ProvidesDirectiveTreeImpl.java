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
package org.sonar.java.model.declaration;

import java.util.List;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.ProvidesDirectiveTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.util.Arrays;
import java.util.Collections;

public class ProvidesDirectiveTreeImpl extends ModuleDirectiveTreeImpl implements ProvidesDirectiveTree {

  private final TypeTree typeName;
  private final InternalSyntaxToken withKeyword;
  private final ListTree<TypeTree> typeNames;

  public ProvidesDirectiveTreeImpl(InternalSyntaxToken providesKeyword, TypeTree typeName, InternalSyntaxToken withKeyword,
    ListTree<TypeTree> typeNames, InternalSyntaxToken semicolonToken) {
    super(providesKeyword, semicolonToken);
    this.typeName =typeName;
    this.withKeyword = withKeyword;
    this.typeNames = typeNames;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitProvidesDirective(this);
  }

  @Override
  public Kind kind() {
    return Tree.Kind.PROVIDES_DIRECTIVE;
  }

  @Override
  public TypeTree typeName() {
    return typeName;
  }

  @Override
  public SyntaxToken withKeyword() {
    return withKeyword;
  }

  @Override
  public ListTree<TypeTree> typeNames() {
    return typeNames;
  }

  @Override
  protected List<Tree> children() {
    return Collections.unmodifiableList(Arrays.asList(
      directiveKeyword(),
      typeName,
      withKeyword,
      typeNames,
      semicolonToken()));
  }

}
