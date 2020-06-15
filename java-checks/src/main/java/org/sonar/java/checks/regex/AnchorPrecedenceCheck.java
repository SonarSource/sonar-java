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
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.DisjunctionTree;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.java.regex.ast.SequenceTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5850")
public class AnchorPrecedenceCheck extends AbstractRegexCheck {

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    if (!regexForLiterals.hasSyntaxErrors()) {
      new Visitor().visit(regexForLiterals.getResult());
    }
  }

  private class Visitor extends RegexBaseVisitor {
    @Override
    public void visitDisjunction(DisjunctionTree tree) {
      RegexTree first = tree.getAlternatives().get(0);
      RegexTree last = tree.getAlternatives().get(tree.getAlternatives().size() - 1);
      if (isAnchoredAtBeginning(first) || isAnchoredAtEnd(last)) {
        reportIssue(tree, "Group the alternatives together to get the intended precedence.", null, Collections.emptyList());
      }
      super.visitDisjunction(tree);
    }

    private boolean isAnchoredAtBeginning(RegexTree tree) {
      return tree.is(RegexTree.Kind.SEQUENCE) && ((SequenceTree) tree).getItems().get(0).is(RegexTree.Kind.BOUNDARY);
    }

    private boolean isAnchoredAtEnd(RegexTree tree) {
      return tree.is(RegexTree.Kind.SEQUENCE) && last(((SequenceTree) tree).getItems()).is(RegexTree.Kind.BOUNDARY);
    }

    private <T> T last(List<T> items) {
      return items.get(items.size() - 1);
    }

  }

}
