/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
  private static final String MESSAGE = "File path \"%s\" should match package name \"%s\". Move file or change package name";

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    PackageDeclarationTree packageDeclaration = tree.packageDeclaration();
    if (packageDeclaration != null) {
      String packageName = PackageUtils.packageName(packageDeclaration, File.separator);
      File javaFile = context.getInputFile().file();
      String dir = javaFile.getParent();
      if (!dir.endsWith(packageName)) {
        String dirWithoutDots = dir.replace(".", File.separator);
        String truncatedPath = dir;

        File rootDirectory = context.getRootProjectWorkingDirectory();
        File fileDirectory = javaFile.getParentFile();

        if(rootDirectory!=null){
          String rootName = rootDirectory.getName();
          List<String> path = new ArrayList<>();

          while (fileDirectory != null && !fileDirectory.getName().equals(rootName)) {
            path.add(0, fileDirectory.getName());
            fileDirectory = fileDirectory.getParentFile();
          }

          truncatedPath = String.join(File.separator, path);
        }

        String issueMessage = String.format(MESSAGE, truncatedPath, packageName.replace(File.separator, "."));

        if (dirWithoutDots.endsWith(packageName)) {
          context.reportIssue(this, packageDeclaration.packageName(), issueMessage + "(Do not use dots in directory names).");
        } else {
          context.reportIssue(this, packageDeclaration.packageName(), issueMessage + ".");
        }
      }
    }
  }

}
