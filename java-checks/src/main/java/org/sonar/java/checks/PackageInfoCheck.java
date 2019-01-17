/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.collect.Sets;
import java.io.File;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;

@Rule(key = "S1228")
public class PackageInfoCheck implements JavaFileScanner {

  Set<File> directoriesWithoutPackageFile = Sets.newHashSet();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    File parentFile = context.getFile().getParentFile();

    if (!packageInfoFileExists(parentFile)
      && !directoriesWithoutPackageFile.contains(parentFile)
      && !context.isTestFile()) {
      context.addIssue(parentFile, PackageInfoCheck.this, -1, "Add a 'package-info.java' file to document the '" + parentFile.getName() + "' package");
      directoriesWithoutPackageFile.add(parentFile);
    }
  }

  private static boolean packageInfoFileExists(File parentDirectory) {
    return new File(parentDirectory, "package-info.java").isFile();
  }

}
