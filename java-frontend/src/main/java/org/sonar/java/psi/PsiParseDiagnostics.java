/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.psi;

import com.intellij.lang.java.lexer.JavaLexer;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.List;
import org.sonar.java.psi.PsiLanguageValidator.Diagnostic;
import org.sonar.java.psi.PsiLanguageValidator.Kind;

final class PsiParseDiagnostics {
  private PsiParseDiagnostics() {
  }

  static List<Diagnostic> additional(PsiJavaFile file, JavaPsiConfiguration configuration) {
    var diagnostics = new ArrayList<Diagnostic>();
    for (var literal : PsiTreeUtil.findChildrenOfType(file, PsiLiteralExpression.class)) {
      if (literal.getText().startsWith("'") && literal.getValue() == null) {
        int start = literal.getTextRange().getStartOffset();
        diagnostics.add(new Diagnostic(Kind.FATAL, start, start, "Invalid character constant"));
      }
    }
    var lexer = new JavaLexer(LanguageLevel.JDK_26_PREVIEW);
    lexer.start(file.getText(), file.getPackageStatement() == null ? 0 : file.getPackageStatement().getTextRange().getEndOffset(), file.getTextLength(), 0);
    boolean inImport = false;
    boolean seenImport = false;
    int emptyDeclaration = -1;
    while (lexer.getTokenType() != null) {
      var token = lexer.getTokenType();
      if (token == JavaTokenType.IMPORT_KEYWORD) {
        if (emptyDeclaration >= 0) {
          boolean afterImport = configuration.sourceVersion() >= 25 && seenImport;
          int start = afterImport ? lexer.getTokenStart() : emptyDeclaration;
          diagnostics.add(new Diagnostic(Kind.FATAL, start, start,
            afterImport ? "Syntax error on token \"import\", int expected" : "Syntax error on token \";\", delete this token"));
          break;
        }
        inImport = true;
        seenImport = true;
      } else if (token == JavaTokenType.SEMICOLON) {
        if (inImport) {
          inImport = false;
        } else {
          emptyDeclaration = lexer.getTokenStart();
        }
      } else if (!inImport && token != com.intellij.psi.TokenType.WHITE_SPACE && !token.toString().contains("COMMENT")) {
        break;
      }
      lexer.advance();
    }
    return diagnostics;
  }
}
