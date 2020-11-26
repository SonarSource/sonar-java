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
package org.sonar.java.checks.regex;

import java.util.Collections;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.RegexTreeHelper;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.AutomatonState;
import org.sonar.java.regex.ast.Quantifier;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.java.regex.ast.RepetitionTree;
import org.sonar.java.regex.ast.StartState;

@Rule(key = "S6019")
public class ReluctantQuantifierWithEmptyContinuationCheck extends AbstractRegexCheckTrackingMatchType {

  private static final String PARTIAL_MATCH_MESSAGE = "Fix this reluctant quantifier that will only ever match the empty string.";
  private static final String FULL_MATCH_MESSAGE = "Remove the '?' from this unnecessarily reluctant quantifier.";

  @Override
  protected void checkRegex(RegexParseResult regex, MatchType matchType) {
    message(matchType).ifPresent(message ->
      new ReluctantQuantifierWithEmptyContinuationFinder(message).visit(regex)
    );
  }

  private static Optional<String> message(MatchType matchType) {
    switch (matchType) {
      case PARTIAL:
        return Optional.of(PARTIAL_MATCH_MESSAGE);
      case FULL:
        return Optional.of(FULL_MATCH_MESSAGE);
      default:
        return Optional.empty();
    }
  }

  private class ReluctantQuantifierWithEmptyContinuationFinder extends RegexBaseVisitor {
    private final String message;
    private AutomatonState endState;

    public ReluctantQuantifierWithEmptyContinuationFinder(String message) {
      this.message = message;
    }

    @Override
    protected void before(RegexParseResult regexParseResult) {
      endState = regexParseResult.getFinalState();
    }

    @Override
    public void visitRepetition(RepetitionTree tree) {
      super.visitRepetition(tree);
      if (tree.getQuantifier().getModifier() == Quantifier.Modifier.RELUCTANT
        && RegexTreeHelper.canReachWithoutConsumingInput(new StartState(tree.continuation(), tree.activeFlags()), endState)) {
        if (RegexTreeHelper.isAnchoredAtEnd(tree.continuation())) {
          reportIssue(tree, FULL_MATCH_MESSAGE, null, Collections.emptyList());
        } else {
          reportIssue(tree, message, null, Collections.emptyList());
        }
      }
    }
  }

}
