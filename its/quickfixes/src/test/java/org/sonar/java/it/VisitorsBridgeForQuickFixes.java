/*
 * SonarQube Java
 * Copyright (C) 2024-2025 SonarSource SA
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
package org.sonar.java.it;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.SonarComponents;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.testing.JavaFileScannerContextForTests;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

public class VisitorsBridgeForQuickFixes extends VisitorsBridge {

  private final Map<Path, List<JavaQuickFix>> quickFixes = new HashMap<>();

  private JavaFileScannerContextForTests testContext;
  private JavaFileScannerContextForTests moduleContext;
  private boolean enableSemantic = true;

  public VisitorsBridgeForQuickFixes(Iterable<? extends JavaCheck> visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents, JavaVersion javaVersion) {
    super(visitors, projectClasspath, sonarComponents, javaVersion);
  }

  @Override
  protected JavaFileScannerContext createScannerContext(CompilationUnitTree tree, @Nullable Sema semanticModel, SonarComponents sonarComponents, boolean failedParsing) {
    Sema model = enableSemantic ? semanticModel : null;
    addAllQuickfixes(testContext);
    testContext = new JavaFileScannerContextForTests(tree, currentFile, model, sonarComponents, javaVersion, failedParsing, inAndroidContext, null);
    return testContext;
  }

  @Override
  protected InputFileScannerContext createScannerContext(
    SonarComponents sonarComponents, InputFile inputFile, JavaVersion javaVersion, boolean inAndroidContext, CacheContext cacheContext
  ) {
    addAllQuickfixes(testContext);
    testContext = new JavaFileScannerContextForTests(null, inputFile, null, sonarComponents, javaVersion, false, inAndroidContext, cacheContext);
    return testContext;
  }

  @Override
  protected ModuleScannerContext createScannerContext(
    @Nullable SonarComponents sonarComponents, JavaVersion javaVersion, boolean inAndroidContext, @Nullable CacheContext cacheContext
  ) {
    addAllQuickfixes(moduleContext);
    moduleContext = new JavaFileScannerContextForTests(null, null, null, sonarComponents, javaVersion, false, inAndroidContext, cacheContext);
    return moduleContext;
  }

  public Map<Path, List<JavaQuickFix>> getQuickFixes() {
    return quickFixes;
  }

  private void addAllQuickfixes(JavaFileScannerContextForTests context) {
    if (context == null) {
      return;
    }
    Path p = context.getInputFile().path();
    for (var entry : context.getQuickFixes().entrySet()) {
      quickFixes.put(p, entry.getValue());
    }
  }

}
