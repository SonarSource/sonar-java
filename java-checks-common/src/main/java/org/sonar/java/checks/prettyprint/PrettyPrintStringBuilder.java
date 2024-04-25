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

public final class PrettyPrintStringBuilder {
  private final FileConfig fileConfig;
  private final String baseIndent;

  private final StringBuilder sb = new StringBuilder();
  private int indentLevel = 0;

  public PrettyPrintStringBuilder(FileConfig fileConfig, String baseIndent, boolean indentFirstLine) {
    this.fileConfig = fileConfig;
    this.baseIndent = baseIndent;
    if (indentFirstLine) {
      makeIndent();
    }
  }

  public PrettyPrintStringBuilder add(String str) {
    var remLines = str.lines().iterator();
    while (remLines.hasNext()) {
      var line = remLines.next();
      sb.append(line);
      if (remLines.hasNext()) {
        newLine();
      }
    }
    return this;
  }

  public PrettyPrintStringBuilder addWithIndentBasedOnLastLine(String str) {
    var lines = str.lines().toList();
    var numCharsToRemove = numLeadingIndentChars(lines.get(lines.size()-1));
    return add(str.indent(-numCharsToRemove));
  }

  private int numLeadingIndentChars(String str){
    var indent = fileConfig.indent();
    var idx = 0;
    while (idx < str.length() && str.startsWith(indent, idx)){
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

  public <T> PrettyPrintStringBuilder addWithSep(Iterable<T> elems, Consumer<T> elemAdder, Consumer<T> separator) {
    var iter = elems.iterator();
    while (iter.hasNext()) {
      var elem = iter.next();
      elemAdder.accept(elem);
      if (iter.hasNext()) {
        separator.accept(elem);
      }
    }
    return this;
  }

  @Override
  public String toString() {
    if (indentLevel != 0) {
      throw new IllegalStateException("protocol violation: trying to build PrettyPrintString with an indentation level different of 0 (" + indentLevel + ")");
    }
    return sb.toString();
  }

  private void makeIndent() {
    sb.append(baseIndent).append(fileConfig.indent().repeat(indentLevel));
  }

}
