/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.api.utils.log.Profiler;
import org.sonar.java.ast.JavaAstScanner;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.ast.visitors.FileLinesVisitor;
import org.sonar.java.ast.visitors.SyntaxHighlighterVisitor;
import org.sonar.java.bytecode.BytecodeScanner;
import org.sonar.java.bytecode.visitor.BytecodeContext;
import org.sonar.java.bytecode.visitor.DefaultBytecodeContext;
import org.sonar.java.filters.CodeVisitorIssueFilter;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.JavaResourceLocator;
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
  private final BytecodeScanner bytecodeScanner;

  public JavaSquid(JavaConfiguration conf,
                   @Nullable SonarComponents sonarComponents, @Nullable Measurer measurer,
                   JavaResourceLocator javaResourceLocator, @Nullable CodeVisitorIssueFilter postAnalysisIssueFilter, CodeVisitor... visitors) {

    List<CodeVisitor> commonVisitors = Lists.<CodeVisitor>newArrayList(javaResourceLocator);
    if (postAnalysisIssueFilter != null) {
      commonVisitors.add(postAnalysisIssueFilter);
    }

    Iterable<CodeVisitor> codeVisitors = Iterables.concat(commonVisitors, Arrays.asList(visitors));
    Collection<CodeVisitor> testCodeVisitors = Lists.newArrayList(commonVisitors);
    if (measurer != null) {
      Iterable<CodeVisitor> measurers = Collections.singletonList((CodeVisitor) measurer);
      codeVisitors = Iterables.concat(measurers, codeVisitors);
      testCodeVisitors.add(measurer.new TestFileMeasurer());
    }
    List<File> classpath = Lists.newArrayList();
    List<File> testClasspath = Lists.newArrayList();
    if (sonarComponents != null) {
      codeVisitors = Iterables.concat(
          codeVisitors,
          Arrays.asList(
              new FileLinesVisitor(sonarComponents, conf.getCharset()),
              new SyntaxHighlighterVisitor(sonarComponents, conf.getCharset())
          )
      );
      testCodeVisitors.add(new SyntaxHighlighterVisitor(sonarComponents, conf.getCharset()));
      classpath = sonarComponents.getJavaClasspath();
      testClasspath = sonarComponents.getJavaTestClasspath();
      testCodeVisitors.addAll(sonarComponents.testCheckClasses());
    }

    //AstScanner for main files
    astScanner = new JavaAstScanner(JavaParser.createParser(conf.getCharset()));
    boolean enableSymbolicExecution = hasASymbolicExecutionCheck(visitors);
    astScanner.setVisitorBridge(createVisitorBridge(codeVisitors, classpath, conf, sonarComponents, enableSymbolicExecution));

    //AstScanner for test files
    astScannerForTests = new JavaAstScanner(astScanner);
    astScannerForTests.setVisitorBridge(createVisitorBridge(testCodeVisitors, testClasspath, conf, sonarComponents, false));

    //Bytecode scanner
    BytecodeContext bytecodeContext = new DefaultBytecodeContext(sonarComponents, javaResourceLocator);
    bytecodeScanner = new BytecodeScanner(bytecodeContext);
    for (CodeVisitor visitor : visitors) {
      bytecodeScanner.accept(visitor);
    }

  }

  private static boolean hasASymbolicExecutionCheck(CodeVisitor[] visitors) {
    for (CodeVisitor visitor : visitors) {
      if(visitor instanceof SECheck) {
        return true;
      }
    }
    return false;
  }

  private static VisitorsBridge createVisitorBridge(
      Iterable<CodeVisitor> codeVisitors, List<File> classpath, JavaConfiguration conf, @Nullable SonarComponents sonarComponents, boolean enableSymbolicExecution) {
    VisitorsBridge visitorsBridge = new VisitorsBridge(codeVisitors, classpath, sonarComponents, enableSymbolicExecution);
    visitorsBridge.setCharset(conf.getCharset());
    visitorsBridge.setAnalyseAccessors(conf.separatesAccessorsFromMethods());
    visitorsBridge.setJavaVersion(conf.javaVersion());
    return visitorsBridge;
  }


  public void scan(Iterable<File> sourceFiles, Iterable<File> testFiles, Collection<File> bytecodeFilesOrDirectories) {
    scanSources(sourceFiles);
    scanBytecode(bytecodeFilesOrDirectories);
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

  private void scanBytecode(Collection<File> bytecodeFilesOrDirectories) {
    if (hasBytecode(bytecodeFilesOrDirectories)) {
      Profiler profiler = Profiler.create(LOG).startInfo("Java bytecode scan");

      bytecodeScanner.scan(bytecodeFilesOrDirectories);
      profiler.stopInfo();
    } else {
      LOG.warn("Java bytecode has not been made available to the analyzer. The " + Joiner.on(", ").join(bytecodeScanner.getVisitors()) + " are disabled.");
    }
  }

  static boolean hasBytecode(Collection<File> bytecodeFilesOrDirectories) {
    if (bytecodeFilesOrDirectories == null) {
      return false;
    }
    for (File bytecodeFilesOrDirectory : bytecodeFilesOrDirectories) {
      if (bytecodeFilesOrDirectory.exists() &&
          (bytecodeFilesOrDirectory.isFile() ||
              !FileUtils.listFiles(bytecodeFilesOrDirectory, new String[]{"class"}, true).isEmpty())) {
        return true;
      }
    }
    return false;
  }

}
