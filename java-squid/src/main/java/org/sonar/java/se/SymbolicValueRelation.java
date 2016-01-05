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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SymbolicValueRelation {

  protected final SymbolicValue v1;
  protected final SymbolicValue v2;

  protected SymbolicValueRelation(SymbolicValue v1, SymbolicValue v2) {
    this.v1 = v1;
    this.v2 = v2;
  }

  @Override
  public int hashCode() {
    long bits = v1.id();
    bits ^= v2.id() * 31;
    bits ^= getClass().hashCode() * 31;
    return ((int) bits) ^ ((int) (bits >> 32));
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof SymbolicValueRelation) {
      return equalsRelation((SymbolicValueRelation) obj);
    }
    return super.equals(obj);
  }

  private boolean equalsRelation(SymbolicValueRelation relation) {
    final int[] key1 = key();
    final int[] key2 = relation.key();
    if (key1[0] == key2[0] && key1[1] == key2[1]) {
      return getClass().equals(relation.getClass());
    }
    return false;
  }

  private int[] key() {
    if (v1.id() < v2.id()) {
      return new int[] {v1.id(), v2.id()};
    } else {
      return new int[] {v2.id(), v1.id()};
    }
  }

  @CheckForNull
  public Boolean impliedBy(Collection<SymbolicValueRelation> knownRelations) {
    return impliedBy(knownRelations, new HashSet<SymbolicValueRelation>());
  }

  @CheckForNull
  public Boolean impliedBy(Collection<SymbolicValueRelation> knownRelations, Set<SymbolicValueRelation> usedRelations) {
    if (knownRelations.isEmpty()) {
      return null;
    }
    if (usedRelations.size() > 200) {
      throw new IllegalStateException();
    }
    for (SymbolicValueRelation relation : knownRelations) {
      Boolean result = relation.implies(this);
      if (result != null) {
        return result;
      }
      usedRelations.add(relation);
      usedRelations.add(relation.symmetric());
    }
    return impliedBy(transitiveReduction(knownRelations, usedRelations), usedRelations);
  }

  private Collection<SymbolicValueRelation> transitiveReduction(Collection<SymbolicValueRelation> knownRelations, Set<SymbolicValueRelation> usedRelations) {
    boolean changed = false;
    List<SymbolicValueRelation> result = new ArrayList<>();
    for (SymbolicValueRelation relation : knownRelations) {
      boolean used = false;
      if (v1.equals(relation.v1)) {
        used = result.addAll(relation.combinedRelations(knownRelations, usedRelations));
      } else if (v1.equals(relation.v2)) {
        used = result.addAll(relation.symmetric().combinedRelations(knownRelations, usedRelations));
      }
      if (used) {
        changed = true;
      } else {
        result.add(relation);
      }
    }
    return changed ? result : Collections.<SymbolicValueRelation>emptyList();
  }

  private Collection<SymbolicValueRelation> combinedRelations(Collection<SymbolicValueRelation> relations, Set<SymbolicValueRelation> usedRelations) {
    List<SymbolicValueRelation> result = new ArrayList<>();
    for (SymbolicValueRelation relation : relations) {
      SymbolicValueRelation combined = null;
      if (v2.equals(relation.v1) && !v1.equals(relation.v2)) {
        combined = relation.combinedAfter(this);
      } else if (v2.equals(relation.v2) && !v1.equals(relation.v2)) {
        combined = relation.symmetric().combinedAfter(this);
      }
      if (combined != null && !usedRelations.contains(combined)) {
        result.add(combined);
      }
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append(v1);
    buffer.append(' ');
    buffer.append(getOperand());
    buffer.append(' ');
    buffer.append(v2);
    return buffer.toString();
  }

  protected abstract String getOperand();

  /**
   * @return a new relation, the negation of the receiver
   */
  protected abstract SymbolicValueRelation inverse();

  /**
   * @return a new relation, equivalent to the receiver with inverted parameters
   */
  protected abstract SymbolicValueRelation symmetric();

  /**
   * @param a relation between symbolic values
   * @return <ul>
   * <li>TRUE  if the receiver implies that the supplied relation is true</li>
   * <li>FALSE if the receiver implies that the supplied relation is false</li>
   * <li>null  otherwise</li>
   * </ul>
   */
  @CheckForNull
  protected Boolean implies(SymbolicValueRelation relation) {
    if (v1.equals(relation.v1) && v2.equals(relation.v2)) {
      return relation.isImpliedBy(this);
    } else if (v1.equals(relation.v2) && v2.equals(relation.v1)) {
      return relation.isImpliedBy(symmetric());
    }
    return null;
  }

  @CheckForNull
  protected abstract Boolean isImpliedBy(SymbolicValueRelation relation);

  @CheckForNull
  protected abstract Boolean impliesEqual();

  @CheckForNull
  protected abstract Boolean impliesNotEqual();

  @CheckForNull
  protected abstract Boolean impliesMethodEquals();

  @CheckForNull
  protected abstract Boolean impliesNotMethodEquals();

  @CheckForNull
  protected abstract SymbolicValueRelation combinedAfter(SymbolicValueRelation relation);

  @CheckForNull
  protected abstract SymbolicValueRelation combinedWithEqual(EqualRelation relation);

  @CheckForNull
  protected abstract SymbolicValueRelation combinedWithNotEqual(NotEqualRelation relation);

  @CheckForNull
  protected abstract SymbolicValueRelation combinedWithMethodEquals(MethodEqualsRelation relation);

  @CheckForNull
  protected abstract SymbolicValueRelation combinedWithNotMethodEquals(NotMethodEqualsRelation relation);
}
