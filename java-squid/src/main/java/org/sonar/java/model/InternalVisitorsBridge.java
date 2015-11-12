/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.java.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.RecognitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.java.CharsetAwareVisitor;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.visitors.SonarSymbolTableVisitor;
import org.sonar.java.ast.visitors.VisitorContext;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.AstScannerExceptionHandler;
import org.sonar.squidbridge.api.SourceFile;

import javax.annotation.Nullable;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

public class InternalVisitorsBridge {

  private static final Logger LOG = LoggerFactory.getLogger(InternalVisitorsBridge.class);

  private final List<JavaFileScanner> scanners;
  private final SonarComponents sonarComponents;
  private final boolean symbolicExecutionEnabled;
  private SemanticModel semanticModel;
  private List<File> projectClasspath;
  private boolean analyseAccessors;
  private VisitorContext context;
  @Nullable
  private Integer javaVersion;

  public InternalVisitorsBridge(Iterable visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents) {
    this(visitors, projectClasspath, sonarComponents, true);
  }

  public InternalVisitorsBridge(Iterable visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents, boolean symbolicExecutionEnabled) {
    ImmutableList.Builder<JavaFileScanner> scannersBuilder = ImmutableList.builder();
    for (Object visitor : visitors) {
      if (visitor instanceof JavaFileScanner) {
        scannersBuilder.add((JavaFileScanner) visitor);
      }
    }
    this.scanners = scannersBuilder.build();
    this.sonarComponents = sonarComponents;
    this.projectClasspath = projectClasspath;
    this.symbolicExecutionEnabled = symbolicExecutionEnabled;
  }

  public void setAnalyseAccessors(boolean analyseAccessors) {
    this.analyseAccessors = analyseAccessors;
  }

  public void setCharset(Charset charset) {
    for (JavaFileScanner scanner : scanners) {
      if (scanner instanceof CharsetAwareVisitor) {
        ((CharsetAwareVisitor) scanner).setCharset(charset);
      }
    }
  }

  public void setJavaVersion(@Nullable Integer javaVersion) {
    this.javaVersion = javaVersion;
  }

  public Integer getJavaVersion() {
    return this.javaVersion;
  }

  public void visitFile(@Nullable Tree parsedTree) {
    semanticModel = null;
    CompilationUnitTree tree = new JavaTree.CompilationUnitTreeImpl(null, Lists.<ImportClauseTree>newArrayList(), Lists.<Tree>newArrayList(), null);
    if ((parsedTree != null) && parsedTree.is(Tree.Kind.COMPILATION_UNIT)) {
      tree = (CompilationUnitTree) parsedTree;
      if (isNotJavaLangOrSerializable(PackageUtils.packageName(tree.packageDeclaration(), "/"))) {
        try {
          semanticModel = SemanticModel.createFor(tree, getProjectClasspath());
        } catch (Exception e) {
          LOG.error("Unable to create symbol table for : " + getContext().getFile().getAbsolutePath(), e);
          return;
        }
        createSonarSymbolTable(tree);
      } else {
        SemanticModel.handleMissingTypes(tree);
      }
    }
    JavaFileScannerContext javaFileScannerContext = createScannerContext(tree, semanticModel, analyseAccessors, sonarComponents);
    // Symbolic execution checks
    if (symbolicExecutionEnabled && isNotJavaLangOrSerializable(PackageUtils.packageName(tree.packageDeclaration(), "/"))) {
      new SymbolicExecutionVisitor().scanFile(javaFileScannerContext);
    }
    for (JavaFileScanner scanner : executableScanners(scanners)) {
      scanner.scanFile(javaFileScannerContext);
    }
    if (semanticModel != null) {
      // Close class loader after all the checks.
      semanticModel.done();
    }
  }

  private List<JavaFileScanner> executableScanners(List<JavaFileScanner> scanners) {
    ImmutableList.Builder<JavaFileScanner> results = ImmutableList.builder();
    for (JavaFileScanner scanner : scanners) {
      if (shouldBeExecuted(scanner)) {
        results.add(scanner);
      }
    }
    return results.build();
  }

  private boolean shouldBeExecuted(JavaFileScanner scanner) {
    if (scanner instanceof JavaVersionAwareVisitor) {
      return ((JavaVersionAwareVisitor) scanner).isCompatibleWithJavaVersion(javaVersion);
    }
    return true;
  }

  protected JavaFileScannerContext createScannerContext(CompilationUnitTree tree, SemanticModel semanticModel, boolean analyseAccessors, SonarComponents sonarComponents) {
    return new DefaultJavaFileScannerContext(
      tree,
      (SourceFile) getContext().peekSourceCode(),
      getContext().getFile(),
      semanticModel,
      analyseAccessors,
      sonarComponents,
      javaVersion);
  }

  private boolean isNotJavaLangOrSerializable(String packageName) {
    String name = getContext().getFile().getName();
    return !(inJavaLang(packageName) || isAnnotation(packageName, name) || isSerializable(packageName, name));
  }

  private static boolean isSerializable(String packageName, String name) {
    return "java/io".equals(packageName) && "Serializable.java".equals(name);
  }

  private static boolean isAnnotation(String packageName, String name) {
    return "java/lang/annotation".equals(packageName) && "Annotation.java".equals(name);
  }

  private static boolean inJavaLang(String packageName) {
    return "java/lang".equals(packageName);
  }

  private List<File> getProjectClasspath() {
    return projectClasspath;
  }

  private void createSonarSymbolTable(CompilationUnitTree tree) {
    if (sonarComponents != null) {
      SonarSymbolTableVisitor symVisitor = new SonarSymbolTableVisitor(sonarComponents.symbolizableFor(getContext().getFile()), semanticModel);
      symVisitor.visitCompilationUnit(tree);
    }
  }

  public void processRecognitionException(RecognitionException e) {
    for (JavaFileScanner scanner : scanners) {
      if (scanner instanceof AstScannerExceptionHandler) {
        ((AstScannerExceptionHandler) scanner).processRecognitionException(e);
      }
    }
  }

  public VisitorContext getContext() {
    return context;
  }

  public void setContext(VisitorContext context) {
    this.context = context;
  }
}
