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
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Set;

@Rule(
  key = "S1158",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class ToStringUsingBoxingCheck extends SquidCheck<LexerlessGrammar> {

  private static final Set<String> PRIMITIVE_WRAPPERS = ImmutableSet.of(
      "Byte",
      "Short",
      "Integer",
      "Long",
      "Float",
      "Double",
      "Character",
      "Boolean");

  @Override
  public void init() {
    subscribeTo(JavaGrammar.UNARY_EXPRESSION, JavaGrammar.UNARY_EXPRESSION_NOT_PLUS_MINUS);
  }

  @Override
  public void visitNode(AstNode node) {
    if (hasToStringSelector(node)) {
      String newlyCreatedClassName = getNewlyCreatedClassName(node);

      if (PRIMITIVE_WRAPPERS.contains(newlyCreatedClassName)) {
        getContext().createLineViolation(
            this,
            "Call the static method " + newlyCreatedClassName + ".toString(...) instead of instantiating a temporary object to perform this to string conversion.",
            node);
      }
    }
  }

  private static boolean hasToStringSelector(AstNode node) {
    AstNode selector = node.getFirstChild(JavaGrammar.SELECTOR);

    return selector != null &&
      selector.hasDirectChildren(JavaTokenType.IDENTIFIER) &&
      "toString".equals(selector.getFirstChild(JavaTokenType.IDENTIFIER).getTokenOriginalValue());
  }

  private static String getNewlyCreatedClassName(AstNode node) {
    AstNode primary = node.getFirstChild(JavaGrammar.PRIMARY);
    AstNode creator = primary.getFirstChild(JavaGrammar.CREATOR);

    return creator == null ? null : getSimpleCreatedName(creator.getFirstChild(JavaGrammar.CREATED_NAME));
  }

  private static String getSimpleCreatedName(AstNode node) {
    return node == null ||
      !node.getToken().equals(node.getLastToken()) ? null : node.getTokenOriginalValue();
  }

}
