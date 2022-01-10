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
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.CharacterTree;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.analyzer.commons.regex.ast.SourceCharacter;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S5866")
public class UnicodeCaseCheck extends AbstractRegexCheck {

  private static final String MESSAGE = "Also use %s to correctly handle non-ASCII letters.";

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    new Visitor(methodInvocationOrAnnotation).visit(regexForLiterals);
  }

  private class Visitor extends RegexBaseVisitor {

    final Set<SourceCharacter> problematicFlags = new HashSet<>();

    boolean problematicFlagSetOutsideOfRegex = false;

    final ExpressionTree methodInvocationOrAnnotation;

    Visitor(ExpressionTree methodInvocationOrAnnotation) {
      this.methodInvocationOrAnnotation = methodInvocationOrAnnotation;
    }

    @Override
    public void visitCharacter(CharacterTree tree) {
      if (isProblematic(tree.activeFlags(), tree.codePointOrUnit())) {
        SourceCharacter character = tree.activeFlags().getJavaCharacterForFlag(Pattern.CASE_INSENSITIVE);
        if (character == null) {
          problematicFlagSetOutsideOfRegex = true;
        } else {
          problematicFlags.add(character);
        }
      }
    }

    @Override
    protected void after(RegexParseResult regexParseResult) {
      if (problematicFlagSetOutsideOfRegex) {
        boolean isAnnotation = methodInvocationOrAnnotation.is(Tree.Kind.ANNOTATION);
        String flagName = isAnnotation ? "\"Flag.UNICODE_CASE\"" : "\"Pattern.UNICODE_CASE\"";
        getFlagsTree(methodInvocationOrAnnotation).ifPresent( flagsTree ->
          reportIssue(flagsTree, String.format(MESSAGE, flagName))
        );
      }
      for (SourceCharacter flag : problematicFlags) {
        reportIssue(flag, String.format(MESSAGE, "the \"u\" flag"), null, Collections.emptyList());
      }
    }

    boolean isNonAsciiLetter(int codePointOrUnit) {
      return codePointOrUnit > 127 && Character.isLetter(codePointOrUnit);
    }

    boolean isProblematic(FlagSet activeFlags, int codePointOrUnit) {
      return activeFlags.contains(Pattern.CASE_INSENSITIVE) && !activeFlags.contains(Pattern.UNICODE_CASE) && isNonAsciiLetter(codePointOrUnit);
    }
  }

}
