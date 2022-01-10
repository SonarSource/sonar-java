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
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.DisjunctionTree;
import org.sonarsource.analyzer.commons.regex.ast.GroupTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.RepetitionTree;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;

@Rule(key = "S5842")
public class EmptyStringRepetitionCheck extends AbstractRegexCheck {

  private static final String MESSAGE = "Rework this part of the regex to not match the empty string.";

  @Override
  public void checkRegex(RegexParseResult regex, ExpressionTree methodInvocationOrAnnotation) {
    new Visitor().visit(regex);
  }

  private class Visitor extends RegexBaseVisitor {

    @Override
    public void visitRepetition(RepetitionTree tree) {
      RegexTree element = tree.getElement();
      if (matchEmptyString(element)) {
        reportIssue(element, MESSAGE, null, Collections.emptyList());
      }
    }

    private boolean matchEmptyString(RegexTree element) {
      switch (element.kind()) {
        case SEQUENCE:
          return ((SequenceTree) element).getItems().stream().allMatch(this::matchEmptyString);
        case DISJUNCTION:
          return ((DisjunctionTree) element).getAlternatives().stream().anyMatch(this::matchEmptyString);
        case REPETITION:
          return ((RepetitionTree) element).getQuantifier().getMinimumRepetitions() == 0;
        case LOOK_AROUND:
        case BOUNDARY:
          return true;
        default:
          if (element instanceof GroupTree) {
            RegexTree nestedElement = ((GroupTree) element).getElement();
            return nestedElement == null || matchEmptyString(nestedElement);
          }
          return false;
      }
    }

  }

}
