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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind;

import javax.annotation.CheckForNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.GREATER_THAN_OR_EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.LESS_THAN;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.METHOD_EQUALS;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.NOT_EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.NOT_METHOD_EQUALS;

public class BinaryRelation {

  private static final int MAX_ITERATIONS = 10_000;
  private static final int MAX_DEDUCED_RELATIONS = 1000;

  private static final Set<Kind> NORMALIZED_OPERATORS = EnumSet.of(
      EQUAL, NOT_EQUAL,
      LESS_THAN, GREATER_THAN_OR_EQUAL,
      METHOD_EQUALS, NOT_METHOD_EQUALS);

  protected final Kind kind;
  protected final SymbolicValue leftOp;
  protected final SymbolicValue rightOp;
  private BinaryRelation inverse;
  private final int hashcode;

  private BinaryRelation(Kind kind, SymbolicValue v1, SymbolicValue v2) {
    if (!NORMALIZED_OPERATORS.contains(kind)) {
      throw new IllegalArgumentException("Relation " + v1 + kind.operand + v2 + " not normalized!");
    }
    this.kind = kind;
    leftOp = v1;
    rightOp = v2;
    hashcode = computeHash();
  }

  private int computeHash() {
    return 31 * (kind.hashCode() + leftOp.hashCode() + rightOp.hashCode());
  }

  static BinaryRelation binaryRelation(Kind kind, SymbolicValue leftOp, SymbolicValue rightOp) {
    switch (kind) {
      case EQUAL:
      case NOT_EQUAL:
      case LESS_THAN:
      case GREATER_THAN_OR_EQUAL:
      case METHOD_EQUALS:
      case NOT_METHOD_EQUALS:
        return new BinaryRelation(kind, leftOp, rightOp);
      case LESS_THAN_OR_EQUAL:
      case GREATER_THAN:
        return new BinaryRelation(kind.symmetric(), rightOp, leftOp);
      default:
        throw new IllegalStateException("Creation of relation of kind " + kind + " is missing!");
    }
  }

  public static class TransitiveRelationExceededException extends RuntimeException {
    public TransitiveRelationExceededException(String msg) {
      super("Number of transitive relations exceeded!" + msg);
    }
  }

  @Override
  public int hashCode() {
    return hashcode;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof BinaryRelation) {
      return equalsRelation((BinaryRelation) obj);
    }
    return false;
  }

  private boolean equalsRelation(BinaryRelation other) {
    if (!kind.equals(other.kind)) {
      return false;
    }
    switch (kind) {
      case EQUAL:
      case NOT_EQUAL:
      case METHOD_EQUALS:
      case NOT_METHOD_EQUALS:
        return hasSameOperandsAs(other);
      default:
        return leftOp.equals(other.leftOp) && rightOp.equals(other.rightOp);
    }
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(leftOp);
    buffer.append(kind.operand);
    buffer.append(rightOp);
    return buffer.toString();
  }

  protected RelationState resolveState(List<BinaryRelation> knownRelations) {
    if (hasSameOperand()) {
      return relationStateForSameOperand();
    }
    Set<BinaryRelation> allRelations = new HashSet<>(knownRelations);
    Deque<BinaryRelation> workList = new ArrayDeque<>(knownRelations);
    int iterations = 0;
    while (!workList.isEmpty()) {
      if (allRelations.size() > MAX_DEDUCED_RELATIONS || iterations > MAX_ITERATIONS) {
        // safety mechanism in case of an error in the algorithm
        // should not happen under normal conditions
        throw new TransitiveRelationExceededException("Used relations: " + allRelations.size() + ". Iterations " + iterations);
      }
      iterations++;
      BinaryRelation relation = workList.pop();
      RelationState result = relation.implies(this);
      if (result.isDetermined()) {
        return result;
      }
      List<BinaryRelation> newRelations = allRelations.stream()
        .map(relation::deduceTransitiveOrSimplified)
        .filter(Objects::nonNull)
        .filter(r -> !allRelations.contains(r))
        .collect(Collectors.toList());

      allRelations.addAll(newRelations);
      workList.addAll(newRelations);
    }
    return RelationState.UNDETERMINED;
  }

  @VisibleForTesting
  BinaryRelation deduceTransitiveOrSimplified(BinaryRelation other) {
    BinaryRelation result = simplify(other);
    if (result != null) {
      return result;
    }
    return combineTransitively(other);
  }

  @CheckForNull
  private BinaryRelation simplify(BinaryRelation other) {
    // a >= b && b >= a -> a == b
    if (kind == GREATER_THAN_OR_EQUAL && other.kind == GREATER_THAN_OR_EQUAL
      && hasSameOperandsAs(other) && !equals(other)) {
      return binaryRelation(EQUAL, leftOp, rightOp);
    }
    return null;
  }

  private boolean hasSameOperand() {
    return leftOp.equals(rightOp);
  }

  private boolean hasOperand(SymbolicValue operand) {
    return leftOp.equals(operand) || rightOp.equals(operand);
  }

  private boolean hasSameOperandsAs(BinaryRelation other) {
    return (leftOp.equals(other.leftOp) && rightOp.equals(other.rightOp))
      || (leftOp.equals(other.rightOp) && rightOp.equals(other.leftOp));
  }

  @VisibleForTesting
  SymbolicValue differentOperand(BinaryRelation other) {
    Preconditions.checkState(potentiallyTransitiveWith(other), "%s is not in transitive relationship with %s", this, other);
    return other.hasOperand(leftOp) ? rightOp : leftOp;
  }

  @VisibleForTesting
  SymbolicValue commonOperand(BinaryRelation other) {
    Preconditions.checkState(potentiallyTransitiveWith(other));
    return other.hasOperand(leftOp) ? leftOp : rightOp;
  }

  @VisibleForTesting
  boolean potentiallyTransitiveWith(BinaryRelation other) {
    if (hasSameOperand() || other.hasSameOperand()) {
      return false;
    }
    return (hasOperand(other.leftOp) || hasOperand(other.rightOp)) && !hasSameOperandsAs(other);
  }

  private RelationState relationStateForSameOperand() {
    switch (kind) {
      case EQUAL:
      case GREATER_THAN_OR_EQUAL:
      case METHOD_EQUALS:
        return RelationState.FULFILLED;
      case NOT_EQUAL:
      case LESS_THAN:
      case NOT_METHOD_EQUALS:
        return RelationState.UNFULFILLED;
      default:
        throw new IllegalStateException("Unknown resolution for same operand " + this);
    }
  }

  /**
   * @return a new relation, the negation of the receiver
   */
  public BinaryRelation inverse() {
    if (inverse == null) {
      inverse = binaryRelation(kind.inverse(), leftOp, rightOp);
    }
    return inverse;
  }

  /**
   * @param relation a relation between symbolic values
   * @return a RelationState<ul>
   * <li>FULFILLED  if the receiver implies that the supplied relation is true</li>
   * <li>UNFULFILLED if the receiver implies that the supplied relation is false</li>
   * <li>UNDETERMINED  otherwise</li>
   * </ul>
   * @see RelationState
   */
  private RelationState implies(BinaryRelation relation) {
    if (this.equals(relation)) {
      return RelationState.FULFILLED;
    }
    if (inverse().equals(relation)) {
      return RelationState.UNFULFILLED;
    }
    if (hasSameOperandsAs(relation)) {
      return RelationStateTable.solveRelation(kind, relation.kind);
    }
    return RelationState.UNDETERMINED;
  }

  @CheckForNull
  private BinaryRelation combineTransitively(BinaryRelation other) {
    if (!potentiallyTransitiveWith(other)) {
      return null;
    }
    BinaryRelation transitive = combineTransitivelyOneWay(other);
    if (transitive != null) {
      return transitive;
    }
    return other.combineTransitivelyOneWay(this);
  }

  @CheckForNull
  private BinaryRelation combineTransitivelyOneWay(BinaryRelation other) {
    BinaryRelation transitive = equalityTransitiveBuilder(other);
    if (transitive != null) {
      return transitive;
    }
    transitive = lessThanTransitiveBuilder(other);
    if (transitive != null) {
      return transitive;
    }
    return greaterThanEqualTransitiveBuilder(other);
  }

  private boolean isEqualityRelation() {
    return kind == EQUAL || kind == METHOD_EQUALS;
  }

  @CheckForNull
  private BinaryRelation equalityTransitiveBuilder(BinaryRelation other) {
    if (!isEqualityRelation()
      || (kind == METHOD_EQUALS && other.kind == EQUAL)) {
      return null;
    }

    return binaryRelation(other.kind,
      hasOperand(other.leftOp) ? differentOperand(other) : other.leftOp,
      hasOperand(other.leftOp) ? other.rightOp : differentOperand(other));
  }

  @CheckForNull
  private BinaryRelation lessThanTransitiveBuilder(BinaryRelation other) {
    if (kind != LESS_THAN) {
      return null;
    }
    if (other.kind == LESS_THAN) {
      // a < x && x < b => a < b
      if (rightOp.equals(other.leftOp)) {
        return binaryRelation(LESS_THAN, leftOp, other.rightOp);
      }
      // x < a && b < x => b < a
      if (leftOp.equals(other.rightOp)) {
        return binaryRelation(LESS_THAN, other.leftOp, rightOp);
      }
    }
    if (other.kind == GREATER_THAN_OR_EQUAL) {
      // a < x && b >= x => a < b
      if (rightOp.equals(other.rightOp)) {
        return binaryRelation(LESS_THAN, leftOp, other.leftOp);
      }
      // x < a && x >= b => b < a
      if (leftOp.equals(other.leftOp)) {
        return binaryRelation(LESS_THAN, other.rightOp, rightOp);
      }
    }
    return null;
  }

  @CheckForNull
  private BinaryRelation greaterThanEqualTransitiveBuilder(BinaryRelation other) {
    // a >= x && x >= b -> a >= b
    if (kind == GREATER_THAN_OR_EQUAL && other.kind == GREATER_THAN_OR_EQUAL && rightOp.equals(other.leftOp)) {
      return binaryRelation(GREATER_THAN_OR_EQUAL, leftOp, other.rightOp);
    }
    return null;
  }

}
