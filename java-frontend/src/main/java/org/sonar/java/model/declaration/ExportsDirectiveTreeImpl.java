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
package org.sonar.java.model.declaration;

import javax.annotation.Nullable;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExportsDirectiveTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.ModuleNameTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

public class ExportsDirectiveTreeImpl extends SimpleModuleDirectiveTreeImpl implements ExportsDirectiveTree {

  public ExportsDirectiveTreeImpl(InternalSyntaxToken exportsKeyword, ExpressionTree packageName, @Nullable InternalSyntaxToken toKeyword, ListTree<ModuleNameTree> moduleNames,
    InternalSyntaxToken semicolonToken) {
    super(exportsKeyword, packageName, toKeyword, moduleNames, semicolonToken);
  }
  
  @Override
  public ExpressionTree packageName() {
    return packageName;
  }

  @Nullable
  @Override
  public SyntaxToken toKeyword() {
    return toKeyword;
  }

  @Override
  public ListTree<ModuleNameTree> moduleNames() {
    return moduleNames;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitExportsDirectiveTree(this);
  }

  @Override
  public Kind kind() {
    return Tree.Kind.EXPORTS_DIRECTIVE;
  }
}
