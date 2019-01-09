/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.sslr.grammar.GrammarRuleKey;

import java.util.Map;

public final class KindMaps {

  private final Map<JavaKeyword, Modifier> modifiers = Maps.newEnumMap(JavaKeyword.class);
  private final Map<JavaPunctuator, Tree.Kind> prefixOperators = Maps.newEnumMap(JavaPunctuator.class);
  private final Map<JavaPunctuator, Tree.Kind> postfixOperators = Maps.newEnumMap(JavaPunctuator.class);
  private final Map<JavaPunctuator, Tree.Kind> binaryOperators = Maps.newEnumMap(JavaPunctuator.class);
  private final Map<JavaPunctuator, Tree.Kind> assignmentOperators = Maps.newEnumMap(JavaPunctuator.class);
  private final Map<GrammarRuleKey, Tree.Kind> literals;

  public KindMaps() {
    ImmutableMap.Builder<GrammarRuleKey, Tree.Kind> literalsBuilder = ImmutableMap.builder();
    literalsBuilder.put(JavaTokenType.INTEGER_LITERAL, Tree.Kind.INT_LITERAL);
    literalsBuilder.put(JavaTokenType.LONG_LITERAL, Tree.Kind.LONG_LITERAL);
    literalsBuilder.put(JavaTokenType.FLOAT_LITERAL, Tree.Kind.FLOAT_LITERAL);
    literalsBuilder.put(JavaTokenType.DOUBLE_LITERAL, Tree.Kind.DOUBLE_LITERAL);
    literalsBuilder.put(JavaKeyword.TRUE, Tree.Kind.BOOLEAN_LITERAL);
    literalsBuilder.put(JavaKeyword.FALSE, Tree.Kind.BOOLEAN_LITERAL);
    literalsBuilder.put(JavaTokenType.CHARACTER_LITERAL, Tree.Kind.CHAR_LITERAL);
    literalsBuilder.put(JavaTokenType.STRING_LITERAL, Tree.Kind.STRING_LITERAL);
    literalsBuilder.put(JavaKeyword.NULL, Tree.Kind.NULL_LITERAL);
    this.literals = literalsBuilder.build();

    modifiers.put(JavaKeyword.PUBLIC, Modifier.PUBLIC);
    modifiers.put(JavaKeyword.PROTECTED, Modifier.PROTECTED);
    modifiers.put(JavaKeyword.PRIVATE, Modifier.PRIVATE);
    modifiers.put(JavaKeyword.ABSTRACT, Modifier.ABSTRACT);
    modifiers.put(JavaKeyword.STATIC, Modifier.STATIC);
    modifiers.put(JavaKeyword.FINAL, Modifier.FINAL);
    modifiers.put(JavaKeyword.TRANSIENT, Modifier.TRANSIENT);
    modifiers.put(JavaKeyword.VOLATILE, Modifier.VOLATILE);
    modifiers.put(JavaKeyword.SYNCHRONIZED, Modifier.SYNCHRONIZED);
    modifiers.put(JavaKeyword.NATIVE, Modifier.NATIVE);
    modifiers.put(JavaKeyword.DEFAULT, Modifier.DEFAULT);
    modifiers.put(JavaKeyword.STRICTFP, Modifier.STRICTFP);

    prefixOperators.put(JavaPunctuator.INC, Tree.Kind.PREFIX_INCREMENT);
    prefixOperators.put(JavaPunctuator.DEC, Tree.Kind.PREFIX_DECREMENT);
    prefixOperators.put(JavaPunctuator.PLUS, Tree.Kind.UNARY_PLUS);
    prefixOperators.put(JavaPunctuator.MINUS, Tree.Kind.UNARY_MINUS);
    prefixOperators.put(JavaPunctuator.TILDA, Tree.Kind.BITWISE_COMPLEMENT);
    prefixOperators.put(JavaPunctuator.BANG, Tree.Kind.LOGICAL_COMPLEMENT);

    postfixOperators.put(JavaPunctuator.INC, Tree.Kind.POSTFIX_INCREMENT);
    postfixOperators.put(JavaPunctuator.DEC, Tree.Kind.POSTFIX_DECREMENT);

    binaryOperators.put(JavaPunctuator.STAR, Tree.Kind.MULTIPLY);
    binaryOperators.put(JavaPunctuator.DIV, Tree.Kind.DIVIDE);
    binaryOperators.put(JavaPunctuator.MOD, Tree.Kind.REMAINDER);
    binaryOperators.put(JavaPunctuator.PLUS, Tree.Kind.PLUS);
    binaryOperators.put(JavaPunctuator.MINUS, Tree.Kind.MINUS);
    binaryOperators.put(JavaPunctuator.SL, Tree.Kind.LEFT_SHIFT);
    binaryOperators.put(JavaPunctuator.SR, Tree.Kind.RIGHT_SHIFT);
    binaryOperators.put(JavaPunctuator.BSR, Tree.Kind.UNSIGNED_RIGHT_SHIFT);
    binaryOperators.put(JavaPunctuator.LT, Tree.Kind.LESS_THAN);
    binaryOperators.put(JavaPunctuator.GT, Tree.Kind.GREATER_THAN);
    binaryOperators.put(JavaPunctuator.LE, Tree.Kind.LESS_THAN_OR_EQUAL_TO);
    binaryOperators.put(JavaPunctuator.GE, Tree.Kind.GREATER_THAN_OR_EQUAL_TO);
    binaryOperators.put(JavaPunctuator.EQUAL, Tree.Kind.EQUAL_TO);
    binaryOperators.put(JavaPunctuator.NOTEQUAL, Tree.Kind.NOT_EQUAL_TO);
    binaryOperators.put(JavaPunctuator.AND, Tree.Kind.AND);
    binaryOperators.put(JavaPunctuator.HAT, Tree.Kind.XOR);
    binaryOperators.put(JavaPunctuator.OR, Tree.Kind.OR);
    binaryOperators.put(JavaPunctuator.ANDAND, Tree.Kind.CONDITIONAL_AND);
    binaryOperators.put(JavaPunctuator.OROR, Tree.Kind.CONDITIONAL_OR);

    assignmentOperators.put(JavaPunctuator.EQU, Tree.Kind.ASSIGNMENT);
    assignmentOperators.put(JavaPunctuator.PLUSEQU, Tree.Kind.PLUS_ASSIGNMENT);
    assignmentOperators.put(JavaPunctuator.STAREQU, Tree.Kind.MULTIPLY_ASSIGNMENT);
    assignmentOperators.put(JavaPunctuator.DIVEQU, Tree.Kind.DIVIDE_ASSIGNMENT);
    assignmentOperators.put(JavaPunctuator.MODEQU, Tree.Kind.REMAINDER_ASSIGNMENT);
    assignmentOperators.put(JavaPunctuator.PLUSEQU, Tree.Kind.PLUS_ASSIGNMENT);
    assignmentOperators.put(JavaPunctuator.MINUSEQU, Tree.Kind.MINUS_ASSIGNMENT);
    assignmentOperators.put(JavaPunctuator.SLEQU, Tree.Kind.LEFT_SHIFT_ASSIGNMENT);
    assignmentOperators.put(JavaPunctuator.SREQU, Tree.Kind.RIGHT_SHIFT_ASSIGNMENT);
    assignmentOperators.put(JavaPunctuator.BSREQU, Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT);
    assignmentOperators.put(JavaPunctuator.ANDEQU, Tree.Kind.AND_ASSIGNMENT);
    assignmentOperators.put(JavaPunctuator.HATEQU, Tree.Kind.XOR_ASSIGNMENT);
    assignmentOperators.put(JavaPunctuator.OREQU, Tree.Kind.OR_ASSIGNMENT);
  }

  public Modifier getModifier(JavaKeyword keyword) {
    return Preconditions.checkNotNull(modifiers.get(keyword), "Mapping not found for modifier %s", keyword);
  }

  public Tree.Kind getPrefixOperator(JavaPunctuator punctuator) {
    return Preconditions.checkNotNull(prefixOperators.get(punctuator), "Mapping not found for prefix operator %s", punctuator);
  }

  public Tree.Kind getPostfixOperator(JavaPunctuator punctuator) {
    return Preconditions.checkNotNull(postfixOperators.get(punctuator), "Mapping not found for postfix operator %s", punctuator);
  }

  public Tree.Kind getBinaryOperator(JavaPunctuator punctuator) {
    return Preconditions.checkNotNull(binaryOperators.get(punctuator), "Mapping not found for binary operator %s", punctuator);
  }

  public Tree.Kind getAssignmentOperator(JavaPunctuator punctuator) {
    return Preconditions.checkNotNull(assignmentOperators.get(punctuator), "Mapping not found for assignment operator %s", punctuator);
  }

  public Tree.Kind getLiteral(GrammarRuleKey grammarRuleKey) {
    return Preconditions.checkNotNull(literals.get(grammarRuleKey), "Mapping not found for literal %s", grammarRuleKey);
  }

}
