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
package org.sonar.java.model.javac;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.InternalSyntaxTrivia;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;

public final class SourceTokens {

  public record Comment(SyntaxTrivia.CommentKind kind, String text, int start, int end) {
  }

  public record Token(String text, int start, int end, List<Comment> leadingComments) {
    public boolean isEof() {
      return text.isEmpty();
    }
  }

  private static final List<String> OPERATORS = List.of(
    ">>>=", "<<=", ">>=", "...", "::", "->", "++", "--", "==", "!=", "<=", ">=", "&&", "||",
    "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<", ">>>", ">>");

  private final String source;
  private final int sourceVersion;
  private final List<Token> tokens;
  private final Map<Token, InternalSyntaxToken> syntaxTokens = new IdentityHashMap<>();
  private final List<Integer> lineStarts = new ArrayList<>();

  public SourceTokens(String source) {
    this(source, Runtime.version().feature());
  }

  public SourceTokens(String source, int sourceVersion) {
    this.source = Objects.requireNonNull(source);
    this.sourceVersion = sourceVersion;
    lineStarts.add(0);
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (c == '\r') {
        if (i + 1 < source.length() && source.charAt(i + 1) == '\n') {
          i++;
        }
        lineStarts.add(i + 1);
      } else if (c == '\n') {
        lineStarts.add(i + 1);
      }
    }
    tokens = scan();
  }

  public List<Token> tokens() {
    return tokens;
  }

  public Token tokenAtOrAfter(int offset) {
    int low = 0;
    int high = tokens.size() - 1;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (tokens.get(middle).end() <= offset) {
        low = middle + 1;
      } else {
        high = middle;
      }
    }
    return tokens.get(low);
  }

  public Token tokenAtOrBefore(int offset) {
    int low = 0;
    int high = tokens.size() - 1;
    while (low < high) {
      int middle = (low + high + 1) >>> 1;
      if (tokens.get(middle).start() <= offset) {
        low = middle;
      } else {
        high = middle - 1;
      }
    }
    return tokens.get(low);
  }

  public List<Token> tokensBetween(int start, int end) {
    List<Token> result = new ArrayList<>();
    for (Token token : tokens) {
      if (token.start() >= end || token.isEof()) {
        break;
      }
      if (token.start() >= start) {
        result.add(token);
      }
    }
    return result;
  }

  public Token firstToken(int start, int end, String text) {
    return tokensBetween(start, end).stream()
      .filter(token -> token.text().equals(text))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Missing token '" + text + "' between " + start + " and " + end));
  }

  public Token lastToken(int start, int end, String text) {
    List<Token> matches = tokensBetween(start, end).stream().filter(token -> token.text().equals(text)).toList();
    if (matches.isEmpty()) {
      throw new IllegalArgumentException("Missing token '" + text + "' between " + start + " and " + end);
    }
    return matches.get(matches.size() - 1);
  }

  public InternalSyntaxToken syntaxToken(Token token) {
    return syntaxTokens.computeIfAbsent(token, this::createSyntaxToken);
  }

  private InternalSyntaxToken createSyntaxToken(Token token) {
    List<SyntaxTrivia> trivias = token.leadingComments().stream()
      .map(comment -> new InternalSyntaxTrivia(comment.kind(), comment.text(), line(comment.start()), column(comment.start())))
      .map(SyntaxTrivia.class::cast)
      .toList();
    return new InternalSyntaxToken(line(token.start()), column(token.start()), token.text(), trivias, token.isEof());
  }

  public int line(int offset) {
    int low = 0;
    int high = lineStarts.size() - 1;
    while (low <= high) {
      int middle = (low + high) >>> 1;
      if (lineStarts.get(middle) <= offset) {
        low = middle + 1;
      } else {
        high = middle - 1;
      }
    }
    return high + 1;
  }

  public int column(int offset) {
    return offset - lineStarts.get(line(offset) - 1);
  }

  private List<Token> scan() {
    List<Token> result = new ArrayList<>();
    List<Comment> comments = new ArrayList<>();
    int offset = 0;
    while (offset < source.length()) {
      char c = source.charAt(offset);
      if (Character.isWhitespace(c)) {
        offset++;
        continue;
      }
      if (c == '/' && offset + 1 < source.length() && source.charAt(offset + 1) == '/') {
        SyntaxTrivia.CommentKind kind = sourceVersion >= 23 && source.startsWith("///", offset)
          ? SyntaxTrivia.CommentKind.MARKDOWN : SyntaxTrivia.CommentKind.LINE;
        int end = lineCommentEnd(offset);
        if (kind == SyntaxTrivia.CommentKind.MARKDOWN) {
          int next = nextMarkdownLine(end);
          while (next >= 0) {
            end = lineCommentEnd(next);
            next = nextMarkdownLine(end);
          }
        }
        comments.add(new Comment(kind, source.substring(offset, end), offset, end));
        offset = end;
        continue;
      }
      if (c == '/' && offset + 1 < source.length() && source.charAt(offset + 1) == '*') {
        int close = source.indexOf("*/", offset + 2);
        int end = close < 0 ? source.length() : close + 2;
        SyntaxTrivia.CommentKind kind = source.startsWith("/**", offset) ? SyntaxTrivia.CommentKind.JAVADOC : SyntaxTrivia.CommentKind.BLOCK;
        comments.add(new Comment(kind, source.substring(offset, end), offset, end));
        offset = end;
        continue;
      }
      int end;
      if (c == '"') {
        end = source.startsWith("\"\"\"", offset) ? scanTextBlock(offset) : scanQuoted(offset, '"');
      } else if (c == '\'') {
        end = scanQuoted(offset, '\'');
      } else if (Character.isJavaIdentifierStart(c)) {
        end = offset + 1;
        while (end < source.length() && Character.isJavaIdentifierPart(source.charAt(end))) {
          end++;
        }
      } else if (Character.isDigit(c)) {
        end = scanNumber(offset);
      } else {
        end = offset + 1;
        for (String operator : OPERATORS) {
          if (source.startsWith(operator, offset) && operator.length() > end - offset) {
            end = offset + operator.length();
          }
        }
      }
      result.add(new Token(source.substring(offset, end), offset, end, List.copyOf(comments)));
      comments.clear();
      offset = end;
    }
    result.add(new Token("", source.length(), source.length(), List.copyOf(comments)));
    return List.copyOf(result);
  }

  private int lineCommentEnd(int start) {
    int end = start + 2;
    while (end < source.length() && source.charAt(end) != '\r' && source.charAt(end) != '\n') {
      end++;
    }
    return end;
  }

  private int nextMarkdownLine(int end) {
    int offset = end;
    if (offset >= source.length()) {
      return -1;
    }
    if (source.charAt(offset++) == '\r' && offset < source.length() && source.charAt(offset) == '\n') {
      offset++;
    }
    while (offset < source.length() && (source.charAt(offset) == ' ' || source.charAt(offset) == '\t')) {
      offset++;
    }
    return source.startsWith("///", offset) ? offset : -1;
  }

  private int scanQuoted(int start, char delimiter) {
    int offset = start + 1;
    while (offset < source.length()) {
      char c = source.charAt(offset++);
      if (c == '\\' && offset < source.length()) {
        offset++;
      } else if (c == delimiter) {
        break;
      }
    }
    return offset;
  }

  private int scanTextBlock(int start) {
    int offset = start + 3;
    while (offset < source.length()) {
      if (source.startsWith("\"\"\"", offset) && (offset == start + 3 || source.charAt(offset - 1) != '\\')) {
        return offset + 3;
      }
      offset++;
    }
    return offset;
  }

  private int scanNumber(int start) {
    int offset = start + 1;
    while (offset < source.length()) {
      char c = source.charAt(offset);
      if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
        offset++;
      } else if ((c == '+' || c == '-') && offset > start && "eEpP".indexOf(source.charAt(offset - 1)) >= 0) {
        offset++;
      } else {
        break;
      }
    }
    return offset;
  }
}
