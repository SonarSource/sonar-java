/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.regex;

import org.sonar.java.regex.ast.IndexRange;

public interface RegexSource {
  String getSourceText();

  default String substringAt(IndexRange range) {
    return getSourceText().substring(range.getBeginningOffset(), Math.min(range.getEndingOffset(), length()));
  }

  default int length() {
    return getSourceText().length();
  }

  CharacterParser createCharacterParser();

  default RegexLexer createLexer() {
    return new RegexLexer(this, createCharacterParser());
  }

  RegexDialect dialect();
}
