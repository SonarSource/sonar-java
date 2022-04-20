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
package org.sonar.java.model;

import java.io.File;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.SonarComponents;
import org.sonar.java.caching.CacheContextImpl;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.caching.CacheContext;

public class DefaultInputFileScannerContext implements InputFileScannerContext {
  protected final SonarComponents sonarComponents;
  protected final InputFile inputFile;
  protected final JavaVersion javaVersion;
  protected final boolean inAndroidContext;
  protected final CacheContext cacheContext;

  public DefaultInputFileScannerContext(@Nullable SonarComponents sonarComponents, InputFile inputFile, JavaVersion javaVersion, boolean inAndroidContext,
                                        @Nullable CacheContext cacheContext) {
    this.sonarComponents = sonarComponents;
    this.inputFile = inputFile;
    this.javaVersion = javaVersion;
    this.inAndroidContext = inAndroidContext;
    if (cacheContext != null) {
      this.cacheContext = cacheContext;
    } else {
      this.cacheContext = CacheContextImpl.of(sonarComponents != null ? sonarComponents.context() : null);
    }
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
  public void addIssueOnProject(JavaCheck check, String message) {
    sonarComponents.addIssue(getProject(), check, -1, message, 0);
  }

  @Override
  public JavaVersion getJavaVersion() {
    return this.javaVersion;
  }

  @Override
  public boolean inAndroidContext() {
    return inAndroidContext;
  }

  @Override
  public InputFile getInputFile() {
    return inputFile;
  }

  @Override
  public InputComponent getProject() {
    return sonarComponents.project();
  }

  @Override
  public File getWorkingDirectory() {
    return sonarComponents.workDir();
  }

  @Override
  public CacheContext getCacheContext() {
    return cacheContext;
  }
}
