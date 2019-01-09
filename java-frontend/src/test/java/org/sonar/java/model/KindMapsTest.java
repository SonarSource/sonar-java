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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;

import static org.assertj.core.api.Assertions.assertThat;

public class KindMapsTest {

  private final KindMaps kindMaps = new KindMaps();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void literals() {
    assertThat(kindMaps.getLiteral(JavaTokenType.INTEGER_LITERAL)).isSameAs(Tree.Kind.INT_LITERAL);
    assertThat(kindMaps.getLiteral(JavaTokenType.LONG_LITERAL)).isSameAs(Tree.Kind.LONG_LITERAL);
    assertThat(kindMaps.getLiteral(JavaTokenType.FLOAT_LITERAL)).isSameAs(Tree.Kind.FLOAT_LITERAL);
    assertThat(kindMaps.getLiteral(JavaTokenType.DOUBLE_LITERAL)).isSameAs(Tree.Kind.DOUBLE_LITERAL);
    assertThat(kindMaps.getLiteral(JavaKeyword.TRUE)).isSameAs(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(kindMaps.getLiteral(JavaKeyword.FALSE)).isSameAs(Tree.Kind.BOOLEAN_LITERAL);
    assertThat(kindMaps.getLiteral(JavaTokenType.CHARACTER_LITERAL)).isSameAs(Tree.Kind.CHAR_LITERAL);
    assertThat(kindMaps.getLiteral(JavaTokenType.STRING_LITERAL)).isSameAs(Tree.Kind.STRING_LITERAL);
    assertThat(kindMaps.getLiteral(JavaKeyword.NULL)).isSameAs(Tree.Kind.NULL_LITERAL);

    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Mapping not found for literal null");
    kindMaps.getLiteral(null);
  }

  @Test
  public void modifiers() {
    assertThat(kindMaps.getModifier(JavaKeyword.PUBLIC)).isSameAs(Modifier.PUBLIC);
    assertThat(kindMaps.getModifier(JavaKeyword.PROTECTED)).isSameAs(Modifier.PROTECTED);
    assertThat(kindMaps.getModifier(JavaKeyword.PRIVATE)).isSameAs(Modifier.PRIVATE);
    assertThat(kindMaps.getModifier(JavaKeyword.ABSTRACT)).isSameAs(Modifier.ABSTRACT);
    assertThat(kindMaps.getModifier(JavaKeyword.STATIC)).isSameAs(Modifier.STATIC);
    assertThat(kindMaps.getModifier(JavaKeyword.FINAL)).isSameAs(Modifier.FINAL);
    assertThat(kindMaps.getModifier(JavaKeyword.TRANSIENT)).isSameAs(Modifier.TRANSIENT);
    assertThat(kindMaps.getModifier(JavaKeyword.VOLATILE)).isSameAs(Modifier.VOLATILE);
    assertThat(kindMaps.getModifier(JavaKeyword.SYNCHRONIZED)).isSameAs(Modifier.SYNCHRONIZED);
    assertThat(kindMaps.getModifier(JavaKeyword.NATIVE)).isSameAs(Modifier.NATIVE);
    assertThat(kindMaps.getModifier(JavaKeyword.STRICTFP)).isSameAs(Modifier.STRICTFP);

    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Mapping not found for modifier null");
    kindMaps.getModifier(null);
  }

  @Test
  public void prefix_operators() {
    assertThat(kindMaps.getPrefixOperator(JavaPunctuator.INC)).isSameAs(Tree.Kind.PREFIX_INCREMENT);
    assertThat(kindMaps.getPrefixOperator(JavaPunctuator.DEC)).isSameAs(Tree.Kind.PREFIX_DECREMENT);
    assertThat(kindMaps.getPrefixOperator(JavaPunctuator.PLUS)).isSameAs(Tree.Kind.UNARY_PLUS);
    assertThat(kindMaps.getPrefixOperator(JavaPunctuator.MINUS)).isSameAs(Tree.Kind.UNARY_MINUS);
    assertThat(kindMaps.getPrefixOperator(JavaPunctuator.TILDA)).isSameAs(Tree.Kind.BITWISE_COMPLEMENT);
    assertThat(kindMaps.getPrefixOperator(JavaPunctuator.BANG)).isSameAs(Tree.Kind.LOGICAL_COMPLEMENT);

    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Mapping not found for prefix operator null");
    kindMaps.getPrefixOperator(null);
  }

  @Test
  public void postfix_operators() {
    assertThat(kindMaps.getPostfixOperator(JavaPunctuator.INC)).isSameAs(Tree.Kind.POSTFIX_INCREMENT);
    assertThat(kindMaps.getPostfixOperator(JavaPunctuator.DEC)).isSameAs(Tree.Kind.POSTFIX_DECREMENT);

    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Mapping not found for postfix operator null");
    kindMaps.getPostfixOperator(null);
  }

  @Test
  public void binary_operators() {
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.STAR)).isSameAs(Tree.Kind.MULTIPLY);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.DIV)).isSameAs(Tree.Kind.DIVIDE);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.MOD)).isSameAs(Tree.Kind.REMAINDER);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.PLUS)).isSameAs(Tree.Kind.PLUS);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.MINUS)).isSameAs(Tree.Kind.MINUS);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.SL)).isSameAs(Tree.Kind.LEFT_SHIFT);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.SR)).isSameAs(Tree.Kind.RIGHT_SHIFT);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.BSR)).isSameAs(Tree.Kind.UNSIGNED_RIGHT_SHIFT);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.LT)).isSameAs(Tree.Kind.LESS_THAN);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.GT)).isSameAs(Tree.Kind.GREATER_THAN);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.LE)).isSameAs(Tree.Kind.LESS_THAN_OR_EQUAL_TO);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.GE)).isSameAs(Tree.Kind.GREATER_THAN_OR_EQUAL_TO);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.EQUAL)).isSameAs(Tree.Kind.EQUAL_TO);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.NOTEQUAL)).isSameAs(Tree.Kind.NOT_EQUAL_TO);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.AND)).isSameAs(Tree.Kind.AND);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.HAT)).isSameAs(Tree.Kind.XOR);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.OR)).isSameAs(Tree.Kind.OR);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.ANDAND)).isSameAs(Tree.Kind.CONDITIONAL_AND);
    assertThat(kindMaps.getBinaryOperator(JavaPunctuator.OROR)).isSameAs(Tree.Kind.CONDITIONAL_OR);

    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Mapping not found for binary operator null");
    kindMaps.getBinaryOperator(null);
  }

  @Test
  public void assignment_operators() {
    assertThat(kindMaps.getAssignmentOperator(JavaPunctuator.EQU)).isSameAs(Tree.Kind.ASSIGNMENT);
    assertThat(kindMaps.getAssignmentOperator(JavaPunctuator.STAREQU)).isSameAs(Tree.Kind.MULTIPLY_ASSIGNMENT);
    assertThat(kindMaps.getAssignmentOperator(JavaPunctuator.DIVEQU)).isSameAs(Tree.Kind.DIVIDE_ASSIGNMENT);
    assertThat(kindMaps.getAssignmentOperator(JavaPunctuator.MODEQU)).isSameAs(Tree.Kind.REMAINDER_ASSIGNMENT);
    assertThat(kindMaps.getAssignmentOperator(JavaPunctuator.PLUSEQU)).isSameAs(Tree.Kind.PLUS_ASSIGNMENT);
    assertThat(kindMaps.getAssignmentOperator(JavaPunctuator.MINUSEQU)).isSameAs(Tree.Kind.MINUS_ASSIGNMENT);
    assertThat(kindMaps.getAssignmentOperator(JavaPunctuator.SLEQU)).isSameAs(Tree.Kind.LEFT_SHIFT_ASSIGNMENT);
    assertThat(kindMaps.getAssignmentOperator(JavaPunctuator.SREQU)).isSameAs(Tree.Kind.RIGHT_SHIFT_ASSIGNMENT);
    assertThat(kindMaps.getAssignmentOperator(JavaPunctuator.BSREQU)).isSameAs(Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT);
    assertThat(kindMaps.getAssignmentOperator(JavaPunctuator.ANDEQU)).isSameAs(Tree.Kind.AND_ASSIGNMENT);
    assertThat(kindMaps.getAssignmentOperator(JavaPunctuator.HATEQU)).isSameAs(Tree.Kind.XOR_ASSIGNMENT);
    assertThat(kindMaps.getAssignmentOperator(JavaPunctuator.OREQU)).isSameAs(Tree.Kind.OR_ASSIGNMENT);

    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Mapping not found for assignment operator null");
    kindMaps.getAssignmentOperator(null);
  }

}
