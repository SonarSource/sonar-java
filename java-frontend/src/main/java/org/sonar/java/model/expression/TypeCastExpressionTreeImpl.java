/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.expression;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonarsource.analyzer.commons.collections.ListUtils;

public class TypeCastExpressionTreeImpl extends AssessableExpressionTree implements TypeCastTree {

  private final InternalSyntaxToken openParenToken;
  private final TypeTree type;
  @Nullable
  private final InternalSyntaxToken andToken;
  private final ListTree<TypeTree> bounds;
  private final InternalSyntaxToken closeParenToken;
  private final ExpressionTree expression;

  public TypeCastExpressionTreeImpl(InternalSyntaxToken openParen, TypeTree type, InternalSyntaxToken closeParen, ExpressionTree expression) {
    this(openParen, type, null, QualifiedIdentifierListTreeImpl.emptyList(), closeParen, expression);
  }

  public TypeCastExpressionTreeImpl(InternalSyntaxToken openParen, TypeTree type, @Nullable InternalSyntaxToken andToken, ListTree<TypeTree> bounds, InternalSyntaxToken closeParen,
    ExpressionTree expression) {
    this.openParenToken = openParen;
    this.type = type;
    this.bounds = bounds;
    this.closeParenToken = closeParen;
    this.expression = expression;
    this.andToken = andToken;
  }

  @Override
  public Kind kind() {
    return Kind.TYPE_CAST;
  }

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public TypeTree type() {
    return type;
  }

  @Nullable
  @Override
  public SyntaxToken andToken() {
    return andToken;
  }

  @Override
  public ListTree<TypeTree> bounds() {
    return bounds;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public ExpressionTree expression() {
    return expression;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitTypeCast(this);
  }

  @Override
  public List<Tree> children() {
    return ListUtils.concat(
      Arrays.asList(openParenToken, type),
      andToken == null ? Collections.<Tree>emptyList() : Collections.singletonList(andToken()),
      Arrays.asList(bounds, closeParenToken, expression)
    );
  }

  @Override
  public Optional<Object> asConstant() {
    if (constant == NOT_INITIALIZED) {
      constant = expression.asConstant();
      Object value = constant.orElse(null);
      if (value != null) {
        String typeName = type.symbolType().name();
        if (value instanceof Number number) {
          constant = switch (typeName) {
            case "byte" -> Optional.of(number.byteValue());
            case "short" -> Optional.of(number.shortValue());
            case "int" -> Optional.of(number.intValue());
            case "long" -> Optional.of(number.longValue());
            case "float" -> Optional.of(number.floatValue());
            case "double" -> Optional.of(number.doubleValue());
            case "char" -> Optional.of((char) number.intValue());
            default -> constant;
          };
        } else if (value instanceof Character ch) {
          constant = switch (typeName) {
            case "byte" -> Optional.of((byte) (int) ch);
            case "short" -> Optional.of((short) (int) ch);
            case "int" -> Optional.of((int) ch);
            case "long" -> Optional.of((long) ch);
            case "float" -> Optional.of((float) ch);
            case "double" -> Optional.of((double) ch);
            default -> constant;
          };
        }
      }
    }
    return constant;
  }

}
