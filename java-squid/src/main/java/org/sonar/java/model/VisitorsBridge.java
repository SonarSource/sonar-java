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
import com.sonar.sslr.api.AstNode;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.SemanticModelProvider;
import org.sonar.java.SemanticModelProviderAwareVisitor;
import org.sonar.java.ast.visitors.JavaAstVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class VisitorsBridge extends JavaAstVisitor implements SemanticModelProviderAwareVisitor {

  private final JavaTreeMaker treeMaker = new JavaTreeMaker();
  private final List<JavaFileScanner> scanners;

  private SemanticModelProvider semanticModelProvider;

  @VisibleForTesting
  public VisitorsBridge(JavaFileScanner visitor) {
    this(Arrays.asList(visitor));
  }

  public VisitorsBridge(Iterable visitors) {
    ImmutableList.Builder<JavaFileScanner> scannersBuilder = ImmutableList.builder();
    for (Object visitor : visitors) {
      if (visitor instanceof JavaFileScanner) {
        scannersBuilder.add((JavaFileScanner) visitor);
      }
    }
    this.scanners = scannersBuilder.build();
  }

  @Override
  public void setSemanticModelProvider(SemanticModelProvider semanticModelProvider) {
    this.semanticModelProvider = semanticModelProvider;
  }

  @Override
  public void visitFile(@Nullable AstNode astNode) {
    if (astNode != null) {
      CompilationUnitTree tree = treeMaker.compilationUnit(astNode);

      JavaFileScannerContext context = new DefaultJavaFileScannerContext(tree, peekSourceFile(), semanticModelProvider);
      for (JavaFileScanner scanner : scanners) {
        scanner.scanFile(context);
      }
    }
  }

  private static class DefaultJavaFileScannerContext implements JavaFileScannerContext {
    private final CompilationUnitTree tree;
    private final SourceFile sourceFile;
    private final SemanticModelProvider semanticModelProvider;

    public DefaultJavaFileScannerContext(CompilationUnitTree tree, SourceFile sourceFile, SemanticModelProvider semanticModelProvider) {
      this.tree = tree;
      this.sourceFile = sourceFile;
      this.semanticModelProvider = semanticModelProvider;
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
      return semanticModelProvider.semanticModel();
    }

  }

}
