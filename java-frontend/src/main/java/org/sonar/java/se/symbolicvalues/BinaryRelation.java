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

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.GREATER_THAN_OR_EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.LESS_THAN;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.METHOD_EQUALS;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.NOT_EQUAL;
import static org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind.NOT_METHOD_EQUALS;

public class BinaryRelation {

  private static final int MAX_ITERATIONS = 10_000;

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
    hashcode = kind.hashCode() + leftOp.hashCode() + rightOp.hashCode();
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

  protected RelationState resolveState(Collection<BinaryRelation> knownRelations) {
    if(hasSameOperand()) {
      return relationStateForSameOperand();
    }
    Set<BinaryRelation> usedRelations = new HashSet<>();
    Set<BinaryRelation> relations = new HashSet<>(knownRelations);
    int iterations = 0;
    while (!relations.isEmpty()) {
      if (usedRelations.size() > 200 || iterations > MAX_ITERATIONS) {
        // safety mechanism in case of an error in the algorithm
        // should not happen under normal conditions
        throw new TransitiveRelationExceededException("Used relations: " + relations.size() + ". Iterations " + iterations);
      }
      iterations++;
      for (BinaryRelation relation : relations) {
        RelationState result = relation.implies(this);
        if (result.isDetermined()) {
          return result;
        }
        usedRelations.add(relation);
      }
      relations = deduce(usedRelations);
    }
    return RelationState.UNDETERMINED;
  }

  static Set<BinaryRelation> deduce(Set<BinaryRelation> relations) {
    Set<BinaryRelation> result = new HashSet<>();
    BinaryRelation[] binaryRelations = relations.toArray(new BinaryRelation[relations.size()]);
    for (int i = 0; i < binaryRelations.length; i++) {
      for (int j = i + 1; j < binaryRelations.length; j++) {
        BinaryRelation a = binaryRelations[i];
        BinaryRelation b = binaryRelations[j];
        BinaryRelation simplified = simplify(a, b);
        if (simplified != null && !relations.contains(simplified)) {
          result.add(simplified);
        }
        if (a.inTransitiveRelationship(b)) {
          BinaryRelation transitive = combineTransitively(a, b);
          if (transitive != null && !relations.contains(transitive)) {
            result.add(transitive);
          }
        }
      }
    }
    return result;
  }

  @CheckForNull
  private static BinaryRelation simplify(BinaryRelation a, BinaryRelation b) {
    // a >= b && b >= a -> a == b
    if (a.kind == GREATER_THAN_OR_EQUAL && b.kind == GREATER_THAN_OR_EQUAL
      && a.hasSameOperandsAs(b) && !a.equals(b)) {
      return binaryRelation(EQUAL, a.leftOp, a.rightOp);
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
    Preconditions.checkState(inTransitiveRelationship(other), "%s is not in transitive relationship with %s", this, other);
    return other.hasOperand(leftOp) ? rightOp : leftOp;
  }

  @VisibleForTesting
  SymbolicValue commonOperand(BinaryRelation other) {
    Preconditions.checkState(inTransitiveRelationship(other));
    return other.hasOperand(leftOp) ? leftOp : rightOp;
  }

  @VisibleForTesting
  boolean inTransitiveRelationship(BinaryRelation other) {
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

  private static BinaryRelation combineTransitively(BinaryRelation a, BinaryRelation b) {
    if (isEqualityRelation(a.kind) || isEqualityRelation(b.kind)) {
      return equalityTransitiveBuilder(a, b);
    }
    if (a.kind == LESS_THAN) {
      return lessThanTransitiveBuilder(a, b);
    }
    if (b.kind == LESS_THAN) {
      return lessThanTransitiveBuilder(b, a);
    }
    if (a.kind == GREATER_THAN_OR_EQUAL && b.kind == GREATER_THAN_OR_EQUAL) {
      BinaryRelation greaterThanEqual = greaterThanEqualTransitiveBuilder(a, b);
      return greaterThanEqual != null ? greaterThanEqual : greaterThanEqualTransitiveBuilder(b, a);
    }
    return null;
  }

  private static boolean isEqualityRelation(Kind kind) {
    return kind == EQUAL || kind == METHOD_EQUALS;
  }

  private static BinaryRelation equalityTransitiveBuilder(BinaryRelation a, BinaryRelation b) {
    BinaryRelation equality = isEqualityRelation(a.kind) ? a : b;
    BinaryRelation other = equality == a ? b : a;
    SymbolicValue transitiveOperand = equality.differentOperand(other);
    boolean leftTransitive = equality.hasOperand(other.leftOp);

    if (equality.kind == METHOD_EQUALS && other.kind == EQUAL) {
      return equalityTransitiveBuilder(other, equality);
    }

    return binaryRelation(other.kind,
      leftTransitive ? transitiveOperand : other.leftOp,
      leftTransitive ? other.rightOp : transitiveOperand);
  }

  @CheckForNull
  private static BinaryRelation lessThanTransitiveBuilder(BinaryRelation lessThan, BinaryRelation other) {
    BinaryRelation deduced = null;
    if (other.kind == LESS_THAN) {
      // a < x && x < b -> a < b
      if (lessThan.rightOp.equals(other.leftOp)) {
        deduced = binaryRelation(LESS_THAN, lessThan.leftOp, other.rightOp);
      }
      // x < a && b < x -> b < a
      if (lessThan.leftOp.equals(other.rightOp)) {
        deduced = binaryRelation(LESS_THAN, other.leftOp, lessThan.rightOp);
      }
    }
    if (other.kind == GREATER_THAN_OR_EQUAL) {
      // a < x && b >= x -> a < b
      if (lessThan.rightOp.equals(other.rightOp)) {
        deduced = binaryRelation(LESS_THAN, lessThan.leftOp, other.leftOp);
      }
      // x < a && x >= b -> b < a
      if (lessThan.leftOp.equals(other.leftOp)) {
        deduced = binaryRelation(LESS_THAN, other.rightOp, lessThan.rightOp);
      }
    }
    return deduced;
  }

  @CheckForNull
  private static BinaryRelation greaterThanEqualTransitiveBuilder(BinaryRelation a, BinaryRelation b) {
    // a >= x && x >= b -> a >= b
    if (a.rightOp.equals(b.leftOp)) {
      return binaryRelation(GREATER_THAN_OR_EQUAL, a.leftOp, b.rightOp);
    }
    return null;
  }

}
