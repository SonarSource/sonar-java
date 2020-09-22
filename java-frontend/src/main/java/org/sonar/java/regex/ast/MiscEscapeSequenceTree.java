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

/**
 * This class represents escape sequences inside regular expression that we don't particularly care about.
 * Therefore the tree provides no information about the escape sequence other than its text.
 */
public class MiscEscapeSequenceTree extends RegexTree {

  public MiscEscapeSequenceTree(RegexSource source, IndexRange range) {
    super(source, range);
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitMiscEscapeSequence(this);
  }

  @Override
  public Kind kind() {
    return Kind.MISC_ESCAPE_SEQUENCE;
  }

}
