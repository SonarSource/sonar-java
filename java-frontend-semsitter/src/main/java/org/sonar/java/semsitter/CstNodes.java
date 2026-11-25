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

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.KeywordSuper;
import org.sonar.java.model.KeywordThis;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

/** Navigation helpers over SemSitter CST nodes. */
final class CstNodes {

  private static final String IDENTIFIER = "identifier";
  private static final String TYPE_IDENTIFIER = "type_identifier";

  private CstNodes() {
  }

  @Nullable
  static CstNode firstProblem(CstNode node) {
    if ("ERROR".equals(node.k()) || node.missing()) {
      return node;
    }
    for (org.sonarsource.semsitter.wire.Node child : node.proto.getChildrenList()) {
      CstNode problem = firstProblem(new CstNode(child));
      if (problem != null) {
        return problem;
      }
    }
    return null;
  }

  static void collectLeaves(CstNode node, List<CstNode> leaves) {
    if (isComment(node) || node.allChildren().isEmpty() || isLiteral(node.k())) {
      if (!node.text().isEmpty()) {
        leaves.add(node);
      }
      return;
    }
    for (CstNode child : node.allChildren()) {
      collectLeaves(child, leaves);
    }
  }

  /** Tree-sitter splits {@code Record throws Ex} into a field named {@code throws} and a compact constructor named {@code Ex}. */
  @Nullable
  static CstNode enumSeparator(List<CstNode> siblings, int index) {
    CstNode next = siblings.get(index + 1);
    if (",".equals(next.text())) {
      return next;
    }
    if ("enum_body_declarations".equals(next.k())) {
      return next.children().get(0);
    }
    return null;
  }

  static boolean nextIsGroup(List<CstNode> siblings, int index) {
    return "switch_block_statement_group".equals(siblings.get(index + 1).k());
  }

  static String dottedName(ExpressionTree tree) {
    if (tree instanceof MemberSelectExpressionTree select) {
      String qualifier = dottedName(select.expression());
      return qualifier.isEmpty() ? "" : (qualifier + "." + select.identifier().name());
    }
    return tree instanceof IdentifierTree identifier ? identifier.name() : "";
  }

  /** The {@code this} or {@code super} written after a qualifier is a keyword, not a name. */
  static IdentifierTreeImpl receiverKeyword(IdentifierTreeImpl identifier) {
    return switch (identifier.name()) {
      case "this" -> new KeywordThis((InternalSyntaxToken) identifier.identifierToken(), Symbol.UNKNOWN_SYMBOL);
      case "super" -> new KeywordSuper((InternalSyntaxToken) identifier.identifierToken(), Symbol.UNKNOWN_SYMBOL);
      default -> identifier;
    };
  }

  static boolean compoundName(CstNode node) {
    return switch (node.k()) {
      case "scoped_type_identifier", "scoped_identifier", "generic_type", "field_access" -> true;
      default -> false;
    };
  }

  static Modifier modifierKeyword(String text) {
    return switch (text) {
      case "public" -> Modifier.PUBLIC;
      case "protected" -> Modifier.PROTECTED;
      case "private" -> Modifier.PRIVATE;
      case "static" -> Modifier.STATIC;
      case "abstract" -> Modifier.ABSTRACT;
      case "final" -> Modifier.FINAL;
      case "native" -> Modifier.NATIVE;
      case "synchronized" -> Modifier.SYNCHRONIZED;
      case "transient" -> Modifier.TRANSIENT;
      case "volatile" -> Modifier.VOLATILE;
      case "strictfp" -> Modifier.STRICTFP;
      case "default" -> Modifier.DEFAULT;
      case "sealed" -> Modifier.SEALED;
      case "non-sealed" -> Modifier.NON_SEALED;
      default -> throw new IllegalArgumentException("Not a modifier: " + text);
    };
  }

  static boolean isTypeKind(String kind) {
    return switch (kind) {
      case TYPE_IDENTIFIER, IDENTIFIER, "scoped_identifier", "scoped_type_identifier", "generic_type", "array_type", "integral_type", "floating_point_type", "boolean_type",
        "void_type", "wildcard", "annotated_type" -> true;
      default -> false;
    };
  }

  private static boolean isLiteral(String kind) {
    return switch (kind) {
      case "string_literal", "character_literal", "text_block", "decimal_integer_literal", "hex_integer_literal", "octal_integer_literal", "binary_integer_literal",
        "decimal_floating_point_literal", "hex_floating_point_literal" -> true;
      default -> false;
    };
  }

  static boolean isComment(CstNode node) {
    return "line_comment".equals(node.k()) || "block_comment".equals(node.k());
  }

  static InternalSyntaxToken closingAngle(CstNode node) {
    CstNode close = lastKeyword(node, ">");
    return new InternalSyntaxToken(close.line(), close.col(), ">", List.of(), false);
  }

  @Nullable
  static CstNode keyword(CstNode node, String text) {
    for (CstNode child : node.children()) {
      if (text.equals(child.text()) || text.equals(child.k())) {
        return child;
      }
    }
    return null;
  }

  static CstNode lastKeyword(CstNode node, String text) {
    List<CstNode> children = node.children();
    for (int i = children.size() - 1; i >= 0; i--) {
      if (text.equals(children.get(i).text())) {
        return children.get(i);
      }
    }
    return node;
  }

  static boolean same(@Nullable CstNode left, @Nullable CstNode right) {
    return left != null && right != null && left.proto == right.proto;
  }

  static CstNode required(CstNode node, String text) {
    CstNode found = keyword(node, text);
    return found == null ? node : found;
  }

  @Nullable
  static CstNode field(CstNode node, String name) {
    for (CstNode child : node.children()) {
      if (name.equals(child.f())) {
        return child;
      }
    }
    return null;
  }

  static CstNode requiredField(CstNode node, String name, @Nullable CstNode fallback) {
    CstNode found = field(node, name);
    if (found != null) {
      return found;
    }
    return fallback == null ? node : fallback;
  }

  @Nullable
  static CstNode childKind(CstNode node, String kind) {
    for (CstNode child : node.children()) {
      if (kind.equals(child.k())) {
        return child;
      }
    }
    return null;
  }

  @Nullable
  static CstNode firstOf(CstNode node, String... kinds) {
    for (CstNode child : node.children()) {
      for (String kind : kinds) {
        if (kind.equals(child.k())) {
          return child;
        }
      }
    }
    return null;
  }

  @Nullable
  static CstNode lastOf(CstNode node, String kind) {
    CstNode found = null;
    for (CstNode child : node.children()) {
      if (kind.equals(child.k())) {
        found = child;
      }
    }
    return found;
  }

  @Nullable
  static CstNode nameNode(CstNode node) {
    CstNode named = field(node, "name");
    return named != null ? named : firstOf(node, IDENTIFIER, TYPE_IDENTIFIER);
  }

  @Nullable
  static CstNode firstNamed(CstNode node) {
    for (CstNode child : node.children()) {
      if ((!child.text().equals(child.k()) || child.k().contains("_"))
        && (!child.children().isEmpty() || child.k().contains("_") || IDENTIFIER.equals(child.k()) || TYPE_IDENTIFIER.equals(child.k()))) {
        return child;
      }
    }
    return node.children().isEmpty() ? node : node.children().get(0);
  }

  static boolean isAnnotation(CstNode node) {
    return "annotation".equals(node.k()) || "marker_annotation".equals(node.k());
  }

  static CstNode lastNamed(CstNode node) {
    List<CstNode> children = node.children();
    for (int i = children.size() - 1; i >= 0; i--) {
      CstNode child = children.get(i);
      if (child.k().contains("_") || IDENTIFIER.equals(child.k())) {
        return child;
      }
    }
    return node;
  }

  @Nullable
  static CstNode firstType(CstNode node) {
    for (CstNode child : node.children()) {
      if (isTypeKind(child.k())) {
        return child;
      }
    }
    return null;
  }

  static InternalSyntaxToken firstKeyword(CstNode node) {
    CstNode keyword = node.children().get(0);
    return new InternalSyntaxToken(keyword.line(), keyword.col(), keyword.text(), List.of(), false);
  }

  static void setParents(Tree node) {
    if (node instanceof JavaTree javaTree && node.kind() != Tree.Kind.INFERED_TYPE && node.kind() != Tree.Kind.TOKEN) {
      for (Tree child : javaTree.getChildren()) {
        if (child instanceof JavaTree javaChild) {
          javaChild.setParent(node);
          setParents(child);
        }
      }
    }
  }
}
