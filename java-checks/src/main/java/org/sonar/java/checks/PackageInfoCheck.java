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
import java.util.HashSet;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.java.EndOfAnalysisCheck;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;

@Rule(key = "S1228")
public class PackageInfoCheck implements JavaFileScanner, EndOfAnalysisCheck {

  @VisibleForTesting
  final Set<String> missingPackageWithoutPackageFile = new HashSet<>();
  private final Set<String> knownPackageWithPackageFile = new HashSet<>();
  private JavaFileScannerContext context;

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;

    PackageDeclarationTree packageDeclaration = context.getTree().packageDeclaration();
    if (packageDeclaration == null) {
      // default package
      return;
    }

    String packageName = ExpressionsHelper.concatenate(packageDeclaration.packageName());

    File parentFile = context.getInputFile().file().getParentFile();
    if (!new File(parentFile, "package-info.java").isFile()) {
      missingPackageWithoutPackageFile.add(packageName);
    } else {
      knownPackageWithPackageFile.add(packageName);
    }
  }

  @Override
  public void endOfAnalysis() {
    missingPackageWithoutPackageFile.removeAll(knownPackageWithPackageFile);
    for (String missingPackageInfo : missingPackageWithoutPackageFile) {
      context.addIssueOnProject(this, "Add a 'package-info.java' file to document the '" + missingPackageInfo + "' package");
    }
  }

}
