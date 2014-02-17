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
package org.sonar.java.ast.api;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.TokenType;
import org.sonar.sslr.grammar.GrammarRuleKey;

public enum JavaPunctuator implements TokenType, GrammarRuleKey {

  AT("@"),
  AND("&"),
  ANDAND("&&"),
  ANDEQU("&="),
  BANG("!"),
  BSR(">>>"),
  BSREQU(">>>="),
  COLON(":"),
  DBLECOLON("::"),
  COMMA(","),
  DEC("--"),
  DIV("/"),
  DIVEQU("/="),
  DOT("."),
  ELLIPSIS("..."),
  EQU("="),
  EQUAL("=="),
  GE(">="),
  GT(">"),
  HAT("^"),
  HATEQU("^="),
  INC("++"),
  LBRK("["),
  LT("<"),
  LE("<="),
  LPAR("("),
  LWING("{"),
  MINUS("-"),
  MINUSEQU("-="),
  MOD("%"),
  MODEQU("%="),
  NOTEQUAL("!="),
  OR("|"),
  OREQU("|="),
  OROR("||"),
  PLUS("+"),
  PLUSEQU("+="),
  QUERY("?"),
  RBRK("]"),
  RPAR(")"),
  RWING("}"),
  SEMI(";"),
  SL("<<"),
  SLEQU("<<="),
  SR(">>"),
  SREQU(">>="),
  STAR("*"),
  STAREQU("*="),
  TILDA("~"),

  LPOINT("<"),
  RPOINT(">"),

  ARROW("->"),
  ;

  private final String value;

  private JavaPunctuator(String word) {
    this.value = word;
  }

  public String getName() {
    return name();
  }

  public String getValue() {
    return value;
  }

  public boolean hasToBeSkippedFromAst(AstNode node) {
    return false;
  }

}
