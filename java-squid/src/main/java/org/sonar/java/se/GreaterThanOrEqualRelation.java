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

public class GreaterThanOrEqualRelation extends SymbolicValueRelation {

  public GreaterThanOrEqualRelation(SymbolicValue v1, SymbolicValue v2) {
    super(v1, v2);
  }

  @Override
  protected SymbolicValueRelation symmetric() {
    return new LessThanOrEqualRelation(v2, v1);
  }

  @Override
  protected SymbolicValueRelation inverse() {
    return new LessThanRelation(v1, v2);
  }

  @Override
  protected String getOperand() {
    return ">=";
  }

  @Override
  protected Boolean isImpliedBy(SymbolicValueRelation relation) {
    return relation.impliesGreaterThanOrEqual();
  }

  @Override
  @CheckForNull
  protected Boolean impliesEqual() {
    return null;
  }

  @Override
  @CheckForNull
  protected Boolean impliesNotEqual() {
    return null;
  }

  @Override
  @CheckForNull
  protected Boolean impliesMethodEquals() {
    return null;
  }

  @Override
  @CheckForNull
  protected Boolean impliesNotMethodEquals() {
    return null;
  }

  @Override
  @CheckForNull
  protected Boolean impliesGreaterThan() {
    return null;
  }

  @Override
  protected Boolean impliesGreaterThanOrEqual() {
    return Boolean.TRUE;
  }

  @Override
  protected Boolean impliesLessThan() {
    return Boolean.FALSE;
  }

  @Override
  @CheckForNull
  protected Boolean impliesLessThanOrEqual() {
    return null;
  }

  @Override
  protected SymbolicValueRelation combinedAfter(SymbolicValueRelation relation) {
    return relation.combinedWithGreaterThanOrEqual(this);
  }

  @Override
  protected SymbolicValueRelation combinedWithEqual(EqualRelation relation) {
    return new GreaterThanOrEqualRelation(v1, relation.v2);
  }

  @Override
  @CheckForNull
  protected SymbolicValueRelation combinedWithNotEqual(NotEqualRelation relation) {
    return null;
  }

  @Override
  @CheckForNull
  protected SymbolicValueRelation combinedWithMethodEquals(MethodEqualsRelation relation) {
    return null;
  }

  @Override
  protected SymbolicValueRelation combinedWithNotMethodEquals(NotMethodEqualsRelation relation) {
    return null;
  }

  @Override
  protected SymbolicValueRelation combinedWithGreaterThan(GreaterThanRelation relation) {
    return new GreaterThanRelation(v1, relation.v2);
  }

  @Override
  protected SymbolicValueRelation combinedWithGreaterThanOrEqual(GreaterThanOrEqualRelation relation) {
    return new GreaterThanOrEqualRelation(v1, relation.v2);
  }

  @Override
  protected SymbolicValueRelation combinedWithLessThan(LessThanRelation relation) {
    return null;
  }

  @Override
  protected SymbolicValueRelation combinedWithLessThanOrEqual(LessThanOrEqualRelation relation) {
    return null;
  }
}
