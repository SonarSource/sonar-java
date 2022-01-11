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
import org.sonar.java.regex.RegexCheck;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.AutomatonState;
import org.sonarsource.analyzer.commons.regex.ast.FinalState;
import org.sonarsource.analyzer.commons.regex.ast.Quantifier;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.ast.RegexSyntaxElement;
import org.sonarsource.analyzer.commons.regex.ast.RepetitionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;

@Rule(key = "S5994")
public class PossessiveQuantifierContinuationCheck extends AbstractRegexCheck {

  private static final String MESSAGE = "Change this impossible to match sub-pattern that conflicts with the previous possessive quantifier.";

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    new Visitor(regexForLiterals.getFinalState()).visit(regexForLiterals);
  }

  private class Visitor extends RegexBaseVisitor {

    private final FinalState finalState;

    public Visitor(FinalState finalState) {
      this.finalState = finalState;
    }

    @Override
    public void visitRepetition(RepetitionTree repetitionTree) {
      AutomatonState continuation = repetitionTree.continuation();
      while(continuation != null && !(continuation instanceof RegexSyntaxElement)) {
        continuation = continuation.continuation();
      }
      if (continuation != null && doesRepetitionContinuationAlwaysFail(repetitionTree)) {
        reportIssue((RegexSyntaxElement) continuation, MESSAGE, null,
            Collections.singletonList(new RegexCheck.RegexIssueLocation(repetitionTree, "Previous possessive repetition")));
      }
      super.visitRepetition(repetitionTree);
    }

    private boolean doesRepetitionContinuationAlwaysFail(RepetitionTree repetitionTree) {
      Quantifier quantifier = repetitionTree.getQuantifier();
      if (!quantifier.isOpenEnded() || quantifier.getModifier() != Quantifier.Modifier.POSSESSIVE) {
        return false;
      }
      SubAutomaton potentialSuperset = new SubAutomaton(repetitionTree.getElement(), repetitionTree.continuation(), false);
      SubAutomaton potentialSubset = new SubAutomaton(repetitionTree.continuation(), finalState, true);
      return RegexTreeHelper.supersetOf(potentialSuperset, potentialSubset, false);
    }
  }

}
