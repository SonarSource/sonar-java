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
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.AtomicGroupTree;
import org.sonar.java.regex.ast.Quantifier;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.java.regex.ast.RepetitionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5852")
public class RedosCheck extends AbstractRegexCheck {

  private static final String MESSAGE = "Make sure the regex used in this method call cannot lead to denial of service.";

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    NestedRepetitionsFinder visitor = new NestedRepetitionsFinder();
    visitor.visit(regexForLiterals);
    if (visitor.containsOffendingRepetitions) {
      reportIssue(ExpressionUtils.methodName(mit), MESSAGE, null, Collections.emptyList());
    }
  }

  private static class NestedRepetitionsFinder extends RegexBaseVisitor {

    private boolean isInsideRepetition = false;
    private boolean containsOffendingRepetitions = false;

    @Override
    public void visitRepetition(RepetitionTree tree) {
      boolean isPotentiallyProblematic = tree.getQuantifier().isOpenEnded()
        && tree.getQuantifier().getModifier() != Quantifier.Modifier.POSSESSIVE;
      if (isPotentiallyProblematic) {
        if (isInsideRepetition) {
          containsOffendingRepetitions = true;
          return;
        }
        isInsideRepetition = true;
      }
      super.visitRepetition(tree);
      if (isPotentiallyProblematic) {
        isInsideRepetition = false;
      }
    }

    @Override
    public void visitAtomicGroup(AtomicGroupTree tree) {
      // Nested repetitions are unproblematic if the inner repetition is inside an atomic group, but not if both
      // repetitions are inside the atomic group. So we enter the group with a clean slate and then restore the
      // state afterwards.
      boolean oldIsInsideRepetition = isInsideRepetition;
      isInsideRepetition = false;
      super.visitAtomicGroup(tree);
      isInsideRepetition = oldIsInsideRepetition;
    }
  }

}
