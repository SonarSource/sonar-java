/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
import java.util.HashSet;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.ModuleScannerContext;

@Rule(key = "S1228")
public class MissingPackageInfoCheck extends AbstractPackageInfoChecker {

  @VisibleForTesting
  final Set<String> missingPackageWithoutPackageFile = new HashSet<>();
  private final Set<String> knownPackageWithPackageFile = new HashSet<>();

  @Override
  protected void processFile(InputFileScannerContext context, String packageName) {
    if (knownPackageWithPackageFile.contains(packageName)) {
      return;
    }

    File parentFile = context.getInputFile().file().getParentFile();
    if (!new File(parentFile, "package-info.java").isFile()) {
      missingPackageWithoutPackageFile.add(packageName);
    } else {
      knownPackageWithPackageFile.add(packageName);
    }
  }

  @Override
  public void endOfAnalysis(ModuleScannerContext context) {
    missingPackageWithoutPackageFile.removeAll(knownPackageWithPackageFile);
    for (String missingPackageInfo : missingPackageWithoutPackageFile) {
      context.addIssueOnProject(this, "Add a 'package-info.java' file to document the '" + missingPackageInfo + "' package");
    }
  }
}
