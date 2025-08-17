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
package org.sonar.java.model.expression;

import java.util.Optional;
import javax.annotation.Nonnull;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.BooleanLiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

public class BooleanLiteralTreeImpl extends LiteralTreeImpl implements BooleanLiteralTree {

  private final boolean booleanValue;

  public BooleanLiteralTreeImpl(InternalSyntaxToken token, boolean booleanValue) {
    super(Tree.Kind.BOOLEAN_LITERAL, token);
    this.booleanValue = booleanValue;
    constant = Optional.of(booleanValue);
  }

  @Override
  public boolean booleanValue() {
    return booleanValue;
  }

  @Override
  @Nonnull
  public Object parsedValue() {
    return booleanValue;
  }

}
