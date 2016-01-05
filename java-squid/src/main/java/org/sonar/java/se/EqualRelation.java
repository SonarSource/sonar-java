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

import javax.annotation.CheckForNull;

public class EqualRelation extends SymbolicValueRelation {

  EqualRelation(SymbolicValue v1, SymbolicValue v2) {
    super(v1, v2);
  }

  @Override
  protected SymbolicValueRelation symmetric() {
    return new EqualRelation(v2, v1);
  }

  @Override
  protected SymbolicValueRelation inverse() {
    return new NotEqualRelation(v1, v2);
  }

  @Override
  protected String getOperand() {
    return "=";
  }

  @Override
  public Boolean isImpliedBy(SymbolicValueRelation relation) {
    return relation.impliesEqual();
  }

  @Override
  protected Boolean impliesEqual() {
    return Boolean.TRUE;
  }

  @Override
  protected Boolean impliesNotEqual() {
    return Boolean.FALSE;
  }

  @Override
  protected Boolean impliesMethodEquals() {
    return Boolean.TRUE;
  }

  @Override
  protected Boolean impliesNotMethodEquals() {
    return Boolean.FALSE;
  }

  @Override
  @CheckForNull
  protected SymbolicValueRelation combinedAfter(SymbolicValueRelation relation) {
    return relation.combinedWithEqual(this);
  }

  @Override
  @CheckForNull
  protected SymbolicValueRelation combinedWithEqual(EqualRelation relation) {
    return new EqualRelation(v1, relation.v2);
  }

  @Override
  @CheckForNull
  protected SymbolicValueRelation combinedWithNotEqual(NotEqualRelation relation) {
    return new NotEqualRelation(v1, relation.v2);
  }

  @Override
  protected SymbolicValueRelation combinedWithMethodEquals(MethodEqualsRelation relation) {
    return new MethodEqualsRelation(v1, relation.v2);
  }

  @Override
  protected SymbolicValueRelation combinedWithNotMethodEquals(NotMethodEqualsRelation relation) {
    return new NotMethodEqualsRelation(v1, relation.v2);
  }
}
