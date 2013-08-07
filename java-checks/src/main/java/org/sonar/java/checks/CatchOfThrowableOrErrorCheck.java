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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1181",
  priority = Priority.BLOCKER)
@BelongsToProfile(title = "Sonar way", priority = Priority.BLOCKER)
public class CatchOfThrowableOrErrorCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CATCH_TYPE);
  }

  @Override
  public void visitNode(AstNode node) {
    for (AstNode qualifiedIdentifier : node.getChildren(JavaGrammar.QUALIFIED_IDENTIFIER)) {
      if (hasSingleIdentifier(qualifiedIdentifier)) {
        String caughtException = qualifiedIdentifier.getTokenOriginalValue();

        if ("Throwable".equals(caughtException) || "Error".equals(caughtException)) {
          getContext().createLineViolation(this, "Catch Exception instead of " + caughtException + ".", qualifiedIdentifier);
        }
      }
    }
  }

  private static boolean hasSingleIdentifier(AstNode node) {
    return !node.hasDirectChildren(JavaPunctuator.DOT);
  }

}
