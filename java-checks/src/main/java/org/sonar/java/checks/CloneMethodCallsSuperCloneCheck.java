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
  key = "S1182",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class CloneMethodCallsSuperCloneCheck extends SquidCheck<LexerlessGrammar> {

  private boolean foundSuperClone;

  @Override
  public void init() {
    subscribeTo(JavaGrammar.MEMBER_DECL);
    subscribeTo(JavaGrammar.PRIMARY);
  }

  @Override
  public void visitNode(AstNode node) {
    if (isCloneMethod(node)) {
      foundSuperClone = false;
    } else if (isSuperCloneCall(node)) {
      foundSuperClone = true;
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (isCloneMethod(node) && !foundSuperClone) {
      getContext().createLineViolation(this, "Use super.clone() to create and seed the cloned instance to be returned.", node);
    }
  }

  private static boolean isCloneMethod(AstNode node) {
    AstNode methodRest = node.getFirstChild(JavaGrammar.METHOD_DECLARATOR_REST);

    return node.is(JavaGrammar.MEMBER_DECL) &&
      methodRest != null &&
      "clone".equals(node.getFirstChild(JavaTokenType.IDENTIFIER).getTokenOriginalValue()) &&
      !methodRest.getFirstChild(JavaGrammar.FORMAL_PARAMETERS).hasDirectChildren(JavaGrammar.FORMAL_PARAMETER_DECLS);
  }

  private static boolean isSuperCloneCall(AstNode node) {
    AstNode superSuffix = node.getFirstChild(JavaGrammar.SUPER_SUFFIX);

    return node.is(JavaGrammar.PRIMARY) &&
      superSuffix != null &&
      isCloneCallSuffix(superSuffix);
  }

  private static boolean isCloneCallSuffix(AstNode node) {
    AstNode identifier = node.getFirstChild(JavaTokenType.IDENTIFIER);
    AstNode arguments = node.getFirstChild(JavaGrammar.ARGUMENTS);

    return identifier != null &&
      "clone".equals(identifier.getTokenOriginalValue()) &&
      arguments != null &&
      isEmptyArguments(arguments);
  }

  private static boolean isEmptyArguments(AstNode node) {
    return !node.hasDirectChildren(JavaGrammar.EXPRESSION);
  }

}
