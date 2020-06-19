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
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.regex.ast.AtomicGroupTree;
import org.sonar.java.regex.ast.BackReferenceTree;
import org.sonar.java.regex.ast.BoundaryTree;
import org.sonar.java.regex.ast.CapturingGroupTree;
import org.sonar.java.regex.ast.CharacterClassIntersectionTree;
import org.sonar.java.regex.ast.CharacterClassTree;
import org.sonar.java.regex.ast.CharacterClassUnionTree;
import org.sonar.java.regex.ast.CharacterRangeTree;
import org.sonar.java.regex.ast.CurlyBraceQuantifier;
import org.sonar.java.regex.ast.DisjunctionTree;
import org.sonar.java.regex.ast.DotTree;
import org.sonar.java.regex.ast.EscapedCharacterClassTree;
import org.sonar.java.regex.ast.FlagSet;
import org.sonar.java.regex.ast.GroupTree;
import org.sonar.java.regex.ast.IndexRange;
import org.sonar.java.regex.ast.JavaCharacter;
import org.sonar.java.regex.ast.LookAroundTree;
import org.sonar.java.regex.ast.NonCapturingGroupTree;
import org.sonar.java.regex.ast.PlainCharacterTree;
import org.sonar.java.regex.ast.Quantifier;
import org.sonar.java.regex.ast.RegexSource;
import org.sonar.java.regex.ast.RegexToken;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.java.regex.ast.RepetitionTree;
import org.sonar.java.regex.ast.SequenceTree;
import org.sonar.java.regex.ast.SimpleQuantifier;

import static org.sonar.java.regex.RegexLexer.EOF;

public class RegexParser {

  private final RegexSource source;

  private final RegexLexer characters;

  private final FlagSet initialFlags;

  private final List<SyntaxError> errors = new ArrayList<>();

  private int groupNumber = 1;

  public RegexParser(RegexSource source, FlagSet initialFlags) {
    this.source = source;
    this.characters = new RegexLexer(source);
    this.characters.setFreeSpacingMode(initialFlags.contains(Pattern.COMMENTS));
    this.initialFlags = initialFlags;
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
    return new RegexParseResult(result, initialFlags, errors);
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
        return Quantifier.Modifier.RELUCTANT;
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
    switch (characters.getCurrentChar()) {
      case '(':
        return parseGroup();
      case '\\':
        return parseEscapeSequence();
      case '[':
        return parseCharacterClass();
      case '.':
        DotTree tree = new DotTree(source, characters.getCurrentIndexRange());
        characters.moveNext();
        return tree;
      case '^':
        BoundaryTree lineStart = new BoundaryTree(source, BoundaryTree.Type.LINE_START, characters.getCurrentIndexRange());
        characters.moveNext();
        return lineStart;
      case '$':
        BoundaryTree lineEnd = new BoundaryTree(source, BoundaryTree.Type.LINE_END, characters.getCurrentIndexRange());
        characters.moveNext();
        return lineEnd;
      default:
        if (isPlainTextCharacter(characters.getCurrentChar())) {
          JavaCharacter character = characters.getCurrent();
          characters.moveNext();
          return plainCharacter(character);
        } else {
          return null;
        }
    }
  }

  private GroupTree parseGroup() {
    JavaCharacter openingParen = characters.getCurrent();
    characters.moveNext();
    if (characters.currentIs("?=")) {
      characters.moveNext(2);
      return finishGroup(openingParen, (range, inner) -> LookAroundTree.positiveLookAhead(source, range, inner));
    } else if (characters.currentIs("?<=")) {
      characters.moveNext(3);
      return finishGroup(openingParen, (range, inner) -> LookAroundTree.positiveLookBehind(source, range, inner));
    } else if (characters.currentIs("?!")) {
      characters.moveNext(2);
      return finishGroup(openingParen, (range, inner) -> LookAroundTree.negativeLookAhead(source, range, inner));
    } else if (characters.currentIs("?<!")) {
      characters.moveNext(3);
      return finishGroup(openingParen, (range, inner) -> LookAroundTree.negativeLookBehind(source, range, inner));
    } else if (characters.currentIs("?>")) {
      characters.moveNext(2);
      return finishGroup(openingParen, (range, inner) -> new AtomicGroupTree(source, range, inner));
    } else if (characters.currentIs("?<")) {
      characters.moveNext(2);
      String name = parseGroupName();
      if (characters.currentIs('>')) {
        characters.moveNext();
      } else {
        expected("'>'");
      }
      return finishGroup(openingParen, newCapturingGroup(name));
    } else if (characters.currentIs("?")) {
      return parseNonCapturingGroup(openingParen);
    } else {
      return finishGroup(openingParen, newCapturingGroup(null));
    }
  }

  private GroupConstructor newCapturingGroup(@Nullable String name) {
    int index = groupNumber;
    groupNumber++;
    return (range, inner) -> new CapturingGroupTree(source, range, name, index, inner);
  }

  private String parseGroupName() {
    StringBuilder sb = new StringBuilder();
    while (characters.isNotAtEnd() && !characters.currentIs('>')) {
      sb.append(characters.getCurrent().getCharacter());
      characters.moveNext();
    }
    String name = sb.toString();
    if (name.isEmpty()) {
      expected("a name for the group");
    }
    return name;
  }

  private GroupTree parseNonCapturingGroup(JavaCharacter openingParen) {
    // Discard '?'
    characters.moveNext();
    FlagSet enabledFlags = parseFlags();
    FlagSet disabledFlags;
    if (characters.currentIs('-')) {
      characters.moveNext();
      disabledFlags = parseFlags();
    } else {
      disabledFlags = new FlagSet();
    }

    boolean previousFreeSpacingMode = characters.getFreeSpacingMode();
    if (disabledFlags.contains(Pattern.COMMENTS)) {
      characters.setFreeSpacingMode(false);
    } else if (enabledFlags.contains(Pattern.COMMENTS)) {
      characters.setFreeSpacingMode(true);
    }

    if (characters.currentIs(')')) {
      JavaCharacter closingParen = characters.getCurrent();
      characters.moveNext();
      IndexRange range = openingParen.getRange().merge(closingParen.getRange());
      return new NonCapturingGroupTree(source, range, enabledFlags, disabledFlags, null);
    }
    if (characters.currentIs(':')) {
      characters.moveNext();
    } else {
      expected("flag or ':' or ')'");
    }
    return finishGroup(previousFreeSpacingMode, openingParen, (range, inner) ->
      new NonCapturingGroupTree(source, range, enabledFlags, disabledFlags, inner)
    );
  }

  private FlagSet parseFlags() {
    FlagSet flags = new FlagSet();
    while (characters.isNotAtEnd()) {
      Integer flag = parseFlag(characters.getCurrent().getCharacter());
      if (flag == null) {
        break;
      }
      flags.add(flag, characters.getCurrent());
      characters.moveNext();
    }
    return flags;
  }

  @CheckForNull
  private static Integer parseFlag(char ch) {
    switch (ch) {
      case 'i':
        return Pattern.CASE_INSENSITIVE;
      case 'd':
        return Pattern.UNIX_LINES;
      case 'm':
        return Pattern.MULTILINE;
      case 's':
        return Pattern.DOTALL;
      case 'u':
        return Pattern.UNICODE_CASE;
      case 'x':
        return Pattern.COMMENTS;
      case 'U':
        return Pattern.UNICODE_CHARACTER_CLASS;
      default:
        return null;
    }
  }

  private GroupTree finishGroup(JavaCharacter openingParen, GroupConstructor groupConstructor) {
    return finishGroup(characters.getFreeSpacingMode(), openingParen, groupConstructor);
  }

  private GroupTree finishGroup(boolean previousFreeSpacingMode, JavaCharacter openingParen, GroupConstructor groupConstructor) {
    RegexTree inner = parseDisjunction();
    characters.setFreeSpacingMode(previousFreeSpacingMode);
    if (characters.currentIs(')')) {
      characters.moveNext();
    } else {
      expected("')'");
    }
    IndexRange range = openingParen.getRange().extendTo(characters.getCurrentStartIndex());
    return groupConstructor.construct(range, inner);
  }

  private RegexTree parseEscapeSequence() {
    JavaCharacter backslash = characters.getCurrent();
    characters.moveNext();
    if (characters.isAtEnd()) {
      expected("any character");
      return plainCharacter(backslash);
    } else {
      JavaCharacter character = characters.getCurrent();
      switch (character.getCharacter()) {
        case 'p':
        case 'P':
          return parseEscapedProperty(backslash);
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
          return parseNumericalBackReference(backslash);
        case 'k':
          return parseNamedBackReference(backslash);
        case 'b':
        case 'B':
        case 'A':
        case 'G':
        case 'Z':
        case 'z':
          return parseBoundary(backslash);
        case 'w':
        case 'W':
        case 'd':
        case 'D':
        case 'S':
        case 's':
        case 'h':
        case 'H':
        case 'v':
        case 'V':
          return parseEscapedCharacterClass(backslash);
        default:
          // TODO other kind of escape sequences such as quotations, special characters, N, x, u, R or X
          characters.moveNext();
          return new PlainCharacterTree(source, backslash.getRange().merge(character.getRange()), character);
      }
    }
  }

  private RegexTree parseEscapedCharacterClass(JavaCharacter backslash) {
    RegexTree result = new EscapedCharacterClassTree(source, backslash, characters.getCurrent());
    characters.moveNext();
    return result;
  }

  private RegexTree parseEscapedProperty(JavaCharacter backslash) {
    return parseEscapedSequence('{', '}', "a property name", dh -> new EscapedCharacterClassTree(source, backslash, dh.marker, dh.opener, dh.closer));
  }

  private RegexTree parseNamedBackReference(JavaCharacter backslash) {
    return parseEscapedSequence('<', '>', "a group name", dh -> new BackReferenceTree(source, backslash, dh.marker, dh.opener, dh.closer));
  }

  private RegexTree parseEscapedSequence(char opener, char closer, String expected, Function<EscapedSequenceDataHolder, RegexTree> builder) {
    JavaCharacter marker = characters.getCurrent();
    characters.moveNext();

    if (!characters.currentIs(opener)) {
      expected(("'" + opener + "'"));
      return plainCharacter(marker);
    }
    JavaCharacter openerChar = characters.getCurrent();
    boolean atLeastOneChar = false;
    do {
      characters.moveNext();
      if (characters.isAtEnd()) {
        expected(atLeastOneChar ? ("'" + closer + "'") : expected);
        return plainCharacter(openerChar);
      }
      if (!atLeastOneChar && characters.currentIs(closer)) {
        expected(expected);
        return plainCharacter(openerChar);
      }
      atLeastOneChar = true;
    } while (!characters.currentIs(closer));
    JavaCharacter closerChar = characters.getCurrent();
    characters.moveNext();
    return builder.apply(new EscapedSequenceDataHolder(marker, openerChar, closerChar));
  }

  private static final class EscapedSequenceDataHolder {
    private final JavaCharacter marker;
    private final JavaCharacter opener;
    private final JavaCharacter closer;

    private EscapedSequenceDataHolder(JavaCharacter marker, JavaCharacter opener, JavaCharacter closer) {
      this.marker = marker;
      this.opener = opener;
      this.closer = closer;
    }
  }

  private RegexTree parseNumericalBackReference(JavaCharacter backslash) {
    JavaCharacter firstDigit = characters.getCurrent();
    JavaCharacter lastDigit = firstDigit;
    // TODO If there are multiple digits and the first one is 0, it's an octal escape, not a back reference
    do {
      characters.moveNext();
      if (!characters.isAtEnd()) {
        JavaCharacter currentChar = characters.getCurrent();
        char asChar = currentChar.getCharacter();
        if (asChar >= '0' && asChar <= '9') {
          lastDigit = currentChar;
        } else {
          break;
        }
      }
    } while (!characters.isAtEnd());
    return new BackReferenceTree(source, backslash, null, firstDigit, lastDigit);
  }

  private RegexTree parseBoundary(JavaCharacter backslash) {
    if (characters.currentIs("b{")) {
      return parseEscapedSequence(
        '{',
        '}',
        "an Unicode extended grapheme cluster",
        dh -> new BoundaryTree(source, BoundaryTree.Type.UNICODE_EXTENDED_GRAPHEME_CLUSTER, backslash.getRange().merge(dh.closer.getRange())));
    }
    JavaCharacter boundary = characters.getCurrent();
    characters.moveNext();
    return new BoundaryTree(source, BoundaryTree.Type.forKey(boundary.getCharacter()), backslash.getRange().merge(boundary.getRange()));
  }

  private RegexTree parseCharacterClass() {
    JavaCharacter openingBracket = characters.getCurrent();
    characters.moveNext();
    boolean negated = false;
    if (characters.currentIs('^')) {
      characters.moveNext();
      negated = true;
    }
    RegexTree contents = parseCharacterClassIntersection();
    if (characters.currentIs(']')) {
      characters.moveNext();
    } else {
      expected("']'");
    }
    IndexRange range = openingBracket.getRange().extendTo(characters.getCurrentStartIndex());
    return new CharacterClassTree(source, range, negated, contents);
  }

  private RegexTree parseCharacterClassIntersection() {
    List<RegexTree> elements = new ArrayList<>();
    elements.add(parseCharacterClassUnion(true));
    while (characters.currentIs("&&")) {
      characters.moveNext(2);
      elements.add(parseCharacterClassUnion(false));
    }
    return combineTrees(elements, (range, items) -> new CharacterClassIntersectionTree(source, range, items));
  }

  private RegexTree parseCharacterClassUnion(boolean isAtBeginning) {
    List<RegexTree> elements = new ArrayList<>();
    RegexTree element = parseCharacterClassElement(isAtBeginning);
    while (element != null) {
      elements.add(element);
      element = parseCharacterClassElement(false);
    }
    if (elements.isEmpty()) {
      IndexRange range = new IndexRange(characters.getCurrentStartIndex(), characters.getCurrentStartIndex());
      return new CharacterClassUnionTree(source, range, elements);
    } else {
      return combineTrees(elements, (range, items) -> new CharacterClassUnionTree(source, range, items));
    }
  }

  @CheckForNull
  private RegexTree parseCharacterClassElement(boolean isAtBeginning) {
    if (characters.isAtEnd() || characters.currentIs("&&")) {
      return null;
    }
    JavaCharacter startCharacter = characters.getCurrent();
    switch (startCharacter.getCharacter()) {
      case '\\':
        RegexTree escape = parseEscapeSequence();
        if (escape.is(RegexTree.Kind.PLAIN_CHARACTER)) {
          return parseCharacterRange(((PlainCharacterTree)escape).getContents());
        } else {
          return escape;
        }
      case '[':
        return parseCharacterClass();
      case ']':
        if (isAtBeginning) {
          characters.moveNext();
          return parseCharacterRange(startCharacter);
        } else {
          return null;
        }
      default:
        characters.moveNext();
        return parseCharacterRange(startCharacter);
    }
  }

  private RegexTree parseCharacterRange(JavaCharacter startCharacter) {
    if (characters.currentIs('-')) {
      int lookAhead = characters.lookAhead(1);
      if (lookAhead == EOF || lookAhead == ']') {
        return plainCharacter(startCharacter);
      } else if (lookAhead == '\\') {
        characters.moveNext();
        JavaCharacter backslash = characters.getCurrent();
        RegexTree escape = parseEscapeSequence();
        if (escape.is(RegexTree.Kind.PLAIN_CHARACTER)) {
          JavaCharacter endCharacter = ((PlainCharacterTree) escape).getContents();
          return characterRange(startCharacter, endCharacter);
        } else {
          expected("simple character");
          return characterRange(startCharacter, backslash);
        }
      } else {
        characters.moveNext();
        JavaCharacter endCharacter = characters.getCurrent();
        characters.moveNext();
        return characterRange(startCharacter, endCharacter);
      }
    } else {
      return plainCharacter(startCharacter);
    }
  }

  private RegexTree plainCharacter(JavaCharacter character) {
    return new PlainCharacterTree(source, character.getRange(), character);
  }

  private CharacterRangeTree characterRange(JavaCharacter startCharacter, JavaCharacter endCharacter) {
    IndexRange range = startCharacter.getRange().merge(endCharacter.getRange());
    return new CharacterRangeTree(source, range, startCharacter, endCharacter);
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

  private interface GroupConstructor {
    GroupTree construct(IndexRange range, RegexTree element);
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
