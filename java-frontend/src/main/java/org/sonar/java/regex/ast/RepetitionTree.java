/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.regex.ast;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

public class RepetitionTree extends RegexTree {

  private final RegexTree element;

  private final Quantifier quantifier;

  public RepetitionTree(RegexSource source, IndexRange range, RegexTree element, Quantifier quantifier, FlagSet activeFlags) {
    super(source, range, activeFlags);
    this.element = element;
    this.quantifier = quantifier;
  }

  public RegexTree getElement() {
    return element;
  }

  public Quantifier getQuantifier() {
    return quantifier;
  }

  public boolean isPossessive() {
    return quantifier.getModifier() == Quantifier.Modifier.POSSESSIVE;
  }

  public boolean isReluctant() {
    return quantifier.getModifier() == Quantifier.Modifier.RELUCTANT;
  }

  @Override
  public void accept(RegexVisitor visitor) {
    visitor.visitRepetition(this);
  }

  @Override
  public Kind kind() {
    return Kind.REPETITION;
  }

  @Nonnull
  @Override
  public TransitionType incomingTransitionType() {
    return TransitionType.EPSILON;
  }

  @Nonnull
  @Override
  public List<AutomatonState> successors() {
    if (quantifier.getMinimumRepetitions() == 0) {
      Integer max = quantifier.getMaximumRepetitions();
      if (max != null && max == 0) {
        return Collections.singletonList(continuation());
      } else {
        return flipIfReluctant(element, continuation());
      }
    } else {
      return Collections.singletonList(element);
    }
  }

  @Override
  public void setContinuation(AutomatonState continuation) {
    continuation = new EndOfRepetitionState(this, continuation);
    super.setContinuation(continuation);
    int min = quantifier.getMinimumRepetitions();
    Integer max = quantifier.getMaximumRepetitions();
    if (max != null && max == 1) {
      element.setContinuation(continuation);
    } else if (min >= 1) {
      element.setContinuation(new BranchState(this, flipIfReluctant(this, continuation), activeFlags()));
    } else {
      element.setContinuation(this);
    }
  }

  private List<AutomatonState> flipIfReluctant(AutomatonState tree1, AutomatonState tree2) {
    if (quantifier.getModifier() == Quantifier.Modifier.RELUCTANT) {
      return Arrays.asList(tree2, tree1);
    } else {
      return Arrays.asList(tree1, tree2);
    }
  }
}
