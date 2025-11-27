/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.plugins.java.api;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.plugins.java.api.tree.Tree;

public interface InputFileScannerContext extends ModuleScannerContext {

  /**
   * Report an issue at file level.
   * @param check The check raising the issue.
   * @param message Message to display to the user
   */
  void addIssueOnFile(JavaCheck check, String message);

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
   * InputFile under analysis.
   * @return the currently analyzed {@link InputFile}.
   * @since SonarJava 5.12: Dropping support of file-related methods
   */
  InputFile getInputFile();

}
