/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.java.PerformanceMeasure.Duration;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.visitors.FileLinesVisitor;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.ast.visitors.SyntaxHighlighterVisitor;
import org.sonar.java.collections.CollectionUtils;
import org.sonar.java.collections.ListUtils;
import org.sonar.java.filters.SonarJavaIssueFilter;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;

public class JavaSquid {

  private static final Logger LOG = Loggers.get(JavaSquid.class);

  private final JavaAstScanner astScanner;
  private final JavaAstScanner astScannerForTests;
  private final JavaAstScanner astScannerForGeneratedFiles;

  public JavaSquid(JavaVersion javaVersion,
                   @Nullable SonarComponents sonarComponents, @Nullable Measurer measurer,
                   JavaResourceLocator javaResourceLocator, @Nullable SonarJavaIssueFilter postAnalysisIssueFilter, JavaCheck... visitors) {
    this(javaVersion, sonarComponents, measurer, javaResourceLocator, postAnalysisIssueFilter, null, visitors);
  }

  public JavaSquid(JavaVersion javaVersion,
                   @Nullable SonarComponents sonarComponents, @Nullable Measurer measurer,
                   JavaResourceLocator javaResourceLocator, @Nullable SonarJavaIssueFilter postAnalysisIssueFilter,
                   @Nullable SubscriptionVisitor symbolicExecutionEngine, JavaCheck... visitors) {

    List<JavaCheck> commonVisitors = new ArrayList<>();
    commonVisitors.add(javaResourceLocator);
    if (postAnalysisIssueFilter != null) {
      commonVisitors.add(postAnalysisIssueFilter);
    }

    List<SubscriptionVisitor> seVisitor = symbolicExecutionEngine == null ? Collections.emptyList() : Collections.singletonList(symbolicExecutionEngine);
    Iterable<JavaCheck> codeVisitors = ListUtils.concat(seVisitor, commonVisitors, Arrays.asList(visitors));
    Collection<JavaCheck> testCodeVisitors = new ArrayList<>(commonVisitors);
    if (measurer != null) {
      Iterable<JavaCheck> measurers = Collections.singletonList(measurer);
      codeVisitors = ListUtils.concat(measurers, codeVisitors);
      testCodeVisitors.add(measurer.new TestFileMeasurer());
    }
    List<File> classpath = new ArrayList<>();
    List<File> testClasspath = new ArrayList<>();
    List<JavaCheck> jspCodeVisitors = new ArrayList<>();
    List<File> jspClasspath = new ArrayList<>();
    if (sonarComponents != null) {
      if(!sonarComponents.isSonarLintContext()) {
        codeVisitors = ListUtils.concat(codeVisitors, Arrays.asList(new FileLinesVisitor(sonarComponents), new SyntaxHighlighterVisitor(sonarComponents)));
        testCodeVisitors.add(new SyntaxHighlighterVisitor(sonarComponents));
      }
      classpath = sonarComponents.getJavaClasspath();
      testClasspath = sonarComponents.getJavaTestClasspath();
      jspClasspath = sonarComponents.getJspClasspath();
      testCodeVisitors.addAll(sonarComponents.testCheckClasses());
      jspCodeVisitors = sonarComponents.jspCodeVisitors();
    }

    //AstScanner for main files
    astScanner = new JavaAstScanner(sonarComponents);
    astScanner.setVisitorBridge(createVisitorBridge(codeVisitors, classpath, javaVersion, sonarComponents));

    //AstScanner for test files
    astScannerForTests = new JavaAstScanner(sonarComponents);
    astScannerForTests.setVisitorBridge(createVisitorBridge(testCodeVisitors, testClasspath, javaVersion, sonarComponents));

    //AstScanner for generated files
    astScannerForGeneratedFiles = new JavaAstScanner(sonarComponents);
    astScannerForGeneratedFiles.setVisitorBridge(createVisitorBridge(jspCodeVisitors, jspClasspath, javaVersion, sonarComponents));
  }

  private static VisitorsBridge createVisitorBridge(
    Iterable<JavaCheck> codeVisitors, List<File> classpath, JavaVersion javaVersion, @Nullable SonarComponents sonarComponents) {
    VisitorsBridge visitorsBridge = new VisitorsBridge(codeVisitors, classpath, sonarComponents);
    visitorsBridge.setJavaVersion(javaVersion);
    return visitorsBridge;
  }

  public void scan(Iterable<InputFile> sourceFiles, Iterable<InputFile> testFiles, Iterable<? extends InputFile> generatedFiles) {
    scanAndMeasureTask(sourceFiles, astScanner::scan, "Main");
    scanAndMeasureTask(testFiles, astScannerForTests::scan, "Test");
    scanAndMeasureTask(generatedFiles, astScannerForGeneratedFiles::scan, "Generated");
  }

  private static <T> void scanAndMeasureTask(Iterable<T> files, Consumer<Iterable<T>> action, String descriptor) {
    if (CollectionUtils.size(files) > 0) {
      Duration mainDuration = PerformanceMeasure.start(descriptor);
      Profiler profiler = Profiler.create(LOG).startInfo(String.format("Java \"%s\" source files AST scan", descriptor));

      action.accept(files);

      profiler.stopInfo();
      mainDuration.stop();
    } else {
      LOG.info(String.format("No \"%s\" source files to scan.", descriptor));
    }
  }
}
