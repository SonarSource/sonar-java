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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.RecognitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.java.CharsetAwareVisitor;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.visitors.ComplexityVisitor;
import org.sonar.java.ast.visitors.SonarSymbolTableVisitor;
import org.sonar.java.ast.visitors.VisitorContext;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.AstScannerExceptionHandler;
import org.sonar.squidbridge.annotations.SqaleLinearRemediation;
import org.sonar.squidbridge.annotations.SqaleLinearWithOffsetRemediation;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class VisitorsBridge {

  private static final Logger LOG = LoggerFactory.getLogger(VisitorsBridge.class);

  private final List<JavaFileScanner> scanners;
  private final SonarComponents sonarComponents;
  private SemanticModel semanticModel;
  private List<File> projectClasspath;
  private boolean analyseAccessors;
  private VisitorContext context;

  @VisibleForTesting
  public VisitorsBridge(JavaFileScanner visitor) {
    this(Arrays.asList(visitor), Lists.<File>newArrayList(), null);
  }

  @VisibleForTesting
  public VisitorsBridge(JavaFileScanner visitor, List<File> projectClasspath) {
    this(Arrays.asList(visitor), projectClasspath, null);
  }

  public VisitorsBridge(Iterable visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents) {
    ImmutableList.Builder<JavaFileScanner> scannersBuilder = ImmutableList.builder();
    for (Object visitor : visitors) {
      if (visitor instanceof JavaFileScanner) {
        scannersBuilder.add((JavaFileScanner) visitor);
      }
    }
    this.scanners = scannersBuilder.build();
    this.sonarComponents = sonarComponents;
    this.projectClasspath = projectClasspath;
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

  public void visitFile(@Nullable Tree parsedTree) {
    semanticModel = null;
    CompilationUnitTree tree = new JavaTree.CompilationUnitTreeImpl(null, Lists.<ImportClauseTree>newArrayList(), Lists.<Tree>newArrayList(), null);
    if (parsedTree != null && parsedTree.is(Tree.Kind.COMPILATION_UNIT)) {
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
    JavaFileScannerContext javaFileScannerContext =
      new DefaultJavaFileScannerContext(tree, (SourceFile) getContext().peekSourceCode(), getContext().getFile(), semanticModel, analyseAccessors);
    for (JavaFileScanner scanner : scanners) {
      scanner.scanFile(javaFileScannerContext);
    }
    if (semanticModel != null) {
      // Close class loader after all the checks.
      semanticModel.done();
    }
  }

  private boolean isNotJavaLangOrSerializable(String packageName) {
    String name = getContext().getFile().getName();
    return !("java/lang".equals(packageName)
        || ("java/lang/annotation".equals(packageName) && "Annotation.java".equals(name))
        || ("java/io".equals(packageName) && "Serializable.java".equals(name))
    );
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

  @VisibleForTesting
  public static class DefaultJavaFileScannerContext implements JavaFileScannerContext {
    private final CompilationUnitTree tree;
    @VisibleForTesting
    public final SourceFile sourceFile;
    private final SemanticModel semanticModel;
    private final ComplexityVisitor complexityVisitor;
    private final File file;

    public DefaultJavaFileScannerContext(CompilationUnitTree tree, SourceFile sourceFile, File file, SemanticModel semanticModel, boolean analyseAccessors) {
      this.tree = tree;
      this.sourceFile = sourceFile;
      this.file = file;
      this.semanticModel = semanticModel;
      this.complexityVisitor = new ComplexityVisitor(analyseAccessors);
    }

    @Override
    public CompilationUnitTree getTree() {
      return tree;
    }

    @Override
    public void addIssue(Tree tree, JavaCheck javaCheck, String message) {
      addIssue(((JavaTree) tree).getLine(), javaCheck, message, null);
    }

    @Override
    public void addIssue(Tree tree, JavaCheck check, String message, @Nullable Double cost) {
      addIssue(((JavaTree) tree).getLine(), check, message, cost);
    }

    @Override
    public void addIssueOnFile(JavaCheck javaCheck, String message) {
      addIssue(-1, javaCheck, message);
    }

    @Override
    public void addIssue(int line, JavaCheck javaCheck, String message) {
      addIssue(line, javaCheck, message, null);
    }

    @Override
    public void addIssue(int line, JavaCheck javaCheck, String message, @Nullable Double cost) {
      Preconditions.checkNotNull(javaCheck);
      Preconditions.checkNotNull(message);
      CheckMessage checkMessage = new CheckMessage(javaCheck, message);
      if (line > 0) {
        checkMessage.setLine(line);
      }
      if (cost == null) {
        Annotation linear = AnnotationUtils.getAnnotation(javaCheck, SqaleLinearRemediation.class);
        Annotation linearWithOffset = AnnotationUtils.getAnnotation(javaCheck, SqaleLinearWithOffsetRemediation.class);
        if (linear != null || linearWithOffset != null) {
          throw new IllegalStateException("A check annotated with a linear sqale function should provide an effort to fix");
        }
      } else {
        checkMessage.setCost(cost);
      }
      sourceFile.log(checkMessage);
    }

    @Override
    public void addIssue(Tree tree, CheckMessage checkMessage) {
      checkMessage.setLine(((JavaTree) tree).getLine());
      sourceFile.log(checkMessage);
    }

    @Override
    @Nullable
    public Object getSemanticModel() {
      return semanticModel;
    }

    @Override
    public String getFileKey() {
      return sourceFile.getKey();
    }

    @Override
    public File getFile() {
      return file;
    }

    @Override
    public int getComplexity(Tree tree) {
      return complexityVisitor.scan(tree);
    }

    @Override
    public int getMethodComplexity(ClassTree enclosingClass, MethodTree methodTree) {
      return complexityVisitor.scan(enclosingClass, methodTree);
    }

    @Override
    public void addNoSonarLines(Set<Integer> lines) {
      sourceFile.addNoSonarTagLines(lines);
    }

  }

}
