/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.sonar.sslr.api.typed.ActionParser;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.ast.visitors.FileLinesVisitor;
import org.sonar.java.ast.visitors.SyntaxHighlighterVisitor;
import org.sonar.java.filters.SonarJavaIssueFilter;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.se.SymbolicExecutionMode;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.Tree;

public class JavaSquid {

  private static final Logger LOG = Loggers.get(JavaSquid.class);

  private final JavaAstScanner astScanner;
  private final JavaAstScanner astScannerForTests;
  private final boolean scanTestsLikeSources;

  public JavaSquid(JavaVersion javaVersion,
    @Nullable SonarComponents sonarComponents, @Nullable Measurer measurer,
    JavaResourceLocator javaResourceLocator, @Nullable SonarJavaIssueFilter postAnalysisIssueFilter, JavaCheck... visitors) {
    this(javaVersion, false, false, sonarComponents, measurer, javaResourceLocator, postAnalysisIssueFilter, visitors);
  }

  public JavaSquid(JavaVersion javaVersion, boolean xFileEnabled, boolean scanTestsLikeSources,
                   @Nullable SonarComponents sonarComponents, @Nullable Measurer measurer,
                   JavaResourceLocator javaResourceLocator, @Nullable SonarJavaIssueFilter postAnalysisIssueFilter, JavaCheck... visitors) {

    this.scanTestsLikeSources = scanTestsLikeSources;

    List<JavaCheck> commonVisitors = new ArrayList<>();
    commonVisitors.add(javaResourceLocator);
    if (postAnalysisIssueFilter != null) {
      commonVisitors.add(postAnalysisIssueFilter);
    }

    List<JavaCheck> codeVisitors = new ArrayList<>(commonVisitors);
    codeVisitors.addAll(Arrays.asList(visitors));

    List<JavaCheck> testCodeVisitors = new ArrayList<>(commonVisitors);
    if (measurer != null) {
      codeVisitors.add(0, measurer);
      if (!scanTestsLikeSources) {
        testCodeVisitors.add(0, measurer.new TestFileMeasurer());
      }
    }
    List<File> classpath = new ArrayList<>();
    List<File> testClasspath = new ArrayList<>();
    if (sonarComponents != null) {
      if(!sonarComponents.isSonarLintContext()) {
        codeVisitors.add(new FileLinesVisitor(sonarComponents));
        codeVisitors.add(new SyntaxHighlighterVisitor(sonarComponents));
        if (!scanTestsLikeSources) {
          testCodeVisitors.add(new SyntaxHighlighterVisitor(sonarComponents));
        }
      }
      classpath = sonarComponents.getJavaClasspath();
      testClasspath = sonarComponents.getJavaTestClasspath();
      testCodeVisitors.addAll(sonarComponents.testCheckClasses());

      if (scanTestsLikeSources) {
        classpath.addAll(sonarComponents.getJavaTestClasspath());
      }
    }

    //AstScanner for main files
    ActionParser<Tree> parser = JavaParser.createParser();
    astScanner = new JavaAstScanner(parser, sonarComponents);
    VisitorsBridge visitorsBridgeForMain = createVisitorBridge(codeVisitors, classpath, javaVersion, sonarComponents, SymbolicExecutionMode.getMode(visitors, xFileEnabled));
    astScanner.setVisitorBridge(visitorsBridgeForMain);

    //AstScanner for test files
    astScannerForTests = new JavaAstScanner(parser, sonarComponents);
    VisitorsBridge visitorBridgeForTests = createVisitorBridge(testCodeVisitors, testClasspath, javaVersion, sonarComponents, SymbolicExecutionMode.DISABLED);
    astScannerForTests.setVisitorBridge(visitorBridgeForTests);
  }

  private static VisitorsBridge createVisitorBridge(
    Iterable<JavaCheck> codeVisitors, List<File> classpath, JavaVersion javaVersion, @Nullable SonarComponents sonarComponents, SymbolicExecutionMode symbolicExecutionMode) {
    VisitorsBridge visitorsBridge = new VisitorsBridge(codeVisitors, classpath, sonarComponents, symbolicExecutionMode);
    visitorsBridge.setJavaVersion(javaVersion);
    return visitorsBridge;
  }

  public void scan(Collection<File> sourceFiles, Collection<File> testFiles) {
    if (scanTestsLikeSources) {
      List<File> newSourceFiles = new ArrayList<>(sourceFiles);
      newSourceFiles.addAll(testFiles);
      scanSources(newSourceFiles);
    } else {
      scanSources(sourceFiles);
      scanTests(testFiles);
    }
  }

  private void scanSources(Collection<File> sourceFiles) {
    Profiler profiler = Profiler.create(LOG).startInfo("Java Main Files AST scan");
    astScanner.scan(sourceFiles);
    profiler.stopInfo();
  }

  private void scanTests(Collection<File> testFiles) {
    Profiler profiler = Profiler.create(LOG).startInfo("Java Test Files AST scan");
    astScannerForTests.scan(testFiles);
    profiler.stopInfo();
  }

}
