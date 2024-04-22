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

package org.sonar.java.checks.quickfixes;

public final class PrettyprintStringBuilder {
  private final FileConfig fileConfig;

  private final StringBuilder sb = new StringBuilder();
  private int indentLevel = 0;

  public PrettyprintStringBuilder(FileConfig fileConfig) {
    this.fileConfig = fileConfig;
  }

  public FileConfig fileConfig(){
    return fileConfig;
  }

  public PrettyprintStringBuilder incIndent() {
    indentLevel += 1;
    return this;
  }

  public PrettyprintStringBuilder decIndent() {
    indentLevel -= 1;
    return this;
  }

  public PrettyprintStringBuilder add(String str) {
    var remLines = str.lines().iterator();
    while (remLines.hasNext()){
      var line = remLines.next();
      sb.append(line);
      if (remLines.hasNext()){
        newLine();
      }
    }
    return this;
  }

  public PrettyprintStringBuilder addln(String str){
    add(str);
    newLine();
    return this;
  }

  public PrettyprintStringBuilder addSpace(){
    return add(" ");
  }

  public PrettyprintStringBuilder addComma(){
    return add(", ");
  }

  public PrettyprintStringBuilder newLine() {
    sb.append(fileConfig.endOfLine()).append(fileConfig.indent().repeat(indentLevel));
    return this;
  }

  public PrettyprintStringBuilder newLineIfNotEmpty() {
    if (!lastLineIsEmpty()) {
      newLine();
    }
    return this;
  }

  @Override
  public String toString() {
    return sb.toString();
  }

  public boolean endsWith(char c){
    return !sb.isEmpty() && sb.charAt(sb.length()-1) == c;
  }

  private boolean lastLineIsEmpty() {
    var eol = fileConfig.endOfLine();
    var lastIdx = sb.lastIndexOf(eol);
    return lastIdx != -1 && lastIdx + eol.length() == sb.length();
  }

}
