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
import java.util.Deque;
import java.util.LinkedList;
import org.sonar.check.Rule;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.DisjunctionTree;
import org.sonar.java.regex.ast.Quantifier;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.java.regex.ast.RepetitionTree;
import org.sonar.java.regex.ast.SequenceTree;
import org.sonar.java.regex.ast.SimpleQuantifier;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5842")
public class EmptyStringRepetitionCheck extends AbstractRegexCheck {

  @Override
  public void checkRegex(RegexParseResult regex, MethodInvocationTree mit) {
    new Visitor().visit(regex);
  }

  private class Visitor extends RegexBaseVisitor {

    Deque<SimpleQuantifier> quantifiers = new LinkedList<>();

    @Override
    public void visitSequence(SequenceTree tree) {
      if (isEmptySequence(tree)) {
        SimpleQuantifier quantifier = quantifiers.peek();
        if (quantifier != null && SimpleQuantifier.Kind.STAR.equals(quantifier.getKind())) {
          reportIssue(tree, "Remove this part of the regex.", null, Collections.emptyList());
        }
      }
      super.visitSequence(tree);
    }

    private boolean isEmptySequence(SequenceTree tree) {
      return tree.getItems().isEmpty();
    }

    @Override
    public void visitDisjunction(DisjunctionTree tree) {
      // TODO All alternatives should be repetition or at least one should be matching the empty String
      super.visitDisjunction(tree);
    }

    @Override
    public void visitRepetition(RepetitionTree tree) {
      Quantifier quantifier = tree.getQuantifier();
      if (quantifier instanceof SimpleQuantifier) {
        quantifiers.push((SimpleQuantifier) quantifier);
      }
      super.visitRepetition(tree);
      if (quantifier instanceof SimpleQuantifier) {
        quantifiers.pop();
      }
    }

  }

}
