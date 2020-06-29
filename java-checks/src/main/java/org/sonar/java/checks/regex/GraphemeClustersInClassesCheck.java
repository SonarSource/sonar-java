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
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.CharacterClassTree;
import org.sonar.java.regex.ast.CharacterClassUnionTree;
import org.sonar.java.regex.ast.JavaCharacter;
import org.sonar.java.regex.ast.PlainCharacterTree;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.java.regex.ast.RegexSyntaxElement;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5868")
public class GraphemeClustersInClassesCheck extends AbstractRegexCheck {

  // M (Mark) is "a character intended to be combined with another character (e.g. accents, umlauts, enclosing boxes, etc.)."
  // See https://www.regular-expressions.info/unicode.html
  private static final Pattern MARK_PATTERN = Pattern.compile("\\p{M}");

  private static final String MESSAGE = "Extract %d Grapheme Cluster(s) from this character class.";

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
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
      RegexSyntaxElement startGrapheme = null;
      RegexSyntaxElement endGrapheme = null;
      for (RegexTree child : tree.getCharacterClasses()) {
        if (child.is(RegexTree.Kind.PLAIN_CHARACTER)) {
          JavaCharacter currentCharacter = ((PlainCharacterTree) child).getContents();
          if (!currentCharacter.isEscapedUnicode()) {
            if (!isMark(currentCharacter)) {
              addCurrentGrapheme(startGrapheme, endGrapheme);
              startGrapheme = child;
              endGrapheme = null;
            } else if (startGrapheme != null) {
              endGrapheme = child;
            }
            continue;
          }
        }
        addCurrentGrapheme(startGrapheme, endGrapheme);
        startGrapheme = null;
        endGrapheme = null;
      }
      addCurrentGrapheme(startGrapheme, endGrapheme);

      super.visitCharacterClassUnion(tree);
    }

    private void addCurrentGrapheme(@Nullable RegexSyntaxElement start, @Nullable RegexSyntaxElement end) {
      if (start != null && end != null) {
        graphemeClusters.add(new RegexIssueLocation(start, end, ""));
      }
    }

    private boolean isMark(JavaCharacter currentChar) {
      return MARK_PATTERN.matcher(String.valueOf(currentChar.getCharacter())).matches();
    }
  }

}
