/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model;

import com.intellij.lang.java.lexer.JavaLexer;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.TokenType;
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;

/** Indexes physical UTF-16 source slices independently of PSI's decoded names and composite operator nodes. */
final class PsiTokenIndex {
  record Lexeme(int start, int end, String kind, InternalSyntaxToken token) {
  }

  private final String source;
  private final LineColumnConverter positions;
  private final List<Lexeme> tokens = new ArrayList<>();
  private final Map<Long, InternalSyntaxToken> slices = new HashMap<>();

  PsiTokenIndex(String source) {
    this.source = source;
    positions = new LineColumnConverter(source);
    var lexer = new JavaLexer(LanguageLevel.JDK_26_PREVIEW);
    var input = unicodeInput(source);
    lexer.start(input.text());
    List<SyntaxTrivia> trivia = new ArrayList<>();
    int commentStart = 0;
    int commentEnd = 0;
    while (lexer.getTokenType() != null) {
      int start = input.offsets()[lexer.getTokenStart()];
      int end = input.offsets()[lexer.getTokenEnd()];
      String kind = lexer.getTokenType().toString();
      int restart = -1;
      if (kind.equals("TEXT_BLOCK_LITERAL") && !opensTextBlock(input.text(), lexer.getTokenStart())) {
        restart = lexer.getTokenStart() + 2;
        end = input.offsets()[restart];
        kind = "STRING_LITERAL";
      }
      String text = source.substring(start, end);
      if (kind.contains("COMMENT")) {
        int logicalEnd = lexer.getTokenEnd();
        if (kind.equals("END_OF_LINE_COMMENT") && logicalEnd < input.text().length()
          && (input.text().charAt(logicalEnd) == '\n' || input.text().charAt(logicalEnd) == '\r')
          && input.offsets()[logicalEnd + 1] - end > 1) {
          end = input.offsets()[logicalEnd + 1];
          text = source.substring(start, end);
        }
        var commentKind = text.startsWith("///") ? SyntaxTrivia.CommentKind.MARKDOWN
          : text.startsWith("/**") && text.length() > 4 ? SyntaxTrivia.CommentKind.JAVADOC
          : text.startsWith("//") ? SyntaxTrivia.CommentKind.LINE : SyntaxTrivia.CommentKind.BLOCK;
        if (commentKind == SyntaxTrivia.CommentKind.MARKDOWN && !trivia.isEmpty()
          && trivia.getLast().commentKind() == SyntaxTrivia.CommentKind.MARKDOWN
          && source.substring(commentEnd, start).matches("(?:\\r\\n?|\\n)[ \t\f]*")) {
          trivia.removeLast();
          start = commentStart;
          text = source.substring(start, end);
        }
        var pos = positions.toPos(start);
        trivia.add(new InternalSyntaxTrivia(commentKind, text, pos.line(), pos.columnOffset()));
        commentStart = start;
        commentEnd = end;
      } else if (lexer.getTokenType() != TokenType.WHITE_SPACE) {
        validateLiteral(kind, text, start);
        if (lexer.getTokenType() == TokenType.BAD_CHARACTER) {
          throw new IllegalArgumentException("Invalid Java token at UTF-16 offset " + start);
        }
        var pos = positions.toPos(start);
        tokens.add(new Lexeme(start, end, kind, new InternalSyntaxToken(pos.line(), pos.columnOffset(), text, List.copyOf(trivia), false)));
        trivia.clear();
      }
      if (restart < 0) {
        lexer.advance();
      } else {
        lexer.start(input.text(), restart, input.text().length(), 0);
      }
    }
    mergeShifts();
    var pos = positions.toPos(source.length());
    tokens.add(new Lexeme(source.length(), source.length(), "EOF",
      new InternalSyntaxToken(pos.line(), pos.columnOffset(), "", List.copyOf(trivia), true)));
  }

  private static boolean opensTextBlock(String text, int start) {
    int i = start + 3;
    while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t' || text.charAt(i) == '\f')) {
      i++;
    }
    return i < text.length() && (text.charAt(i) == '\r' || text.charAt(i) == '\n');
  }

  private static void validateLiteral(String kind, String raw, int offset) {
    if (!kind.equals("STRING_LITERAL") && !kind.equals("CHARACTER_LITERAL") && !kind.equals("TEXT_BLOCK_LITERAL")) {
      return;
    }
    String text = decoded(raw);
    boolean valid;
    if (kind.equals("TEXT_BLOCK_LITERAL")) {
      valid = text.length() >= 6 && text.endsWith("\"\"\"");
    } else {
      char quote = kind.equals("STRING_LITERAL") ? '"' : '\'';
      var value = new StringBuilder();
      valid = text.length() >= 2 && text.charAt(text.length() - 1) == quote
        && text.indexOf('\n') < 0 && text.indexOf('\r') < 0
        && PsiLiteralExpressionImpl.parseStringCharacters(text.substring(1, text.length() - 1), value, null)
        && (quote == '"' || value.length() == 1);
    }
    if (!valid) {
      throw new IllegalArgumentException("Invalid Java literal at UTF-16 offset " + offset);
    }
  }

  record UnicodeInput(String text, int[] offsets) {
  }

  static UnicodeInput unicodeInput(String raw) {
    var decoded = new StringBuilder(raw.length());
    int[] offsets = new int[raw.length() + 1];
    int slashes = 0;
    boolean escaped = false;
    for (int i = 0; i < raw.length();) {
      offsets[decoded.length()] = i;
      char c = raw.charAt(i++);
      boolean unicode = false;
      if (c == '\\' && (escaped || slashes % 2 == 0) && i < raw.length() && raw.charAt(i) == 'u') {
        while (i < raw.length() && raw.charAt(i) == 'u') {
          i++;
        }
        if (i + 4 > raw.length()) {
          throw new IllegalArgumentException("Incomplete Unicode escape at UTF-16 offset " + i);
        }
        int value = 0;
        for (int j = 0; j < 4; j++) {
          int digit = Character.digit(raw.charAt(i++), 16);
          if (digit < 0) {
            throw new IllegalArgumentException("Invalid Unicode escape at UTF-16 offset " + (i - 1));
          }
          value = value * 16 + digit;
        }
        c = (char) value;
        unicode = true;
      }
      decoded.append(c);
      slashes = c == '\\' ? slashes + 1 : 0;
      escaped = unicode;
    }
    offsets[decoded.length()] = raw.length();
    return new UnicodeInput(decoded.toString(), offsets);
  }

  static String decoded(String text) {
    return unicodeInput(text).text();
  }

  private void mergeShifts() {
    for (int i = 0; i < tokens.size(); i++) {
      var first = tokens.get(i);
      if (!decoded(first.token().text()).equals(">")) {
        continue;
      }
      int j = i + 1;
      int end = first.end();
      String operator = ">";
      while (j < tokens.size() && tokens.get(j).start() == end && operator.length() < 3 && decoded(tokens.get(j).token().text()).equals(">")) {
        end = tokens.get(j++).end();
        operator += ">";
      }
      if (j < tokens.size() && tokens.get(j).start() == end && decoded(tokens.get(j).token().text()).equals("=")) {
        end = tokens.get(j++).end();
        operator += "=";
      }
      if (j > i + 1) {
        var pos = positions.toPos(first.start());
        tokens.set(i, new Lexeme(first.start(), end, operator,
          new InternalSyntaxToken(pos.line(), pos.columnOffset(), source.substring(first.start(), end), first.token().trivias(), false)));
        tokens.subList(i + 1, j).clear();
      }
    }
  }

  List<Lexeme> lexemes() {
    return java.util.Collections.unmodifiableList(tokens);
  }

  InternalSyntaxToken eof() {
    return tokens.getLast().token();
  }

  int index(int offset) {
    int low = 0;
    int high = tokens.size() - 1;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (tokens.get(middle).start() < offset) {
        low = middle + 1;
      } else {
        high = middle;
      }
    }
    return low;
  }

  InternalSyntaxToken at(int offset) {
    return tokens.get(index(offset)).token();
  }

  InternalSyntaxToken find(int start, int end, String text, boolean last) {
    InternalSyntaxToken result = null;
    for (int i = index(start); i < tokens.size() && tokens.get(i).start() < end; i++) {
      var token = tokens.get(i).token();
      if (decoded(token.text()).equals(text)) {
        result = token;
        if (!last) {
          break;
        }
      }
    }
    if (result == null) {
      throw new IllegalArgumentException("Missing token '" + text + "' in [" + start + ", " + end + ")");
    }
    return result;
  }

  InternalSyntaxToken slice(int start, int end) {
    int index = index(start);
    if (index > 0 && tokens.get(index - 1).end() > start) {
      index--;
    }
    var lexeme = tokens.get(index);
    if (lexeme.start() == start && lexeme.end() == end) {
      return lexeme.token();
    }
    long key = ((long) start << 32) | end;
    return slices.computeIfAbsent(key, unused -> {
      var pos = positions.toPos(start);
      List<SyntaxTrivia> trivia = lexeme.start() == start ? lexeme.token().trivias() : List.of();
      return new InternalSyntaxToken(pos.line(), pos.columnOffset(), source.substring(start, end), trivia, false);
    });
  }
}
