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
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.sonar.plugins.java.api.tree.SyntaxToken;

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
    UNUSED_IMPORT(IProblem.UnusedImport, JavaCore.COMPILER_PB_UNUSED_IMPORT),
    ASSIGNMENT_HAS_NO_EFFECT(IProblem.AssignmentHasNoEffect, JavaCore.COMPILER_PB_NO_EFFECT_ASSIGNMENT);

    private final int warningID;
    private final String compilerOptionKey;

    private static final Set<String> COMPILER__OPTIONS = new HashSet<>();

    Type(int warningID, String compilerOptionKey) {
      this.warningID = warningID;
      this.compilerOptionKey = compilerOptionKey;
    }

    boolean isMatching(IProblem warning) {
      return warning.getID() == warningID;
    }

    public static Set<String> compilerOptions() {
      if (COMPILER__OPTIONS.isEmpty()) {
        Stream.of(Type.values())
          .map(t -> t.compilerOptionKey)
          .forEach(COMPILER__OPTIONS::add);
      }
      return Collections.unmodifiableSet(COMPILER__OPTIONS);
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

  public boolean contains(SyntaxToken syntaxToken) {
    int tokenLine = syntaxToken.line();
    int tokenStartColumn = syntaxToken.column();
    int tokendEndColumn = tokenStartColumn + syntaxToken.text().length();

    if (startLine == endLine) {
      return startLine == tokenLine
        && startColumn <= tokenStartColumn
        && endColumn >= tokendEndColumn;
    }
    if (startLine == tokenLine) {
      return startColumn <= tokenStartColumn;
    }
    if (endLine == tokenLine) {
      return endColumn >= tokendEndColumn;
    }
    return tokenLine > startLine && tokenLine < endLine;
  }
}
