/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
