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
package org.sonar.java.model;

import com.intellij.lang.java.lexer.JavaLexer;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.List;
import org.sonar.java.psi.JavaPsiConfiguration;
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
    lexer.start(file.getText());
    boolean imported = false;
    boolean inImport = false;
    int emptyDeclaration = -1;
    while (lexer.getTokenType() != null) {
      var token = lexer.getTokenType();
      if (token == JavaTokenType.IMPORT_KEYWORD) {
        if (emptyDeclaration >= 0) {
          int start = configuration.sourceVersion() >= 25 ? lexer.getTokenStart() : emptyDeclaration;
          diagnostics.add(new Diagnostic(Kind.FATAL, start, start,
            configuration.sourceVersion() >= 25 ? "Syntax error on token \"import\", int expected" : "Syntax error on token \";\", delete this token"));
          break;
        }
        imported = true;
        inImport = true;
      } else if (token == JavaTokenType.SEMICOLON) {
        if (inImport) {
          inImport = false;
        } else if (imported) {
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
