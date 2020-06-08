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

public class CharacterClassTree extends RegexTree {

  private final RegexTree contents;

  private final boolean negated;

  public CharacterClassTree(RegexSource source, IndexRange range, boolean negated, RegexTree contents) {
    super(source, range);
    this.negated = negated;
    this.contents = contents;
  }

  public RegexTree getContents() {
    return contents;
  }

  public boolean isNegated() {
    return negated;
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitCharacterClass(this);
  }

  @Override
  public Kind kind() {
    return RegexTree.Kind.CHARACTER_CLASS;
  }

}
