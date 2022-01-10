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
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.AutomatonState;
import org.sonarsource.analyzer.commons.regex.ast.Quantifier;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.ast.RepetitionTree;
import org.sonarsource.analyzer.commons.regex.ast.StartState;
import org.sonar.plugins.java.api.tree.ExpressionTree;

@Rule(key = "S6019")
public class ReluctantQuantifierWithEmptyContinuationCheck extends AbstractRegexCheckTrackingMatchType {

  private static final String PARTIAL_MATCH_MESSAGE = "Fix this reluctant quantifier that will only ever match the empty string.";
  private static final String FULL_MATCH_MESSAGE = "Remove the '?' from this unnecessarily reluctant quantifier.";

  @Override
  protected void checkRegex(RegexParseResult regex, ExpressionTree methodInvocationOrAnnotation, MatchType matchType) {
    if (matchType == MatchType.PARTIAL || matchType == MatchType.FULL) {
      new ReluctantQuantifierWithEmptyContinuationFinder(matchType).visit(regex);
    }
  }

  private class ReluctantQuantifierWithEmptyContinuationFinder extends RegexBaseVisitor {
    private AutomatonState endState;
    private final MatchType matchType;

    public ReluctantQuantifierWithEmptyContinuationFinder(MatchType matchType) {
      this.matchType = matchType;
    }

    @Override
    protected void before(RegexParseResult regexParseResult) {
      endState = regexParseResult.getFinalState();
    }

    private boolean isAnchoredAtEnd(AutomatonState state) {
      return matchType == MatchType.FULL || RegexTreeHelper.isAnchoredAtEnd(state);
    }

    @Override
    public void visitRepetition(RepetitionTree tree) {
      super.visitRepetition(tree);
      if (tree.getQuantifier().getModifier() == Quantifier.Modifier.RELUCTANT) {
        if (isAnchoredAtEnd(tree.continuation())) {
          if (RegexTreeHelper.onlyMatchesEmptySuffix(tree.continuation())) {
            reportIssue(tree, FULL_MATCH_MESSAGE, null, Collections.emptyList());
          }
        } else if (RegexTreeHelper.canReachWithoutConsumingInput(new StartState(tree.continuation(), tree.activeFlags()), endState)) {
          reportIssue(tree, PARTIAL_MATCH_MESSAGE, null, Collections.emptyList());
        }
      }
    }
  }

}
