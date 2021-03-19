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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.sonar.java.regex.ast.BoundaryTree;

public class IntersectAutomataChecker extends AbstractAutomataChecker {
  private static final Map<BoundaryTree.Type, Map<BoundaryTree.Type, Boolean>> BOUNDARIES_INTERSECT_MAP = new EnumMap<>(BoundaryTree.Type.class);
  static {
    Arrays.stream(BoundaryTree.Type.values())
      .forEach(type -> mapIntersect(type, type, true));

    mapIntersect(BoundaryTree.Type.LINE_START, BoundaryTree.Type.INPUT_START);
    mapIntersect(BoundaryTree.Type.LINE_END, BoundaryTree.Type.INPUT_END_FINAL_TERMINATOR, BoundaryTree.Type.INPUT_END);

    mapIntersect(BoundaryTree.Type.NON_WORD, BoundaryTree.Type.WORD, false);
    mapIntersect(BoundaryTree.Type.INPUT_START, BoundaryTree.Type.INPUT_END_FINAL_TERMINATOR, false);
    mapIntersect(BoundaryTree.Type.INPUT_START, BoundaryTree.Type.INPUT_END, false);
    mapIntersect(BoundaryTree.Type.INPUT_START, BoundaryTree.Type.LINE_END, false);
    mapIntersect(BoundaryTree.Type.LINE_START, BoundaryTree.Type.INPUT_END_FINAL_TERMINATOR, false);
    mapIntersect(BoundaryTree.Type.LINE_START, BoundaryTree.Type.INPUT_END, false);
    mapIntersect(BoundaryTree.Type.LINE_START, BoundaryTree.Type.LINE_END, false);
  }

  public IntersectAutomataChecker(boolean defaultAnswer) {
    super(defaultAnswer);
  }

  private static void mapIntersect(BoundaryTree.Type... types) {
    for (int i = 0; i < types.length; i++) {
      for (int j = 0; j < types.length; j++) {
        mapIntersect(types[i], types[j], true);
      }
    }
  }

  private static void mapIntersect(BoundaryTree.Type type1, BoundaryTree.Type type2, Boolean intersect) {
    BOUNDARIES_INTERSECT_MAP.computeIfAbsent(type1, t -> new EnumMap<>(BoundaryTree.Type.class)).put(type2, intersect);
    BOUNDARIES_INTERSECT_MAP.computeIfAbsent(type2, t -> new EnumMap<>(BoundaryTree.Type.class)).put(type1, intersect);
  }

  @Override
  protected boolean neutralAnswer() {
    return false;
  }

  @Override
  protected boolean checkAuto1AndAuto2Successors(SubAutomaton auto1, SubAutomaton auto2, boolean defaultAnswer, boolean hasConsumedInput) {
    SimplifiedRegexCharacterClass characterClass1 = SimplifiedRegexCharacterClass.of(auto1.start);
    SimplifiedRegexCharacterClass characterClass2 = SimplifiedRegexCharacterClass.of(auto2.start);
    if (characterClass1 != null && characterClass2 != null) {
      return checkCharacterClassIntersect(auto1, characterClass1, auto2, characterClass2);
    }

    BoundaryTree.Type boundaryType1 = boundaryTypeOf(auto1.start);
    BoundaryTree.Type boundaryType2 = boundaryTypeOf(auto2.start);
    if (boundaryType1 != null && boundaryType2 != null) {
      return checkBoundariesIntersect(auto1, boundaryType1, auto2, boundaryType2);
    }

    if (boundaryType2 != null) {
      // characterClass1 != null,
      return defaultAnswer && checkAuto2Successors(auto1, auto2, defaultAnswer, hasConsumedInput);
    } else {
      // boundaryType1 != null && characterClass2 != null
      return defaultAnswer && checkAuto1Successors(auto1, auto2, defaultAnswer, hasConsumedInput);
    }
  }

  private boolean checkBoundariesIntersect(SubAutomaton auto1, BoundaryTree.Type boundaryType1, SubAutomaton auto2, BoundaryTree.Type boundaryType2) {
    return BOUNDARIES_INTERSECT_MAP.getOrDefault(boundaryType1, Collections.emptyMap()).getOrDefault(boundaryType2, defaultAnswer)
      && checkSuccessors(auto1, auto2);
  }

  private boolean checkCharacterClassIntersect(
    SubAutomaton auto1, SimplifiedRegexCharacterClass characterClass1,
    SubAutomaton auto2, SimplifiedRegexCharacterClass characterClass2) {
    return characterClass1.intersects(characterClass2, defaultAnswer)
      && checkSuccessors(auto1, auto2);
  }

  private boolean checkSuccessors(SubAutomaton auto1, SubAutomaton auto2) {
    return auto1.anySuccessorMatch(successor1 -> auto2.anySuccessorMatch(successor2 -> check(successor1, successor2, true)));
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
