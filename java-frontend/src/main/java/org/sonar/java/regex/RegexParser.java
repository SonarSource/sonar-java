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
import org.sonar.java.regex.ast.JavaCharacter;
import org.sonar.java.regex.ast.PlainCharacterTree;
import org.sonar.java.regex.ast.Quantifier;
import org.sonar.java.regex.ast.RegexSource;
import org.sonar.java.regex.ast.RegexToken;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.java.regex.ast.RepetitionTree;
import org.sonar.java.regex.ast.SequenceTree;
import org.sonar.java.regex.ast.SimpleQuantifier;

import static org.sonar.java.regex.JavaCharacterParser.EOF;

public class RegexParser {

  private final RegexSource source;

  private final JavaCharacterParser characters;

  private final List<SyntaxError> errors;

  public RegexParser(RegexSource source) {
    this.source = source;
    this.characters = new JavaCharacterParser(source);
    this.errors = new ArrayList<>();
  }

  public RegexParseResult parse() {
    List<RegexTree> results = new ArrayList<>();
    do {
      RegexTree result = parseDisjunction();
      results.add(result);
      if (characters.isNotAtEnd()) {
        error("Unexpected '" + characters.getCurrent().getCharacter() + "'");
        characters.moveNext();
      }
    } while (characters.isNotAtEnd());
    RegexTree result = combineTrees(results, (range, elements) -> new SequenceTree(source, range, elements));
    return new RegexParseResult(result, errors);
  }

  private RegexTree parseDisjunction() {
    List<RegexTree> alternatives = new ArrayList<>();
    RegexTree first = parseSequence();
    alternatives.add(first);
    while (characters.currentIs('|')) {
      characters.moveNext();
      RegexTree next = parseSequence();
      alternatives.add(next);
    }
    return combineTrees(alternatives, (range, elements) -> new DisjunctionTree(source, range, elements));
  }

  private RegexTree parseSequence() {
    List<RegexTree> elements = new ArrayList<>();
    RegexTree element = parseRepetition();
    while (element != null) {
      elements.add(element);
      element = parseRepetition();
    }
    if (elements.isEmpty()) {
      int index = characters.getCurrentStartIndex();
      return new SequenceTree(source, new IndexRange(index, index), elements);
    } else {
      return combineTrees(elements, (range, items) -> new SequenceTree(source, range, items));
    }
  }

  @CheckForNull
  private RegexTree parseRepetition() {
    RegexTree element = parsePrimaryExpression();
    Quantifier quantifier = parseQuantifier();
    if (element == null) {
      if (quantifier != null) {
        errors.add(new SyntaxError(quantifier, "Unexpected quantifier '" + quantifier.getText() + "'"));
      }
      return null;
    }
    if (quantifier == null) {
      return element;
    } else {
      return new RepetitionTree(source, element.getRange().merge(quantifier.getRange()), element, quantifier);
    }
  }

  @CheckForNull
  private Quantifier parseQuantifier() {
    SimpleQuantifier.Kind kind;
    switch (characters.getCurrentChar()) {
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
    JavaCharacter current = characters.getCurrent();
    characters.moveNext();
    Quantifier.Modifier modifier = parseQuantifierModifier();
    IndexRange range = current.getRange().extendTo(characters.getCurrentStartIndex());
    return new SimpleQuantifier(source, range, modifier, kind);
  }

  CurlyBraceQuantifier parseCurlyBraceQuantifier() {
    JavaCharacter openingBrace = characters.getCurrent();
    characters.moveNext();
    RegexToken lowerBound = parseInteger();
    if (lowerBound == null) {
      expected("integer");
      return null;
    }
    RegexToken comma = null;
    RegexToken upperBound = null;
    if (characters.currentIs(',')) {
      comma = new RegexToken(source, characters.getCurrent().getRange());
      characters.moveNext();
      upperBound = parseInteger();
    }
    Quantifier.Modifier modifier;
    if (characters.currentIs('}')) {
      characters.moveNext();
    } else {
      if (comma == null) {
        expected("',' or '}'");
      } else if (upperBound == null) {
        expected("integer or '}'");
      } else {
        expected("'}'");
      }
    }
    modifier = parseQuantifierModifier();
    IndexRange range = openingBrace.getRange().extendTo(characters.getCurrentStartIndex());
    return new CurlyBraceQuantifier(source, range, modifier, lowerBound, comma, upperBound);
  }

  Quantifier.Modifier parseQuantifierModifier() {
    switch (characters.getCurrentChar()) {
      case '+':
        characters.moveNext();
        return Quantifier.Modifier.POSSESSIVE;
      case '?':
        characters.moveNext();
        return Quantifier.Modifier.LAZY;
      default:
        return Quantifier.Modifier.GREEDY;
    }
  }

  @CheckForNull
  private RegexToken parseInteger() {
    int startIndex = characters.getCurrentStartIndex();
    if (!isAsciiDigit(characters.getCurrentChar())) {
      return null;
    }
    while(isAsciiDigit(characters.getCurrentChar())) {
      characters.moveNext();
    }
    IndexRange range = new IndexRange(startIndex, characters.getCurrentStartIndex());
    return new RegexToken(source, range);
  }

  @CheckForNull
  private RegexTree parsePrimaryExpression() {
    if (characters.currentIs('(')) {
      return parseGroup();
    } else if (characters.currentIs('\\')) {
      return parseEscapeSequence();
    } else if (isPlainTextCharacter(characters.getCurrentChar())) {
      JavaCharacter character = characters.getCurrent();
      characters.moveNext();
      return new PlainCharacterTree(source, character.getRange(), character);
    } else {
      return null;
    }
  }

  private GroupTree parseGroup() {
    JavaCharacter openingParen = characters.getCurrent();
    characters.moveNext();
    RegexTree inner = parseDisjunction();
    if (characters.currentIs(')')) {
      characters.moveNext();
    } else {
      expected("')'");
    }
    IndexRange range = openingParen.getRange().extendTo(characters.getCurrentStartIndex());
    return new GroupTree(source, range, inner);
  }

  private RegexTree parseEscapeSequence() {
    JavaCharacter backslash = characters.getCurrent();
    characters.moveNext();
    if (characters.isAtEnd()) {
      expected("any character");
      return new PlainCharacterTree(source, backslash.getRange(), backslash);
    } else {
      // TODO: Properly handle escape sequences that aren't escaped metacharacters
      JavaCharacter character = characters.getCurrent();
      characters.moveNext();
      return new PlainCharacterTree(source, backslash.getRange().merge(character.getRange()), character);
    }
  }

  private void expected(String expectedToken) {
    String actual = characters.isAtEnd() ? "the end of the regex" : ("'" + characters.getCurrent().getCharacter() + "'");
    error("Expected " + expectedToken + ", but found " + actual);
  }

  private void error(String message) {
    IndexRange range = characters.getCurrentIndexRange();
    RegexToken offendingToken = new RegexToken(source, range);
    errors.add(new SyntaxError(offendingToken, message));
  }

  private static RegexTree combineTrees(List<RegexTree> elements, TreeConstructor treeConstructor) {
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

  private static boolean isAsciiDigit(int c) {
    return '0' <= c && c <= '9';
  }

  private static boolean isPlainTextCharacter(int c) {
    switch (c) {
      case EOF:
      case '(':
      case ')':
      case '{':
      case '\\':
      case '*':
      case '+':
      case '?':
      case '|':
      case '[':
      case '.':
        return false;
      default:
        return true;
    }
  }

}
