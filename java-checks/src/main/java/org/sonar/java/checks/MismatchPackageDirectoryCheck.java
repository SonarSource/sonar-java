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
package org.sonar.java.checks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.PackageUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;

@Rule(key = "S1598")
public class MismatchPackageDirectoryCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  private static final String MESSAGE = "File path \"%s\" should match package name \"%s\". Move the file or change the package name";

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    PackageDeclarationTree packageDeclaration = tree.packageDeclaration();
    if (packageDeclaration == null) {
      return;
    }

    String packageName = PackageUtils.packageName(packageDeclaration, File.separator);
    File fileDirectory = context.getInputFile().file().getParentFile();
    String fileDirectoryPath = fileDirectory.getPath();
    boolean packageNameIsSuffixOfDirPath = fileDirectoryPath.endsWith(packageName);

    // In this case, path matches package name.
    if (packageNameIsSuffixOfDirPath) {
      return;
    }

    File rootProjectWorkingDirectory = null;
    try {
      rootProjectWorkingDirectory = context.getRootProjectWorkingDirectory();
    } catch (NullPointerException ignored) {
      // RootProjectWorkingDirectory is initialized to null.
      // NullPointerExceptions should not be thrown when accessing rootDirectory, but for now some context do it. See: SONARJAVA-5158.
    }

    String truncatedFilePath = fileDirectoryPath;

    if (rootProjectWorkingDirectory != null) {
      truncatedFilePath = truncateFileDirectoryPath(
        fileDirectory,
        rootProjectWorkingDirectory.getName()
      );
    }

    String issueMessage = String.format(MESSAGE, truncatedFilePath, packageName.replace(File.separator, "."));

    String dirWithoutDots = fileDirectoryPath.replace(".", File.separator);
    if (dirWithoutDots.endsWith(packageName)) {
      context.reportIssue(this, packageDeclaration.packageName(), issueMessage + "(Do not use dots in directory names).");
    } else {
      context.reportIssue(this, packageDeclaration.packageName(), issueMessage + ".");
    }
  }

  private static String truncateFileDirectoryPath(File fileDirectory, String rootProjectDirectoryName) {
    List<String> path = new ArrayList<>();

    while (fileDirectory != null && !fileDirectory.getName().equals(rootProjectDirectoryName)) {
      path.add(0, fileDirectory.getName());
      fileDirectory = fileDirectory.getParentFile();
    }

    return String.join(File.separator, path);
  }

}
