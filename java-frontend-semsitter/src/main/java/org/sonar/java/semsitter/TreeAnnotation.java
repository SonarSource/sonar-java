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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.model.AnnotationValueImpl;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

final class TreeAnnotation implements AnnotationInstance {

  private final JSema sema;
  private final AnnotationTree tree;

  TreeAnnotation(JSema sema, AnnotationTree tree) {
    this.sema = sema;
    this.tree = tree;
  }

  @Override
  public Symbol.TypeSymbol symbol() {
    Type type = tree.annotationType().symbolType();
    if (type.isUnknown() && tree.symbolType().isUnknown() && !"!Unknown!".equals(tree.symbolType().fullyQualifiedName())) {
      // an unresolved annotation still carries its written name
      type = tree.symbolType();
    }
    return type.symbol();
  }

  @Override
  public List<AnnotationValue> values() {
    List<AnnotationValue> values = new ArrayList<>();
    if (symbol().isUnknown()) {
      // the members of an annotation nothing declares cannot be told apart from other expressions
      return values;
    }
    for (ExpressionTree argument : tree.arguments()) {
      if (argument instanceof AssignmentExpressionTree assignment) {
        String name = assignment.variable().toString();
        values.add(new AnnotationValueImpl(name, value(name, assignment.expression())));
      } else {
        values.add(new AnnotationValueImpl("value", value("value", argument)));
      }
    }
    return values;
  }

  /**
   * The value of the member {@code name}, wrapped in an array when the member is array typed, as the value of an
   * array typed member is always an array no matter how many elements the annotation spells out.
   */
  private Object value(String name, ExpressionTree expression) {
    Object value = literal(expression);
    return value instanceof Object[] || !isArrayTyped(name) ? value : new Object[] {value};
  }

  private boolean isArrayTyped(String name) {
    return symbol().lookupSymbols(name).stream()
      .anyMatch(member -> member instanceof Symbol.MethodSymbol method && method.returnType().type().isArray());
  }

  private Object literal(ExpressionTree expression) {
    return literal(sema, expression);
  }

  /**
   * The compile time value of {@code literal}, typed as the literal is: a {@link String} for text, a {@link Boolean}
   * for booleans and an {@link Integer} for an int that fits. Anything else keeps its source spelling.
   */
  private static Object constantValue(LiteralTree literal) {
    String value = literal.value();
    return switch (literal.kind()) {
      case STRING_LITERAL, TEXT_BLOCK -> LiteralUtils.trimQuotes(value);
      case BOOLEAN_LITERAL -> Boolean.valueOf(value);
      case INT_LITERAL -> {
        Integer integer = LiteralUtils.intLiteralValue(literal);
        yield integer == null ? value : integer;
      }
      case LONG_LITERAL -> {
        Long number = LiteralUtils.longLiteralValue(literal);
        yield number == null ? value : number;
      }
      case CHAR_LITERAL -> (char) LiteralUtils.charLiteralValue(literal);
      case FLOAT_LITERAL, DOUBLE_LITERAL -> floatingValue(value, literal.is(Tree.Kind.FLOAT_LITERAL));
      default -> value;
    };
  }

  private static Object floatingValue(String value, boolean isFloat) {
    String digits = value.replace("_", "");
    if (isFloat) {
      return Float.valueOf(digits);
    }
    return Double.valueOf(digits);
  }

  static Object literal(JSema sema, ExpressionTree expression) {
    if (expression instanceof LiteralTree literal) {
      return constantValue(literal);
    }
    if (expression instanceof AnnotationTree annotation) {
      return new TreeAnnotation(sema, annotation);
    }
    if (expression instanceof NewArrayTree array) {
      return array.initializers().stream().map(initializer -> literal(sema, initializer)).toArray();
    }
    if (expression instanceof MemberSelectExpressionTree select && "class".equals(select.identifier().name())) {
      return select.expression().symbolType().symbol();
    }
    Symbol symbol = symbolOf(expression);
    if (symbol instanceof Symbol.VariableSymbol variable && !variable.isUnknown()) {
      return variable.constantValue().orElse(variable.isEnum() ? variable : null);
    }
    if (symbol != null && symbol.isUnknown()) {
      return null;
    }
    return expression.asConstant().orElse(null);
  }

  @Nullable
  private static Symbol symbolOf(ExpressionTree expression) {
    if (expression instanceof MemberSelectExpressionTree select) {
      return select.identifier().symbol();
    }
    if (expression instanceof IdentifierTree identifier) {
      return identifier.symbol();
    }
    return null;
  }

}
