/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.se.symbolicvalues;

import javax.annotation.CheckForNull;

public class GreaterThanRelation extends BinaryRelation {

  GreaterThanRelation(SymbolicValue v1, SymbolicValue v2) {
    super(RelationalSymbolicValue.Kind.GREATER_THAN, v1, v2);
  }

  @Override
  protected RelationState isImpliedBy(BinaryRelation relation) {
    return relation.impliesGreaterThan();
  }

  @Override
  protected RelationState impliesEqual() {
    return RelationState.UNFULFILLED;
  }

  @Override
  protected RelationState impliesNotEqual() {
    return RelationState.FULFILLED;
  }

  @Override
  protected RelationState impliesMethodEquals() {
    return RelationState.UNFULFILLED;
  }

  @Override
  protected RelationState impliesNotMethodEquals() {
    return RelationState.FULFILLED;
  }

  @Override
  protected RelationState impliesGreaterThanOrEqual() {
    return RelationState.FULFILLED;
  }

  @Override
  protected RelationState impliesGreaterThan() {
    return RelationState.FULFILLED;
  }

  @Override
  protected RelationState impliesLessThan() {
    return RelationState.UNFULFILLED;
  }

  @Override
  protected RelationState impliesLessThanOrEqual() {
    return RelationState.UNFULFILLED;
  }

  @Override
  protected BinaryRelation combinedAfter(BinaryRelation relation) {
    return relation.combinedWithGreaterThan(this);
  }

  @Override
  protected BinaryRelation combinedWithEqual(EqualRelation relation) {
    return binaryRelation(RelationalSymbolicValue.Kind.GREATER_THAN, leftOp, relation.rightOp);
  }

  @Override
  @CheckForNull
  protected BinaryRelation combinedWithNotEqual(NotEqualRelation relation) {
    return null;
  }

  @Override
  @CheckForNull
  protected BinaryRelation combinedWithMethodEquals(MethodEqualsRelation relation) {
    return binaryRelation(RelationalSymbolicValue.Kind.GREATER_THAN, leftOp, relation.rightOp);
  }

  @Override
  @CheckForNull
  protected BinaryRelation combinedWithNotMethodEquals(NotMethodEqualsRelation relation) {
    return null;
  }

  @Override
  protected BinaryRelation combinedWithGreaterThan(GreaterThanRelation relation) {
    return binaryRelation(RelationalSymbolicValue.Kind.GREATER_THAN, leftOp, relation.rightOp);
  }

  @Override
  protected BinaryRelation combinedWithGreaterThanOrEqual(GreaterThanOrEqualRelation relation) {
    return binaryRelation(RelationalSymbolicValue.Kind.GREATER_THAN, leftOp, relation.rightOp);
  }

  @Override
  @CheckForNull
  protected BinaryRelation combinedWithLessThan(LessThanRelation relation) {
    return null;
  }

  @Override
  @CheckForNull
  protected BinaryRelation combinedWithLessThanOrEqual(LessThanOrEqualRelation relation) {
    return null;
  }
}
