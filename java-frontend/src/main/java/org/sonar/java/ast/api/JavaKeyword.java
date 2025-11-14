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
 * Keywords for java grammar.
 */
public enum JavaKeyword implements GrammarRuleKey {

  ASSERT("assert"),
  BREAK("break"),
  CASE("case"),
  CATCH("catch"),
  CLASS("class"),
  CONTINUE("continue"),
  DEFAULT("default"),
  DO("do"),
  ELSE("else"),
  ENUM("enum"),
  EXTENDS("extends"),
  FINALLY("finally"),
  FINAL("final"),
  FOR("for"),
  IF("if"),
  IMPLEMENTS("implements"),
  IMPORT("import"),
  INTERFACE("interface"),
  INSTANCEOF("instanceof"),
  NEW("new"),
  PACKAGE("package"),
  RETURN("return"),
  STATIC("static"),
  SUPER("super"),
  SWITCH("switch"),
  SYNCHRONIZED("synchronized"),
  THIS("this"),
  THROWS("throws"),
  THROW("throw"),
  TRY("try"),
  VOID("void"),
  WHILE("while"),
  TRUE("true"),
  FALSE("false"),
  NULL("null"),
  PUBLIC("public"),
  PROTECTED("protected"),
  PRIVATE("private"),
  ABSTRACT("abstract"),
  NATIVE("native"),
  TRANSIENT("transient"),
  VOLATILE("volatile"),
  STRICTFP("strictfp"),
  BYTE("byte"),
  SHORT("short"),
  CHAR("char"),
  INT("int"),
  LONG("long"),
  FLOAT("float"),
  DOUBLE("double"),
  BOOLEAN("boolean");

  private final String value;

  JavaKeyword(String value) {
    this.value = value;
  }

  public String getName() {
    return name();
  }

  public String getValue() {
    return value;
  }

  /**
   * keywords as String.
   * @return an array containing all keywords as typed in Java
   */
  public static String[] keywordValues() {
    JavaKeyword[] keywordsEnum = JavaKeyword.values();
    String[] keywords = new String[keywordsEnum.length];
    for (int i = 0; i < keywords.length; i++) {
      keywords[i] = keywordsEnum[i].getValue();
    }
    return keywords;
  }

}
