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
package org.sonar.java.ast.parser;

import java.util.ArrayList;
import javax.annotation.Nullable;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.declaration.VariableTreeImpl;

public class FormalParametersListTreeImpl extends ListTreeImpl<VariableTreeImpl> {

  @Nullable
  private InternalSyntaxToken openParenToken;
  @Nullable
  private InternalSyntaxToken closeParenToken;

  public FormalParametersListTreeImpl(@Nullable InternalSyntaxToken openParenToken, @Nullable InternalSyntaxToken closeParenToken) {
    super(new ArrayList<>());
    // parenthesis will be null for record's compact constructor
    this.openParenToken = openParenToken;
    this.closeParenToken = closeParenToken;
  }

  @Nullable
  public InternalSyntaxToken openParenToken() {
    return openParenToken;
  }

  @Nullable
  public InternalSyntaxToken closeParenToken() {
    return closeParenToken;
  }

}
