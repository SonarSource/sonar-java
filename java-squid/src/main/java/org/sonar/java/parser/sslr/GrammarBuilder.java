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
package org.sonar.java.parser.sslr;

import com.sonar.sslr.api.AstNode;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.sslr.grammar.GrammarRuleKey;

import java.util.List;

public interface GrammarBuilder {

  <T> NonterminalBuilder<T> nonterminal();

  <T> NonterminalBuilder<T> nonterminal(GrammarRuleKey ruleKey);

  <T> T firstOf(T... methods);

  <T> Optional<T> optional(T method);

  <T> List<T> oneOrMore(T method);

  <T> Optional<List<T>> zeroOrMore(T method);

  JavaTree invokeRule(GrammarRuleKey ruleKey);
  InternalSyntaxToken invokeRule(JavaPunctuator ruleKey);

  AstNode token(String value);

}
