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

public class LookAroundTree extends RegexTree {

  public enum Direction {
    AHEAD, BEHIND
  }

  public enum Polarity {
    POSITIVE, NEGATIVE
  }

  private final Polarity polarity;

  private final Direction direction;

  private final RegexTree element;

  public LookAroundTree(RegexSource source, IndexRange range, Polarity polarity, Direction direction, RegexTree element) {
    super(source, range);
    this.polarity = polarity;
    this.direction = direction;
    this.element = element;
  }

  public Polarity getPolarity() {
    return polarity;
  }

  public Direction getDirection() {
    return direction;
  }

  public RegexTree getElement() {
    return element;
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitLookAround(this);
  }

  @Override
  public Kind kind() {
    return RegexTree.Kind.LOOK_AROUND;
  }

}
