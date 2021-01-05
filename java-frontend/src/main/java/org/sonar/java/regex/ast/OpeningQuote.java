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
package org.sonar.java.regex.ast;

import java.util.Collections;
import java.util.List;

/**
 * This class should only be instantiated by RegexParseResult.openingQuote() and only used when using
 * the opening quote of a regex as an issue location. It should never appear within a regex AST.
 */
public class OpeningQuote extends AbstractRegexSyntaxElement {

  public OpeningQuote(RegexSource source) {
    super(source, new IndexRange(-1, 0));
  }

  @Override
  public String getText() {
    throw new UnsupportedOperationException("getText should not be called on OpeningQuote objects.");
  }

  @Override
  public List<Location> getLocations() {
    return Collections.singletonList(new Location(getSource().getStringLiterals().get(0), -1, 0));
  }
}
