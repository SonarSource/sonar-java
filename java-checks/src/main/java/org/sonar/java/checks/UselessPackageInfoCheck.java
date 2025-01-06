/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;

@Rule(key = "S4032")
public class UselessPackageInfoCheck extends AbstractPackageInfoChecker {

  private final Map<String, InputFileScannerContext> unneededPackageInfoFiles = new HashMap<>();
  private final Set<String> knownPackagesWithOtherFiles = new HashSet<>();

  @Override
  protected void processFile(InputFileScannerContext context, String packageName) {
    if (knownPackagesWithOtherFiles.contains(packageName)) {
      // already processed package
      return;
    }

    File packageDirectory = context.getInputFile().file().getParentFile();
    File packageInfoFile = new File(packageDirectory, "package-info.java");
    boolean hasOtherFiles = !isOnlyFileFromPackage(packageDirectory, packageInfoFile);

    if (hasOtherFiles) {
      knownPackagesWithOtherFiles.add(packageName);
    } else if (packageInfoFile.isFile()) {
      unneededPackageInfoFiles.put(packageName, context);
    }
  }

  @Override
  public void endOfAnalysis(ModuleScannerContext context) {
    unneededPackageInfoFiles.keySet().removeAll(knownPackagesWithOtherFiles);
    for (var uselessPackageInfoFileContext : unneededPackageInfoFiles.values()) {
      uselessPackageInfoFileContext.addIssueOnFile(this, "Remove this package.");
    }
    unneededPackageInfoFiles.clear();
    knownPackagesWithOtherFiles.clear();
  }

  private static boolean isOnlyFileFromPackage(File packageDirectory, File file) {
    File[] filesInPackage = packageDirectory.listFiles(f -> !f.equals(file));
    return filesInPackage != null && filesInPackage.length == 0;
  }
}
