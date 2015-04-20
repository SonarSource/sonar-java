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
package org.sonar.java.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sonar.sslr.api.AstNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.java.CharsetAwareVisitor;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.visitors.ComplexityVisitor;
import org.sonar.java.ast.visitors.SonarSymbolTableVisitor;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.annotations.SqaleLinearRemediation;
import org.sonar.squidbridge.annotations.SqaleLinearWithOffsetRemediation;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;
import org.sonar.sslr.parser.LexerlessGrammar;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class VisitorsBridge extends SquidAstVisitor<LexerlessGrammar> implements CharsetAwareVisitor {

  private static final Logger LOG = LoggerFactory.getLogger(VisitorsBridge.class);

  private final List<JavaFileScanner> scanners;
  private final SonarComponents sonarComponents;
  private SemanticModel semanticModel;
  private List<File> projectClasspath;
  private boolean analyseAccessors;

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

  @Override
  public void setCharset(Charset charset) {
    for (JavaFileScanner scanner : scanners) {
      if (scanner instanceof CharsetAwareVisitor) {
        ((CharsetAwareVisitor) scanner).setCharset(charset);
      }
    }
  }

  @Override
  public void visitFile(@Nullable AstNode astNode) {
    semanticModel = null;
    if (astNode != null) {
      CompilationUnitTree tree = (CompilationUnitTree) astNode;
      if (isNotJavaLangOrSerializable()) {
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
      JavaFileScannerContext context = new DefaultJavaFileScannerContext(tree, (SourceFile) getContext().peekSourceCode(), getContext().getFile(), semanticModel, analyseAccessors);
      for (JavaFileScanner scanner : scanners) {
        scanner.scanFile(context);
      }
      if (semanticModel != null) {
        // Close class loader after all the checks.
        semanticModel.done();
      }
    }
  }

  private boolean isNotJavaLangOrSerializable() {
    String[] path = getContext().peekSourceCode().getName().split(Pattern.quote(File.separator));
    boolean isJavaLang = path.length > 3 && "java".equals(path[path.length - 3]) && "lang".equals(path[path.length - 2]);
    boolean isJavaLangAnnotation = path.length > 4 && "Annotation.java".equals(path[path.length - 1]) && "java".equals(path[path.length - 4])
        && "lang".equals(path[path.length - 3]) && "annotation".equals(path[path.length - 2]);
    boolean isSerializable = path.length > 3 && "Serializable.java".equals(path[path.length - 1]) && "java".equals(path[path.length - 3]) && "io".equals(path[path.length - 2]);
    return !(isJavaLang || isJavaLangAnnotation || isSerializable);
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
        if(linear != null || linearWithOffset != null) {
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
