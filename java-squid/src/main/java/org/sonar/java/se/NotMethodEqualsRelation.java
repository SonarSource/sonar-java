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

public class NotMethodEqualsRelation extends SymbolicValueRelation {

  public NotMethodEqualsRelation(SymbolicValue v1, SymbolicValue v2) {
    super(v1, v2);
  }

  @Override
  protected SymbolicValueRelation symmetric() {
    return new NotMethodEqualsRelation(v2, v1);
  }

  @Override
  protected SymbolicValueRelation inverse() {
    return new MethodEqualsRelation(v1, v2);
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append('!');
    buffer.append(v1);
    buffer.append(".equals(");
    buffer.append(v2);
    buffer.append(')');
    return buffer.toString();
  }

  @Override
  protected String getOperand() {
    // Unused because toString() is overwritten
    return "";
  }

  @Override
  @CheckForNull
  protected Boolean isImpliedBy(SymbolicValueRelation relation) {
    return relation.impliesNotMethodEquals();
  }

  @Override
  protected Boolean impliesEqual() {
    return Boolean.FALSE;
  }

  @Override
  protected Boolean impliesNotEqual() {
    return Boolean.TRUE;
  }

  @Override
  protected Boolean impliesMethodEquals() {
    return Boolean.FALSE;
  }

  @Override
  protected Boolean impliesNotMethodEquals() {
    return Boolean.TRUE;
  }

  @Override
  @CheckForNull
  protected Boolean impliesGreaterThan() {
    return null;
  }

  @Override
  @CheckForNull
  protected Boolean impliesGreaterThanOrEqual() {
    return null;
  }

  @Override
  @CheckForNull
  protected Boolean impliesLessThan() {
    return null;
  }

  @Override
  @CheckForNull
  protected Boolean impliesLessThanOrEqual() {
    return null;
  }

  @Override
  @CheckForNull
  protected SymbolicValueRelation combinedAfter(SymbolicValueRelation relation) {
    return combinedWithNotMethodEquals(this);
  }

  @Override
  protected SymbolicValueRelation combinedWithEqual(EqualRelation relation) {
    return new NotMethodEqualsRelation(v1, relation.v2);
  }

  @Override
  protected SymbolicValueRelation combinedWithNotEqual(NotEqualRelation relation) {
    return null;
  }

  @Override
  protected SymbolicValueRelation combinedWithMethodEquals(MethodEqualsRelation relation) {
    return new NotMethodEqualsRelation(v1, relation.v2);
  }

  @Override
  @CheckForNull
  protected SymbolicValueRelation combinedWithNotMethodEquals(NotMethodEqualsRelation relation) {
    return null;
  }

  @Override
  @CheckForNull
  protected SymbolicValueRelation combinedWithGreaterThan(GreaterThanRelation relation) {
    return null;
  }

  @Override
  @CheckForNull
  protected SymbolicValueRelation combinedWithGreaterThanOrEqual(GreaterThanOrEqualRelation relation) {
    return null;
  }

  @Override
  @CheckForNull
  protected SymbolicValueRelation combinedWithLessThan(LessThanRelation relation) {
    return null;
  }

  @Override
  @CheckForNull
  protected SymbolicValueRelation combinedWithLessThanOrEqual(LessThanOrEqualRelation relation) {
    return null;
  }
}
