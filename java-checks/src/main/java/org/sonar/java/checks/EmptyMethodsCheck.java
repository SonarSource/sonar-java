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
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "S1186",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class EmptyMethodsCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(JavaGrammar.VOID_METHOD_DECLARATOR_REST);
    subscribeTo(JavaGrammar.METHOD_DECLARATOR_REST);
  }

  @Override
  public void visitNode(AstNode node) {
    if (hasEmptyMethodBody(node) && !isInAbstractClass(node)) {
      getContext().createLineViolation(
        this,
        "Add a nested comment explaining why this method is empty, throw an UnsupportedOperationException or complete the implementation.",
        node);
    }
  }

  private static boolean hasEmptyMethodBody(AstNode node) {
    AstNode methodBody = node.getFirstChild(JavaGrammar.METHOD_BODY);
    if (methodBody == null) {
      return false;
    }

    return isEmptyMethodBody(methodBody);
  }

  private static boolean isEmptyMethodBody(AstNode node) {
    AstNode block = node.getFirstChild(JavaGrammar.BLOCK);

    return isEmptyBlock(block) &&
      !hasComments(block.getFirstChild(JavaPunctuator.RWING));
  }

  private static boolean isEmptyBlock(AstNode node) {
    return node.getFirstChild(JavaGrammar.BLOCK_STATEMENTS).getNumberOfChildren() == 0;
  }

  private static boolean hasComments(AstNode node) {
    return node.getToken().hasTrivia();
  }

  private static boolean isInAbstractClass(AstNode node) {
    AstNode modifier = getFirstAncestor(node, JavaGrammar.CLASS_DECLARATION, JavaGrammar.ENUM_BODY_DECLARATIONS).getPreviousAstNode();
    while (modifier != null && modifier.is(JavaGrammar.MODIFIER)) {
      if (modifier.hasDirectChildren(JavaKeyword.ABSTRACT)) {
        return true;
      }
      modifier = modifier.getPreviousAstNode();
    }

    return false;
  }

  private static AstNode getFirstAncestor(AstNode node, AstNodeType t1, AstNodeType t2) {
    AstNode result = node.getParent();

    while (result != null && !result.is(t1, t2)) {
      result = result.getParent();
    }

    return result;
  }

}
