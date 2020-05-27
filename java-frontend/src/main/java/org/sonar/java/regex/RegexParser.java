/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.regex;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.java.regex.ast.CurlyBraceQuantifier;
import org.sonar.java.regex.ast.DisjunctionTree;
import org.sonar.java.regex.ast.GroupTree;
import org.sonar.java.regex.ast.IndexRange;
import org.sonar.java.regex.ast.PlainTextTree;
import org.sonar.java.regex.ast.Quantifier;
import org.sonar.java.regex.ast.RegexSource;
import org.sonar.java.regex.ast.RegexToken;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.java.regex.ast.RepetitionTree;
import org.sonar.java.regex.ast.SequenceTree;
import org.sonar.java.regex.ast.SimpleQuantifier;

public class RegexParser {

  private final RegexSource source;

  private final String sourceText;

  private int index;

  private final List<SyntaxError> errors;

  public RegexParser(RegexSource source) {
    this.source = source;
    this.sourceText = source.getSourceText();
    this.index = 0;
    this.errors = new ArrayList<>();
  }

  public RegexParseResult parse() {
    List<RegexTree> results = new ArrayList<>();
    while (index < sourceText.length()) {
      RegexTree result = parseDisjunction();
      results.add(result);
      if (index < sourceText.length()) {
        char offendingChar = sourceText.charAt(index);
        error("Unexpected '" + offendingChar + "'");
        index++;
      }
    }
    RegexTree result = combineTrees(results, (range, elements) -> new SequenceTree(source, range, elements));
    return new RegexParseResult(result, errors);
  }

  private RegexTree parseDisjunction() {
    List<RegexTree> alternatives = new ArrayList<>();
    RegexTree first = parseRepetition();
    alternatives.add(first);
    while (currentChar() == '|') {
      index++;
      RegexTree next = parseRepetition();
      alternatives.add(next);
    }
    return combineTrees(alternatives, (range, elements) -> new DisjunctionTree(source, range, elements));
  }

  private RegexTree parseRepetition() {
    RegexTree element = parseSequence();
    Quantifier quantifier = parseQuantifier();
    if (quantifier == null) {
      return element;
    } else {
      return new RepetitionTree(source, element.getRange().merge(quantifier.getRange()), element, quantifier);
    }
  }

  @CheckForNull
  private Quantifier parseQuantifier() {
    int startIndex = index;
    SimpleQuantifier.Kind kind;
    switch (currentChar()) {
      case '*':
        kind = SimpleQuantifier.Kind.STAR;
        break;
      case '+':
        kind = SimpleQuantifier.Kind.PLUS;
        break;
      case '?':
        kind = SimpleQuantifier.Kind.QUESTION_MARK;
        break;
      case '{':
        return parseCurlyBraceQuantifier();
      default:
        return null;
    }
    index++;
    Quantifier.Modifier modifier = parseQuantifierModifier();
    return new SimpleQuantifier(source, new IndexRange(startIndex, index), modifier, kind);
  }

  CurlyBraceQuantifier parseCurlyBraceQuantifier() {
    int startIndex = index;
    // Discard '{'
    index++;
    RegexToken lowerBound = parseInteger();
    if (lowerBound == null) {
      error("Integer expected");
      return null;
    }
    RegexToken comma = null;
    RegexToken upperBound = null;
    if (currentChar() == ',') {
      comma = new RegexToken(source, new IndexRange(index, index + 1));
      index++;
      upperBound = parseInteger();
    }
    Quantifier.Modifier modifier;
    if (currentChar() == '}') {
      index++;
      modifier = parseQuantifierModifier();
    } else {
      error("'}' expected");
      modifier = Quantifier.Modifier.GREEDY;
    }
    IndexRange range = new IndexRange(startIndex, index);
    return new CurlyBraceQuantifier(source, range, modifier, lowerBound, comma, upperBound);
  }

  Quantifier.Modifier parseQuantifierModifier() {
    switch (currentChar()) {
      case '+':
        index++;
        return Quantifier.Modifier.POSSESSIVE;
      case '?':
        index++;
        return Quantifier.Modifier.LAZY;
      default:
        return Quantifier.Modifier.GREEDY;
    }
  }

  @CheckForNull
  private RegexToken parseInteger() {
    int startIndex = index;
    if (!isAsciiDigit(currentChar())) {
      return null;
    }
    while(isAsciiDigit(currentChar())) {
      index++;
    }
    IndexRange range = new IndexRange(startIndex, index);
    return new RegexToken(source, range);
  }

  private RegexTree parseSequence() {
    int startIndex = index;
    List<RegexTree> elements = new ArrayList<>();
    RegexTree element = parsePrimaryExpression();
    do {
      elements.add(element);
      element = parsePrimaryExpression();
    } while (!element.getRange().isEmpty());
    if (elements.size() == 1) {
      return elements.get(0);
    } else {
      return new SequenceTree(source, new IndexRange(startIndex, index), elements);
    }
  }

  private RegexTree parsePrimaryExpression() {
    if (currentChar() == '(') {
      return parseGroup();
    } else {
      return parsePlainText();
    }
  }

  private GroupTree parseGroup() {
    int startIndex = index;
    // Discard '('
    index++;
    RegexTree inner = parseDisjunction();
    if (currentChar() == ')') {
      index++;
    } else {
      error("')' expected");
    }
    IndexRange range = new IndexRange(startIndex, index);
    return new GroupTree(source, range, inner);
  }

  private PlainTextTree parsePlainText() {
    int startIndex = index;
    while (isPlainTextCharacter(currentChar())) {
      index++;
    }
    return new PlainTextTree(new RegexToken(source, new IndexRange(startIndex, index)));
  }

  private char currentChar() {
    return sourceText.charAt(index);
  }

  private void error(String message) {
    errors.add(new SyntaxError(source.locationsFor(index, index + 1), message));
  }

  private RegexTree combineTrees(List<RegexTree> elements, TreeConstructor treeConstructor) {
    if (elements.size() == 1) {
      return elements.get(0);
    } else {
      IndexRange range = elements.get(0).getRange().merge(elements.get(elements.size() - 1).getRange());
      return treeConstructor.construct(range, elements);
    }
  }

  private interface TreeConstructor {
    RegexTree construct(IndexRange range, List<RegexTree> elements);
  }

  private static boolean isAsciiDigit(char c) {
    return '0' <= c && c <= '9';
  }

  private static boolean isPlainTextCharacter(char c) {
    // TODO: Amend this when more syntax is supported
    switch (c) {
      case '(':
      case ')':
      case '{':
        return true;
      default:
        return false;
    }
  }

}
