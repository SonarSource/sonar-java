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
package org.sonar.java.checks.helpers;

import org.sonar.java.regex.ast.BoundaryTree;

import static org.sonar.java.regex.ast.AutomatonState.TransitionType.BOUNDARY;

public class IntersectAutomataChecker extends AbstractAutomataChecker {
  public IntersectAutomataChecker(boolean defaultAnswer) {
    super(defaultAnswer);
  }

  @Override
  protected boolean neutralAnswer() {
    return false;
  }

  @Override
  protected boolean checkAuto1AndAuto2Successors(SubAutomaton auto1, SubAutomaton auto2, boolean defaultAnswer, boolean hasConsumedInput) {
    boolean start1IntersectsStart2;
    if (auto1.incomingTransitionType() == BOUNDARY && auto2.incomingTransitionType() == BOUNDARY) {
      start1IntersectsStart2 = ((BoundaryTree) auto1.start).type() == ((BoundaryTree) auto2.start).type();
    } else {
      SimplifiedRegexCharacterClass characterClass1 = SimplifiedRegexCharacterClass.of(auto1.start);
      SimplifiedRegexCharacterClass characterClass2 = SimplifiedRegexCharacterClass.of(auto2.start);
      if (characterClass1 == null || characterClass2 == null) {
        return defaultAnswer;
      }
      start1IntersectsStart2 = characterClass1.intersects(characterClass2, defaultAnswer);
    }
    return start1IntersectsStart2 &&
      auto1.anySuccessorMatch(successor1 -> auto2.anySuccessorMatch(successor2 ->
      check(successor1, successor2, true)));
  }

  @Override
  protected boolean checkAuto1Successors(SubAutomaton auto1, SubAutomaton auto2, boolean defaultAnswer, boolean hasConsumedInput) {
    return auto1.anySuccessorMatch(successor -> check(successor, auto2, hasConsumedInput));
  }

  @Override
  protected boolean checkAuto2Successors(SubAutomaton auto1, SubAutomaton auto2, boolean defaultAnswer, boolean hasConsumedInput) {
    return auto2.anySuccessorMatch(successor -> check(auto1, successor, hasConsumedInput));
  }
}
