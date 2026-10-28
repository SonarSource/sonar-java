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
package org.sonar.java.model.psi;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.javadoc.PsiDocComment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.InternalSyntaxTrivia;
import org.sonar.java.model.LineColumnConverter;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;

final class PsiTokenIndex {

  private final PsiSourceSnapshot source;
  private final LineColumnConverter positions;
  private final List<Entry> entries = new ArrayList<>();
  private final List<SyntaxTrivia> comments = new ArrayList<>();
  private final Map<InternalSyntaxToken, String> logicalTexts = new IdentityHashMap<>();
  private final Map<Integer, Integer> closingAnglePositions = new HashMap<>();

  PsiTokenIndex(PsiJavaFile file, String source) {
    this(file, PsiSourceSnapshot.identity(source));
  }

  PsiTokenIndex(PsiJavaFile file, PsiSourceSnapshot source) {
    if (!file.getText().equals(source.psiText())) {
      throw new IllegalArgumentException("PSI contents differ from the source snapshot");
    }
    this.source = source;
    positions = new LineColumnConverter(source.original());
    collect(file);
    add(source.psiText().length(), source.psiText().length(), true);
  }

  private void collect(PsiElement element) {
    int start = element.getTextRange().getStartOffset();
    int end = element.getTextRange().getEndOffset();
    if (element instanceof PsiWhiteSpace || start == end) {
      return;
    }
    if (element instanceof PsiComment comment) {
      String text = source.originalText(start, end);
      SyntaxTrivia.CommentKind kind = comment instanceof PsiDocComment doc
        ? doc.isMarkdownComment() ? SyntaxTrivia.CommentKind.MARKDOWN : SyntaxTrivia.CommentKind.JAVADOC
        : comment.getTokenType() == JavaTokenType.END_OF_LINE_COMMENT ? SyntaxTrivia.CommentKind.LINE : SyntaxTrivia.CommentKind.BLOCK;
      if (kind == SyntaxTrivia.CommentKind.LINE) {
        text = source.originalText(start, lineCommentEnd(end));
      }
      var pos = positions.toPos(source.originalOffset(start));
      comments.add(new InternalSyntaxTrivia(kind, text, pos.line(), pos.columnOffset()));
    } else if (element.getFirstChild() == null) {
      add(start, end, false);
    } else {
      for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
        collect(child);
      }
    }
  }

  private void add(int start, int end, boolean eof) {
    var pos = positions.toPos(source.originalOffset(start));
    var token = new InternalSyntaxToken(pos.line(), pos.columnOffset(), source.originalText(start, end), List.copyOf(comments), eof);
    entries.add(new Entry(start, end, token));
    logicalTexts.put(token, source.logicalText(start, end));
    comments.clear();
    if (matches(token, ">")) {
      closingAnglePositions.put(source.originalOffset(start), source.originalOffset(end) - 1);
    }
  }

  /** Matches ECJ's inclusion of escaped line terminators, including a physical CR paired with an escaped LF. */
  private int lineCommentEnd(int end) {
    if (end == source.psiText().length()) {
      return end;
    }
    if (source.psiText().charAt(end) == '\r' && end + 1 < source.psiText().length() && source.psiText().charAt(end + 1) == '\n'
      && source.originalOffset(end + 2) - source.originalOffset(end + 1) > 1) {
      return end + 2;
    }
    return source.originalOffset(end + 1) - source.originalOffset(end) > 1 ? end + 1 : end;
  }

  InternalSyntaxToken token(PsiElement element) {
    return at(element.getTextRange().getStartOffset());
  }

  InternalSyntaxToken at(int offset) {
    int index = lowerBound(offset);
    if (index < entries.size() && entries.get(index).start == offset) {
      return entries.get(index).token;
    }
    throw new IllegalArgumentException("No PSI token at offset " + offset);
  }

  private int lowerBound(int offset) {
    int low = 0;
    int high = entries.size();
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (entries.get(middle).start < offset) {
        low = middle + 1;
      } else {
        high = middle;
      }
    }
    return low;
  }

  InternalSyntaxToken first(PsiElement element, String text) {
    return find(element.getTextRange().getStartOffset(), element.getTextRange().getEndOffset(), text, false);
  }

  InternalSyntaxToken last(PsiElement element, String text) {
    return find(element.getTextRange().getStartOffset(), element.getTextRange().getEndOffset(), text, true);
  }

  InternalSyntaxToken find(int start, int end, String text, boolean last) {
    int from = lowerBound(start);
    int to = lowerBound(end);
    for (int i = last ? to - 1 : from; i >= from && i < to; i += last ? -1 : 1) {
      Entry entry = entries.get(i);
      if (entry.end <= end && matches(entry.token, text)) {
        return entry.token;
      }
    }
    throw new IllegalArgumentException("Missing PSI token '" + text + "' in [" + start + ", " + end + ")");
  }

  InternalSyntaxToken before(PsiElement element, String text) {
    return find(0, element.getTextRange().getStartOffset(), text, true);
  }

  InternalSyntaxToken after(PsiElement element, String text) {
    return find(element.getTextRange().getEndOffset(), source.psiText().length(), text, false);
  }

  InternalSyntaxToken next(PsiElement element) {
    return entries.get(lowerBound(element.getTextRange().getEndOffset())).token;
  }

  int originalOffset(int psiOffset) {
    return source.originalOffset(psiOffset);
  }

  boolean matches(InternalSyntaxToken token, String text) {
    return text.equals(logicalTexts.get(token));
  }

  /** ECJ keeps the starting offset when its closing-angle scan encounters trivia instead of a greater-than token. */
  int closingAnglePosition(int originalOffset) {
    return closingAnglePositions.getOrDefault(originalOffset, originalOffset);
  }

  InternalSyntaxToken closingAngle(PsiElement element) {
    int from = lowerBound(element.getTextRange().getStartOffset());
    int to = lowerBound(element.getTextRange().getEndOffset());
    for (int i = to - 1; i >= from; i--) {
      var entry = entries.get(i);
      if (matches(entry.token, ">")) {
        var position = positions.toPos(source.originalOffset(entry.end) - 1);
        return new InternalSyntaxToken(position.line(), position.columnOffset(), ">", entry.token.trivias(), false);
      }
    }
    throw new IllegalArgumentException("Missing PSI closing angle in " + element.getTextRange());
  }

  InternalSyntaxToken syntheticAtOriginalOffset(int originalOffset, String text) {
    var position = positions.toPos(originalOffset);
    return new InternalSyntaxToken(position.line(), position.columnOffset(), text, List.of(), false);
  }

  InternalSyntaxToken eof() {
    return entries.getLast().token;
  }

  List<InternalSyntaxToken> within(PsiElement element) {
    int start = element.getTextRange().getStartOffset();
    int end = element.getTextRange().getEndOffset();
    return entries.subList(lowerBound(start), lowerBound(end)).stream().map(Entry::token).toList();
  }

  List<InternalSyntaxToken> tokens() {
    return entries.stream().map(Entry::token).toList();
  }

  private record Entry(int start, int end, InternalSyntaxToken token) {
  }
}
