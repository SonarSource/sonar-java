/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.plugins.java.api;

import com.google.common.annotations.Beta;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.io.File;
import java.util.List;
import java.util.Objects;

@Beta
public interface JavaFileScannerContext {

  CompilationUnitTree getTree();

  /**
   * @deprecated use reportIssue instead to benefit from precise issue location.
   */
  @Deprecated
  void addIssue(Tree tree, JavaCheck check, String message);

  /**
   * @deprecated use reportIssue instead to benefit from precise issue location.
   */
  @Deprecated
  void addIssue(Tree tree, JavaCheck check, String message, @Nullable Double cost);

  void addIssueOnFile(JavaCheck check, String message);

  void addIssue(int line, JavaCheck check, String message);

  void addIssue(int line, JavaCheck javaCheck, String message, @Nullable Double cost);

  void addIssue(File file, JavaCheck check, int line, String message);

  @Nullable
  Object getSemanticModel();

  String getFileKey();

  File getFile();

  JavaVersion getJavaVersion();

  boolean fileParsed();

  List<Tree> getComplexityNodes(Tree tree);

  List<Tree> getMethodComplexityNodes(ClassTree enclosingClass, MethodTree methodTree);

  void reportIssue(JavaCheck javaCheck, Tree tree, String message);

  void reportIssue(JavaCheck javaCheck, Tree tree, String message, List<Location> secondaryLocations, @Nullable Integer cost);

  void reportIssue(JavaCheck javaCheck, Tree starTree, Tree endTree, String message);

  class Location {
    public final String msg;
    public final Tree syntaxNode;

    public Location(String msg, Tree syntaxNode) {
      this.msg = msg;
      this.syntaxNode = syntaxNode;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Location location = (Location) o;
      return Objects.equals(msg, location.msg) &&
        Objects.equals(syntaxNode, location.syntaxNode);
    }

    @Override
    public int hashCode() {
      return Objects.hash(msg, syntaxNode);
    }
  }
}
