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
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;

@Rule(
  key = "S1194",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ErrorClassExtendedCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CLASS_DECLARATION);
  }

  @Override
  public void visitNode(AstNode node) {
    AstNode extendedClass = node.getFirstChild(JavaGrammar.CLASS_TYPE);

    if (extendedClass != null && isErrorClass(extendedClass)) {
      getContext().createLineViolation(this, "Extend \"java.lang.Exception\" or one of its subclass.", node);
    }
  }

  private static boolean isErrorClass(AstNode node) {
    return isError(node) || isJavaLangError(node);
  }

  private static boolean isError(AstNode node) {
    return "Error".equals(node.getTokenOriginalValue()) &&
      !node.hasDirectChildren(JavaPunctuator.DOT);
  }

  private static boolean isJavaLangError(AstNode node) {
    List<AstNode> identifiers = node.getChildren(JavaTokenType.IDENTIFIER);
    return identifiers.size() == 3 &&
      "java".equals(identifiers.get(0).getTokenOriginalValue()) &&
      "lang".equals(identifiers.get(1).getTokenOriginalValue()) &&
      "Error".equals(identifiers.get(2).getTokenOriginalValue());
  }

}
