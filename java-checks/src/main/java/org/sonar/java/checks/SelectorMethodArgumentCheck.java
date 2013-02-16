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

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaGrammar;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.visitors.JavaAstCheck;

@Rule(key = "SelectorMethodArgument", priority = Priority.MAJOR)
public class SelectorMethodArgumentCheck extends JavaAstCheck {

  @Override
  public void init() {
    JavaGrammar grammar = getContext().getGrammar();
    subscribeTo(grammar.type);
  }

  @Override
  public void visitNode(AstNode node) {
    if (isAMethodParameter(node)) {
      if (isOfTypeBooleanBasic(node)) {
        String argumentName = getArgumentNameOfBasicType(node);
        createViolation(node, argumentName);
      } else if (isOfTypeBooleanClass(node)) {
        String argumentName = getArgumentNameOfClassType(node);
        createViolation(node, argumentName);
      }
    }
  }

  private void createViolation(AstNode node, String argumentName) {
    String message = String.format("The argument '%s' implies this method can have two states, consider to create two separate methods.", argumentName);
    getContext().createLineViolation(this, message, node);
  }

  private String getArgumentNameOfClassType(AstNode node) {
    return findFirstIdentifier(node.getParent().getChildren().get(1));
  }

  private String getArgumentNameOfBasicType(AstNode node) {
    return findFirstIdentifier(node.getParent());
  }

  private String findFirstIdentifier(AstNode node) {
    return node.findFirstChild(GenericTokenType.IDENTIFIER).getTokenValue();
  }

  private boolean isOfTypeBooleanBasic(AstNode node) {
    JavaGrammar grammar = getContext().getGrammar();
    AstNode firstChild = node.findFirstDirectChild(grammar.basicType);

    return firstChild != null && firstChild.findFirstChild(JavaKeyword.BOOLEAN) != null;
  }

  private boolean isOfTypeBooleanClass(AstNode node) {
    JavaGrammar grammar = getContext().getGrammar();
    AstNode firstChild = node.findFirstDirectChild(grammar.classType);

    if (firstChild == null) {
      return false;
    } else {
      // each identifier has a token, no null check neccessary
      String identifier = firstChild.findFirstDirectChild(GenericTokenType.IDENTIFIER).getTokenValue();
      return "Boolean".equals(identifier);
    }
  }

  private boolean isAMethodParameter(AstNode node) {
    JavaGrammar grammar = getContext().getGrammar();
    return node.getParent().getType().equals(grammar.formalParameterDecls);
  }
}
