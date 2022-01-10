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

import java.util.Collections;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.RegexTreeHelper;
import org.sonar.java.checks.helpers.SubAutomaton;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.FinalState;
import org.sonarsource.analyzer.commons.regex.ast.LookAroundTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;

@Rule(key = "S6002")
public class RegexLookaheadCheck extends AbstractRegexCheckTrackingMatchType {

  private static final String MESSAGE = "Remove or fix this lookahead assertion that can never be true.";

  @Override
  protected void checkRegex(RegexParseResult regex, ExpressionTree methodInvocationOrAnnotation, MatchType matchType) {
    new LookaheadFinder(matchType, regex.getFinalState()).visit(regex);
  }

  private class LookaheadFinder extends RegexBaseVisitor {

    private final MatchType matchType;

    private final FinalState finalState;

    public LookaheadFinder(MatchType matchType, FinalState finalState) {
      this.matchType = matchType;
      this.finalState = finalState;
    }

    @Override
    public void visitLookAround(LookAroundTree tree) {
      if (tree.getDirection() == LookAroundTree.Direction.AHEAD && doesLookaheadContinuationAlwaysFail(tree)) {
        reportIssue(tree, MESSAGE, null, Collections.emptyList());
      }
      super.visitLookAround(tree);
    }

    private boolean doesLookaheadContinuationAlwaysFail(LookAroundTree lookAround) {
      RegexTree lookAroundElement = lookAround.getElement();
      boolean canLookAroundBeAPrefix = matchType != MatchType.FULL;
      SubAutomaton lookAroundSubAutomaton = new SubAutomaton(lookAroundElement, lookAroundElement.continuation(), canLookAroundBeAPrefix);
      SubAutomaton continuationSubAutomaton = new SubAutomaton(lookAround.continuation(), finalState, true);

      if (lookAround.getPolarity() == LookAroundTree.Polarity.NEGATIVE) {
        return RegexTreeHelper.supersetOf(lookAroundSubAutomaton, continuationSubAutomaton, false);
      }
      return !RegexTreeHelper.intersects(lookAroundSubAutomaton, continuationSubAutomaton, true);
    }

  }

}
