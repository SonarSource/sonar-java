/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.io.File;
import java.util.Set;

@Rule(
  key = PackageInfoCheck.RULE_KEY,
  name = "Packages should have a javadoc file 'package-info.java'",
  tags = {"convention"},
  priority = Priority.MINOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("20min")
public class PackageInfoCheck implements JavaFileScanner {

  public static final String RULE_KEY = "S1228";

  private Set<File> directoriesWithPackageFile = Sets.newHashSet();
  private Set<File> directoriesWithoutPackageFile = Sets.newHashSet();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    File parentFile = context.getFile().getParentFile();
    if(!directoriesWithPackageFile.contains(parentFile)) {
      directoriesWithoutPackageFile.add(parentFile);
    }
    if ("package-info.java".equals(context.getFile().getName())) {
      directoriesWithoutPackageFile.remove(parentFile);
      directoriesWithPackageFile.add(parentFile);
    }
  }

  public Set<File> getDirectoriesWithoutPackageFile() {
    return directoriesWithoutPackageFile;
  }
}
