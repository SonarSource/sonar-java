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
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.sonar.api.resources.InputFile;
import org.sonar.api.resources.InputFileUtils;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.graph.DirectedGraph;
import org.sonar.graph.DirectedGraphAccessor;
import org.sonar.java.ast.AstScanner;
import org.sonar.java.ast.visitors.FileLinesVisitor;
import org.sonar.java.ast.visitors.SymbolTableVisitor;
import org.sonar.java.ast.visitors.SyntaxHighlighterVisitor;
import org.sonar.java.bytecode.BytecodeScanner;
import org.sonar.java.bytecode.ClassLoaderBuilder;
import org.sonar.java.bytecode.asm.AsmClassProviderImpl;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.java.bytecode.visitor.DITVisitor;
import org.sonar.java.bytecode.visitor.DependenciesVisitor;
import org.sonar.java.bytecode.visitor.LCOM4Visitor;
import org.sonar.java.bytecode.visitor.NOCVisitor;
import org.sonar.java.bytecode.visitor.RFCVisitor;
import org.sonar.squid.api.CodeVisitor;
import org.sonar.squid.api.Query;
import org.sonar.squid.api.SourceCode;
import org.sonar.squid.api.SourceCodeEdge;
import org.sonar.squid.api.SourceCodeSearchEngine;
import org.sonar.squid.indexer.QueryByType;
import org.sonar.squid.indexer.SquidIndex;

import javax.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class JavaSquid implements DirectedGraphAccessor<SourceCode, SourceCodeEdge>, SourceCodeSearchEngine {

  private final SquidIndex squidIndex;
  private final AstScanner astScanner;
  private final BytecodeScanner bytecodeScanner;
  private final DirectedGraph<SourceCode, SourceCodeEdge> graph = new DirectedGraph<SourceCode, SourceCodeEdge>();

  private boolean bytecodeScanned = false;

  @VisibleForTesting
  public JavaSquid(JavaConfiguration conf, CodeVisitor... visitors) {
    this(conf, null, visitors);
  }

  public JavaSquid(JavaConfiguration conf, @Nullable SonarComponents sonarComponents, CodeVisitor... visitors) {
    astScanner = JavaAstScanner.create(conf);
    if (sonarComponents != null) {
      astScanner.accept(new FileLinesVisitor(sonarComponents.getFileLinesContextFactory(), conf.getCharset()));
      astScanner.accept(new SyntaxHighlighterVisitor(sonarComponents.getResourcePerspectives(), conf.getCharset()));
      astScanner.accept(new SymbolTableVisitor(sonarComponents.getResourcePerspectives()));
    }

    // TODO unchecked cast
    squidIndex = (SquidIndex) astScanner.getIndex();

    bytecodeScanner = new BytecodeScanner(squidIndex);
    bytecodeScanner.accept(new DITVisitor());
    bytecodeScanner.accept(new RFCVisitor());
    bytecodeScanner.accept(new NOCVisitor());
    bytecodeScanner.accept(new LCOM4Visitor(conf.getFieldsToExcludeFromLcom4Calculation()));
    bytecodeScanner.accept(new DependenciesVisitor(graph));

    // External visitors (typically Check ones):
    for (CodeVisitor visitor : visitors) {
      if (visitor instanceof CharsetAwareVisitor) {
        ((CharsetAwareVisitor) visitor).setCharset(conf.getCharset());
      }
      if (visitor instanceof SourceAndBytecodeVisitor) {
        astScanner.accept(((SourceAndBytecodeVisitor) visitor).getSourceVisitor());
      }
      astScanner.accept(visitor);
      bytecodeScanner.accept(visitor);
    }
  }

  @VisibleForTesting
  public void scanDirectories(Collection<File> sourceDirectories, Collection<File> bytecodeFilesOrDirectories) {
    List<InputFile> sourceFiles = Lists.newArrayList();
    for (File dir : sourceDirectories) {
      sourceFiles.addAll(InputFileUtils.create(dir, FileUtils.listFiles(dir, new String[] {"java"}, true)));
    }
    scan(sourceFiles, bytecodeFilesOrDirectories);
  }

  public void scan(Collection<InputFile> sourceFiles, Collection<File> bytecodeFilesOrDirectories) {
    scanSources(sourceFiles);
    scanBytecode(bytecodeFilesOrDirectories);
  }

  private void scanSources(Collection<InputFile> sourceFiles) {
    TimeProfiler profiler = new TimeProfiler(getClass()).start("Java AST scan");
    astScanner.scan(sourceFiles);
    profiler.stop();
  }

  private void scanBytecode(Collection<File> bytecodeFilesOrDirectories) {
    if (hasBytecode(bytecodeFilesOrDirectories)) {
      TimeProfiler profiler = new TimeProfiler(getClass()).start("Java bytecode scan");

      ClassLoader classLoader = ClassLoaderBuilder.create(bytecodeFilesOrDirectories);
      AsmClassProviderImpl classProvider = new AsmClassProviderImpl(classLoader);

      for (BytecodeVisitor visitor : bytecodeScanner.getVisitors()) {
        if (visitor instanceof ClassBytecodeProviderAwareVisitor) {
          ((ClassBytecodeProviderAwareVisitor) visitor).setClassProvider(classProvider);
        }
      }

      bytecodeScanner.scan(bytecodeFilesOrDirectories, classProvider);

      ((SquidClassLoader) classLoader).close();

      bytecodeScanned = true;
      profiler.stop();
    } else {
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
        !FileUtils.listFiles(bytecodeFilesOrDirectory, new String[] {"class"}, true).isEmpty())) {
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
