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
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.CharacterClassElementTree;
import org.sonar.java.regex.ast.CharacterClassUnionTree;
import org.sonar.java.regex.ast.CharacterRangeTree;
import org.sonar.java.regex.ast.CharacterTree;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5869")
public class DuplicatesInCharacterClassCheck extends AbstractRegexCheck {

  private static final String MESSAGE = "Remove duplicates in this character class.";

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    new DuplicateFinder().visit(regexForLiterals);
  }

  private class DuplicateFinder extends RegexBaseVisitor {

    @Override
    public void visitCharacterClassUnion(CharacterClassUnionTree tree) {
      List<CharacterClassElementTree> duplicates = new ArrayList<>();
      TreeMap<Integer, Boolean> inCharacterClass = new TreeMap<>();
      for (CharacterClassElementTree element : tree.getCharacterClasses()) {
        if (element.is(CharacterClassElementTree.Kind.PLAIN_CHARACTER, CharacterClassElementTree.Kind.UNICODE_CODE_POINT)) {
          int ch = ((CharacterTree) element).codePointOrUnit();
          processRange(duplicates, inCharacterClass, ch, ch, element);
        } else if (element.is(CharacterClassElementTree.Kind.CHARACTER_RANGE)) {
          CharacterRangeTree range = (CharacterRangeTree) element;
          int lower = range.getLowerBound().codePointOrUnit();
          int upper = range.getUpperBound().codePointOrUnit();
          processRange(duplicates, inCharacterClass, lower, upper, range);
        }
      }
      if (!duplicates.isEmpty()) {
        List<RegexIssueLocation> secondaries = duplicates.stream()
          .skip(1)
          .map(duplicate -> new RegexIssueLocation(duplicate, "Additional duplicate"))
          .collect(Collectors.toList());
        reportIssue(duplicates.get(0), MESSAGE, null, secondaries);
      }
      super.visitCharacterClassUnion(tree);
    }

    void processRange(List<CharacterClassElementTree> duplicates, TreeMap<Integer, Boolean> inCharacterClass, int from, int to, CharacterClassElementTree tree) {
      if (to < from) {
        return;
      }
      from = caseFold(from);
      to = caseFold(to);
      if (containsOverlap(inCharacterClass, from, to)) {
        duplicates.add(tree);
      }
      inCharacterClass.put(from, true);
      for (Map.Entry<Integer, Boolean> entry : inCharacterClass.subMap(from, true, to, true).entrySet()) {
        entry.setValue(true);
      }
      int next = to + 1;
      if (!inCharacterClass.containsKey(next)) {
        inCharacterClass.put(next, false);
      }
    }

    boolean containsOverlap(TreeMap<Integer, Boolean> inCharacterClass, int from, int to) {
      Map.Entry<Integer, Boolean> fromEntry = inCharacterClass.floorEntry(from);
      Map.Entry<Integer, Boolean> toEntry = inCharacterClass.floorEntry(to);
      return (fromEntry != null && fromEntry.getValue()) || !Objects.equals(fromEntry, toEntry);
    }

    int caseFold(int ch) {
      if (flagActive(Pattern.CASE_INSENSITIVE) && (flagActive(Pattern.UNICODE_CASE) || ('A' <= ch && ch <= 'Z'))) {
        return Character.toLowerCase(Character.toUpperCase(ch));
      }
      return ch;
    }

  }

}
