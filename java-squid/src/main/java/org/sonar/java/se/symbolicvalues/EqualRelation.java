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
package org.sonar.java.se.symbolicvalues;

import javax.annotation.CheckForNull;

public class EqualRelation extends BinaryRelation {

  EqualRelation(SymbolicValue v1, SymbolicValue v2) {
    super(RelationalSymbolicValue.Kind.EQUAL, v1, v2);
  }

  @Override
  protected BinaryRelation symmetric() {
    return new EqualRelation(rightOp, leftOp);
  }

  @Override
  public BinaryRelation inverse() {
    return new NotEqualRelation(leftOp, rightOp);
  }

  @Override
  protected RelationState isImpliedBy(BinaryRelation relation) {
    return relation.impliesEqual();
  }

  @Override
  protected RelationState impliesEqual() {
    return RelationState.FULFILLED;
  }

  @Override
  protected RelationState impliesNotEqual() {
    return RelationState.UNFULFILLED;
  }

  @Override
  protected RelationState impliesMethodEquals() {
    return RelationState.FULFILLED;
  }

  @Override
  protected RelationState impliesNotMethodEquals() {
    return RelationState.UNFULFILLED;
  }

  @Override
  protected RelationState impliesGreaterThan() {
    return RelationState.UNFULFILLED;
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
    return RelationState.FULFILLED;
  }

  @Override
  @CheckForNull
  protected BinaryRelation combinedAfter(BinaryRelation relation) {
    return relation.combinedWithEqual(this);
  }

  @Override
  @CheckForNull
  protected BinaryRelation combinedWithEqual(EqualRelation relation) {
    return new EqualRelation(leftOp, relation.rightOp);
  }

  @Override
  @CheckForNull
  protected BinaryRelation combinedWithNotEqual(NotEqualRelation relation) {
    return new NotEqualRelation(leftOp, relation.rightOp);
  }

  @Override
  protected BinaryRelation combinedWithMethodEquals(MethodEqualsRelation relation) {
    return new MethodEqualsRelation(leftOp, relation.rightOp);
  }

  @Override
  protected BinaryRelation combinedWithNotMethodEquals(NotMethodEqualsRelation relation) {
    return new NotMethodEqualsRelation(leftOp, relation.rightOp);
  }

  @Override
  protected BinaryRelation combinedWithGreaterThan(GreaterThanRelation relation) {
    return new GreaterThanRelation(leftOp, relation.rightOp);
  }

  @Override
  protected BinaryRelation combinedWithGreaterThanOrEqual(GreaterThanOrEqualRelation relation) {
    return new GreaterThanOrEqualRelation(leftOp, relation.rightOp);
  }

  @Override
  protected BinaryRelation combinedWithLessThan(LessThanRelation relation) {
    return new LessThanRelation(leftOp, relation.rightOp);
  }

  @Override
  protected BinaryRelation combinedWithLessThanOrEqual(LessThanOrEqualRelation relation) {
    return new LessThanOrEqualRelation(leftOp, relation.rightOp);
  }
}
