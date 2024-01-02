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
package org.sonar.java.model;

import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.SonarComponents;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.caching.CacheContext;

public class DefaultInputFileScannerContext extends DefaultModuleScannerContext implements InputFileScannerContext {
  protected final InputFile inputFile;

  public DefaultInputFileScannerContext(@Nullable SonarComponents sonarComponents, InputFile inputFile, JavaVersion javaVersion, boolean inAndroidContext,
                                        @Nullable CacheContext cacheContext) {
    super(sonarComponents, javaVersion, inAndroidContext, cacheContext);
    this.inputFile = inputFile;
  }

  @Override
  public void addIssueOnFile(JavaCheck javaCheck, String message) {
    addIssue(-1, javaCheck, message);
  }

  @Override
  public void addIssue(int line, JavaCheck javaCheck, String message) {
    addIssue(line, javaCheck, message, null);
  }

  @Override
  public void addIssue(int line, JavaCheck javaCheck, String message, @Nullable Integer cost) {
    sonarComponents.addIssue(inputFile, javaCheck, line, message, cost);
  }

  @Override
  public InputFile getInputFile() {
    return inputFile;
  }

}
