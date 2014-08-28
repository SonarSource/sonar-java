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
package org.sonar.java.ast.visitors;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.Token;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

public class SuppressWarningsAnnotationUtils {

  private SuppressWarningsAnnotationUtils() {
  }

  private static final String SUPPRESS_WARNINGS_ANNOTATION_NAME = "SuppressWarnings";
  private static final String SUPPRESS_WARNINGS_ANNOTATION_FQ_NAME = "java.lang." + SUPPRESS_WARNINGS_ANNOTATION_NAME;
  private static final String VALUE = "\"all\"";

  public static boolean isSuppressAllWarnings(AstNode astNode) {
    if (astNode.is(JavaGrammar.CLASS_DECLARATION, JavaGrammar.INTERFACE_DECLARATION, JavaGrammar.ENUM_DECLARATION)) {
      AstNode modifiersCandidate = astNode.getPreviousAstNode();
      if (!modifiersCandidate.is(JavaGrammar.MODIFIERS)) {
        return false;
      }

      ModifiersTree modifiers = (ModifiersTree) modifiersCandidate;
      return containsAnnotationSuppressAllWarnings(modifiers.annotations());
    } else if (astNode.is(Kind.ANNOTATION_TYPE)) {
      return containsAnnotationSuppressAllWarnings(((ClassTree) astNode).modifiers().annotations());
    }

    final AstNode node;
    if (astNode.is(JavaGrammar.METHOD_DECLARATOR_REST, JavaGrammar.VOID_METHOD_DECLARATOR_REST, JavaGrammar.CONSTRUCTOR_DECLARATOR_REST)) {
      node = astNode.getFirstAncestor(JavaGrammar.CLASS_BODY_DECLARATION);
    } else if (astNode.is(JavaGrammar.INTERFACE_METHOD_DECLARATOR_REST, JavaGrammar.VOID_INTERFACE_METHOD_DECLARATORS_REST)) {
      node = astNode.getFirstAncestor(JavaGrammar.INTERFACE_BODY_DECLARATION);
    } else if (astNode.is(Kind.METHOD)) {
      return containsAnnotationSuppressAllWarnings(((MethodTree) astNode).modifiers().annotations());
    } else {
      throw new IllegalArgumentException("Unexpected AstNodeType: " + astNode.getType());
    }
    ModifiersTree modifiers = (ModifiersTree) node.getFirstChild(JavaGrammar.MODIFIERS);
    return containsAnnotationSuppressAllWarnings(modifiers.annotations());
  }

  private static boolean containsAnnotationSuppressAllWarnings(List<AnnotationTree> annotations) {
    for (AnnotationTree annotation : annotations) {
      return isAnnotationSupressAllWarnings(annotation);
    }
    return false;
  }

  private static boolean isAnnotationSupressAllWarnings(AnnotationTree annotation) {
    String name = getAnnotationName((AstNode) annotation.annotationType());
    if (SUPPRESS_WARNINGS_ANNOTATION_NAME.equals(name) || SUPPRESS_WARNINGS_ANNOTATION_FQ_NAME.equals(name)) {
      for (ExpressionTree expression : annotation.arguments()) {
        if (expression.is(Kind.STRING_LITERAL) && VALUE.equals(((LiteralTree) expression).value())) {
          return true;
        }
      }
    }
    return false;
  }

  private static String getAnnotationName(AstNode astNode) {
    StringBuilder sb = new StringBuilder();
    for (Token token : astNode.getTokens()) {
      sb.append(token.getValue());
    }
    return sb.toString();
  }

}
