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

import java.util.Collections;
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
    subscribeTo(JavaKeyword.THROWS);
  }

  @Override
  public void visitNode(AstNode node) {
    for (AstNode nameNode : getExceptionNameNodes(node)) {
      String name = merge(nameNode);
      if (RAW_EXCEPTIONS.contains(name)) {
        getContext().createLineViolation(this, "Define and throw a dedicated exception instead of using a generic one.", nameNode);
      }
    }
  }

  private static Iterable<AstNode> getExceptionNameNodes(AstNode node) {
    return node.is(JavaGrammar.THROW_STATEMENT) ? getThrowStatementExceptionNames(node) : getThrowsDeclarationExceptionNames(node);
  }

  private static Iterable<AstNode> getThrowStatementExceptionNames(AstNode node) {
    AstNode primary = node.getFirstChild(JavaGrammar.EXPRESSION).getFirstChild(JavaGrammar.PRIMARY);
    if (primary == null || primary.getFirstChild().isNot(JavaKeyword.NEW)) {
      return Collections.EMPTY_LIST;
    }
    AstNode createdName = primary.getFirstDescendant(JavaGrammar.CREATED_NAME);
    if (createdName == null) {
      return Collections.EMPTY_LIST;
    }
    return Collections.singleton(createdName);
  }

  private static Iterable<AstNode> getThrowsDeclarationExceptionNames(AstNode node) {
    AstNode qualifiedIdentifierList = node.getNextSibling();
    return qualifiedIdentifierList.getChildren(JavaGrammar.QUALIFIED_IDENTIFIER);
  }

  private static String merge(AstNode node) {
    StringBuilder sb = new StringBuilder();
    for (Token token : node.getTokens()) {
      sb.append(token.getValue());
    }
    return sb.toString();
  }

}
