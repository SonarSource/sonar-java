/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.java.annotations.Beta;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Context injected in check classes and used to report issues.
 */
@Beta
public interface JavaFileScannerContext extends InputFileScannerContext {

  /**
   * Parsed tree of the current file.
   * @return CompilationUnitTree ready for scan by checks.
   */
  CompilationUnitTree getTree();

  /**
   * Get semantic analysis.
   * @return SemanticModel if semantic analysis was successful, null otherwise.
   */
  @Nullable
  Object getSemanticModel();

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
   * Lines of the currently analyzed file.
   * @return list of file lines.
   */
  List<String> getFileLines();

  /**
   * Content of the currently analyzed file.
   * @return the file content as a String.
   */
  String getFileContent();

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
      this.syntaxNode = Objects.requireNonNull(syntaxNode);
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

  /**
   * Return JSR 45 source map for current input file
   * @return source map if available
   */
  default Optional<SourceMap> sourceMap() {
    return Optional.empty();
  }
}
