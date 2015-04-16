/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.design.Dependency;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.graph.DirectedGraph;
import org.sonar.java.ast.AstScanner;
import org.sonar.java.ast.visitors.FileLinesVisitor;
import org.sonar.java.ast.visitors.FileVisitor;
import org.sonar.java.ast.visitors.SyntaxHighlighterVisitor;
import org.sonar.java.bytecode.BytecodeScanner;
import org.sonar.java.bytecode.visitor.DependenciesVisitor;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.plugins.java.api.JavaResourceLocator;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.api.Query;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceCodeSearchEngine;
import org.sonar.squidbridge.indexer.SquidIndex;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class JavaSquid implements SourceCodeSearchEngine {

  private static final Logger LOG = LoggerFactory.getLogger(JavaSquid.class);

  private final SquidIndex squidIndex;
  private final AstScanner astScanner;
  private final AstScanner astScannerForTests;
  private final BytecodeScanner bytecodeScanner;
  private final DirectedGraph<Resource, Dependency> graph = new DirectedGraph<>();

  private boolean bytecodeScanned = false;

  @VisibleForTesting
  public JavaSquid(JavaConfiguration conf, JavaResourceLocator javaResourceLocator, CodeVisitor... visitors) {
    this(conf, null, null, javaResourceLocator, visitors);
  }

  public JavaSquid(JavaConfiguration conf,
                   @Nullable SonarComponents sonarComponents, @Nullable Measurer measurer,
                   JavaResourceLocator javaResourceLocator, CodeVisitor... visitors) {

    astScanner = JavaAstScanner.create(conf);

    Iterable<CodeVisitor> visitorsToBridge = Iterables.concat(Arrays.asList(javaResourceLocator), Arrays.asList(visitors));
    if(measurer != null) {
      Iterable<CodeVisitor> measurers = Arrays.asList((CodeVisitor)measurer);
      visitorsToBridge =  Iterables.concat(visitorsToBridge, measurers);
    }
    List<File> classpath = Lists.newArrayList();
    List<File> testClasspath = Lists.newArrayList();
    Collection<CodeVisitor> testCheckClasses = Lists.<CodeVisitor>newArrayList(javaResourceLocator);
    if(sonarComponents != null) {
      classpath = sonarComponents.getJavaClasspath();
      testClasspath = sonarComponents.getJavaTestClasspath();
      testCheckClasses.addAll(sonarComponents.testCheckClasses());
    }
    setupAstScanner(astScanner, visitorsToBridge, classpath, conf, sonarComponents);
    // TODO unchecked cast
    squidIndex = (SquidIndex) astScanner.getIndex();

    bytecodeScanner = new BytecodeScanner(squidIndex, javaResourceLocator);
    bytecodeScanner.accept(new DependenciesVisitor(graph));

    // External visitors (typically Check ones):
    for (CodeVisitor visitor : visitors) {
      if (visitor instanceof CharsetAwareVisitor) {
        ((CharsetAwareVisitor) visitor).setCharset(conf.getCharset());
      }
      astScanner.accept(visitor);
      bytecodeScanner.accept(visitor);
    }

    astScannerForTests = new AstScanner(astScanner);
    astScannerForTests.accept(new FileVisitor());
    setupAstScanner(astScannerForTests, testCheckClasses, testClasspath, conf, sonarComponents);
  }

  private void setupAstScanner(AstScanner astScanner, Iterable<CodeVisitor> visitorsToBridge,
                               List<File> classpath, JavaConfiguration conf, @Nullable SonarComponents sonarComponents) {
    if(sonarComponents != null) {
      astScanner.accept(new FileLinesVisitor(sonarComponents, conf.getCharset()));
      astScanner.accept(new SyntaxHighlighterVisitor(sonarComponents, conf.getCharset()));
    }
    VisitorsBridge visitorsBridgeTest = new VisitorsBridge(visitorsToBridge, classpath, sonarComponents);
    visitorsBridgeTest.setCharset(conf.getCharset());
    visitorsBridgeTest.setAnalyseAccessors(conf.separatesAccessorsFromMethods());
    astScanner.accept(visitorsBridgeTest);
  }



  public void scan(Iterable<File> sourceFiles, Iterable<File> testFiles, Collection<File> bytecodeFilesOrDirectories) {
    scanSources(sourceFiles);
    scanBytecode(bytecodeFilesOrDirectories);
    scanTests(testFiles);
  }

  private void scanSources(Iterable<File> sourceFiles) {
    TimeProfiler profiler = new TimeProfiler(getClass()).start("Java Main Files AST scan");
    astScanner.scan(sourceFiles);
    profiler.stop();
  }

  private void scanTests(Iterable<File> testFiles) {
    TimeProfiler profiler = new TimeProfiler(getClass()).start("Java Test Files AST scan");
    astScannerForTests.simpleScan(testFiles);
    profiler.stop();
  }

  private void scanBytecode(Collection<File> bytecodeFilesOrDirectories) {
    if (hasBytecode(bytecodeFilesOrDirectories)) {
      TimeProfiler profiler = new TimeProfiler(getClass()).start("Java bytecode scan");

      bytecodeScanner.scan(bytecodeFilesOrDirectories);
      bytecodeScanned = true;
      profiler.stop();
    } else {
      LOG.warn("Java bytecode has not been made available to the analyzer. The " + Joiner.on(", ").join(bytecodeScanner.getVisitors()) + " are disabled.");
      bytecodeScanned = false;
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

  public boolean isBytecodeScanned() {
    return bytecodeScanned;
  }

  public SquidIndex getIndex() {
    return squidIndex;
  }

  public DirectedGraph<Resource, Dependency> getGraph() {
    return graph;
  }

  @Override
  public SourceCode search(String key) {
    return squidIndex.search(key);
  }

  @Override
  public Collection<SourceCode> search(Query... query) {
    return squidIndex.search(query);
  }

}
