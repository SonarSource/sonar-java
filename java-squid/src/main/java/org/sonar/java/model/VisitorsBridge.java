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
import org.sonar.api.rule.RuleKey;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.visitors.JavaAstVisitor;
import org.sonar.java.ast.visitors.SonarSymbolTableVisitor;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class VisitorsBridge extends JavaAstVisitor {

  private static final Logger LOG = LoggerFactory.getLogger(VisitorsBridge.class);

  private final JavaTreeMaker treeMaker = new JavaTreeMaker();
  private final List<JavaFileScanner> scanners;

  private SemanticModel semanticModel;
  private SonarComponents sonarComponents;

  @VisibleForTesting
  public VisitorsBridge(JavaFileScanner visitor) {
    this(Arrays.asList(visitor), null);
  }

  public VisitorsBridge(Iterable visitors, @Nullable SonarComponents sonarComponents) {
    ImmutableList.Builder<JavaFileScanner> scannersBuilder = ImmutableList.builder();
    for (Object visitor : visitors) {
      if (visitor instanceof JavaFileScanner) {
        scannersBuilder.add((JavaFileScanner) visitor);
      }
    }
    this.scanners = scannersBuilder.build();
    this.sonarComponents = sonarComponents;
  }

  @Override
  public void visitFile(@Nullable AstNode astNode) {
    semanticModel = null;
    if (astNode != null) {
      CompilationUnitTree tree = treeMaker.compilationUnit(astNode);
      if (isNotJavaLangOrSerializable()) {
        try {
          semanticModel = SemanticModel.createFor(tree, getProjectClasspath());
        } catch (Exception e) {
          LOG.error("Unable to create symbol table for : " + getContext().getFile().getName(), e);
          return;
        }
        createSonarSymbolTable(tree);
      } else {
        SemanticModel.handleMissingTypes(tree);
      }
      JavaFileScannerContext context = new DefaultJavaFileScannerContext(tree, peekSourceFile(), semanticModel);
      for (JavaFileScanner scanner : scanners) {
        scanner.scanFile(context);
      }
    }
  }

  private boolean isNotJavaLangOrSerializable() {
    String[] path = peekSourceFile().getName().split(Pattern.quote(File.separator));
    boolean isJavaLang = path.length > 3 && "java".equals(path[path.length - 3]) && "lang".equals(path[path.length - 2]);
    boolean isJavaLangAnnotation = path.length > 4 && "Annotation.java".equals(path[path.length - 1]) && "java".equals(path[path.length - 4]) && "lang".equals(path[path.length - 3]) && "annotation".equals(path[path.length - 2]);
    boolean isSerializable = path.length > 3 && "Serializable.java".equals(path[path.length-1]) && "java".equals(path[path.length-3]) && "io".equals(path[path.length-2]);
    return !(isJavaLang || isJavaLangAnnotation || isSerializable);
  }

  private List<File> getProjectClasspath() {
    List<File> projectClasspath = Lists.newArrayList();
    if (sonarComponents != null) {
      projectClasspath = sonarComponents.getProjectClasspath();
    }
    return projectClasspath;
  }

  private void createSonarSymbolTable(CompilationUnitTree tree) {
    if (sonarComponents != null) {
      SonarSymbolTableVisitor symVisitor = new SonarSymbolTableVisitor(sonarComponents.symbolizableFor(getContext().getFile()), semanticModel);
      symVisitor.visitCompilationUnit(tree);
    }
  }

  private static class DefaultJavaFileScannerContext implements JavaFileScannerContext {
    private final CompilationUnitTree tree;
    private final SourceFile sourceFile;
    private final SemanticModel semanticModel;

    public DefaultJavaFileScannerContext(CompilationUnitTree tree, SourceFile sourceFile, SemanticModel semanticModel) {
      this.tree = tree;
      this.sourceFile = sourceFile;
      this.semanticModel = semanticModel;
    }

    @Override
    public CompilationUnitTree getTree() {
      return tree;
    }

    @Override
    public void addIssue(Tree tree, RuleKey ruleKey, String message) {
      Preconditions.checkNotNull(ruleKey);
      Preconditions.checkNotNull(message);
      int line = ((JavaTree) tree).getLine();

      CheckMessage checkMessage = new CheckMessage(ruleKey, message);
      checkMessage.setLine(line);
      sourceFile.log(checkMessage);
    }

    @Override
    @Nullable
    public Object getSemanticModel() {
      return semanticModel;
    }

    @Override
    public String getFileName() {
      return sourceFile.getName();
    }

  }

}
