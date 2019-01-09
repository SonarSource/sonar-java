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
package org.sonar.java.ast.api;

import org.sonar.sslr.grammar.GrammarRuleKey;

/**
 * Type of tokens for java grammar.
 */
public enum JavaTokenType implements GrammarRuleKey {
  CHARACTER_LITERAL,
  INTEGER_LITERAL,
  FLOAT_LITERAL,
  LONG_LITERAL,
  DOUBLE_LITERAL,
  STRING_LITERAL,
  IDENTIFIER;

  public String getName() {
    return name();
  }

  public String getValue() {
    return name();
  }

}
