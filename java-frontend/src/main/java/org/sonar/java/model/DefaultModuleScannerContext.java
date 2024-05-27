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

import java.io.File;
import javax.annotation.Nullable;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.java.SonarComponents;
import org.sonar.java.caching.CacheContextImpl;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;

public class DefaultModuleScannerContext implements ModuleScannerContext {
  // TODO: Why is this field not treated as nullable, even though the constructor accepts a nullable argument?
  @Nullable
  protected final SonarComponents sonarComponents;
  protected final JavaVersion javaVersion;
  protected final boolean inAndroidContext;
  protected final CacheContext cacheContext;

  public DefaultModuleScannerContext(@Nullable SonarComponents sonarComponents, JavaVersion javaVersion, boolean inAndroidContext, @Nullable CacheContext cacheContext) {
    this.sonarComponents = sonarComponents;
    this.javaVersion = javaVersion;
    this.inAndroidContext = inAndroidContext;
    if (cacheContext != null) {
      this.cacheContext = cacheContext;
    } else {
      this.cacheContext = CacheContextImpl.of(sonarComponents);
    }
  }

  public void addIssueOnProject(JavaCheck check, String message) {
    sonarComponents.addIssue(getProject(), check, -1, message, 0);
  }

  public JavaVersion getJavaVersion() {
    return this.javaVersion;
  }

  public boolean inAndroidContext() {
    return inAndroidContext;
  }

  public InputComponent getProject() {
    return sonarComponents.project();
  }

  @Override
  public File getWorkingDirectory() {
    return sonarComponents.projectLevelWorkDir();
  }

  public CacheContext getCacheContext() {
    return cacheContext;
  }

  public void reportIssue(AnalyzerMessage message) {
    sonarComponents.reportIssue(message);
  }

  @Override
  public File getRootProjectWorkingDirectory() {
    return sonarComponents.projectLevelWorkDir();
  }

  @Override
  public String getModuleKey() {
    return sonarComponents.getModuleKey();
  }

  @Override
  public SonarProduct sonarProduct() {
    if (sonarComponents == null) {
      return null;
    }

    var context = sonarComponents.context();
    if (context == null) {
      return null;
    }

    return context.runtime().getProduct();
  }
}
