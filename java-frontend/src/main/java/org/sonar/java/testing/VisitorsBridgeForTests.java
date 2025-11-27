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
package org.sonar.java.testing;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.SonarComponents;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.JavaVersionImpl;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;

public class VisitorsBridgeForTests extends VisitorsBridge {

  private List<JavaFileScannerContextForTests> testContexts = new ArrayList<>();
  private JavaFileScannerContextForTests moduleContext;
  private final boolean enableSemantic;

  private VisitorsBridgeForTests(Builder builder) {
    super(builder.visitors, builder.projectClasspath, builder.sonarComponents, builder.javaVersion, builder.inAndroidContext);
    this.enableSemantic = builder.enableSemantic;
  }

  public static class Builder {
    Iterable<? extends JavaCheck> visitors;
    SonarComponents sonarComponents;
    JavaVersion javaVersion;
    List<File> projectClasspath;
    boolean enableSemantic;
    boolean inAndroidContext;

    public Builder(JavaFileScanner visitor) {
      this(Collections.singletonList(visitor));
    }

    public Builder(Iterable<? extends JavaCheck> visitors) {
      this.visitors = visitors;
      this.sonarComponents = null;
      this.javaVersion = new JavaVersionImpl();
      this.projectClasspath = Collections.emptyList();
      this.enableSemantic = false;
      this.inAndroidContext = false;
    }

    public Builder withJavaVersion(JavaVersion javaVersion) {
      this.javaVersion = javaVersion;
      return this;
    }

    public Builder withSonarComponents(SonarComponents sonarComponents) {
      this.sonarComponents = sonarComponents;
      return this;
    }

    public Builder enableSemanticWithProjectClasspath(List<File> projectClasspath) {
      this.projectClasspath = projectClasspath;
      this.enableSemantic = true;
      return this;
    }

    public Builder withAndroidContext(boolean inAndroidContext) {
      this.inAndroidContext = inAndroidContext;
      return this;
    }

    public VisitorsBridgeForTests build() {
      return new VisitorsBridgeForTests(this);
    }
  }

  @VisibleForTesting
  boolean inAndroidContext() {
    return inAndroidContext;
  }

  @Override
  protected JavaFileScannerContext createScannerContext(CompilationUnitTree tree, @Nullable Sema semanticModel, SonarComponents sonarComponents, boolean failedParsing) {
    Sema model = enableSemantic ? semanticModel : null;
    var testContext = new JavaFileScannerContextForTests(tree, currentFile, model, sonarComponents, javaVersion, failedParsing, inAndroidContext, null);
    testContexts.add(testContext);
    return testContext;
  }

  @Override
  protected InputFileScannerContext createScannerContext(
    SonarComponents sonarComponents, InputFile inputFile, JavaVersion javaVersion, boolean inAndroidContext, CacheContext cacheContext) {
    var testContext = new JavaFileScannerContextForTests(null, inputFile, null, sonarComponents, javaVersion, false, inAndroidContext, cacheContext);
    testContexts.add(testContext);
    return testContext;
  }

  @Override
  protected ModuleScannerContext createScannerContext(
    @Nullable SonarComponents sonarComponents, JavaVersion javaVersion, boolean inAndroidContext, @Nullable CacheContext cacheContext) {
    moduleContext = new JavaFileScannerContextForTests(null, null, null, sonarComponents, javaVersion, false, inAndroidContext, cacheContext);
    return moduleContext;
  }

  public List<JavaFileScannerContextForTests> testContexts() {
    return testContexts;
  }

  @CheckForNull
  public JavaFileScannerContextForTests lastCreatedModuleContext() {
    return moduleContext;
  }
}
