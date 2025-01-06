/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.regex;

import java.util.Collections;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.ast.CharacterTree;
import org.sonarsource.analyzer.commons.regex.ast.RegexBaseVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;

@Rule(key = "S6070")
public class EscapeSequenceControlCharacterCheck extends AbstractRegexCheck {

  private static final String MESSAGE = "Remove or replace this problematic use of \\c.";

  private static final Pattern WRONG_ESCAPED_SEQUENCE = Pattern.compile("\\\\\\\\c[^@-_]");

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, ExpressionTree methodInvocationOrAnnotation) {
    new WrongEscapeSequenceVisitor().visit(regexForLiterals);
  }

  private class WrongEscapeSequenceVisitor extends RegexBaseVisitor {
    @Override
    public void visitCharacter(CharacterTree tree) {
      if (WRONG_ESCAPED_SEQUENCE.matcher(tree.getText()).matches()) {
        reportIssue(tree, MESSAGE, null, Collections.emptyList());
      }
    }
  }


}
