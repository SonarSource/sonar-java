/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.sslr.parser.LexerlessGrammar;

/**
 * Note that {@link org.sonar.squidbridge.checks.AbstractNoSonarCheck} can't be used because of bug SSLRSQBR-16.
 */
@Rule(key = "NoSonar", priority = Priority.INFO)
public class NoSonarCheck extends SquidCheck<LexerlessGrammar> implements AstAndTokenVisitor {

  @Override
  public void visitToken(Token token) {
    SourceFile sourceFile = getSourceFile();

    for (Trivia trivia : token.getTrivia()) {
      if (trivia.isComment()) {
        String[] commentLines = getContext().getCommentAnalyser().getContents(trivia.getToken().getOriginalValue()).split("(\r)?\n|\r", -1);
        int line = trivia.getToken().getLine();

        for (String commentLine : commentLines) {
          if (commentLine.contains("NOSONAR")) {
            CheckMessage checkMessage = new CheckMessage(this, "Is //NOSONAR used to exclude false-positive or to hide real quality flaw ?");
            checkMessage.setBypassExclusion(true);
            checkMessage.setLine(line);
            sourceFile.log(checkMessage);
          }

          line++;
        }
      }
    }
  }

  private SourceFile getSourceFile() {
    if (getContext().peekSourceCode() instanceof SourceFile) {
      return (SourceFile) getContext().peekSourceCode();
    } else {
      return getContext().peekSourceCode().getParent(SourceFile.class);
    }
  }

}
