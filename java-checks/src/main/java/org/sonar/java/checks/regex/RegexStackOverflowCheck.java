/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.AutomatonState;
import org.sonarsource.analyzer.commons.regex.ast.BackReferenceTree;
import org.sonarsource.analyzer.commons.regex.ast.CapturingGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterTree;
import org.sonarsource.analyzer.commons.regex.ast.DisjunctionTree;
import org.sonarsource.analyzer.commons.regex.ast.EndOfRepetitionState;
import org.sonarsource.analyzer.commons.regex.ast.GroupTree;
import org.sonarsource.analyzer.commons.regex.ast.Quantifier;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.RepetitionTree;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;
import org.sonarsource.analyzer.commons.regex.ast.StartState;
import org.sonar.plugins.java.api.tree.ExpressionTree;

@Rule(key = "S5998")
public class RegexStackOverflowCheck extends AbstractRegexCheck {

  private static final String MESSAGE = "Refactor this repetition that can lead to a stack overflow for large inputs.";

  private static final String SECONDARY_MESSAGE = "Refactor this repetition";

  private static final double DEFAULT_MAX_STACK_CONSUMPTION_FACTOR = 5;

  @RuleProperty(
    key = "maxStackConsumptionFactor",
    description = "An indicator approximately proportional to how quickly the stack grows relative to the input size. An " +
      "issue will be reported if the value for a regex exceeds the maximum set here. Setting this to 0 will cause an issue " +
      "to be reported for all regular expressions with non-constant stack consumption.",
    defaultValue = "" + DEFAULT_MAX_STACK_CONSUMPTION_FACTOR)
  private double maxStackConsumptionFactor = DEFAULT_MAX_STACK_CONSUMPTION_FACTOR;

  public void setMaxStackConsumptionFactor(int max) {
    this.maxStackConsumptionFactor = max;
  }

  @Override
  public void checkRegex(RegexParseResult parseResult, ExpressionTree methodInvocationOrAnnotation) {
    new StackOverflowFinder().visit(parseResult);
  }

  private static class PathInfo {
    int numberOfConsumedCharacters;
    int recursionDepth;

    PathInfo(int numberOfConsumedCharacters, int recursionDepth) {
      this.numberOfConsumedCharacters = numberOfConsumedCharacters;
      this.recursionDepth = recursionDepth;
    }

    PathInfo add(PathInfo other) {
      numberOfConsumedCharacters += other.numberOfConsumedCharacters;
      recursionDepth += other.recursionDepth;
      return this;
    }

    PathInfo multiply(int factor) {
      numberOfConsumedCharacters *= factor;
      recursionDepth *= factor;
      return this;
    }

    double stackConsumptionFactor() {
      return (double) recursionDepth*2 / numberOfConsumedCharacters;
    }
  }

  private class StackOverflowFinder extends RegexBaseVisitor {

    private final Map<CapturingGroupTree, Integer> consumedCharactersByCapturingGroupCache = new HashMap<>();
    private final List<RegexTree> offendingTrees = new ArrayList<>();

    @Override
    public void visitRepetition(RepetitionTree tree) {
      if (!isPossessive(tree) && tree.getQuantifier().isOpenEnded()) {
        if (containsBacktrackableBranch(tree.getElement())
          && stackConsumption(new StartState(tree.getElement(), tree.activeFlags()), tree.continuation()) > maxStackConsumptionFactor) {
          offendingTrees.add(tree);
        }
      } else {
        // Only visit the children if this isn't the kind of repetition we check
        // Otherwise, if the parent doesn't overflow the stack, neither will its children, and if it does overflow
        // it, there's no point in reporting additional issues for the children
        super.visitRepetition(tree);
      }
    }

    @Override
    protected void after(RegexParseResult regexParseResult) {
      if (!offendingTrees.isEmpty()) {
        List<RegexIssueLocation> secondaries = offendingTrees.stream()
          .skip(1)
          .map(tree -> new RegexIssueLocation(tree, SECONDARY_MESSAGE))
          .toList();
        reportIssue(offendingTrees.get(0), MESSAGE, null, secondaries);
      }
    }

    private boolean isPossessive(RepetitionTree tree) {
      return tree.getQuantifier().getModifier() == Quantifier.Modifier.POSSESSIVE;
    }

    private boolean containsBacktrackableBranch(@Nullable RegexTree tree) {
      if (tree == null) {
        return false;
      }
      switch (tree.kind()) {
        case DISJUNCTION:
          return true;
        case REPETITION:
          RepetitionTree repetition = (RepetitionTree) tree;
          return !repetition.getQuantifier().isFixed() || containsBacktrackableBranch(repetition.getElement());
        case CAPTURING_GROUP:
        case NON_CAPTURING_GROUP:
          return containsBacktrackableBranch(((GroupTree) tree).getElement());
        case SEQUENCE:
          for (RegexTree child : ((SequenceTree) tree).getItems()) {
            if (containsBacktrackableBranch(child)) {
              return true;
            }
          }
          return false;
        default:
          return false;
      }
    }

    private double stackConsumption(AutomatonState start, AutomatonState stop) {
      Comparator<PathInfo> worstPathComparator = Comparator.comparingDouble(PathInfo::stackConsumptionFactor).reversed();
      PathInfo path = shortestPath(start, stop, worstPathComparator);
      return path.stackConsumptionFactor();
    }

    /**
     * We assume that all paths eventually lead to `end`, i.e. `end` must be the end of a construct, such as the end of
     * the regex or the continuation of some sub-expression and `start` must be within that construct.
     */
    private PathInfo shortestPath(AutomatonState start, AutomatonState end, Comparator<PathInfo> shortestPathComparator) {
      if (start == end) {
        return new PathInfo(0, 0);
      }
      AutomatonState next = start.continuation();
      if (start instanceof RegexTree) {
        if (start instanceof CharacterTree && next instanceof CharacterTree) {
          // Consecutive characters don't create an extra recursion, so we skip the character edge between them and use
          // a 1,0 edge instead.
          return new PathInfo(1, 0).add(shortestPath(next, end, shortestPathComparator));
        }
        PathInfo path = shortestInnerPath((RegexTree) start, shortestPathComparator);
        path.add(edgeCost(next));
        path.add(shortestPath(next, end, shortestPathComparator));
        return path;
      }
      return edgeCost(next).add(shortestPath(next, end, shortestPathComparator));
    }

    private boolean ignoredNode(AutomatonState state) {
      // Java's regex implementation does not have an equivalent of these nodes, so we consider them zero cost
      return state instanceof SequenceTree || state instanceof EndOfRepetitionState;
    }

    private PathInfo edgeCost(AutomatonState state) {
      switch (state.incomingTransitionType()) {
        case EPSILON:
          return new PathInfo(0, ignoredNode(state) ? 0 : 1);
        case CHARACTER:
          return new PathInfo(1, 1);
        case BACK_REFERENCE:
          return backReferenceCost((BackReferenceTree) state);
        default:
          throw new IllegalStateException("Lookaround should have been skipped");
      }
    }

    private PathInfo backReferenceCost(BackReferenceTree backReference) {
      Integer consumedCharacters = 0;
      CapturingGroupTree group = backReference.group();
      if (group != null) {
        consumedCharacters = consumedCharactersByCapturingGroupCache.get(group);
        if (consumedCharacters == null) {
          // prevent reentrancy while we are computing the value
          consumedCharactersByCapturingGroupCache.put(group, 1);
          Comparator<PathInfo> pathLengthComparator = Comparator.comparingInt(p -> p.numberOfConsumedCharacters);
          RegexTree element = group.getElement();
          PathInfo pathInfo = edgeCost(element).add(shortestPath(element, element.continuation(), pathLengthComparator));
          consumedCharacters = pathInfo.numberOfConsumedCharacters;
          consumedCharactersByCapturingGroupCache.put(group, consumedCharacters);
        }
      }
      // Referencing a capturing group does not increase the stack size as parsing the group would just retrieve a saved string
      return new PathInfo(consumedCharacters, 0);
    }

    /**
     * Find the shortest path from the beginning to the end of a nested construct, such as a group, repetition or
     * disjunction, and append it to the given path
     */
    private PathInfo shortestInnerPath(RegexTree tree, Comparator<PathInfo> shortestPathComparator) {
      switch (tree.kind()) {
        case REPETITION:
          RepetitionTree repetition = (RepetitionTree) tree;
          if (repetition.getQuantifier().getMinimumRepetitions() == 0) {
            return new PathInfo(0, 0);
          }
          int repetitions = repetition.getQuantifier().getMinimumRepetitions();
          RegexTree element = repetition.getElement();
          return edgeCost(element).add(shortestPath(element, repetition.continuation(), shortestPathComparator)).multiply(repetitions);
        case DISJUNCTION:
          return ((DisjunctionTree) tree).getAlternatives().stream()
            .map(alt -> edgeCost(alt).add(shortestInnerPath(alt, shortestPathComparator)))
            .min(shortestPathComparator)
            .get();
        case SEQUENCE:
          List<RegexTree> items = ((SequenceTree) tree).getItems();
          if (items.isEmpty()) {
            return new PathInfo(0, 0);
          }
          RegexTree first = items.get(0);
          return edgeCost(first).add(shortestPath(first, tree.continuation(), shortestPathComparator));
        case NON_CAPTURING_GROUP:
        case CAPTURING_GROUP:
          return Optional.ofNullable(((GroupTree) tree).getElement())
            .map(groupElement -> edgeCost(groupElement).add(shortestInnerPath(groupElement, shortestPathComparator)))
            .orElse(new PathInfo(0, 0));
        default:
          return new PathInfo(0, 0);
      }
    }

  }

}
