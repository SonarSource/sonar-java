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
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.StringLiteralTree;

public class StringLiteralTreeImpl extends LiteralTreeImpl implements StringLiteralTree {

  private final String unquotedValue;
  private final String stringValue;

  public StringLiteralTreeImpl(Kind kind, InternalSyntaxToken token, String stringValue) {
    super(kind, token);
    if (kind == Kind.TEXT_BLOCK) {
      this.unquotedValue = LiteralUtils.removeTextBlockQuoteIndentationAndTrailingWhitespaces(token.text());
    } else {
      this.unquotedValue = LiteralUtils.unquote(token.text(), '"');
    }
    this.stringValue = stringValue;
    constant = Optional.of(stringValue);
  }

  @Override
  public String unquotedValue() {
    return unquotedValue;
  }

  @Override
  public String stringValue() {
    return stringValue;
  }

  @Override
  @Nonnull
  public Object parsedValue() {
    return stringValue;
  }

}
