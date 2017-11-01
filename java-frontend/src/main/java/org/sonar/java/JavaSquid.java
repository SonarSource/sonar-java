/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.typed.ActionParser;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.ast.visitors.FileLinesVisitor;
import org.sonar.java.ast.visitors.SyntaxHighlighterVisitor;
import org.sonar.java.filters.CodeVisitorIssueFilter;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.se.SymbolicExecutionMode;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.CodeVisitor;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class JavaSquid {

  private static final Logger LOG = Loggers.get(JavaSquid.class);

  private final JavaAstScanner astScanner;
  private final JavaAstScanner astScannerForTests;

  public JavaSquid(JavaVersion javaVersion,
    @Nullable SonarComponents sonarComponents, @Nullable Measurer measurer,
    JavaResourceLocator javaResourceLocator, @Nullable CodeVisitorIssueFilter postAnalysisIssueFilter, CodeVisitor... visitors) {
    this(javaVersion, false, sonarComponents, measurer, javaResourceLocator, postAnalysisIssueFilter, visitors);
  }

  public JavaSquid(JavaVersion javaVersion, boolean xFileEnabled,
                   @Nullable SonarComponents sonarComponents, @Nullable Measurer measurer,
                   JavaResourceLocator javaResourceLocator, @Nullable CodeVisitorIssueFilter postAnalysisIssueFilter, CodeVisitor... visitors) {

    List<CodeVisitor> commonVisitors = Lists.newArrayList(javaResourceLocator);
    if (postAnalysisIssueFilter != null) {
      commonVisitors.add(postAnalysisIssueFilter);
    }

    Iterable<CodeVisitor> codeVisitors = Iterables.concat(commonVisitors, Arrays.asList(visitors));
    Collection<CodeVisitor> testCodeVisitors = Lists.newArrayList(commonVisitors);
    if (measurer != null) {
      Iterable<CodeVisitor> measurers = Collections.singletonList(measurer);
      codeVisitors = Iterables.concat(measurers, codeVisitors);
      testCodeVisitors.add(measurer.new TestFileMeasurer());
    }
    List<File> classpath = Lists.newArrayList();
    List<File> testClasspath = Lists.newArrayList();
    if (sonarComponents != null) {
      if(!sonarComponents.isSonarLintContext()) {
        codeVisitors = Iterables.concat(
          codeVisitors,
          Arrays.asList(
            new FileLinesVisitor(sonarComponents),
            new SyntaxHighlighterVisitor(sonarComponents)
          )
        );
        testCodeVisitors.add(new SyntaxHighlighterVisitor(sonarComponents));
      }
      classpath = sonarComponents.getJavaClasspath();
      testClasspath = sonarComponents.getJavaTestClasspath();
      testCodeVisitors.addAll(sonarComponents.testCheckClasses());
    }

    //AstScanner for main files
    ActionParser<Tree> parser = JavaParser.createParser();
    astScanner = new JavaAstScanner(parser, sonarComponents);
    astScanner.setVisitorBridge(createVisitorBridge(codeVisitors, classpath, javaVersion, sonarComponents, SymbolicExecutionMode.getMode(visitors, xFileEnabled)));

    //AstScanner for test files
    astScannerForTests = new JavaAstScanner(parser, sonarComponents);
    astScannerForTests.setVisitorBridge(createVisitorBridge(testCodeVisitors, testClasspath, javaVersion, sonarComponents, SymbolicExecutionMode.DISABLED));

  }

  private static VisitorsBridge createVisitorBridge(
    Iterable<CodeVisitor> codeVisitors, List<File> classpath, JavaVersion javaVersion, @Nullable SonarComponents sonarComponents, SymbolicExecutionMode symbolicExecutionMode) {
    VisitorsBridge visitorsBridge = new VisitorsBridge(codeVisitors, classpath, sonarComponents, symbolicExecutionMode);
    visitorsBridge.setJavaVersion(javaVersion);
    return visitorsBridge;
  }


  public void scan(Iterable<File> sourceFiles, Iterable<File> testFiles) {
    scanSources(sourceFiles);
    scanTests(testFiles);
  }

  private void scanSources(Iterable<File> sourceFiles) {
    Profiler profiler = Profiler.create(LOG).startInfo("Java Main Files AST scan");
    astScanner.scan(sourceFiles);
    profiler.stopInfo();
  }

  private void scanTests(Iterable<File> testFiles) {
    Profiler profiler = Profiler.create(LOG).startInfo("Java Test Files AST scan");
    astScannerForTests.scan(testFiles);
    profiler.stopInfo();
  }

}
