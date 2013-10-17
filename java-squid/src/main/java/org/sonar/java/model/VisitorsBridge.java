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
import org.sonar.api.batch.SquidUtils;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.ast.visitors.JavaAstVisitor;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class VisitorsBridge extends JavaAstVisitor {

  @Nullable
  private final ResourcePerspectives resourcePerspectives;

  private final JavaTreeMaker treeMaker = new JavaTreeMaker();
  private final List<JavaFileScanner> scanners;

  @VisibleForTesting
  public VisitorsBridge(JavaFileScanner visitor) {
    this(null, Arrays.asList(visitor));
  }

  public VisitorsBridge(@Nullable ResourcePerspectives resourcePerspectives, List visitors) {
    this.resourcePerspectives = resourcePerspectives;
    ImmutableList.Builder<JavaFileScanner> scannersBuilder = ImmutableList.builder();
    for (Object visitor : visitors) {
      if (visitor instanceof JavaFileScanner) {
        scannersBuilder.add((JavaFileScanner) visitor);
      }
    }
    this.scanners = scannersBuilder.build();
  }

  @Override
  public void visitFile(@Nullable AstNode astNode) {
    if (astNode != null) {
      CompilationUnitTree tree = treeMaker.compilationUnit(astNode);

      SourceFile sourceFile = peekSourceFile();
      JavaFile sonarFile = SquidUtils.convertJavaFileKeyFromSquidFormat(sourceFile.getKey());
      Issuable issuable = resourcePerspectives == null ? null : resourcePerspectives.as(Issuable.class, sonarFile);
      JavaFileScannerContext context = new DefaultJavaFileScannerContext(tree, sourceFile, issuable);
      for (JavaFileScanner scanner : scanners) {
        scanner.scanFile(context);
      }
    }
  }

  private static class DefaultJavaFileScannerContext implements JavaFileScannerContext {
    private final CompilationUnitTree tree;
    private final SourceFile sourceFile;
    private final Issuable issuable;

    public DefaultJavaFileScannerContext(CompilationUnitTree tree, SourceFile sourceFile, @Nullable Issuable issuable) {
      this.tree = tree;
      this.sourceFile = sourceFile;
      this.issuable = issuable;
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

      if (issuable == null) {
        CheckMessage checkMessage = new CheckMessage(ruleKey, message);
        checkMessage.setLine(line);
        sourceFile.log(checkMessage);
      } else {
        issuable.addIssue(issuable.newIssueBuilder().ruleKey(ruleKey).line(line).message(message).build());
      }
    }
  }

}
