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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.CharacterClassTree;
import org.sonar.java.regex.ast.PlainCharacterTree;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.java.regex.ast.SequenceTree;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

import static org.sonar.java.checks.helpers.RegexTreeHelper.getGraphemeInList;

@Rule(key = "S5854")
public class CanonEqFlagInRegexCheck extends AbstractRegexCheck {

  protected static final MethodMatchers STRING_MATCHES = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("matches")
    .addParametersMatcher(JAVA_LANG_STRING)
    .build();

  protected static final MethodMatchers STRING_REPLACE_ALL = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("replaceAll")
    .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING)
    .build();

  protected static final MethodMatchers STRING_REPLACE_FIRST = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("replaceFirst")
    .addParametersMatcher(JAVA_LANG_STRING, JAVA_LANG_STRING)
    .build();

  protected static final MethodMatchers PATTERN_MATCHES = MethodMatchers.create()
    .ofTypes("java.util.regex.Pattern")
    .names("matches")
    .addParametersMatcher(JAVA_LANG_STRING, "java.lang.CharSequence")
    .build();

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    if (regexForLiterals.getInitialFlags().contains(Pattern.CANON_EQ)) {
      return;
    }
    PlainCharacterVisitor visitor = new PlainCharacterVisitor();
    visitor.visit(regexForLiterals);

    if (!visitor.subjectToNormalization.isEmpty()) {
      String endOfMessage;
      if (STRING_MATCHES.matches(mit) || PATTERN_MATCHES.matches(mit)) {
        endOfMessage = "\"Pattern.compile(regex, CANON_EQ).matcher(input).matches()\"";
      } else if (STRING_REPLACE_ALL.matches(mit)) {
        endOfMessage = "\"Pattern.compile(pattern, CANON_EQ).matcher(input).replaceAll(replacement)\"";
      } else if (STRING_REPLACE_FIRST.matches(mit)) {
        endOfMessage = "\"Pattern.compile(pattern, CANON_EQ).matcher(input).replaceFirst(replacement)\"";
      } else {
        endOfMessage = "this pattern";
      }

      reportIssue(regexForLiterals.getResult(), String.format("Use the CANON_EQ flag with %s.", endOfMessage), null, visitor.subjectToNormalization);
    }
  }

  private static class PlainCharacterVisitor extends RegexBaseVisitor {

    private final List<RegexIssueLocation> subjectToNormalization = new ArrayList<>();

    @Override
    public void visitSequence(SequenceTree tree) {
      subjectToNormalization.addAll(getGraphemeInList(tree.getItems()));
      super.visitSequence(tree);
    }

    @Override
    public void visitCharacterClass(CharacterClassTree tree) {
      // Stop visit in classes, S5868 will report an eventual issue for it.
    }

    @Override
    public void visitPlainCharacter(PlainCharacterTree tree) {
      String str = String.valueOf(tree.getCharacter());
      if (isSubjectToNormalization(str)) {
        subjectToNormalization.add(new RegexIssueLocation(tree, ""));
      }
    }

    private static boolean isSubjectToNormalization(String str) {
      return !Normalizer.isNormalized(str, Normalizer.Form.NFD);
    }

  }

}
