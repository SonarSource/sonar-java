/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

/**
 * Context injected in check classes and used to report issues.
 */
@Beta
public interface JavaFileScannerContext {

  /**
   * Parsed tree of the current file.
   * @return CompilationUnitTree ready for scan by checks.
   */
  CompilationUnitTree getTree();

  /**
   * Report an issue at file level.
   * @param check The check raising the issue.
   * @param message Message to display to the user
   */
  void addIssueOnFile(JavaCheck check, String message);

  /**
   * Report an issue on a specific line.
   * @see {@link JavaFileScannerContext#reportIssue(JavaCheck, Tree, String)} which should be prefered as reporting will be more precise.
   * @param line line on which to report the issue
   * @param check The check raising the issue.
   * @param message Message to display to the user
   */
  void addIssue(int line, JavaCheck check, String message);

  /**
   * Report an issue on a specific line.
   * @see {@link JavaFileScannerContext#reportIssue(JavaCheck, Tree, String, List, Integer)} which should be prefered as reporting will be more precise.
   * @param line line on which to report the issue
   * @param check The check raising the issue.
   * @param message Message to display to the user
   * @param cost computed remediation cost if applicable, null if not.
   */
  void addIssue(int line, JavaCheck check, String message, @Nullable Integer cost);

  /**
   * Report an issue at a specific line of a given file.
   * This method is used for one
   * @param file File on which to report
   * @param check The check raising the issue.
   * @param line line on which to report the issue
   * @param message Message to display to the user
   */
  void addIssue(File file, JavaCheck check, int line, String message);

  /**
   * Get semantic analysis.
   * @return SemanticModel if semantic analysis was successful, null otherwise.
   */
  @Nullable
  Object getSemanticModel();

  /**
   * FileKey of currently analyzed file.
   * @return the fileKey of the file currently analyzed.
   */
  String getFileKey();

  /**
   * File under analysis.
   * @return the currently analysed file.
   */
  File getFile();

  /**
   * Java version defined for the analysis using sonar.java.version parameter.
   * @return JavaVersion object with API to act on it.
   */
  JavaVersion getJavaVersion();

  /**
   * Checks if file has been parsed correctly.
   * @return true if parsing was successful
   */
  boolean fileParsed();

  /**
   * Computes the list of syntax nodes which are contributing to increase the complexity for the given methodTree.
   * @param tree the tree to compute the complexity.
   * @return the list of syntax nodes incrementing the complexity.
   */
  List<Tree> getComplexityNodes(Tree tree);

  /**
   * Computes the list of syntax nodes which are contributing to increase the complexity for the given methodTree.
   * @deprecated use {@link #getComplexityNodes(Tree)} instead
   * @param enclosingClass not used.
   * @param methodTree the methodTree to compute the complexity.
   * @return the list of syntax nodes incrementing the complexity.
   */
  @Deprecated
  List<Tree> getMethodComplexityNodes(ClassTree enclosingClass, MethodTree methodTree);

  /**
   * Report an issue.
   * @param javaCheck check raising the issue
   * @param tree syntax node on which to raise the issue.
   * @param message Message to display to the user.
   */
  void reportIssue(JavaCheck javaCheck, Tree tree, String message);

  /**
   * Report an issue.
   * @param javaCheck check raising the issue
   * @param tree syntax node on which to raise the issue.
   * @param message Message to display to the user.
   * @param secondaryLocations List of {@link Location} to display secondary location for the issue.
   * @param cost computed remediation cost if applicable, null if not.
   */
  void reportIssue(JavaCheck javaCheck, Tree tree, String message, List<Location> secondaryLocations, @Nullable Integer cost);


  /**
   * Report an issue.
   * @param javaCheck check raising the issue
   * @param tree syntax node on which to raise the issue.
   * @param message Message to display to the user.
   * @param flows List of list of {@link Location} to display flows for the issue.
   * @param cost computed remediation cost if applicable, null if not.
   */
  void reportIssueWithFlow(JavaCheck javaCheck, Tree tree, String message, Iterable<List<Location>> flows, @Nullable Integer cost);

  /**
   * Report an issue.
   * @param javaCheck check raising the issue
   * @param startTree syntax node on which to start the highlighting of the issue.
   * @param endTree syntax node on which to end the highlighting of the issue.
   * @param message Message to display to the user.
   */
  void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message);

  /**
   * Report an issue.
   * @param javaCheck check raising the issue
   * @param startTree syntax node on which to start the highlighting of the issue.
   * @param endTree syntax node on which to end the highlighting of the issue.
   * @param message Message to display to the user.
   * @param secondaryLocations List of {@link Location} to display secondary location for the issue.
   * @param cost computed remediation cost if applicable, null if not.
   */
  void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message, List<Location> secondaryLocations, @Nullable Integer cost);

  /**
   * Message and syntaxNode for a secondary location.
   */
  class Location {
    /**
     * Message of the secondary location.
     */
    public final String msg;
    /**
     * Syntax node on which to raise the secondary location.
     */
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
