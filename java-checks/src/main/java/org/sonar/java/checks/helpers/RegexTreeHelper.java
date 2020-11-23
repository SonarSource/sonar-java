/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.java.regex.RegexCheck;
import org.sonar.java.regex.ast.AutomatonState;
import org.sonar.java.regex.ast.AutomatonState.TransitionType;
import org.sonar.java.regex.ast.BoundaryTree;
import org.sonar.java.regex.ast.CharacterClassElementTree;
import org.sonar.java.regex.ast.CharacterTree;
import org.sonar.java.regex.ast.EndOfLookaroundState;
import org.sonar.java.regex.ast.LookAroundTree;
import org.sonar.java.regex.ast.RegexSyntaxElement;
import org.sonar.java.regex.ast.RepetitionTree;

import static org.sonar.java.regex.ast.AutomatonState.TransitionType.BACK_REFERENCE;
import static org.sonar.java.regex.ast.AutomatonState.TransitionType.EPSILON;
import static org.sonar.java.regex.ast.AutomatonState.TransitionType.LOOKAROUND_BACKTRACKING;
import static org.sonar.java.regex.ast.AutomatonState.TransitionType.NEGATION;

public class RegexTreeHelper {

  // M (Mark) is "a character intended to be combined with another character (e.g. accents, umlauts, enclosing boxes, etc.)."
  // See https://www.regular-expressions.info/unicode.html
  private static final Pattern MARK_PATTERN = Pattern.compile("\\p{M}");

  private RegexTreeHelper() {
    // Utils class
  }

  public static List<RegexCheck.RegexIssueLocation> getGraphemeInList(List<? extends RegexSyntaxElement> trees) {
    List<RegexCheck.RegexIssueLocation> result = new ArrayList<>();
    RegexSyntaxElement startGrapheme = null;
    RegexSyntaxElement endGrapheme = null;
    for (RegexSyntaxElement child : trees) {
      if (child instanceof CharacterTree) {
        CharacterTree currentCharacter = (CharacterTree) child;
        if (!currentCharacter.isEscapeSequence()) {
          if (!isMark(currentCharacter)) {
            addCurrentGrapheme(result, startGrapheme, endGrapheme);
            startGrapheme = child;
            endGrapheme = null;
          } else if (startGrapheme != null) {
            endGrapheme = child;
          }
          continue;
        }
      }
      addCurrentGrapheme(result, startGrapheme, endGrapheme);
      startGrapheme = null;
      endGrapheme = null;
    }
    addCurrentGrapheme(result, startGrapheme, endGrapheme);
    return result;
  }

  private static boolean isMark(CharacterTree currentChar) {
    return MARK_PATTERN.matcher(currentChar.characterAsString()).matches();
  }

  private static void addCurrentGrapheme(List<RegexCheck.RegexIssueLocation> result, @Nullable RegexSyntaxElement start, @Nullable RegexSyntaxElement end) {
    if (start != null && end != null) {
      result.add(new RegexCheck.RegexIssueLocation(start, end, ""));
    }
  }

  public static boolean canReachWithoutConsumingInput(AutomatonState start, AutomatonState goal) {
    return canReachWithoutConsumingInput(start, goal, new HashSet<>());
  }

  private static boolean canReachWithoutConsumingInput(AutomatonState start, AutomatonState goal, Set<AutomatonState> visited) {
    if (start == goal) {
      return true;
    }
    if (visited.contains(start)) {
      return false;
    }
    visited.add(start);
    for (AutomatonState successor : start.successors()) {
      // We don't generally consider elements behind backtracking edges to be 0-input reachable because what comes
      // after the edge won't directly follow what's before the edge. However, we do consider the end-of-lookahead
      // state itself reachable (but not any state behind it), so that we can check whether the end of the lookahead
      // can be reached without input from a given place within the lookahead.
      if ((successor.incomingTransitionType() == EPSILON && canReachWithoutConsumingInput(successor, goal, visited))
        || (successor instanceof EndOfLookaroundState && successor == goal)) {
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
    return intersects(auto1, auto2, defaultAnswer, new OrderedStatePairCache<>());
  }

  private static boolean intersects(SubAutomaton auto1, SubAutomaton auto2, boolean defaultAnswer, OrderedStatePairCache<Boolean> cache) {
    return computeIfAbsentFromCache(auto1, auto2, defaultAnswer, cache,
      () -> auto1.anySuccessorMatch(successor -> intersects(successor, auto2, defaultAnswer, cache)),
      () -> auto2.anySuccessorMatch(successor -> intersects(auto1, successor, defaultAnswer, cache)),
      () -> {
        if (auto1.start instanceof CharacterClassElementTree && auto2.start instanceof CharacterClassElementTree) {
          SimplifiedRegexCharacterClass characterClass1 = new SimplifiedRegexCharacterClass((CharacterClassElementTree) auto1.start);
          SimplifiedRegexCharacterClass characterClass2 = new SimplifiedRegexCharacterClass((CharacterClassElementTree) auto2.start);
          return characterClass1.intersects(characterClass2, defaultAnswer) &&
            auto1.anySuccessorMatch(successor1 -> auto2.anySuccessorMatch(successor2 -> intersects(successor1, successor2, defaultAnswer, cache)));
        } else {
          return defaultAnswer;
        }
      });
  }

  /**
   * Here auto2.allowPrefix means that if supersetOf(auto1, auto2), then for every string matched by auto2, auto1 can match a prefix of it
   * auto1.allowPrefix means that if supersetOf(auto1, auto2), then for every string matched by auto2, auto1 can match a continuation of it
   * If both are set, it means either one can be the case.
   */
  public static boolean supersetOf(SubAutomaton auto1, SubAutomaton auto2, boolean defaultAnswer) {
    return  supersetOf(auto1, auto2, defaultAnswer, new OrderedStatePairCache<>());
  }

  private static boolean supersetOf(SubAutomaton auto1, SubAutomaton auto2, boolean defaultAnswer, OrderedStatePairCache<Boolean> cache) {
    return computeIfAbsentFromCache(auto1, auto2, defaultAnswer, cache,
      () -> auto1.anySuccessorMatch(successor -> supersetOf(successor, auto2, defaultAnswer, cache)),
      () -> auto2.allSuccessorMatch(successor -> supersetOf(auto1, successor, defaultAnswer, cache)),
      () -> {
        if (auto1.start instanceof CharacterClassElementTree && auto2.start instanceof CharacterClassElementTree) {
          SimplifiedRegexCharacterClass characterClass1 = new SimplifiedRegexCharacterClass((CharacterClassElementTree) auto1.start);
          SimplifiedRegexCharacterClass characterClass2 = new SimplifiedRegexCharacterClass((CharacterClassElementTree) auto2.start);
          return characterClass1.supersetOf(characterClass2, defaultAnswer) &&
            auto1.anySuccessorMatch(successor1 -> auto2.anySuccessorMatch(successor2 -> supersetOf(successor1, successor2, defaultAnswer, cache)));
        } else {
          // DotTree is not yet supported
          return defaultAnswer;
        }
      });
  }

  private static boolean hasNotSupportedTransitionType(SubAutomaton auto) {
    TransitionType transition = auto.start.incomingTransitionType();
    return transition == LOOKAROUND_BACKTRACKING ||
      transition == NEGATION ||
      auto.start instanceof LookAroundTree ||
      auto.start instanceof BoundaryTree ||
      // We could support back-references by having a stack of sub-automata, onto which we push the referenced group,
      // but for now we'll simply bail here
      transition == BACK_REFERENCE ||
      // Properly supporting fixed-max loops would require unrolling the automaton, potentially making it huge
      // Technically the case where min > 1 is unsupported for the same reason, but in that case we treat it
      // as if min were 1, which should hopefully not produce a lot of FPs
      isMoreThanOneFiniteRepetition(auto.start);
  }

  private static boolean isMoreThanOneFiniteRepetition(AutomatonState state) {
    if (state instanceof RepetitionTree) {
      Integer maximumRepetitions = ((RepetitionTree) state).getQuantifier().getMaximumRepetitions();
      return maximumRepetitions != null && maximumRepetitions > 1;
    }
    return false;
  }

  public static boolean computeIfAbsentFromCache(SubAutomaton auto1, SubAutomaton auto2, boolean defaultAnswer, OrderedStatePairCache<Boolean> cache,
    BooleanSupplier evaluateAuto1Successors, BooleanSupplier evaluateAuto2Successors, BooleanSupplier compareAuto1AndAuto2) {
    if (hasNotSupportedTransitionType(auto1) || hasNotSupportedTransitionType(auto2)) {
      return defaultAnswer;
    }
    OrderedStatePair entry = new OrderedStatePair(auto1.start, auto2.start);
    Boolean cachedValue = cache.startCalculation(entry, defaultAnswer);
    if (cachedValue != null) {
      return cachedValue;
    }
    if (auto1.isAtEnd() && auto2.isAtEnd()) {
      return cache.save(entry, true);
    } else if (auto1.isAtEnd() && auto2.incomingTransitionType() != EPSILON) {
      return cache.save(entry, auto2.allowPrefix);
    } else if (auto2.isAtEnd() && auto1.incomingTransitionType() != EPSILON) {
      return cache.save(entry, auto1.allowPrefix);
    } else if (auto2.incomingTransitionType() == EPSILON && !auto2.isAtEnd()) {
      return cache.save(entry, evaluateAuto2Successors.getAsBoolean());
    } else if (auto1.incomingTransitionType() == EPSILON && !auto1.isAtEnd()) {
      return cache.save(entry, evaluateAuto1Successors.getAsBoolean());
    } else {
      return cache.save(entry, compareAuto1AndAuto2.getAsBoolean());
    }
  }

}
