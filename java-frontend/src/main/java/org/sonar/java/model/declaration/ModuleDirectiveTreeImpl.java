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

import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;

public abstract class ModuleDirectiveTreeImpl extends JavaTree implements ModuleDirectiveTree {

  private final InternalSyntaxToken directiveKeyword;
  private final InternalSyntaxToken semicolonToken;

  protected ModuleDirectiveTreeImpl(InternalSyntaxToken directiveKeyword, InternalSyntaxToken semicolonToken) {
    this.directiveKeyword = directiveKeyword;
    this.semicolonToken = semicolonToken;
  }

  @Override
  public SyntaxToken directiveKeyword() {
    return directiveKeyword;
  }

  @Override
  public SyntaxToken semicolonToken() {
    return semicolonToken;
  }

}
