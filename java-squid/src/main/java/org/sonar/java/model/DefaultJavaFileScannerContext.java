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
import com.google.common.collect.ImmutableList;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.rule.RuleKey;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.CompIssue;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.visitors.ComplexityVisitor;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
import org.sonar.java.syntaxtoken.LastSyntaxTokenFinder;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.SourceFile;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Set;

public class DefaultJavaFileScannerContext implements JavaFileScannerContext {
  private final CompilationUnitTree tree;
  @VisibleForTesting
  public final SourceFile sourceFile;
  private final SemanticModel semanticModel;
  private final SonarComponents sonarComponents;
  private final ComplexityVisitor complexityVisitor;
  private final File file;
  private final Integer javaVersion;

  public DefaultJavaFileScannerContext(
    CompilationUnitTree tree, SourceFile sourceFile, File file, SemanticModel semanticModel, boolean analyseAccessors, @Nullable SonarComponents sonarComponents,
    @Nullable Integer javaVersion) {
    this.tree = tree;
    this.sourceFile = sourceFile;
    this.file = file;
    this.semanticModel = semanticModel;
    this.sonarComponents = sonarComponents;
    this.complexityVisitor = new ComplexityVisitor(analyseAccessors);
    this.javaVersion = javaVersion;
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
    sonarComponents.addIssue(file, javaCheck, line, message, cost);
  }

  @Override
  @Nullable
  public Object getSemanticModel() {
    return semanticModel;
  }

  @Override
  @Nullable
  public Integer getJavaVersion() {
    return this.javaVersion;
  }

  @Override
  public String getFileKey() {
    return sourceFile.getKey();
  }

  @CheckForNull
  private RuleKey getRuleKey(JavaCheck check) {
    if (sonarComponents != null) {
      return sonarComponents.getRuleKey(check);
    }
    return null;
  }

  @Override
  public void addIssue(File file, JavaCheck check, int line, String message) {
    if (sonarComponents != null) {
      sonarComponents.addIssue(file, check, line, message, null);
    }
  }

  /**
   * FIXME(mpaladin) DO NOT GO ON RELEASE WITH THIS CONSTANT SET TO TRUE
   **/
  private static final boolean ENABLE_NEW_APIS = true;

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree tree, String message) {
    reportIssue(javaCheck, tree, message, ImmutableList.<Location>of(), null);
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree syntaxNode, String message, List<Location> secondary, @Nullable Integer cost) {
    if (ENABLE_NEW_APIS) {
      InputFile inputFile = sonarComponents.inputFromIOFile(file);
      RuleKey ruleKey = getRuleKey(javaCheck);
      if (ruleKey != null) {
        CompIssue compIssue = CompIssue.create(inputFile, sonarComponents.issuableFor(file), ruleKey, cost != null ? (double) cost : null);
        AnalyzerMessage.TextSpan textSpan = textSpanFor(syntaxNode);
        if (textSpan == null) {
          compIssue.setPrimaryLocation(message, null);
        } else {
          compIssue.setPrimaryLocation(message, textSpan.startLine, textSpan.startCharacter, textSpan.endLine, textSpan.endCharacter);
        }
        for (Location location : secondary) {
          AnalyzerMessage.TextSpan secondarySpan = textSpanFor(location.syntaxNode);
          compIssue.addSecondaryLocation(secondarySpan.startLine, secondarySpan.startCharacter, secondarySpan.endLine, secondarySpan.endCharacter, location.msg);
        }
        compIssue.save();
      }
    } else {
      addIssue(syntaxNode, javaCheck, message, cost != null ? (double) cost : null);
    }
  }

  protected static AnalyzerMessage.TextSpan textSpanFor(Tree syntaxNode) {
    SyntaxToken firstSyntaxToken = FirstSyntaxTokenFinder.firstSyntaxToken(syntaxNode);
    SyntaxToken lastSyntaxToken = LastSyntaxTokenFinder.lastSyntaxToken(syntaxNode);
    return new AnalyzerMessage.TextSpan(
      firstSyntaxToken.line(),
      firstSyntaxToken.column(),
      lastSyntaxToken.line(),
      lastSyntaxToken.column() + lastSyntaxToken.text().length()
    );
  }

  @Override
  public File getFile() {
    return file;
  }

  @Override
  public int getComplexity(Tree tree) {
    return getComplexityNodes(tree).size();
  }

  @Override
  public int getMethodComplexity(ClassTree enclosingClass, MethodTree methodTree) {
    return getMethodComplexityNodes(enclosingClass, methodTree).size();
  }

  @Override
  public List<Tree> getComplexityNodes(Tree tree) {
    return complexityVisitor.scan(tree);
  }

  @Override
  public List<Tree> getMethodComplexityNodes(ClassTree enclosingClass, MethodTree methodTree) {
    return complexityVisitor.scan(enclosingClass, methodTree);
  }

  @Override
  public void addNoSonarLines(Set<Integer> lines) {
    sourceFile.addNoSonarTagLines(lines);
  }

}
