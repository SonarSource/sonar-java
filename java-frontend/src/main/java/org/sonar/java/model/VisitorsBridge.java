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
package org.sonar.java.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.RecognitionException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.CharsetAwareVisitor;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.visitors.SonarSymbolTableVisitor;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.AstScannerExceptionHandler;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

public class VisitorsBridge {

  private static final Logger LOG = Loggers.get(VisitorsBridge.class);

  private final List<JavaFileScanner> scanners;
  private List<JavaFileScanner> executableScanners;
  private final SonarComponents sonarComponents;
  private final boolean symbolicExecutionEnabled;
  private SemanticModel semanticModel;
  private List<File> projectClasspath;
  protected File currentFile;
  protected JavaVersion javaVersion;

  @VisibleForTesting
  public VisitorsBridge(JavaFileScanner visitor) {
    this(Collections.singletonList(visitor), Lists.newArrayList(), null);
  }

  @VisibleForTesting
  public VisitorsBridge(Iterable visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents) {
    this(visitors, projectClasspath, sonarComponents, true);
  }

  public VisitorsBridge(Iterable visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents, boolean symbolicExecutionEnabled) {
    ImmutableList.Builder<JavaFileScanner> scannersBuilder = ImmutableList.builder();
    for (Object visitor : visitors) {
      if (visitor instanceof JavaFileScanner) {
        scannersBuilder.add((JavaFileScanner) visitor);
      }
    }
    this.scanners = scannersBuilder.build();
    this.executableScanners = scanners;
    this.sonarComponents = sonarComponents;
    this.projectClasspath = projectClasspath;
    this.symbolicExecutionEnabled = symbolicExecutionEnabled;
  }

  public void setCharset(Charset charset) {
    for (JavaFileScanner scanner : scanners) {
      if (scanner instanceof CharsetAwareVisitor) {
        ((CharsetAwareVisitor) scanner).setCharset(charset);
      }
    }
  }

  public void setJavaVersion(JavaVersion javaVersion) {
    this.javaVersion = javaVersion;
    this.executableScanners = executableScanners(scanners, javaVersion);
  }

  public void visitFile(@Nullable Tree parsedTree) {
    semanticModel = null;
    CompilationUnitTree tree = new JavaTree.CompilationUnitTreeImpl(null, Lists.newArrayList(), Lists.newArrayList(), null);
    boolean fileParsed = parsedTree != null;
    if (fileParsed && parsedTree.is(Tree.Kind.COMPILATION_UNIT)) {
      tree = (CompilationUnitTree) parsedTree;
      if (isNotJavaLangOrSerializable(PackageUtils.packageName(tree.packageDeclaration(), "/"))) {
        try {
          semanticModel = SemanticModel.createFor(tree, getProjectClasspath());
        } catch (Exception e) {
          LOG.error("Unable to create symbol table for : " + currentFile.getAbsolutePath(), e);
          return;
        }
        createSonarSymbolTable(tree);
      } else {
        SemanticModel.handleMissingTypes(tree);
      }
    }
    JavaFileScannerContext javaFileScannerContext = createScannerContext(tree, semanticModel, sonarComponents, fileParsed);
    // Symbolic execution checks
    if (symbolicExecutionEnabled && isNotJavaLangOrSerializable(PackageUtils.packageName(tree.packageDeclaration(), "/"))) {
      new SymbolicExecutionVisitor(executableScanners).scanFile(javaFileScannerContext);
    }
    for (JavaFileScanner scanner : executableScanners) {
      scanner.scanFile(javaFileScannerContext);
    }
    if (semanticModel != null) {
      // Close class loader after all the checks.
      semanticModel.done();
    }
  }

  private static List<JavaFileScanner> executableScanners(List<JavaFileScanner> scanners, JavaVersion javaVersion) {
    ImmutableList.Builder<JavaFileScanner> results = ImmutableList.builder();
    for (JavaFileScanner scanner : scanners) {
      if (!(scanner instanceof JavaVersionAwareVisitor) || ((JavaVersionAwareVisitor) scanner).isCompatibleWithJavaVersion(javaVersion)) {
        results.add(scanner);
      }
    }
    return results.build();
  }

  protected JavaFileScannerContext createScannerContext(
    CompilationUnitTree tree, SemanticModel semanticModel, SonarComponents sonarComponents, boolean fileParsed) {
    return new DefaultJavaFileScannerContext(
      tree,
      currentFile,
      semanticModel,
      sonarComponents,
      javaVersion,
      fileParsed);
  }

  private boolean isNotJavaLangOrSerializable(String packageName) {
    String name = currentFile.getName();
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
      SonarSymbolTableVisitor symVisitor = new SonarSymbolTableVisitor(sonarComponents.symbolizableFor(currentFile), semanticModel);
      symVisitor.visitCompilationUnit(tree);
    }
  }

  public void processRecognitionException(RecognitionException e, File file) {
    if(sonarComponents == null || !sonarComponents.reportAnalysisError(e, file)) {
      this.visitFile(null);
      scanners.stream()
        .filter(scanner -> scanner instanceof AstScannerExceptionHandler)
        .forEach(scanner -> ((AstScannerExceptionHandler) scanner).processRecognitionException(e));
    }

  }

  public void setCurrentFile(File currentFile) {
    this.currentFile = currentFile;
  }

}
