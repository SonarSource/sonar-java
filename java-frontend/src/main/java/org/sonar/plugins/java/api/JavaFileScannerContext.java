/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import java.io.File;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

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
   * Report an issue at at the project level.
   * @param check The check raising the issue.
   * @param message Message to display to the user
   * @since SonarJava 5.12: Dropping support of file-related methods
   */
  void addIssueOnProject(JavaCheck check, String message);

  /**
   * Report an issue on a specific line. Prefer {@link JavaFileScannerContext#reportIssue(JavaCheck, Tree, String)} for more precise reporting.
   * @param line line on which to report the issue
   * @param check The check raising the issue.
   * @param message Message to display to the user
   */
  void addIssue(int line, JavaCheck check, String message);

  /**
   * Report an issue on a specific line. Prefer {@link JavaFileScannerContext#reportIssue(JavaCheck, Tree, String, List, Integer)} for more precise reporting.
   * @param line line on which to report the issue
   * @param check The check raising the issue.
   * @param message Message to display to the user
   * @param cost computed remediation cost if applicable, null if not.
   */
  void addIssue(int line, JavaCheck check, String message, @Nullable Integer cost);

  /**
   * Get semantic analysis.
   * @return SemanticModel if semantic analysis was successful, null otherwise.
   */
  @Nullable
  Object getSemanticModel();

  /**
   * FileKey of currently analyzed file.
   *
   * @return the fileKey of the file currently analyzed.
   * @deprecated since SonarJava 5.12: Rely on the InputFile key instead, using {@link #getInputFile()}.
   */
  @Deprecated
  String getFileKey();

  /**
   * InputFile under analysis.
   * @return the currently analyzed {@link InputFile}.
   * @since SonarJava 5.12: Dropping support of file-related methods
   */
  InputFile getInputFile();

  /**
   * {@link InputComponent} representing the project being analyzed
   * @return the project component
   * @since SonarJava 5.12: Dropping support of file-related methods
   */
  InputComponent getProject();

  /**
   * The working directory used by the analysis.
   * @return the current working directory.
   */
  File getWorkingDirectory();

  /**
   * Java version defined for the analysis using {@code sonar.java.version} parameter.
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
   * @return source map or {@code null} if there is no source map available
   */
  @Nullable
  default SourceMap sourceMap() {
    return null;
  }

}
