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
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.SimplifiedRegexCharacterClass;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.CharacterClassElementTree;
import org.sonar.java.regex.ast.CharacterClassUnionTree;
import org.sonar.java.regex.ast.RegexBaseVisitor;
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
      SimplifiedRegexCharacterClass characterClass = new SimplifiedRegexCharacterClass();
      for (CharacterClassElementTree element : tree.getCharacterClasses()) {
        if (characterClass.intersects(new SimplifiedRegexCharacterClass(element, getActiveFlagSet()), false)) {
          duplicates.add(element);
        }
        characterClass.add(element, getActiveFlagSet());
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

  }

}
