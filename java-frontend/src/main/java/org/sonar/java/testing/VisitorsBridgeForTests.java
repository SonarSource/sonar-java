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
package org.sonar.java.testing;

import java.io.File;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.SonarComponents;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.model.Sema;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

public class VisitorsBridgeForTests extends VisitorsBridge {

  private JavaFileScannerContextForTests testContext;
  private JavaFileScannerContextForTests moduleContext;
  private boolean enableSemantic = true;


  @VisibleForTesting
  public VisitorsBridgeForTests(JavaFileScanner visitor, SonarComponents sonarComponents) {
    this(Collections.singletonList(visitor), Collections.emptyList(), sonarComponents, new JavaVersionImpl());
  }

  public VisitorsBridgeForTests(Iterable<? extends JavaCheck> visitors, @Nullable SonarComponents sonarComponents, JavaVersion javaVersion) {
    super(visitors, Collections.emptyList(), sonarComponents, javaVersion);
    enableSemantic = false;
  }

  public VisitorsBridgeForTests(Iterable<? extends JavaCheck> visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents, JavaVersion javaVersion) {
    super(visitors, projectClasspath, sonarComponents, javaVersion);
  }

  @Override
  protected JavaFileScannerContext createScannerContext(CompilationUnitTree tree, @Nullable Sema semanticModel, SonarComponents sonarComponents, boolean failedParsing) {
    Sema model = enableSemantic ? semanticModel : null;
    testContext = new JavaFileScannerContextForTests(tree, currentFile, model, sonarComponents, javaVersion, failedParsing, inAndroidContext, null);
    return testContext;
  }

  @Override
  protected InputFileScannerContext createScannerContext(
    SonarComponents sonarComponents, InputFile inputFile, JavaVersion javaVersion, boolean inAndroidContext, CacheContext cacheContext
  ) {
    testContext = new JavaFileScannerContextForTests(null, inputFile, null, sonarComponents, javaVersion, false, inAndroidContext, cacheContext);
    return testContext;
  }

  @Override
  protected ModuleScannerContext createScannerContext(
    @Nullable SonarComponents sonarComponents, JavaVersion javaVersion, boolean inAndroidContext, @Nullable CacheContext cacheContext
  ) {
    moduleContext = new JavaFileScannerContextForTests(null, null, null, sonarComponents, javaVersion, false, inAndroidContext, cacheContext);
    return moduleContext;
  }

  public JavaFileScannerContextForTests lastCreatedTestContext() {
    return testContext;
  }

  public JavaFileScannerContextForTests lastCreatedModuleContext() {
    return moduleContext;
  }
}
