/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.ast.api;

import org.sonar.sslr.grammar.GrammarRuleKey;

/**
 * Punctuators for java grammar.
 */
public enum JavaPunctuator implements GrammarRuleKey {

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

  JavaPunctuator(String word) {
    this.value = word;
  }

  public String getName() {
    return name();
  }

  public String getValue() {
    return value;
  }

}
