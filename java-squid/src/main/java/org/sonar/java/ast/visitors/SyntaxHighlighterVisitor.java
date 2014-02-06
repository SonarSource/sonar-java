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
package org.sonar.java.ast.visitors;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.sonar.sslr.api.AstAndTokenVisitor;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.api.Token;
import com.sonar.sslr.api.Trivia;
import org.sonar.api.source.Highlightable;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class SyntaxHighlighterVisitor extends JavaAstVisitor implements AstAndTokenVisitor {

  private final SonarComponents sonarComponents;
  private final Map<AstNodeType, String> types;
  private final Charset charset;

  private Highlightable.HighlightingBuilder highlighting;
  private List<Integer> lineStart;

  public SyntaxHighlighterVisitor(SonarComponents sonarComponents, Charset charset) {
    this.sonarComponents = sonarComponents;
    this.charset = charset;

    ImmutableMap.Builder<AstNodeType, String> typesBuilder = ImmutableMap.builder();
    for (AstNodeType type : JavaKeyword.values()) {
      typesBuilder.put(type, "k");
    }
    typesBuilder.put(JavaTokenType.CHARACTER_LITERAL, "s");
    typesBuilder.put(JavaTokenType.LITERAL, "s");
    typesBuilder.put(JavaTokenType.FLOAT_LITERAL, "c");
    typesBuilder.put(JavaTokenType.DOUBLE_LITERAL, "c");
    typesBuilder.put(JavaTokenType.LONG_LITERAL, "c");
    typesBuilder.put(JavaTokenType.INTEGER_LITERAL, "c");
    typesBuilder.put(JavaGrammar.ANNOTATION, "a");
    types = typesBuilder.build();
  }

  @Override
  public void init() {
    for (AstNodeType type : types.keySet()) {
      subscribeTo(type);
    }
  }

  @Override
  public void visitFile(AstNode astNode) {
    if (astNode == null) {
      // parse error
      return;
    }

    highlighting = sonarComponents.highlightableFor(getContext().getFile()).newHighlighting();

    lineStart = Lists.newArrayList();
    final String content;
    try {
      content = Files.toString(getContext().getFile(), charset);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
    lineStart.add(0);
    for (int i = 0; i < content.length(); i++) {
      if (content.charAt(i) == '\n' || (content.charAt(i) == '\r' && i + 1 < content.length() && content.charAt(i + 1) != '\n')) {
        lineStart.add(i + 1);
      }
    }
  }

  @Override
  public void visitNode(AstNode astNode) {
    if (astNode.is(JavaGrammar.ANNOTATION)) {
      highlighting.highlight(astNode.getFromIndex(), astNode.getFirstChild(JavaGrammar.QUALIFIED_IDENTIFIER).getToIndex(), types.get(astNode.getType()));
    } else {
      highlighting.highlight(astNode.getFromIndex(), astNode.getToIndex(), types.get(astNode.getType()));
    }
  }

  @Override
  public void visitToken(Token token) {
    for (Trivia trivia : token.getTrivia()) {
      if (trivia.isComment()) {
        Token triviaToken = trivia.getToken();
        int offset = getOffset(triviaToken.getLine(), triviaToken.getColumn());
        highlighting.highlight(offset, offset + triviaToken.getValue().length(), "cppd");
      }
    }
  }

  /**
   * @param line starts from 1
   * @param column starts from 0
   */
  private int getOffset(int line, int column) {
    return lineStart.get(line - 1) + column;
  }

  @Override
  public void leaveFile(AstNode astNode) {
    if (astNode == null) {
      // parse error
      return;
    }

    highlighting.done();
  }

}
