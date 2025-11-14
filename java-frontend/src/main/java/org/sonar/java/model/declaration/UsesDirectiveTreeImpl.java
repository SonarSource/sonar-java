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
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UsesDirectiveTree;

import java.util.Arrays;
import java.util.Collections;

public class UsesDirectiveTreeImpl extends ModuleDirectiveTreeImpl implements UsesDirectiveTree {

  private final TypeTree typeName;

  public UsesDirectiveTreeImpl(InternalSyntaxToken usesKeyword, TypeTree typeName, InternalSyntaxToken semicolonToken) {
    super(usesKeyword, semicolonToken);
    this.typeName = typeName;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitUsesDirective(this);
  }

  @Override
  public Kind kind() {
    return Tree.Kind.USES_DIRECTIVE;
  }

  @Override
  public TypeTree typeName() {
    return typeName;
  }

  @Override
  protected List<Tree> children() {
    return Collections.unmodifiableList(Arrays.asList(
      directiveKeyword(),
      typeName,
      semicolonToken()));
  }

}
