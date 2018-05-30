/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
package org.sonar.java;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.InstantiationStrategy;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.config.Configuration;

/**
 * This class computes the working directory of the project in order to collect UCFGs
 * in one single location to have cross module analysis
 */
@ScannerSide
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public class UCFGCollector {

  private UCFGJavaVisitor ucfgJavaVisitor;

  public UCFGCollector(Configuration config) {
    this.ucfgJavaVisitor = new UCFGJavaVisitor(projectWorkDirFromConfig(config));
  }

  public UCFGJavaVisitor getVisitor() {
    return ucfgJavaVisitor;
  }

  @VisibleForTesting
  static File projectWorkDirFromConfig(Configuration config) {
    File baseDir = new File(config.get("sonar.projectBaseDir").orElseThrow(() -> new IllegalStateException("sonar.projectBaseDir is not defined")));
    String workDir = config.get(CoreProperties.WORKING_DIRECTORY).orElse("");
    if (StringUtils.isBlank(workDir)) {
      return new File(baseDir, CoreProperties.WORKING_DIRECTORY_DEFAULT_VALUE);
    }

    File customWorkDir = new File(workDir);
    if (customWorkDir.isAbsolute()) {
      return customWorkDir;
    }
    return new File(baseDir, customWorkDir.getPath());
  }
}

