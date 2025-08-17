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
import org.sonar.plugins.java.api.tree.FloatLiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

public class FloatLiteralTreeImpl extends LiteralTreeImpl implements FloatLiteralTree {

  private final float floatValue;

  public FloatLiteralTreeImpl(InternalSyntaxToken token, float floatValue) {
    super(Tree.Kind.FLOAT_LITERAL, token);
    this.floatValue = floatValue;
    constant = Optional.of(floatValue);
  }

  @Override
  public float floatValue() {
    return floatValue;
  }

  @Override
  @Nonnull
  public Object parsedValue() {
    return floatValue;
  }

}
