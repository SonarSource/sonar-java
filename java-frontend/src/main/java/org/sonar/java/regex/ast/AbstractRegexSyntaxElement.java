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

import org.sonar.java.regex.RegexSource;

public abstract class AbstractRegexSyntaxElement implements RegexSyntaxElement {

  private final RegexSource source;

  private final IndexRange range;

  protected AbstractRegexSyntaxElement(RegexSource source, IndexRange range) {
    this.source = source;
    this.range = range;
  }

  @Override
  public String getText() {
    return source.substringAt(range);
  }

  @Override
  public IndexRange getRange() {
    return range;
  }

  @Override
  public RegexSource getSource() {
    return source;
  }

}
