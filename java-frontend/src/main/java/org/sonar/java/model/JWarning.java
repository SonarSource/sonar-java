/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;

public final class JWarning {

  private final String message;
  private final Type type;
  private final int startLine;
  private final int startColumn;
  private final int endLine;
  private final int endColumn;

  public JWarning(String message, Type type, int startLine, int startColumn, int endLine, int endColumn) {
    this.message = message;
    this.type = type;
    this.startLine = startLine;
    this.startColumn = startColumn;
    this.endLine = endLine;
    this.endColumn = endColumn;
  }

  public enum Type {
    UNUSED_IMPORT(IProblem.UnusedImport);

    private final int warningID;

    Type(int warningID) {
      this.warningID = warningID;
    }

    boolean isMatching(IProblem warning) {
      return warning.getID() == warningID;
    }
  }

  public static Map<Type, List<JWarning>> getWarnings(CompilationUnit astNode) {
    Map<JWarning.Type, List<JWarning>> results = new EnumMap<>(Type.class);
    for (IProblem warning : astNode.getProblems()) {
      for (Type type : Type.values()) {
        if (type.isMatching(warning)) {
          JWarning newWarning = new JWarning(
            warning.getMessage(),
            type,
            warning.getSourceLineNumber(),
            astNode.getColumnNumber(warning.getSourceStart()),
            astNode.getLineNumber(warning.getSourceEnd()),
            astNode.getColumnNumber(warning.getSourceEnd()));
          results.computeIfAbsent(type, k -> new ArrayList<>()).add(newWarning);
        }
      }
    }
    return results;
  }

  public String getMessage() {
    return message;
  }

  public Type getType() {
    return type;
  }

  public int getStartLine() {
    return startLine;
  }

  public int getStartColumn() {
    return startColumn;
  }

  public int getEndLine() {
    return endLine;
  }

  public int getEndColumn() {
    return endColumn;
  }
}
