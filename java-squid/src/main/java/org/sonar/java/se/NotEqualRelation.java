/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.se;

public class NotEqualRelation extends SymbolicValueRelation {

  NotEqualRelation(SymbolicValue v1, SymbolicValue v2) {
    super(v1, v2);
  }

  @Override
  protected SymbolicValueRelation inverse() {
    return new EqualRelation(v1, v2);
  }

  @Override
  protected String getOperand() {
    return "!=";
  }

  @Override
  protected Boolean implies(SymbolicValueRelation relation) {
    if (hasSameOperand(relation)) {
      return relation instanceof NotEqualRelation;
    }
    return null;
  }

  @Override
  protected SymbolicValueRelation underHypothesis(SymbolicValueRelation relation) {
    return relation.hypothesisWithNotEqual(this);
  }

  @Override
  protected SymbolicValueRelation hypothesisWithEqual(EqualRelation relation) {
    return null;
  }

  @Override
  protected SymbolicValueRelation hypothesisWithNotEqual(NotEqualRelation relation) {
    return null;
  }
}
