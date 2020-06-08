/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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

import java.util.List;

public abstract class RegexSyntaxElement {

  private final RegexSource source;

  private final IndexRange range;

  public RegexSyntaxElement(RegexSource source, IndexRange range) {
    this.source = source;
    this.range = range;
  }

  public List<Location> getLocations() {
    return source.locationsFor(range);
  }

  public String getText() {
    return source.substringAt(range);
  }

  public IndexRange getRange() {
    return range;
  }

  public RegexSource getSource() {
    return source;
  }

}
