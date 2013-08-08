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
import com.sonar.sslr.squid.checks.SquidCheck;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1135",
  priority = Priority.INFO)
@BelongsToProfile(title = "Sonar way", priority = Priority.INFO)
public class TodoTagPresenceCheck extends SquidCheck<LexerlessGrammar> implements AstAndTokenVisitor {

  private static final String PATTERN = "TODO";

  @Override
  public void visitToken(Token token) {
    for (Trivia trivia : token.getTrivia()) {
      String comment = trivia.getToken().getOriginalValue();
      if (StringUtils.containsIgnoreCase(comment, PATTERN)) {
        String[] lines = comment.split("\r\n?|\n");
        for (int i = 0; i < lines.length; i++) {
          if (StringUtils.containsIgnoreCase(lines[i], PATTERN)) {
            getContext().createLineViolation(this, "Complete the task associated to this TODO comment.", trivia.getToken().getLine() + i);
          }
        }
      }
    }
  }

}
