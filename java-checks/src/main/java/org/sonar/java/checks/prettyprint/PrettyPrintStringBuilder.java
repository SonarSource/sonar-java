/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

package org.sonar.java.checks.prettyprint;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.checks.helpers.QuickFixHelper.contentForTree;
import static org.sonar.java.checks.prettyprint.Associativity.isKnownAssociativeOperator;
import static org.sonar.java.checks.prettyprint.Precedence.precedence;

public final class PrettyPrintStringBuilder {
  private final FileConfig fileConfig;
  private final String baseIndent;

  private final StringBuilder sb = new StringBuilder();
  private int indentLevel = 0;

  public PrettyPrintStringBuilder(FileConfig fileConfig, SyntaxToken indentReferenceToken, boolean indentFirstLine) {
    var baseIndentLevel = indentReferenceToken.firstToken().range().start().column() - 1;
    this.fileConfig = fileConfig;
    this.baseIndent = fileConfig.indentMode().indentCharAsStr().repeat(baseIndentLevel);
    if (indentFirstLine) {
      makeIndent();
    }
  }

  public PrettyPrintStringBuilder add(String str) {
    return addLines(str.lines());
  }

  public PrettyPrintStringBuilder addStripLeading(String str) {
    var remLines = str.lines().map(String::stripLeading);
    return addLines(remLines);
  }

  public PrettyPrintStringBuilder addLines(Stream<String> lines){
    var remLines = lines.iterator();
    while (remLines.hasNext()) {
      var line = remLines.next();
      sb.append(line);
      if (remLines.hasNext()) {
        newLine();
      }
    }
    return this;
  }

  public PrettyPrintStringBuilder addIf(String str, boolean condition){
    if (condition){
      add(str);
    }
    return this;
  }

  public PrettyPrintStringBuilder addWithIndentBasedOnLastLine(String str) {
    var lines = str.lines().toList();
    var numCharsToRemove = numLeadingIndentChars(lines.get(lines.size() - 1));
    return add(str.indent(-numCharsToRemove));
  }

  private int numLeadingIndentChars(String str) {
    var indent = fileConfig.indent();
    var idx = 0;
    while (idx < str.length() && str.startsWith(indent, idx)) {
      idx += indent.length();
    }
    return idx;
  }

  public PrettyPrintStringBuilder addSpace() {
    return add(" ");
  }

  public PrettyPrintStringBuilder newLine() {
    sb.append(fileConfig.endOfLine());
    makeIndent();
    return this;
  }

  public PrettyPrintStringBuilder incIndent() {
    indentLevel += 1;
    return this;
  }

  public PrettyPrintStringBuilder decIndent() {
    indentLevel -= 1;
    if (indentLevel < 0) {
      throw new IllegalStateException("negative indentation level");
    }
    return this;
  }

  public PrettyPrintStringBuilder blockStart() {
    return add("{").incIndent().newLine();
  }

  public PrettyPrintStringBuilder blockEnd() {
    return decIndent().newLine().add("}");
  }

  public PrettyPrintStringBuilder semicolonAndNewLine() {
    sb.append(";");
    newLine();
    return this;
  }

  public PrettyPrintStringBuilder addTreeContentRaw(Tree tree, JavaFileScannerContext ctx){
    return add(contentForTree(tree, ctx));
  }

  public PrettyPrintStringBuilder addTreeContentWithIndentBasedOnLastLine(Tree tree, JavaFileScannerContext ctx){
    return addWithIndentBasedOnLastLine(contentForTree(tree, ctx));
  }

  public <T> PrettyPrintStringBuilder addWithSep(Iterable<T> elems, Consumer<T> elemAdder, Runnable separator) {
    var iter = elems.iterator();
    while (iter.hasNext()) {
      var elem = iter.next();
      elemAdder.accept(elem);
      if (iter.hasNext()) {
        separator.run();
      }
    }
    return this;
  }

  public <T> PrettyPrintStringBuilder addWithSep(Iterable<T> elems, Consumer<T> elemAdder, String separator) {
    return addWithSep(elems, elemAdder, () -> add(separator));
  }

  public <T> PrettyPrintStringBuilder addWithSep(Iterable<T> elems, Runnable separator) {
    return addWithSep(elems, elem -> add(elem.toString()), separator);
  }

  public <T> PrettyPrintStringBuilder addWithSep(Iterable<T> elems, String separator) {
    return addWithSep(elems, () -> add(separator));
  }

  public PrettyPrintStringBuilder addTreesContentWithSep(Iterable<? extends Tree> elems, Runnable separator, JavaFileScannerContext ctx) {
    return addWithSep(elems, elem -> add(contentForTree(elem, ctx)), separator);
  }

  public PrettyPrintStringBuilder addTreesContentWithSep(Iterable<? extends Tree> elems, String separator, JavaFileScannerContext ctx) {
    return addWithSep(elems, elem -> addTreeContentRaw(elem, ctx), separator);
  }

  public PrettyPrintStringBuilder addBinop(ExpressionTree lhs, Tree.Kind operator, ExpressionTree rhs, JavaFileScannerContext ctx){
    var operatorPrecedence = precedence(operator);
    var rhsPrecedence = precedence(rhs);
    var parenthesizeLhs = operatorPrecedence.bindsStrongerThan(precedence(lhs));
    var parenthesizeRhs = operatorPrecedence.bindsStrongerThan(rhsPrecedence)
      || (rhsPrecedence == operatorPrecedence && !isKnownAssociativeOperator(operator));
    addIf("(", parenthesizeLhs);
    addTreeContentRaw(lhs, ctx);
    addIf(")", parenthesizeLhs);
    addSpace();
    add(KindsPrinter.printExprKind(operator));
    addSpace();
    addIf("(", parenthesizeRhs);
    addTreeContentRaw(rhs, ctx);
    addIf(")", parenthesizeRhs);
    return this;
  }

  @Override
  public String toString() {
    return sb.toString();
  }

  private void makeIndent() {
    sb.append(baseIndent).append(fileConfig.indent().repeat(indentLevel));
  }

}
