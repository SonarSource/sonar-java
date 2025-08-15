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

import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.DoubleLiteralTree;
import org.sonar.plugins.java.api.tree.Tree;

public class DoubleLiteralTreeImpl extends LiteralTreeImpl implements DoubleLiteralTree {

  private final double doubleValue;

  public DoubleLiteralTreeImpl(InternalSyntaxToken token) {
    this(token, LiteralUtils.parseJavaLiteralDouble(token.text()));
  }

  public DoubleLiteralTreeImpl(InternalSyntaxToken token, double doubleValue) {
    super(Tree.Kind.DOUBLE_LITERAL, token);
    this.doubleValue = doubleValue;
  }

  @Override
  public double doubleValue() {
    return doubleValue;
  }

  @Override
  public Object parsedValue() {
    return doubleValue;
  }

}
