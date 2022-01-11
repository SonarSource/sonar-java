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

import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassTree;
import org.sonarsource.analyzer.commons.regex.ast.CharacterClassUnionTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;

import static org.sonar.java.checks.helpers.RegexTreeHelper.getGraphemeInList;

@Rule(key = "S5868")
public class GraphemeClustersInClassesCheck extends AbstractRegexCheck {

  private static final String MESSAGE = "Extract %d Grapheme Cluster(s) from this character class.";

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    new GraphemeInClassVisitor().visit(regexForLiterals);
  }

  private class GraphemeInClassVisitor extends RegexBaseVisitor {

    private final List<RegexIssueLocation> graphemeClusters = new ArrayList<>();

    @Override
    public void visitCharacterClass(CharacterClassTree tree) {
      super.visitCharacterClass(tree);
      if (!graphemeClusters.isEmpty()) {
        reportIssue(tree, String.format(MESSAGE, graphemeClusters.size()), null, graphemeClusters);
      }
      graphemeClusters.clear();
    }

    @Override
    public void visitCharacterClassUnion(CharacterClassUnionTree tree) {
      graphemeClusters.addAll(getGraphemeInList(tree.getCharacterClasses()));
      super.visitCharacterClassUnion(tree);
    }

  }

}
