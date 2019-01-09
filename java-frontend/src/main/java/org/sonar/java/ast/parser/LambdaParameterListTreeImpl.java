/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.declaration.VariableTreeImpl;

import javax.annotation.Nullable;

import java.util.List;

public class LambdaParameterListTreeImpl extends ListTreeImpl<VariableTreeImpl> {

  private final InternalSyntaxToken openParenToken;
  private final InternalSyntaxToken closeParenToken;

  public LambdaParameterListTreeImpl(@Nullable InternalSyntaxToken openParenToken, List<VariableTreeImpl> params,
    @Nullable InternalSyntaxToken closeParenToken) {
    super(JavaLexer.LAMBDA_PARAMETERS, params);

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
