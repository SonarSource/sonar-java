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
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.graph.DirectedGraph;
import org.sonar.graph.DirectedGraphAccessor;
import org.sonar.java.ast.AstScanner;
import org.sonar.java.ast.visitors.ClassVisitor;
import org.sonar.java.ast.visitors.FileLinesVisitor;
import org.sonar.java.ast.visitors.FileVisitor;
import org.sonar.java.ast.visitors.PackageVisitor;
import org.sonar.java.ast.visitors.SyntaxHighlighterVisitor;
import org.sonar.java.ast.visitors.TestVisitor;
import org.sonar.java.bytecode.BytecodeScanner;
import org.sonar.java.bytecode.visitor.DependenciesVisitor;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squidbridge.api.CodeVisitor;
import org.sonar.squidbridge.api.Query;
import org.sonar.squidbridge.api.SourceCode;
import org.sonar.squidbridge.api.SourceCodeEdge;
import org.sonar.squidbridge.api.SourceCodeSearchEngine;
import org.sonar.squidbridge.indexer.QueryByType;
import org.sonar.squidbridge.indexer.SquidIndex;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class JavaSquid implements DirectedGraphAccessor<SourceCode, SourceCodeEdge>, SourceCodeSearchEngine {

  private static final Logger LOG = LoggerFactory.getLogger(JavaSquid.class);

  private final SquidIndex squidIndex;
  private final AstScanner astScanner;
  private final AstScanner astScannerForTests;
  private final BytecodeScanner bytecodeScanner;
  private final DirectedGraph<SourceCode, SourceCodeEdge> graph = new DirectedGraph<SourceCode, SourceCodeEdge>();

  private boolean bytecodeScanned = false;

  @VisibleForTesting
  public JavaSquid(JavaConfiguration conf, CodeVisitor... visitors) {
    this(conf, null, visitors);
  }

  public JavaSquid(JavaConfiguration conf, @Nullable SonarComponents sonarComponents, CodeVisitor... visitors) {

    astScanner = JavaAstScanner.create(conf);

    Iterable<CodeVisitor> visitorsToBridge = Arrays.asList(visitors);
    if (sonarComponents != null) {
      visitorsToBridge = Iterables.concat(
          sonarComponents.createJavaFileScanners(),
          visitorsToBridge
      );
    }
    VisitorsBridge visitorsBridge = new VisitorsBridge(visitorsToBridge, sonarComponents);
    astScanner.accept(visitorsBridge);

    if (sonarComponents != null) {
      astScanner.accept(new FileLinesVisitor(sonarComponents, conf.getCharset()));
      astScanner.accept(new SyntaxHighlighterVisitor(sonarComponents, conf.getCharset()));
    }

    // TODO unchecked cast
    squidIndex = (SquidIndex) astScanner.getIndex();

    bytecodeScanner = new BytecodeScanner(squidIndex);
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
    astScannerForTests.accept(new PackageVisitor());
    astScannerForTests.accept(new FileVisitor());
    astScannerForTests.accept(new TestVisitor());
    astScannerForTests.accept(new ClassVisitor());
  }

  @VisibleForTesting
  public void scanDirectories(Collection<File> sourceDirectories, Collection<File> bytecodeFilesOrDirectories) {
    List<InputFile> sourceFiles = Lists.newArrayList();
    for (File dir : sourceDirectories) {
      sourceFiles.addAll(InputFileUtils.create(dir, FileUtils.listFiles(dir, new String[]{"java"}, true)));
    }
    scan(sourceFiles, Collections.<InputFile>emptyList(), bytecodeFilesOrDirectories);
  }

  public void scan(Collection<InputFile> sourceFiles, Collection<InputFile> testFiles, Collection<File> bytecodeFilesOrDirectories) {
    scanSources(sourceFiles);
    scanBytecode(bytecodeFilesOrDirectories);
    scanTests(testFiles);
  }

  private void scanSources(Collection<InputFile> sourceFiles) {
    TimeProfiler profiler = new TimeProfiler(getClass()).start("Java Main Files AST scan");
    astScanner.scan(sourceFiles);
    profiler.stop();
  }

  private void scanTests(Collection<InputFile> testFiles) {
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

  public DirectedGraph<SourceCode, SourceCodeEdge> getGraph() {
    return graph;
  }

  @Override
  public SourceCodeEdge getEdge(SourceCode from, SourceCode to) {
    return graph.getEdge(from, to);
  }

  @Override
  public boolean hasEdge(SourceCode from, SourceCode to) {
    return graph.hasEdge(from, to);
  }

  @Override
  public Set<SourceCode> getVertices() {
    return graph.getVertices();
  }

  @Override
  public Collection<SourceCodeEdge> getOutgoingEdges(SourceCode from) {
    return graph.getOutgoingEdges(from);
  }

  @Override
  public Collection<SourceCodeEdge> getIncomingEdges(SourceCode to) {
    return graph.getIncomingEdges(to);
  }

  public List<SourceCodeEdge> getEdges(Collection<SourceCode> sourceCodes) {
    return graph.getEdges(sourceCodes);
  }

  public Collection<SourceCode> search(QueryByType queryByType) {
    return squidIndex.search(queryByType);
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
