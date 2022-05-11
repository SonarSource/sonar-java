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
package org.sonar.java.checks.regex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.RegexReachabilityChecker;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonarsource.analyzer.commons.regex.MatchType;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.AtomicGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.AutomatonState;
import org.sonarsource.analyzer.commons.regex.ast.BackReferenceTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassElementTree;
import org.sonarsource.analyzer.commons.regex.ast.DisjunctionTree;
import org.sonarsource.analyzer.commons.regex.ast.DotTree;
import org.sonarsource.analyzer.commons.regex.ast.GroupTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.RepetitionTree;
import org.sonarsource.analyzer.commons.regex.helpers.IntersectAutomataChecker;
import org.sonarsource.analyzer.commons.regex.helpers.SimplifiedRegexCharacterClass;
import org.sonarsource.analyzer.commons.regex.helpers.SubAutomaton;

import static org.sonarsource.analyzer.commons.regex.helpers.RegexReachabilityChecker.canReachWithoutConsumingInput;
import static org.sonarsource.analyzer.commons.regex.helpers.RegexReachabilityChecker.canReachWithoutConsumingInputNorCrossingBoundaries;
import static org.sonarsource.analyzer.commons.regex.helpers.RegexTreeHelper.isAnchoredAtEnd;

@Rule(key = "S5852")
public class RedosCheck extends AbstractRegexCheckTrackingMatchType {

  private static final String MESSAGE = "Make sure the regex used here, which is vulnerable to %s runtime due to backtracking," +
    " cannot lead to denial of service%s.";
  private static final String JAVA8_MESSAGE = " or make sure the code is only run using Java 9 or later";
  private static final String EXP = "exponential";
  private static final String POLY = "polynomial";

  /**
   * The maximum number of repetitions we keep track of in order to find overlapping consecutive repetitions.
   * If a regex contains more repetitions than this, we will ignore some combinations of them to avoid performance
   * problems (possibly causing FNs).
   */
  private static final int MAX_TRACKED_REPETITIONS = 10;

  /**
   * The maximum regex length that we analyze. If a regex contains more characters than this, we skip this rule to avoid
   * performance problems.
   */
  private static final int MAX_REGEX_LENGTH = 1000;

  private boolean regexContainsBackReference;
  private BacktrackingType foundBacktrackingType;

  private final RegexReachabilityChecker reachabilityChecker = new RegexReachabilityChecker(false);
  private final IntersectAutomataChecker intersectionChecker = new IntersectAutomataChecker(false);

  // Java 9 introduced a loop optimization that's applied to greedy repetitions in regexes that don't use capturing groups.
  // Without this optimization any loop where for the same input multiple paths can be taken through the loop's body,
  // has exponential runtime. With the optimization such loops, if they are greedy, have either quadratic runtime (if
  // the paths go through an inner loop) or linear (i.e. safe) runtime.
  // Consecutive (not nested) loops that can overlap each other cause quadratic runtime and are unaffected by this
  // optimization.
  enum BacktrackingType {
    ALWAYS_EXPONENTIAL,
    QUADRATIC_WHEN_OPTIMIZED,
    ALWAYS_QUADRATIC,
    LINEAR_WHEN_OPTIMIZED,
    NO_ISSUE
  }

  private boolean isJava9OrHigher() {
    return context.getJavaVersion().isNotSet() || context.getJavaVersion().asInt() >= 9;
  }
  
  private Optional<String> message() {
    boolean canBeOptimized = !regexContainsBackReference;
    boolean optimized = isJava9OrHigher() && canBeOptimized;
    switch (foundBacktrackingType) {
      case ALWAYS_EXPONENTIAL:
        return Optional.of(String.format(MESSAGE, EXP, ""));
      case QUADRATIC_WHEN_OPTIMIZED:
        // We only suggest upgrading to Java 9+ when that would make the regex safe (i.e. linear runtime), not if it would
        // merely improve it from exponential to quadratic.
        return Optional.of(String.format(MESSAGE, optimized ? POLY : EXP, ""));
      case LINEAR_WHEN_OPTIMIZED:
        if (optimized) {
          return Optional.empty();
        } else {
          return Optional.of(String.format(MESSAGE, EXP, canBeOptimized ? JAVA8_MESSAGE : ""));
        }
      case ALWAYS_QUADRATIC:
        return Optional.of(String.format(MESSAGE, POLY, ""));
      case NO_ISSUE:
        return Optional.empty();
    }
    throw new IllegalStateException("This line is not actually reachable");
  }

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation, MatchType matchType) {
    if (regexForLiterals.getResult().getText().length() > MAX_REGEX_LENGTH) {
      return;
    }
    regexContainsBackReference = false;
    foundBacktrackingType = BacktrackingType.NO_ISSUE;
    reachabilityChecker.clearCache();
    intersectionChecker.clearCache();
    boolean isUsedForFullMatch = matchType == MatchType.FULL || matchType == MatchType.BOTH;
    boolean isUsedForPartialMatch = matchType == MatchType.PARTIAL || matchType == MatchType.BOTH;
    RedosFinder visitor = new RedosFinder(regexForLiterals.getStartState(), regexForLiterals.getFinalState(), isUsedForFullMatch, isUsedForPartialMatch);
    visitor.visit(regexForLiterals);
    message().ifPresent(message ->
      reportIssue(methodOrAnnotationName(methodInvocationOrAnnotation), message, null, Collections.emptyList())
    );
  }

  private void addBacktracking(BacktrackingType newBacktrackingType) {
    if (newBacktrackingType.ordinal() < foundBacktrackingType.ordinal()) {
      foundBacktrackingType = newBacktrackingType;
    }
  }

  private class RedosFinder extends RegexBaseVisitor {


    private final Deque<RepetitionTree> nonPossessiveRepetitions = new ArrayDeque<>();
    private final Map<AutomatonState, Boolean> canFailCache = new HashMap<>();

    private final AutomatonState startOfRegex;
    private final AutomatonState endOfRegex;
    private final boolean isUsedForFullMatch;
    private final boolean isUsedForPartialMatch;

    public RedosFinder(AutomatonState startOfRegex, AutomatonState endOfRegex, boolean isUsedForFullMatch, boolean isUsedForPartialMatch) {
      this.startOfRegex = startOfRegex;
      this.endOfRegex = endOfRegex;
      this.isUsedForFullMatch = isUsedForFullMatch;
      this.isUsedForPartialMatch = isUsedForPartialMatch;
    }

    @Override
    public void visitRepetition(RepetitionTree tree) {
      if (canFail(tree.continuation())) {
        if (!tree.isPossessive() && tree.getQuantifier().isOpenEnded()) {
          new BacktrackingFinder(tree.isReluctant(), tree.continuation()).visit(tree.getElement());
        } else {
          super.visitRepetition(tree);
        }
        checkForOverlappingRepetitions(tree);
      }
    }

    private void checkForOverlappingRepetitions(RepetitionTree tree) {
      if (tree.getQuantifier().isOpenEnded() && canFail(tree)) {
        for (RepetitionTree repetition : nonPossessiveRepetitions) {
          if (reachabilityChecker.canReach(repetition, tree)) {
            SubAutomaton repetitionAuto = new SubAutomaton(repetition.getElement(), repetition.continuation(), false);
            SubAutomaton continuationAuto = new SubAutomaton(repetition.continuation(), tree, false);
            SubAutomaton treeAuto = new SubAutomaton(tree.getElement(), tree.continuation(), false);
            if (subAutomatonCanConsume(repetitionAuto, continuationAuto)
              && automatonIsEmptyOrIntersects(continuationAuto, treeAuto)
              && intersectionChecker.check(repetitionAuto, treeAuto)) {
              addBacktracking(BacktrackingType.ALWAYS_QUADRATIC);
            }
          }
        }
        if (overlapsWithImplicitMatchAlls(tree)) {
          addBacktracking(BacktrackingType.ALWAYS_QUADRATIC);
        }
        addIfNonPossessive(tree);
      }
    }

    private boolean subAutomatonCanConsume(SubAutomaton auto1, SubAutomaton auto2) {
      return canReachWithoutConsumingInputNorCrossingBoundaries(auto1.end, auto2.end)
        || intersectionChecker.check(auto1, auto2);
    }

    private boolean automatonIsEmptyOrIntersects(SubAutomaton auto1, SubAutomaton auto2) {
      return canReachWithoutConsumingInputNorCrossingBoundaries(auto1.start, auto1.end)
        || intersectionChecker.check(auto1, auto2);
    }

    private void addIfNonPossessive(RepetitionTree tree) {
      if (!tree.isPossessive()) {
        nonPossessiveRepetitions.add(tree);
        if (nonPossessiveRepetitions.size() > MAX_TRACKED_REPETITIONS) {
          nonPossessiveRepetitions.removeFirst();
        }
      }
    }

    /**
     * When used for partial matches, a regex acts as if it had `(?s:.*)` attached to its beginning and end unless anchored.
     */
    private boolean overlapsWithImplicitMatchAlls(RepetitionTree tree) {
      return isUsedForPartialMatch && canReachWithoutConsumingInputNorCrossingBoundaries(startOfRegex, tree);
    }

    @Override
    public void visitBackReference(BackReferenceTree tree) {
      regexContainsBackReference = true;
    }

    private boolean canFail(AutomatonState state) {
      return canFail(state, !isUsedForFullMatch && !isAnchoredAtEnd(state));
    }

    private boolean canFail(AutomatonState state, boolean succeedOnEnd) {
      if (canFailCache.containsKey(state)) {
        return canFailCache.get(state);
      }
      canFailCache.put(state, true);
      if (state.incomingTransitionType() != AutomatonState.TransitionType.EPSILON) {
        return true;
      }
      if (canMatchAnything(state)) {
        succeedOnEnd = true;
        state = state.continuation();
      }
      if ((succeedOnEnd && canReachWithoutConsumingInput(state, endOfRegex))) {
        canFailCache.put(state, false);
        return false;
      }
      for (AutomatonState successor : state.successors()) {
        if (!canFail(successor, succeedOnEnd)) {
          canFailCache.put(state, false);
          return false;
        }
      }
      return true;
    }

    private boolean canMatchAnything(AutomatonState state) {
      if (!(state instanceof RepetitionTree)) {
        return false;
      }
      RepetitionTree repetition = (RepetitionTree) state;
      return repetition.getQuantifier().getMinimumRepetitions() == 0 && repetition.getQuantifier().isOpenEnded()
        && canMatchAnyCharacter(repetition.getElement());
    }

    private boolean canMatchAnyCharacter(RegexTree tree) {
      SimplifiedRegexCharacterClass characterClass = new SimplifiedRegexCharacterClass();
      for (RegexTree singleCharacter : collectSingleCharacters(tree, new ArrayList<>())) {
        if (singleCharacter.is(RegexTree.Kind.DOT)) {
          characterClass.add((DotTree) singleCharacter);
        } else {
          characterClass.add((CharacterClassElementTree) singleCharacter);
        }
      }
      return characterClass.matchesAnyCharacter();
    }

    private List<RegexTree> collectSingleCharacters(@Nullable RegexTree tree, List<RegexTree> accumulator) {
      if (tree == null) {
        return accumulator;
      }
      if (tree instanceof CharacterClassElementTree || tree.is(RegexTree.Kind.DOT)) {
        accumulator.add(tree);
      } else if (tree.is(RegexTree.Kind.DISJUNCTION)) {
        for (RegexTree alternative : ((DisjunctionTree) tree).getAlternatives()) {
          collectSingleCharacters(alternative, accumulator);
        }
      } else if (tree instanceof GroupTree) {
        collectSingleCharacters(((GroupTree) tree).getElement(), accumulator);
      } else if (tree.is(RegexTree.Kind.REPETITION)) {
        RepetitionTree repetition = (RepetitionTree) tree;
        if (repetition.getQuantifier().getMinimumRepetitions() <= 1) {
          collectSingleCharacters(repetition.getElement(), accumulator);
        }
      }
      return accumulator;
    }

  }

  private class BacktrackingFinder extends RegexBaseVisitor {

    private final boolean isReluctant;
    private final AutomatonState endOfLoop;

    public BacktrackingFinder(boolean isReluctant, AutomatonState endOfLoop) {
      this.isReluctant = isReluctant;
      this.endOfLoop = endOfLoop;
    }

    @Override
    public void visitAtomicGroup(AtomicGroupTree tree) {
      new RedosFinder(tree, tree.continuation(), false, false).visit(tree);
    }

    @Override
    public void visitRepetition(RepetitionTree tree) {
      if (tree.isPossessive()) {
        new RedosFinder(tree, tree.continuation(), false, false).visit(tree);
      } else if (containsIntersections(Arrays.asList(tree.getElement(), tree.continuation()))) {
        BacktrackingType greedyComplexity = tree.getQuantifier().isOpenEnded() ? BacktrackingType.QUADRATIC_WHEN_OPTIMIZED : BacktrackingType.LINEAR_WHEN_OPTIMIZED;
        addBacktracking(isReluctant ? BacktrackingType.ALWAYS_EXPONENTIAL : greedyComplexity);
        super.visitRepetition(tree);
      } else {
        super.visitRepetition(tree);
      }
    }

    @Override
    public void visitDisjunction(DisjunctionTree tree) {
      if (containsIntersections(tree.getAlternatives())) {
        addBacktracking(isReluctant ? BacktrackingType.ALWAYS_EXPONENTIAL : BacktrackingType.LINEAR_WHEN_OPTIMIZED);
      } else {
        super.visitDisjunction(tree);
      }
    }

    @Override
    public void visitBackReference(BackReferenceTree tree) {
      regexContainsBackReference = true;
    }

    boolean containsIntersections(List<? extends AutomatonState> alternatives) {
      for (int i = 0; i < alternatives.size() - 1; i++) {
        AutomatonState state1 = alternatives.get(i);
        for (int j = i + 1; j < alternatives.size(); j++) {
          AutomatonState state2 = alternatives.get(j);
          SubAutomaton auto1 = new SubAutomaton(state1, endOfLoop, false);
          SubAutomaton auto2 = new SubAutomaton(state2, endOfLoop, false);
          if (intersectionChecker.check(auto1, auto2)) {
            return true;
          }
        }
      }
      return false;
    }
  }

}
