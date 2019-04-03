/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java.ast.visitors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaRestrictedKeyword;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

public class SyntaxHighlighterVisitor extends SubscriptionVisitor {

  private final SonarComponents sonarComponents;
  private final Map<Tree.Kind, TypeOfText> typesByKind;
  private final Set<String> keywords;
  private final Set<String> restrictedKeywords;

  private NewHighlighting highlighting;
  private boolean withinModule = false;

  public SyntaxHighlighterVisitor(SonarComponents sonarComponents) {
    this.sonarComponents = sonarComponents;

    keywords = ImmutableSet.copyOf(JavaKeyword.keywordValues());
    restrictedKeywords = ImmutableSet.copyOf(JavaRestrictedKeyword.restrictedKeywordValues());

    ImmutableMap.Builder<Tree.Kind, TypeOfText> typesByKindBuilder = ImmutableMap.builder();
    typesByKindBuilder.put(Tree.Kind.STRING_LITERAL, TypeOfText.STRING);
    typesByKindBuilder.put(Tree.Kind.CHAR_LITERAL, TypeOfText.STRING);
    typesByKindBuilder.put(Tree.Kind.FLOAT_LITERAL, TypeOfText.CONSTANT);
    typesByKindBuilder.put(Tree.Kind.DOUBLE_LITERAL, TypeOfText.CONSTANT);
    typesByKindBuilder.put(Tree.Kind.LONG_LITERAL, TypeOfText.CONSTANT);
    typesByKindBuilder.put(Tree.Kind.INT_LITERAL, TypeOfText.CONSTANT);
    typesByKindBuilder.put(Tree.Kind.ANNOTATION, TypeOfText.ANNOTATION);
    typesByKindBuilder.put(Tree.Kind.VAR_TYPE, TypeOfText.KEYWORD);
    typesByKind = typesByKindBuilder.build();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.<Tree.Kind>builder()
      .addAll(typesByKind.keySet().iterator())
      .add(Tree.Kind.MODULE)
      .add(Tree.Kind.TOKEN)
      .add(Tree.Kind.TRIVIA)
      .build();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    highlighting = sonarComponents.highlightableFor(context.getInputFile());

    super.scanFile(context);

    highlighting.save();
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.MODULE)) {
      withinModule = true;
      return;
    }
    if (tree.is(Tree.Kind.ANNOTATION)) {
      AnnotationTree annotationTree = (AnnotationTree) tree;
      highlight(annotationTree.atToken(), annotationTree.annotationType(), typesByKind.get(Tree.Kind.ANNOTATION));
    } else {
      highlight(tree, typesByKind.get(tree.kind()));
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.MODULE)) {
      withinModule = false;
    }
  }

  private void highlight(Tree tree, TypeOfText typeOfText) {
    highlight(tree, tree, typeOfText);
  }

  private void highlight(Tree from, Tree to, TypeOfText typeOfText) {
    SyntaxToken firstToken = from.firstToken();
    SyntaxToken lastToken = to.lastToken();
    highlighting.highlight(firstToken.line(), firstToken.column(), lastToken.line(), lastToken.column() + lastToken.text().length(), typeOfText);
  }

  @Override
  public void visitToken(SyntaxToken syntaxToken) {
    String text = syntaxToken.text();
    if (keywords.contains(text)) {
      if (isInterfaceOfAnnotationType(syntaxToken)) {
        ClassTreeImpl annotationType = (ClassTreeImpl) syntaxToken.parent();
        highlight(annotationType.atToken(), annotationType.declarationKeyword(), TypeOfText.KEYWORD);
      } else {
        highlight(syntaxToken, TypeOfText.KEYWORD);
      }
    } else if (isRestrictedKeyword(syntaxToken)) {
      highlight(syntaxToken, TypeOfText.KEYWORD);
    }
  }

  private static boolean isInterfaceOfAnnotationType(SyntaxToken syntaxToken) {
    return JavaKeyword.INTERFACE.getValue().equals(syntaxToken.text()) && syntaxToken.parent().is(Tree.Kind.ANNOTATION_TYPE);
  }

  private boolean isRestrictedKeyword(SyntaxToken syntaxToken) {
    return withinModule
      && restrictedKeywords.contains(syntaxToken.text())
      && !syntaxToken.parent().is(Tree.Kind.IDENTIFIER);
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    String comment = syntaxTrivia.comment();
    int startLine = syntaxTrivia.startLine();
    int startColumn = syntaxTrivia.column();

    String[] lines = comment.split("\\r\\n|\\n|\\r");
    int numberLines = lines.length;

    int endLine = startLine + numberLines - 1;
    int endColumn = numberLines == 1 ? (startColumn + comment.length()) : lines[numberLines - 1].length();
    boolean isJavadoc = lines[0].trim().startsWith("/**");
    highlighting.highlight(startLine, startColumn, endLine, endColumn, isJavadoc ? TypeOfText.STRUCTURED_COMMENT : TypeOfText.COMMENT);
  }
}
