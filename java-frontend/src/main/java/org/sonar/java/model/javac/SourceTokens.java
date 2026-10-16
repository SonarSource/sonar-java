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

import java.net.URI;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import org.sonar.java.javac.main.Option;
import org.sonar.java.javac.parser.Scanner;
import org.sonar.java.javac.parser.ScannerFactory;
import org.sonar.java.javac.parser.Tokens;
import org.sonar.java.javac.util.Context;
import org.sonar.java.javac.util.Log;
import org.sonar.java.javac.util.Options;
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

  private final String source;
  private final int sourceVersion;
  private final List<Token> tokens;
  private final Map<Token, InternalSyntaxToken> syntaxTokens = new IdentityHashMap<>();
  private final List<Integer> lineStarts = new ArrayList<>();

  public SourceTokens(String source) {
    this(source, 27);
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
    String scannedSource = source + "\n__sonar_eof__";
    Context context = new Context();
    Options options = Options.instance(context);
    options.put(Option.SOURCE, Integer.toString(sourceVersion));
    if (sourceVersion < 23) {
      options.put(Option.DISABLE_LINE_DOC_COMMENTS, "true");
    }
    JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///SourceTokens.java"), JavaFileObject.Kind.SOURCE) {
      @Override
      public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return scannedSource;
      }
    };
    Log.instance(context).useSource(file);
    Scanner scanner = ScannerFactory.instance(context).newScanner(scannedSource, true);
    List<Token> result = new ArrayList<>();
    while (true) {
      scanner.nextToken();
      Tokens.Token scanned = scanner.token();
      List<Comment> comments = new ArrayList<>();
      if (scanned.comments != null) {
        for (Tokens.Comment comment : scanned.comments.reverse()) {
          SyntaxTrivia.CommentKind kind = switch (comment.getStyle()) {
            case LINE -> SyntaxTrivia.CommentKind.LINE;
            case BLOCK -> SyntaxTrivia.CommentKind.BLOCK;
            case JAVADOC_LINE -> SyntaxTrivia.CommentKind.MARKDOWN;
            case JAVADOC_BLOCK -> SyntaxTrivia.CommentKind.JAVADOC;
          };
          int start = comment.getPos().getStartPosition();
          int end = Math.min(comment.getPos().getEndPosition(), source.length());
          comments.add(new Comment(kind, source.substring(start, end), start, end));
        }
      }
      if (scanned.pos >= source.length()) {
        result.add(new Token("", source.length(), source.length(), List.copyOf(comments)));
        break;
      }
      String raw = source.substring(scanned.pos, scanned.endPos);
      result.add(new Token(raw, scanned.pos, scanned.endPos, List.copyOf(comments)));
    }
    return List.copyOf(result);
  }
}
