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
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.ast.AstSelect;
import org.sonar.sslr.parser.LexerlessGrammar;

@Rule(
  key = "MissingDeprecatedCheck",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class MissingDeprecatedCheck extends SquidCheck<LexerlessGrammar> {

  @Override
  public void init() {
    subscribeTo(
        JavaGrammar.TYPE_DECLARATION,
        JavaGrammar.CLASS_BODY_DECLARATION,
        JavaGrammar.INTERFACE_BODY_DECLARATION,
        JavaGrammar.ANNOTATION_TYPE_ELEMENT_DECLARATION,
        JavaGrammar.BLOCK_STATEMENT);
  }

  @Override
  public void visitNode(AstNode node) {
    boolean hasDeprecatedAnnotation = hasDeprecatedAnnotation(node);
    boolean hasJavadocDeprecatedTag = hasJavadocDeprecatedTag(node);

    if (hasDeprecatedAnnotation && !hasJavadocDeprecatedTag) {
      getContext().createLineViolation(this, "Add the missing @deprecated Javadoc tag.", node);
    } else if (hasJavadocDeprecatedTag && !hasDeprecatedAnnotation) {
      getContext().createLineViolation(this, "Add the missing @Deprecated annotation.", node);
    }
  }

  private static boolean isDeprecated(AstNode node) {
    AstNode qualifiedIdentifier = node.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER);
    return qualifiedIdentifier.getNumberOfChildren() == 1 &&
      "Deprecated".equals(qualifiedIdentifier.getFirstChild(JavaTokenType.IDENTIFIER).getTokenOriginalValue());
  }

  private static boolean hasJavadocDeprecatedTag(AstNode node) {
    Token token = node.getToken();
    for (Trivia trivia : token.getTrivia()) {
      String comment = trivia.getToken().getOriginalValue();
      if (hasJavadocDeprecatedTag(comment)) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasJavadocDeprecatedTag(String comment) {
    return comment.startsWith("/**") && comment.contains("@deprecated");
  }

  private static boolean hasDeprecatedAnnotation(AstNode node) {
    AstSelect annotations = node.select()
        .children(JavaGrammar.MODIFIER)
        .children(JavaGrammar.ANNOTATION);

    for (AstNode annotation : annotations) {
      if (isDeprecated(annotation)) {
        return true;
      }
    }

    return false;
  }

}
