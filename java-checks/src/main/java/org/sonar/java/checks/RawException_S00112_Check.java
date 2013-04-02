/*
 * Sonar Java
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

import com.google.common.collect.ImmutableSet;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Set;

@Rule(
  key = "S00112",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class RawException_S00112_Check extends SquidCheck<LexerlessGrammar> {

  private static final Set<String> RAW_EXCEPTIONS = ImmutableSet.of("Throwable", "Error", "Exception", "RuntimeException");

  @Override
  public void init() {
    subscribeTo(JavaGrammar.THROW_STATEMENT);
  }

  @Override
  public void visitNode(AstNode astNode) {
    AstNode primary = astNode.getFirstChild(JavaGrammar.EXPRESSION).getFirstChild(JavaGrammar.PRIMARY);
    if (primary == null || primary.getFirstChild().isNot(JavaKeyword.NEW)) {
      return;
    }
    AstNode createdName = primary.getFirstDescendant(JavaGrammar.CREATED_NAME);
    if (createdName == null) {
      return;
    }
    String name = tokensToString(createdName);
    if (RAW_EXCEPTIONS.contains(name)) {
      getContext().createLineViolation(this, "Define and throw a dedicated exception instead of using a generic one.", astNode);
    }
  }

  private static String tokensToString(AstNode astNode) {
    StringBuilder sb = new StringBuilder();
    for (Token token : astNode.getTokens()) {
      sb.append(token.getValue());
    }
    return sb.toString();
  }

}
