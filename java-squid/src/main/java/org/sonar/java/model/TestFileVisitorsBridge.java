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

import com.sonar.sslr.api.AstNode;
import org.sonar.api.rule.RuleKey;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.SquidAstVisitor;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.sslr.parser.LexerlessGrammar;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Set;

/**
 * This visitor is used to add the mapping between className and SonarResource for test files into {@link org.sonar.plugins.java.api.JavaResourceLocator}
 */
public class TestFileVisitorsBridge extends SquidAstVisitor<LexerlessGrammar> {

  private final JavaFileScanner visitor;

  public TestFileVisitorsBridge(JavaFileScanner visitor) {
    this.visitor = visitor;
  }

  @Override
  public void visitFile(@Nullable AstNode astNode) {
    if (astNode != null) {
      CompilationUnitTree tree = (CompilationUnitTree) astNode;
      JavaFileScannerContext context = new JavaTestFileScannerContext(tree, getContext().getFile());
      visitor.scanFile(context);
    }
  }

  private static class JavaTestFileScannerContext implements JavaFileScannerContext {
    private final CompilationUnitTree tree;
    private final File file;

    public JavaTestFileScannerContext(CompilationUnitTree tree, File file) {
      this.tree = tree;
      this.file = file;
    }

    @Override
    public CompilationUnitTree getTree() {
      return tree;
    }

    @Override
    public void addIssue(Tree tree, RuleKey ruleKey, String message) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addIssueOnFile(RuleKey ruleKey, String message) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addIssue(int line, RuleKey ruleKey, String message) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addIssue(Tree tree, CheckMessage checkMessage) {
      throw new UnsupportedOperationException();
    }

    @Override
    @Nullable
    public Object getSemanticModel() {
      return null;
    }

    @Override
    public String getFileKey() {
      return null;
    }

    @Override
    public File getFile() {
      return file;
    }

    @Override
    public int getComplexity(Tree tree) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getMethodComplexity(ClassTree enclosingClass, MethodTree methodTree) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addNoSonarLines(Set<Integer> lines) {
      // NOOP for tests.
    }
  }

}
