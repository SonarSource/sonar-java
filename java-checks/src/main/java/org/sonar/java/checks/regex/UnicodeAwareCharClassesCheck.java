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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.collections.MapBuilder;
import org.sonarsource.analyzer.commons.collections.SetUtils;
import org.sonar.java.regex.RegexCheck;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.CharacterRangeTree;
import org.sonarsource.analyzer.commons.regex.ast.EscapedCharacterClassTree;
import org.sonarsource.analyzer.commons.regex.ast.NonCapturingGroupTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5867")
public class UnicodeAwareCharClassesCheck extends AbstractRegexCheck {

  private static final List<Character> unicodeAwareClassesWithFlag = Arrays.asList('s', 'S', 'w', 'W');
  private static final Set<String> unicodeAwarePropertiesWithFlag = SetUtils.immutableSetOf(
    "Lower", "Upper", "Alpha", "Alnum", "Punct", "Graph", "Print", "Blank", "Space");

  private static final Map<Character, Character> unicodeUnawareCharacterRanges = MapBuilder.<Character, Character>newMap()
    .put('a', 'z')
    .put('A', 'Z')
    .build();

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    new UnicodeUnawareCharClassFinder(methodInvocationOrAnnotation).visit(regexForLiterals);
  }

  private class UnicodeUnawareCharClassFinder extends RegexBaseVisitor {

    private final ExpressionTree methodInvocationOrAnnotation;
    private final List<CharacterRangeTree> unicodeUnawareRanges = new ArrayList<>();
    private final List<RegexTree> unicodeAwareWithFlag = new ArrayList<>();
    private boolean containsUnicodeCharacterFlag = false;

    public UnicodeUnawareCharClassFinder(ExpressionTree methodInvocationOrAnnotation) {
      this.methodInvocationOrAnnotation = methodInvocationOrAnnotation;
    }

    @Override
    protected void before(RegexParseResult regexParseResult) {
      containsUnicodeCharacterFlag |= regexParseResult.getInitialFlags().contains(Pattern.UNICODE_CHARACTER_CLASS);
    }

    @Override
    protected void after(RegexParseResult regexParseResult) {
      int unicodeUnawareRangeSize = unicodeUnawareRanges.size();
      if (unicodeUnawareRangeSize == 1) {
        reportIssue(unicodeUnawareRanges.get(0),
          "Replace this character range with a Unicode-aware character class.", null, Collections.emptyList());
      } else if (unicodeUnawareRangeSize > 1) {
        List<RegexCheck.RegexIssueLocation> secondaries = unicodeUnawareRanges.stream()
          .map(tree -> new RegexIssueLocation(tree, "Character range"))
          .collect(Collectors.toList());

        reportIssue(regexParseResult.getResult(),
          "Replace these character ranges with Unicode-aware character classes.", null, secondaries);
      }


      if (!unicodeAwareWithFlag.isEmpty() && !containsUnicodeCharacterFlag) {
        List<RegexCheck.RegexIssueLocation> secondaries = unicodeAwareWithFlag.stream()
          .map(tree -> new RegexIssueLocation(tree, "Predefined/POSIX character class"))
          .collect(Collectors.toList());
        // Can not propose to use UNICODE_CHARACTER_CLASS for annotation because the matching flag
        // javax.validation.constraints.Pattern.Flag.UNICODE_CHARACTER_CLASS does not exist in javaee-api <= 8.0
        String flagName = methodInvocationOrAnnotation.is(Tree.Kind.ANNOTATION) ? "(?U)" : "UNICODE_CHARACTER_CLASS";
        reportIssue(methodOrAnnotationName(methodInvocationOrAnnotation),
          "Enable the \"" + flagName + "\" flag or use a Unicode-aware alternative.", null, secondaries);
      }
    }

    @Override
    public void visitCharacterRange(CharacterRangeTree tree) {
      int lowerBound = tree.getLowerBound().codePointOrUnit();
      if (lowerBound < 0xFFFF) {
        Character expectedUpperBoundChar = unicodeUnawareCharacterRanges.get((char) lowerBound);
        if (expectedUpperBoundChar != null && expectedUpperBoundChar == tree.getUpperBound().codePointOrUnit()) {
          unicodeUnawareRanges.add(tree);
        }
      }
    }

    @Override
    public void visitEscapedCharacterClass(EscapedCharacterClassTree tree) {
      String property = tree.property();
      if ((property != null && unicodeAwarePropertiesWithFlag.contains(property)) ||
        unicodeAwareClassesWithFlag.contains(tree.getType())) {

        unicodeAwareWithFlag.add(tree);
      }
    }

    @Override
    public void visitNonCapturingGroup(NonCapturingGroupTree tree) {
      containsUnicodeCharacterFlag |= tree.activeFlags().contains(Pattern.UNICODE_CHARACTER_CLASS);
      super.visitNonCapturingGroup(tree);
    }
  }
}
