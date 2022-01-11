/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
