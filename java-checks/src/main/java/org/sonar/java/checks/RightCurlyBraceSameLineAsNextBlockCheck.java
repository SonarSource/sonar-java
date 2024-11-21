/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.checks;

import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.LineUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonarsource.analyzer.commons.annotations.DeprecatedRuleKey;

import static org.sonar.java.reporting.AnalyzerMessage.textSpanBetween;

@DeprecatedRuleKey(ruleKey = "RightCurlyBraceSameLineAsNextBlockCheck", repositoryKey = "squid")
@Rule(key = "S1107")
public class RightCurlyBraceSameLineAsNextBlockCheck extends RightCurlyBraceToNextBlockAbstractVisitor {

  @Override
  protected void checkTokenPosition(SyntaxToken syntaxToken, BlockTree previousBlock) {
    if (LineUtils.startLine(syntaxToken) != LineUtils.startLine(previousBlock.closeBraceToken())) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onTree(syntaxToken)
        .withMessage("Move this \"%s\" on the same line that the previous closing curly brace.", syntaxToken.text())
        .withQuickFix(() -> JavaQuickFix.newQuickFix("Move to the same line as the closing curly brace")
          .addTextEdit(JavaTextEdit.replaceTextSpan(textSpanBetween(previousBlock, false, syntaxToken, false), " ")
          ).build())
        .report();
    }
  }
}
