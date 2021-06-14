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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongUnaryOperator;
import java.util.stream.Collectors;
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

import static java.nio.charset.StandardCharsets.UTF_8;

public class JavaSquid {

  private static final Logger LOG = Loggers.get(JavaSquid.class);

  public static long startTime = System.nanoTime();
  public static List<Long> issueLatency = new ArrayList<>();
  public static long[] issuesPerPercentOfFileAnalysisTime = new long[101];

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
    startMeasureLatencyBeforeAllFiles();
    scanAndMeasureTask(sourceFiles, astScanner::scan, "Main");
    scanAndMeasureTask(testFiles, astScannerForTests::scan, "Test");
    scanAndMeasureTask(generatedFiles, astScannerForGeneratedFiles::scan, "Generated");
    endMeasureLatencyAfterAllFiles();
  }

  public static void startMeasureLatencyBeforeAllFiles() {
    issuesPerPercentOfFileAnalysisTime = new long[101];
  }

  public static void endMeasureLatencyAfterAllFiles() {
    try {
      Path rulingLatencyFile = Paths.get("/tmp/ruling-latency.txt");
      if (Files.exists(rulingLatencyFile)) {
        List<String> lines = Files.readAllLines(rulingLatencyFile, UTF_8);
        Preconditions.checkArgument(lines.size() == 101);
        for (int i = 0; i < lines.size(); i++) {
          issuesPerPercentOfFileAnalysisTime[i] += Long.parseLong(lines.get(i));
        }
      }
      Files.write(rulingLatencyFile, Arrays.stream(issuesPerPercentOfFileAnalysisTime)
        .mapToObj(String::valueOf)
        .collect(Collectors.joining("\n")).getBytes(UTF_8));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void startMeasureLatencyBeforeAFile() {
    startTime = System.nanoTime();
    issueLatency = new ArrayList<>();
  }

  public static void endMeasureLatencyAfterAFile() {
    long fileDuration = System.nanoTime() - JavaSquid.startTime;
    LongUnaryOperator numberOfIssueBeforeAGivenTime = time -> JavaSquid.issueLatency.stream()
      .filter(duration -> duration < time)
      .count();
    for (int i = 0; i <= 100; i++) {
      issuesPerPercentOfFileAnalysisTime[i] += numberOfIssueBeforeAGivenTime.applyAsLong(fileDuration * i / 100);
    }
  }

  public static void addOneIssueLatency() {
    issueLatency.add(System.nanoTime() - JavaSquid.startTime);
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
