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

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class SymbolicValueRelation {

  protected final SymbolicValue v1;
  protected final SymbolicValue v2;

  protected SymbolicValueRelation(SymbolicValue v1, SymbolicValue v2) {
    this.v1 = v1;
    this.v2 = v2;
  }

  public Boolean impliedBy(List<SymbolicValueRelation> knownRelations) {
    for (SymbolicValueRelation relation : knownRelations) {
      Boolean result = relation.implies(this);
      if (result != null) {
        return result;
      }
    }
    for (int i = 0; i < knownRelations.size(); i++) {
      SymbolicValueRelation relation = knownRelations.get(i);
      SymbolicValueRelation combined = underHypothesis(relation);
      if (combined != null) {
        final List<SymbolicValueRelation> relations = new ArrayList<>(knownRelations);
        relations.remove(i);
        Boolean result = combined.impliedBy(relations);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  protected boolean hasSameOperand(SymbolicValueRelation relation) {
    return (v1.equals(relation.v1) && v2.equals(relation.v2))
      || (v1.equals(relation.v2) && v2.equals(relation.v1));
  }

  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append('(');
    buffer.append(v1);
    buffer.append(' ');
    buffer.append(getOperand());
    buffer.append(' ');
    buffer.append(v2);
    buffer.append(')');
    return buffer.toString();
  }

  protected abstract String getOperand();

  protected abstract SymbolicValueRelation inverse();

  /**
   * @param a relation between symbolic values
   * @return <ul>
   * <li>TRUE  if the supplied relation implies that the receiver is true</li>
   * <li>FALSE if the supplied relation implies that the receiver is false</li>
   * <li>null  otherwise</li>
   * </ul>
   */
  protected abstract Boolean implies(SymbolicValueRelation relation);

  /**
   * Returns a new relation to be checked against a new set of hypotheses without the supplied relation.
   * The precondition is that none of the known hypotheses yields direct information on the receiver (the checked relation).
   * This result is a combination of the receiver (currently checked relation against the set of hypotheses) and the supplied relation.
   * If the receiver combined with the supplied relation does not yield new information, null is returned.
   *
   * @param relation, belonging to the set of known hypotheses
   * @return a SymbolicValueRelation or null
   */
  @Nullable
  protected abstract SymbolicValueRelation underHypothesis(SymbolicValueRelation relation);

  protected abstract SymbolicValueRelation hypothesisWithEqual(EqualRelation relation);

  protected abstract SymbolicValueRelation hypothesisWithNotEqual(NotEqualRelation relation);
}
