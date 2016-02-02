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
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.visitors.ComplexityVisitor;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.checks.SECheck;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DefaultJavaFileScannerContext implements JavaFileScannerContext {
  private final CompilationUnitTree tree;
  @VisibleForTesting
  private final SemanticModel semanticModel;
  private final SonarComponents sonarComponents;
  private final ComplexityVisitor complexityVisitor;
  private final File file;
  private final JavaVersion javaVersion;
  private final boolean fileParsed;
  private final Map<Class<? extends SECheck>, SetMultimap<Tree, SEIssue>> seIssues = new HashMap<>();

  public DefaultJavaFileScannerContext(CompilationUnitTree tree, File file, SemanticModel semanticModel, boolean analyseAccessors,
                                       @Nullable SonarComponents sonarComponents, JavaVersion javaVersion, boolean fileParsed) {
    this.tree = tree;
    this.file = file;
    this.semanticModel = semanticModel;
    this.sonarComponents = sonarComponents;
    this.complexityVisitor = new ComplexityVisitor(analyseAccessors);
    this.javaVersion = javaVersion;
    this.fileParsed = fileParsed;
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
    sonarComponents.reportIssue(createAnalyzerMessage(javaCheck, syntaxNode, message, secondary, cost));
  }

  private AnalyzerMessage createAnalyzerMessage(JavaCheck javaCheck, Tree syntaxNode, String message, List<Location> secondary, @Nullable Integer cost) {
    AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, file, AnalyzerMessage.textSpanFor(syntaxNode), message, cost != null ? cost : 0);
    for (Location location : secondary) {
      AnalyzerMessage secondaryLocation = new AnalyzerMessage(javaCheck, file, AnalyzerMessage.textSpanFor(location.syntaxNode), location.msg, 0);
      analyzerMessage.secondaryLocations.add(secondaryLocation);
    }
    return analyzerMessage;
  }

  @Override
  public void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message) {
    sonarComponents.reportIssue(new AnalyzerMessage(javaCheck, file, AnalyzerMessage.textSpanBetween(startTree, endTree), message, 0));
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

  public void reportSEIssue(Class<? extends SECheck> check, Tree tree, String message, List<Location> secondary) {
    if (!seIssues.containsKey(check)) {
      seIssues.put(check, LinkedHashMultimap.<Tree, SEIssue>create());
    }
    seIssues.get(check).put(tree, new SEIssue(tree, message, secondary));
  }

  public Multimap<Tree, SEIssue> getSEIssues(Class<? extends SECheck> check) {
    if (seIssues.containsKey(check)) {
      return seIssues.get(check);
    } else {
      return ImmutableMultimap.of();
    }
  }

  public static class SEIssue {
    private final Tree tree;
    private final String message;
    private final List<Location> secondary;

    public SEIssue(Tree tree, String message, List<Location> secondary) {
      this.tree = tree;
      this.message = message;
      this.secondary = secondary;
    }

    public Tree getTree() {
      return tree;
    }

    public String getMessage() {
      return message;
    }

    public List<Location> getSecondary() {
      return secondary;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SEIssue seIssue = (SEIssue) o;
      return Objects.equals(tree, seIssue.tree) &&
        Objects.equals(message, seIssue.message) &&
        Objects.equals(secondary, seIssue.secondary);
    }

    @Override
    public int hashCode() {
      return Objects.hash(tree, message, secondary);
    }
  }
}
