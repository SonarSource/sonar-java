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

package org.sonar.java.prettyprint;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.tree.SyntaxToken;

import static org.sonar.plugins.java.api.precedence.Precedence.precedence;

public final class PrettyPrintStringBuilder {
  private final FileConfig fileConfig;
  private final String baseIndent;

  private final StringBuilder sb = new StringBuilder();
  private int indentLevel = 0;

  public PrettyPrintStringBuilder(FileConfig fileConfig, @Nullable SyntaxToken indentReferenceToken, boolean indentFirstLine) {
    var baseIndentLevel = indentReferenceToken == null ? 0 : (indentReferenceToken.firstToken().range().start().column() - 1);
    this.fileConfig = fileConfig;
    this.baseIndent = fileConfig.indentMode().indentCharAsStr().repeat(baseIndentLevel);
    if (indentFirstLine) {
      makeIndent();
    }
  }

  public PrettyPrintStringBuilder add(String str) {
    return addLines(str.lines());
  }

  public PrettyPrintStringBuilder addLines(Stream<String> lines) {
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

  public PrettyPrintStringBuilder addSpace() {
    return add(" ");
  }

  public PrettyPrintStringBuilder addSemicolon(){
    return add(";");
  }

  public PrettyPrintStringBuilder forceSemicolon(){
    if (!(endsWithIgnoreSpaces(";") || endsWithIgnoreSpaces("}"))){
      addSemicolon();
    }
    return this;
  }

  public PrettyPrintStringBuilder newLine() {
    sb.append(fileConfig.endOfLine());
    makeIndent();
    return this;
  }

  /**
   * Increment current indentation level
   */
  public PrettyPrintStringBuilder incIndent() {
    indentLevel += 1;
    return this;
  }

  /**
   * Decrement current indentation level
   */
  public PrettyPrintStringBuilder decIndent() {
    indentLevel -= 1;
    if (indentLevel < 0) {
      throw new IllegalStateException("negative indentation level");
    }
    return this;
  }

  /**
   * Start a block: "{" followed by a new line with one more level of indentation
   */
  public PrettyPrintStringBuilder blockStart() {
    return add("{").incIndent().newLine();
  }

  /**
   * End a block: new line and "}", after reducing the indentation level
   */
  public PrettyPrintStringBuilder blockEnd() {
    return decIndent().newLine().add("}");
  }

  public boolean endsWithIgnoreSpaces(String s) {
    // TODO optimize(?)
    var chars = sb.toString();
    for (var i = chars.length()-1; i >= 0; i--){
      var c = chars.charAt(i);
      if (c == ';' || c == '}'){
        return true;
      } else if (!Character.isWhitespace(c)){
        return false;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return sb.toString();
  }

  private void makeIndent() {
    sb.append(baseIndent).append(fileConfig.indent().repeat(indentLevel));
  }

}
