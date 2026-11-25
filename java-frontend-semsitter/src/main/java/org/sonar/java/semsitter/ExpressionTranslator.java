/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.semsitter;

import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.semsitter.Bindings.literal;

/**
 * Translates names, literals and annotations. Other expressions are not translated yet: a file using them is parsed by
 * the default frontend.
 */
final class ExpressionTranslator extends TranslatorPart {

  ExpressionTranslator(TranslationState state) {
    super(state);
  }

  ExpressionTree expression(CstNode node) {
    return switch (node.k()) {
      case "decimal_integer_literal", "hex_integer_literal", "octal_integer_literal", "binary_integer_literal" -> names().integerLiteral(node);
      case "decimal_floating_point_literal", "hex_floating_point_literal" -> names().floatingLiteral(node);
      case "string_literal", "string_fragment" -> literal(node.text().startsWith("\"\"\"") ? Tree.Kind.TEXT_BLOCK : Tree.Kind.STRING_LITERAL, token(node), sema.getClassType("java.lang.String"));
      case "character_literal" -> literal(Tree.Kind.CHAR_LITERAL, token(node), sema.getClassType("char"));
      case "scoped_identifier", "scoped_type_identifier", "field_access" -> names().memberSelect(node);
      case "element_value_pair" -> members().elementValuePair(node);
      case "annotation", "marker_annotation" -> members().annotation(node);
      case "generic_type", "array_type", "integral_type", "floating_point_type", "boolean_type", "void_type", "wildcard", "annotated_type" -> invocations().typeExpression(node);
      case "identifier", "type_identifier", "this", "super", "true", "false", "null_literal" -> names().primary(node);
      default -> throw new UnsupportedConstructException(node.k());
    };
  }

  ArgumentListTreeImpl arguments(CstNode node) {
    ArgumentListTreeImpl list = ArgumentListTreeImpl.emptyList();
    InternalSyntaxToken open = null;
    InternalSyntaxToken close = null;
    for (CstNode child : node.children()) {
      if ("(".equals(child.text())) {
        open = token(child);
      } else if (")".equals(child.text())) {
        close = token(child);
      } else if (",".equals(child.text())) {
        list.separators().add(token(child));
      } else {
        list.add(expression(child));
      }
    }
    return list.complete(open, close);
  }
}
