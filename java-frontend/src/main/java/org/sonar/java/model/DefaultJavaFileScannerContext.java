/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonar.java.AnalyzerMessage;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.visitors.ComplexityVisitor;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.io.File;
import java.util.List;

public class DefaultJavaFileScannerContext implements JavaFileScannerContext {
  private final CompilationUnitTree tree;
  @VisibleForTesting
  private final SemanticModel semanticModel;
  private final SonarComponents sonarComponents;
  private final ComplexityVisitor complexityVisitor;
  private final File file;
  private final JavaVersion javaVersion;
  private final boolean fileParsed;

  public DefaultJavaFileScannerContext(CompilationUnitTree tree, File file, SemanticModel semanticModel,
                                       @Nullable SonarComponents sonarComponents, JavaVersion javaVersion, boolean fileParsed) {
    this.tree = tree;
    this.file = file;
    this.semanticModel = semanticModel;
    this.sonarComponents = sonarComponents;
    this.complexityVisitor = new ComplexityVisitor();
    this.javaVersion = javaVersion;
    this.fileParsed = fileParsed;
  }

  @Override
  public CompilationUnitTree getTree() {
    return tree;
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
  public void addIssue(int line, JavaCheck javaCheck, String message, @Nullable Integer cost) {
    sonarComponents.addIssue(file, javaCheck, line, message, cost);
  }

  @Override
  @Nullable
  public Object getSemanticModel() {
    return semanticModel;
  }

  @Override
  public JavaVersion getJavaVersion() {
    return this.javaVersion;
  }

  @Override
  public boolean fileParsed() {
    return fileParsed;
  }

  @Override
  public String getFileKey() {
    return file.getAbsolutePath();
  }

  @Override
  public void addIssue(File file, JavaCheck check, int line, String message) {
    if (sonarComponents != null) {
      sonarComponents.addIssue(file, check, line, message, null);
    }
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree tree, String message) {
    reportIssue(javaCheck, tree, message, ImmutableList.<Location>of(), null);
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree syntaxNode, String message, List<Location> secondary, @Nullable Integer cost) {
    sonarComponents.reportIssue(createAnalyzerMessage(file, javaCheck, syntaxNode, null, message, secondary, cost));
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message) {
    reportIssue(javaCheck, startTree, endTree, message, ImmutableList.<Location>of(), null);
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message, List<Location> secondary, @Nullable Integer cost) {
    sonarComponents.reportIssue(createAnalyzerMessage(file, javaCheck, startTree, endTree, message, secondary, cost));
  }

  protected static AnalyzerMessage createAnalyzerMessage(File file, JavaCheck javaCheck, Tree startTree, @Nullable Tree endTree, String message, List<Location> secondary,
    @Nullable Integer cost) {
    AnalyzerMessage.TextSpan textSpan = endTree != null ? AnalyzerMessage.textSpanBetween(startTree, endTree) : AnalyzerMessage.textSpanFor(startTree);
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, file, textSpan, message, cost != null ? cost : 0);
    for (Location location : secondary) {
      AnalyzerMessage secondaryLocation = new AnalyzerMessage(javaCheck, file, AnalyzerMessage.textSpanFor(location.syntaxNode), location.msg, 0);
      analyzerMessage.secondaryLocations.add(secondaryLocation);
    }
    return analyzerMessage;
  }

  @Override
  public File getFile() {
    return file;
  }

  @Override
  public List<Tree> getComplexityNodes(Tree tree) {
    return complexityVisitor.scan(tree);
  }

  @Override
  public List<Tree> getMethodComplexityNodes(ClassTree enclosingClass, MethodTree methodTree) {
    return complexityVisitor.scan(enclosingClass, methodTree);
  }
}
