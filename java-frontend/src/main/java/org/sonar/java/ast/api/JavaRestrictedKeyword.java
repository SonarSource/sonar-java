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
package org.sonar.java.ast.api;

import org.sonar.java.annotations.Beta;

import org.sonar.sslr.grammar.GrammarRuleKey;

/**
 * Restricted keywords for java grammar.
 *
 * JLS9 - §3.9
 *
 * @since Java 9
 */
@Beta
public enum JavaRestrictedKeyword implements GrammarRuleKey {
  OPEN("open"),
  MODULE("module"),
  REQUIRES("requires"),
  TRANSITIVE("transitive"),
  EXPORTS("exports"),
  OPENS("opens"),
  TO("to"),
  USES("uses"),
  PROVIDES("provides"),
  WITH("with");

  private final String value;

  JavaRestrictedKeyword(String word) {
    this.value = word;
  }

  public String getName() {
    return name();
  }

  public String getValue() {
    return value;
  }

  /**
   * Restricted keywords as String.
   * @return an array containing all restricted keywords as typed in Java
   */
  public static String[] restrictedKeywordValues() {
    JavaRestrictedKeyword[] restrictedKeywordsEnum = JavaRestrictedKeyword.values();
    String[] restrictedKeywords = new String[restrictedKeywordsEnum.length];
    for (int i = 0; i < restrictedKeywords.length; i++) {
      restrictedKeywords[i] = restrictedKeywordsEnum[i].getValue();
    }
    return restrictedKeywords;
  }

}
