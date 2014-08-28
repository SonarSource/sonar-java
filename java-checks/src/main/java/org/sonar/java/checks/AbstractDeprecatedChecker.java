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
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.checks.SquidCheck;
import org.sonar.sslr.ast.AstSelect;
import org.sonar.sslr.parser.LexerlessGrammar;

public class AbstractDeprecatedChecker extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(
      JavaGrammar.TYPE_DECLARATION,
      JavaGrammar.CLASS_BODY_DECLARATION,
      JavaGrammar.INTERFACE_BODY_DECLARATION,
      JavaGrammar.ANNOTATION_TYPE_ELEMENT_DECLARATION,
      JavaGrammar.BLOCK_STATEMENT);
  }

  public static boolean hasJavadocDeprecatedTag(AstNode node) {
    Token token = node.getToken();
    for (Trivia trivia : token.getTrivia()) {
      String comment = trivia.getToken().getOriginalValue();
      if (hasJavadocDeprecatedTag(comment)) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasJavadocDeprecatedTag(String comment) {
    return comment.startsWith("/**") && comment.contains("@deprecated");
  }

  public static boolean hasDeprecatedAnnotationExcludingLocalVariables(AstNode node) {
    AstSelect annotations = node.select()
      .children(JavaGrammar.MODIFIERS)
      .children(Kind.ANNOTATION);

    return hasDeprecatedAnnotation(annotations);
  }

  public static boolean hasDeprecatedAnnotationOnLocalVariables(AstNode node) {
    AstSelect annotations = node.select()
      .children(JavaGrammar.LOCAL_VARIABLE_DECLARATION_STATEMENT)
      .children(JavaGrammar.VARIABLE_MODIFIERS)
      .children(Kind.ANNOTATION);

    return hasDeprecatedAnnotation(annotations);
  }

  public static boolean hasDeprecatedAnnotation(Iterable<AstNode> query) {
    for (AstNode annotationAstNode : query) {
      AnnotationTree annotation = (AnnotationTree) annotationAstNode;
      if (isDeprecated(annotation)) {
        return true;
      }
    }

    return false;
  }

  public static boolean isDeprecated(AnnotationTree tree) {
    return tree.annotationType().is(Kind.IDENTIFIER) &&
      "Deprecated".equals(((IdentifierTree) tree.annotationType()).name());
  }

}
