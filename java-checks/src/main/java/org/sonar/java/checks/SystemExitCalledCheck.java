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
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1147",
  priority = Priority.CRITICAL)
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class SystemExitCalledCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.PRIMARY);
  }

  @Override
  public void visitNode(AstNode node) {
    if (isMethodCall(node) && isCallToExitMethod(node)) {
      getContext().createLineViolation(this, "Remove this exit() call or ensure it is really required.", node);
    }
  }

  private static boolean isMethodCall(AstNode node) {
    AstNode suffix = node.getFirstChild(JavaGrammar.IDENTIFIER_SUFFIX);
    return suffix != null &&
      suffix.hasDirectChildren(JavaGrammar.ARGUMENTS);
  }

  private static boolean isCallToExitMethod(AstNode node) {
    AstNode qualifiedIdentifier = node.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER);
    return AstNodeTokensMatcher.matches(qualifiedIdentifier, "System.exit") ||
      AstNodeTokensMatcher.matches(qualifiedIdentifier, "Runtime.getRuntime") &&
      hasExitCallSuffix(node.getParent());
  }

  private static boolean hasExitCallSuffix(AstNode node) {
    AstNode selector = node.getFirstChild(JavaGrammar.SELECTOR);
    return selector != null &&
      selector.hasDirectChildren(JavaGrammar.ARGUMENTS) &&
      "exit".equals(selector.getFirstChild(JavaTokenType.IDENTIFIER).getTokenValue());
  }

}
