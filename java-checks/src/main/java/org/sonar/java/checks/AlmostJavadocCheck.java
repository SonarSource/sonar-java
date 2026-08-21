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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sonar.check.Rule;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.SyntaxTrivia.CommentKind;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S9355")
public class AlmostJavadocCheck extends IssuableSubscriptionVisitor {

  static final String MESSAGE = "This comment contains Javadoc or HTML tags, but isn't started with a double asterisk (/**); is it meant to be Javadoc?";

  private static final Pattern CLOSING_HTML = Pattern.compile("</(?:em|b|a|strong|i|pre|code)>");
  private static final Pattern AT_TAG = Pattern.compile("(?<!\\w)@[a-zA-Z]+\\b");
  private static final Set<String> JAVADOC_TAGS = Set.of(
    "@author", "@code", "@deprecated", "@docRoot", "@exception", "@inheritDoc",
    "@link", "@linkplain", "@literal", "@param", "@return", "@see", "@serial",
    "@serialData", "@serialField", "@since", "@snippet", "@throws", "@value", "@version");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    List<Tree.Kind> kinds = new ArrayList<>(Arrays.asList(PublicApiChecker.apiKinds()));
    kinds.add(Tree.Kind.ENUM_CONSTANT);
    return kinds;
  }

  @Override
  public void visitNode(Tree tree) {
    if (!isDocumentableDeclaration(tree)) {
      return;
    }
    SyntaxToken firstToken = tree.firstToken();
    if (firstToken == null) {
      return;
    }
    List<SyntaxTrivia> trivias = firstToken.trivias();
    if (trivias.stream().anyMatch(trivia -> trivia.isComment(CommentKind.JAVADOC, CommentKind.MARKDOWN))) {
      return;
    }
    for (SyntaxTrivia trivia : trivias) {
      if (!belongsToPreviousMember(tree, trivia) && isAlmostJavadoc(trivia)) {
        reportAlmostJavadoc(trivia);
      }
    }
  }

  private static boolean isDocumentableDeclaration(Tree tree) {
    if (tree.is(Tree.Kind.IMPLICIT_CLASS) || isInsideMethodOrConstructor(tree)) {
      return false;
    }
    if (tree.is(Tree.Kind.VARIABLE)) {
      return tree.parent() instanceof ClassTree classTree && isFirstDeclarator(classTree, (VariableTree) tree);
    }
    return true;
  }

  private static boolean isFirstDeclarator(ClassTree classTree, VariableTree variable) {
    List<Tree> members = classTree.members();
    int index = members.indexOf(variable);
    if (index <= 0) {
      return true;
    }
    Tree preceding = members.get(index - 1);
    return !(preceding.is(Tree.Kind.VARIABLE) && preceding.firstToken().equals(variable.firstToken()));
  }

  private static boolean isInsideMethodOrConstructor(Tree tree) {
    for (Tree current = tree.parent(); current != null; current = current.parent()) {
      if (current.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR, Tree.Kind.LAMBDA_EXPRESSION)) {
        return true;
      }
    }
    return false;
  }

  private static boolean belongsToPreviousMember(Tree tree, SyntaxTrivia trivia) {
    Tree previous = previousSibling(tree);
    if (previous == null) {
      return false;
    }
    SyntaxToken previousEnd = previous.lastToken();
    return previousEnd != null && trivia.range().start().line() == previousEnd.range().end().line();
  }

  private static Tree previousSibling(Tree tree) {
    Tree parent = tree.parent();
    if (parent instanceof ClassTree classTree) {
      List<Tree> members = classTree.members();
      int index = members.indexOf(tree);
      return index > 0 ? members.get(index - 1) : null;
    }
    if (parent instanceof CompilationUnitTree compilationUnit) {
      List<Tree> types = compilationUnit.types();
      int index = types.indexOf(tree);
      return index > 0 ? types.get(index - 1) : null;
    }
    return null;
  }

  private static boolean isAlmostJavadoc(SyntaxTrivia trivia) {
    String text = trivia.comment().stripTrailing();
    if (text.contains("(non-Javadoc)")) {
      return false;
    }
    if (trivia.isComment(CommentKind.BLOCK)) {
      return hasTag(text);
    }
    return trivia.isComment(CommentKind.LINE)
      && text.endsWith("*/")
      && hasTag(text);
  }

  private static boolean hasTag(String text) {
    if (CLOSING_HTML.matcher(text).find()) {
      return true;
    }
    Matcher matcher = AT_TAG.matcher(text);
    while (matcher.find()) {
      if (JAVADOC_TAGS.contains(matcher.group())) {
        return true;
      }
    }
    return false;
  }

  private void reportAlmostJavadoc(SyntaxTrivia trivia) {
    Position start = trivia.range().start();
    Position end = trivia.range().end();
    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onRange(start.line(), start.columnOffset(), end.line(), end.columnOffset())
      .withMessage(MESSAGE)
      .withQuickFix(() -> convertToJavadoc(trivia))
      .report();
  }

  private static JavaQuickFix convertToJavadoc(SyntaxTrivia trivia) {
    Position start = trivia.range().start();
    String text = trivia.comment();
    JavaTextEdit edit;
    if (trivia.isComment(CommentKind.LINE) && text.startsWith("// /**")) {
      edit = JavaTextEdit.replaceTextSpan(firstCharacters(start, 2), "");
    } else if (trivia.isComment(CommentKind.BLOCK)) {
      edit = JavaTextEdit.insertAtPosition(start.line(), start.columnOffset() + 1, "*");
    } else {
      edit = JavaTextEdit.replaceTextSpan(firstCharacters(start, 2), "/**");
    }
    return JavaQuickFix.newQuickFix("Convert to Javadoc comment")
      .addTextEdit(edit)
      .build();
  }

  private static AnalyzerMessage.TextSpan firstCharacters(Position start, int length) {
    return new AnalyzerMessage.TextSpan(start.line(), start.columnOffset(), start.line(), start.columnOffset() + length);
  }
}
