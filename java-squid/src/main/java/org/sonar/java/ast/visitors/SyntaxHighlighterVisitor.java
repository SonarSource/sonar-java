/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.sonar.api.source.Highlightable;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SyntaxHighlighterVisitor extends SubscriptionVisitor {

  private final SonarComponents sonarComponents;
  private final Map<Tree.Kind, String> typesByKind;
  private final Set<String> keywords;
  private final Charset charset;

  private Highlightable.HighlightingBuilder highlighting;
  private List<Integer> lineStart;

  public SyntaxHighlighterVisitor(SonarComponents sonarComponents, Charset charset) {
    this.sonarComponents = sonarComponents;
    this.charset = charset;

    ImmutableSet.Builder<String> keywordsBuilder = ImmutableSet.builder();
    keywordsBuilder.add(JavaKeyword.keywordValues());
    keywords = keywordsBuilder.build();

    ImmutableMap.Builder<Tree.Kind, String> typesByKindBuilder = ImmutableMap.builder();
    typesByKindBuilder.put(Tree.Kind.STRING_LITERAL, "s");
    typesByKindBuilder.put(Tree.Kind.CHAR_LITERAL, "s");
    typesByKindBuilder.put(Tree.Kind.FLOAT_LITERAL, "c");
    typesByKindBuilder.put(Tree.Kind.DOUBLE_LITERAL, "c");
    typesByKindBuilder.put(Tree.Kind.LONG_LITERAL, "c");
    typesByKindBuilder.put(Tree.Kind.INT_LITERAL, "c");
    typesByKindBuilder.put(Tree.Kind.ANNOTATION, "a");
    typesByKind = typesByKindBuilder.build();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.<Tree.Kind>builder()
      .addAll(typesByKind.keySet().iterator())
      .add(Tree.Kind.TOKEN)
      .add(Tree.Kind.TRIVIA)
      .build();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    File file = context.getFile();
    highlighting = sonarComponents.highlightableFor(file).newHighlighting();
    lineStart = startLines(file, this.charset);

    super.scanFile(context);

    highlighting.done();
    lineStart.clear();
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.ANNOTATION)) {
      AnnotationTree annotationTree = (AnnotationTree) tree;
      highlighting.highlight(start(annotationTree), end(annotationTree), typesByKind.get(Tree.Kind.ANNOTATION));
    } else {
      Tree.Kind kind = tree.kind();
      if (typesByKind.containsKey(kind)) {
        SyntaxToken token = ((LiteralTree) tree).token();
        highlighting.highlight(start(token), end(token), typesByKind.get(kind));
      }
    }
  }

  @Override
  public void visitToken(SyntaxToken syntaxToken) {
    String text = syntaxToken.text();
    if (keywords.contains(text)) {
      highlighting.highlight(start(syntaxToken), end(syntaxToken), "k");
    }
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    highlighting.highlight(start(syntaxTrivia), end(syntaxTrivia), "cppd");
  }

  private int start(AnnotationTree annotationTree) {
    return start(annotationTree.atToken());
  }

  private int start(SyntaxToken token) {
    return getOffset(token.line(), token.column());
  }

  private int start(SyntaxTrivia syntaxTrivia) {
    return getOffset(syntaxTrivia.startLine(), syntaxTrivia.column());
  }

  /**
   * @param line starts from 1
   * @param column starts from 0
   */
  private int getOffset(int line, int column) {
    return lineStart.get(line - 1) + column;
  }

  private int end(AnnotationTree annotationTree) {
    TypeTree annotationType = annotationTree.annotationType();
    SyntaxToken token;
    if (annotationType.is(Tree.Kind.MEMBER_SELECT)) {
      token = ((MemberSelectExpressionTree) annotationType).identifier().identifierToken();
    } else {
      token = ((IdentifierTree) annotationType).identifierToken();
    }
    return end(token);
  }

  private int end(SyntaxToken token) {
    return getOffset(token.line(), token.column()) + token.text().length();
  }

  private int end(SyntaxTrivia trivia) {
    return getOffset(trivia.startLine(), trivia.column()) + trivia.comment().length();
  }

  private static List<Integer> startLines(File file, Charset charset) {
    List<Integer> startLines = Lists.newArrayList();
    final String content;
    try {
      content = Files.toString(file, charset);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
    startLines.add(0);
    for (int i = 0; i < content.length(); i++) {
      if (content.charAt(i) == '\n' || (content.charAt(i) == '\r' && i + 1 < content.length() && content.charAt(i + 1) != '\n')) {
        startLines.add(i + 1);
      }
    }
    return startLines;
  }
}
