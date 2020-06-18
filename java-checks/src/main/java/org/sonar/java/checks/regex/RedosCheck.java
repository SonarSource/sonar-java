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

import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.AtomicGroupTree;
import org.sonar.java.regex.ast.Quantifier;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.java.regex.ast.RepetitionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5852")
public class RedosCheck extends AbstractRegexCheck {

  private static final String MESSAGE = "Make sure this regex cannot lead to denial of service here.";

  private static final String SECONDARY_MESSAGE = "Nested non-possessive unbounded repetition";

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    new NestedRepetitionsFinder().visit(regexForLiterals);
  }

  private class NestedRepetitionsFinder extends RegexBaseVisitor {

    private boolean isInsideRepetition = false;

    private final List<RegexIssueLocation> offendingRepetitions = new ArrayList<>();

    @Override
    public void visitRepetition(RepetitionTree tree) {
      boolean isPotentiallyProblematic = tree.getQuantifier().isOpenEnded()
        && tree.getQuantifier().getModifier() != Quantifier.Modifier.POSSESSIVE;
      if (isPotentiallyProblematic) {
        if (isInsideRepetition) {
          offendingRepetitions.add(new RegexIssueLocation(tree, SECONDARY_MESSAGE));
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

    @Override
    protected void after(RegexParseResult regexParseResult) {
      if (!offendingRepetitions.isEmpty()) {
        reportIssue(regexParseResult.getResult(), MESSAGE, null, offendingRepetitions);
      }
    }
  }

}
