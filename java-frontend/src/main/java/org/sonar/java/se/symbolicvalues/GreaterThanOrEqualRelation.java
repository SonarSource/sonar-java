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

import com.google.common.base.Preconditions;

import javax.annotation.CheckForNull;

public class GreaterThanOrEqualRelation extends BinaryRelation {

  GreaterThanOrEqualRelation(SymbolicValue v1, SymbolicValue v2) {
    super(RelationalSymbolicValue.Kind.GREATER_THAN_OR_EQUAL, v1, v2);
  }

  @Override
  protected RelationState isImpliedBy(BinaryRelation relation) {
    return relation.impliesGreaterThanOrEqual();
  }

  @Override
  protected RelationState impliesEqual() {
    return RelationState.UNDETERMINED;
  }

  @Override
  protected RelationState impliesNotEqual() {
    return RelationState.UNDETERMINED;
  }

  @Override
  protected RelationState impliesMethodEquals() {
    return RelationState.UNDETERMINED;
  }

  @Override
  protected RelationState impliesNotMethodEquals() {
    return RelationState.UNDETERMINED;
  }

  @Override
  protected RelationState impliesGreaterThan() {
    return RelationState.UNDETERMINED;
  }

  @Override
  protected RelationState impliesGreaterThanOrEqual() {
    return RelationState.FULFILLED;
  }

  @Override
  protected RelationState impliesLessThan() {
    return RelationState.UNFULFILLED;
  }

  @Override
  protected RelationState impliesLessThanOrEqual() {
    return RelationState.UNDETERMINED;
  }

  @Override
  protected BinaryRelation combinedAfter(BinaryRelation relation) {
    return relation.combinedWithGreaterThanOrEqual(this);
  }

  @Override
  protected BinaryRelation combinedWithEqual(EqualRelation relation) {
    return binaryRelation(RelationalSymbolicValue.Kind.GREATER_THAN_OR_EQUAL, leftOp, relation.rightOp);
  }

  @Override
  @CheckForNull
  protected BinaryRelation combinedWithNotEqual(NotEqualRelation relation) {
    return null;
  }

  @Override
  @CheckForNull
  protected BinaryRelation combinedWithMethodEquals(MethodEqualsRelation relation) {
    return binaryRelation(RelationalSymbolicValue.Kind.GREATER_THAN_OR_EQUAL, leftOp, relation.rightOp);
  }

  @Override
  protected BinaryRelation combinedWithNotMethodEquals(NotMethodEqualsRelation relation) {
    return null;
  }

  @Override
  protected BinaryRelation combinedWithGreaterThan(GreaterThanRelation relation) {
    return binaryRelation(RelationalSymbolicValue.Kind.GREATER_THAN, leftOp, relation.rightOp);
  }

  @Override
  protected BinaryRelation combinedWithGreaterThanOrEqual(GreaterThanOrEqualRelation relation) {
    return binaryRelation(RelationalSymbolicValue.Kind.GREATER_THAN_OR_EQUAL, leftOp, relation.rightOp);
  }

  @Override
  protected BinaryRelation combinedWithLessThan(LessThanRelation relation) {
    return null;
  }

  @Override
  protected BinaryRelation combinedWithLessThanOrEqual(LessThanOrEqualRelation relation) {
    return null;
  }

  @Override
  @CheckForNull
  protected BinaryRelation conjunction(BinaryRelation relation) {
    Preconditions.checkArgument(leftOp.equals(relation.leftOp) && rightOp.equals(relation.rightOp), "Conjunction condition not matched!");
    switch (relation.kind) {
      case NOT_EQUAL:
      case NOT_METHOD_EQUALS:
        return binaryRelation(RelationalSymbolicValue.Kind.GREATER_THAN, leftOp, rightOp);
      case LESS_THAN_OR_EQUAL:
        return binaryRelation(RelationalSymbolicValue.Kind.EQUAL, leftOp, rightOp);
      default:
        return super.conjunction(relation);
    }
  }
}
