/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.java.regex.RegexCheck;
import org.sonarsource.analyzer.commons.regex.ast.AutomatonState;
import org.sonarsource.analyzer.commons.regex.ast.AutomatonState.TransitionType;
import org.sonarsource.analyzer.commons.regex.ast.BoundaryTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterTree;
import org.sonarsource.analyzer.commons.regex.ast.EndOfLookaroundState;
import org.sonarsource.analyzer.commons.regex.ast.FinalState;
import org.sonarsource.analyzer.commons.regex.ast.LookAroundTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexSyntaxElement;

import static org.sonarsource.analyzer.commons.regex.ast.AutomatonState.TransitionType.EPSILON;
import static org.sonarsource.analyzer.commons.regex.ast.AutomatonState.TransitionType.NEGATION;

public class RegexTreeHelper {

  // M (Mark) is "a character intended to be combined with another character (e.g. accents, umlauts, enclosing boxes, etc.)."
  // See https://www.regular-expressions.info/unicode.html
  private static final Pattern MARK_PATTERN = Pattern.compile("\\p{M}");

  private RegexTreeHelper() {
    // Utils class
  }

  public static List<RegexCheck.RegexIssueLocation> getGraphemeInList(List<? extends RegexSyntaxElement> trees) {
    List<RegexCheck.RegexIssueLocation> result = new ArrayList<>();
    List<RegexSyntaxElement> codePoints = new ArrayList<>();
    for (RegexSyntaxElement child : trees) {
      if (child instanceof CharacterTree) {
        CharacterTree currentCharacter = (CharacterTree) child;
        if (!currentCharacter.isEscapeSequence()) {
          if (!isMark(currentCharacter)) {
            addCurrentGrapheme(result, codePoints);
            codePoints.clear();
            codePoints.add(currentCharacter);
          } else if (!codePoints.isEmpty()) {
            codePoints.add(currentCharacter);
          }
          continue;
        }
      }
      addCurrentGrapheme(result, codePoints);
      codePoints.clear();
    }
    addCurrentGrapheme(result, codePoints);
    return result;
  }

  private static boolean isMark(CharacterTree currentChar) {
    return MARK_PATTERN.matcher(currentChar.characterAsString()).matches();
  }

  private static void addCurrentGrapheme(List<RegexCheck.RegexIssueLocation> result, List<RegexSyntaxElement> codePoints) {
    if (codePoints.size() > 1) {
      result.add(new RegexCheck.RegexIssueLocation(new ArrayList<>(codePoints), ""));
    }
  }

  public static boolean canReachWithoutConsumingInput(AutomatonState start, AutomatonState goal) {
    return canReachWithoutConsumingInput(start, goal, false, new HashSet<>());
  }

  public static boolean canReachWithoutConsumingInputOrGoingThroughBoundaries(AutomatonState start, AutomatonState goal) {
    return canReachWithoutConsumingInput(start, goal, true, new HashSet<>());
  }

  private static boolean canReachWithoutConsumingInput(AutomatonState start, AutomatonState goal, boolean stopAtBoundaries, Set<AutomatonState> visited) {
    if (start == goal) {
      return true;
    }
    if (visited.contains(start) || (stopAtBoundaries && start instanceof BoundaryTree)) {
      return false;
    }
    visited.add(start);
    for (AutomatonState successor : start.successors()) {
      TransitionType transition = successor.incomingTransitionType();
      // We don't generally consider elements behind backtracking edges to be 0-input reachable because what comes
      // after the edge won't directly follow what's before the edge. However, we do consider the end-of-lookahead
      // state itself reachable (but not any state behind it), so that we can check whether the end of the lookahead
      // can be reached without input from a given place within the lookahead.
      if ((successor instanceof EndOfLookaroundState && successor == goal)
        || ((transition == EPSILON || transition == NEGATION) && canReachWithoutConsumingInput(successor, goal, stopAtBoundaries, visited))) {
        return true;
      }
    }
    return false;
  }

  /**
   * If both sub-automata have allowPrefix set to true, this method will check whether auto1 intersects
   * the prefix of auto2 or auto2 intersects the prefix of auto1. This is different than checking whether
   * the prefix of auto1 intersects the prefix of auto2 (which would always be true because both prefix
   * always contain the empty string).
   * defaultAnswer will be returned in case of unsupported features or the state limit is exceeded.
   * It should be whichever answer does not lead to an issue being reported to avoid false positives.
   */
  public static boolean intersects(SubAutomaton auto1, SubAutomaton auto2, boolean defaultAnswer) {
    return new IntersectAutomataChecker(defaultAnswer).check(auto1, auto2);
  }

  /**
   * Here auto2.allowPrefix means that if supersetOf(auto1, auto2), then for every string matched by auto2, auto1 can match a prefix of it
   * auto1.allowPrefix means that if supersetOf(auto1, auto2), then for every string matched by auto2, auto1 can match a continuation of it
   * If both are set, it means either one can be the case.
   */
  public static boolean supersetOf(SubAutomaton auto1, SubAutomaton auto2, boolean defaultAnswer) {
    return new SupersetAutomataChecker(defaultAnswer).check(auto1, auto2);
  }

  public static boolean isAnchoredAtEnd(AutomatonState start) {
    return isAnchoredAtEnd(start, new HashSet<>());
  }

  private static boolean isAnchoredAtEnd(AutomatonState start, Set<AutomatonState> visited) {
    if (isEndBoundary(start)) {
      return true;
    }
    if (start instanceof FinalState || visited.contains(start)) {
      return false;
    }
    visited.add(start);
    for (AutomatonState successor : start.successors()) {
      if (!isAnchoredAtEnd(successor, visited)) {
        return false;
      }
    }
    return true;
  }

  public static boolean isEndBoundary(AutomatonState state) {
    if (!(state instanceof BoundaryTree)) {
      return false;
    }
    switch (((BoundaryTree) state).type()) {
      case LINE_END:
      case INPUT_END:
      case INPUT_END_FINAL_TERMINATOR:
        return true;
      default:
        return false;
    }
  }

  public static boolean onlyMatchesEmptySuffix(AutomatonState start) {
    return onlyMatchesEmptySuffix(start, new HashSet<>());
  }

  private static boolean onlyMatchesEmptySuffix(AutomatonState start, Set<AutomatonState> visited) {
    if (start instanceof FinalState || visited.contains(start)) {
      return true;
    }
    visited.add(start);
    if (start instanceof LookAroundTree) {
      return onlyMatchesEmptySuffix(start.continuation(), visited);
    }
    if (start.incomingTransitionType() != EPSILON) {
      return false;
    }

    for (AutomatonState successor : start.successors()) {
      if (!onlyMatchesEmptySuffix(successor, visited)) {
        return false;
      }
    }
    return true;
  }
}
