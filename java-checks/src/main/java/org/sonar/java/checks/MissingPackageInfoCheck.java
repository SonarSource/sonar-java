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
