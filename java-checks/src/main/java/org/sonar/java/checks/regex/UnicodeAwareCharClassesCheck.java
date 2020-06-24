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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.regex.RegexCheck;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.CharacterRangeTree;
import org.sonar.java.regex.ast.EscapedCharacterClassTree;
import org.sonar.java.regex.ast.NonCapturingGroupTree;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5867")
public class UnicodeAwareCharClassesCheck extends AbstractRegexCheck {

  private static final Set<Character> unicodeAwareClassesWithFlag = ImmutableSet.of('s', 'S', 'w', 'W');
  private static final Set<String> unicodeAwarePropertiesWithFlag = ImmutableSet.of(
    "Lower", "Upper", "Alpha", "Alnum", "Punct", "Graph", "Print", "Blank", "Space");

  private static final Map<Character, Character> unicodeUnawareCharacterRanges = ImmutableMap.of(
    'a', 'z', 'A', 'Z');

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    new UnicodeUnawareCharClassFinder(mit).visit(regexForLiterals);
  }

  private class UnicodeUnawareCharClassFinder extends RegexBaseVisitor {

    private final MethodInvocationTree mit;
    private final List<RegexTree> unicodeUnawareRange = new ArrayList<>();
    private final List<RegexTree> unicodeAwareWithFlag = new ArrayList<>();
    private boolean containsUnicodeCharacterFlag = false;

    public UnicodeUnawareCharClassFinder(MethodInvocationTree mit) {
      this.mit = mit;
    }

    @Override
    protected void before(RegexParseResult regexParseResult) {
      containsUnicodeCharacterFlag |= regexParseResult.getInitialFlags().contains(Pattern.UNICODE_CHARACTER_CLASS);
    }

    @Override
    protected void after(RegexParseResult regexParseResult) {
      unicodeUnawareRange.forEach(unawareRange -> reportIssue(unawareRange,
        "Replace this character range with a Unicode-aware character class.", null, Collections.emptyList()));

      if (!unicodeAwareWithFlag.isEmpty() && !containsUnicodeCharacterFlag) {
        List<RegexCheck.RegexIssueLocation> secondaries = unicodeAwareWithFlag.stream()
          .map(tree -> new RegexIssueLocation(tree, "Predefined/POSIX character class"))
          .collect(Collectors.toList());
        reportIssue(ExpressionUtils.methodName(mit),
          "Enable the \"UNICODE_CHARACTER_CLASS\" flag or use a Unicode-aware alternative.", null, secondaries);
      }
    }

    @Override
    public void visitCharacterRange(CharacterRangeTree tree) {
      Character expectedUpperBoundChar = unicodeUnawareCharacterRanges.get(tree.getLowerBound().getCharacter());
      if (expectedUpperBoundChar != null && expectedUpperBoundChar == tree.getUpperBound().getCharacter()) {
        unicodeUnawareRange.add(tree);
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
    protected void doVisitNonCapturingGroup(NonCapturingGroupTree tree) {
      containsUnicodeCharacterFlag |= flagActive(Pattern.UNICODE_CHARACTER_CLASS);
      super.doVisitNonCapturingGroup(tree);
    }
  }
}
