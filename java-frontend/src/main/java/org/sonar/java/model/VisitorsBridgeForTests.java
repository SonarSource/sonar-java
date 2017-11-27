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
import com.google.common.collect.Iterables;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.SonarComponents;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.SymbolicExecutionMode;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.api.CodeVisitor;

public class VisitorsBridgeForTests extends VisitorsBridge {

  private TestJavaFileScannerContext testContext;
  private boolean enableSemantic = true;


  @VisibleForTesting
  public VisitorsBridgeForTests(JavaFileScanner visitor, SonarComponents sonarComponents) {
    this(Collections.singletonList(visitor), new ArrayList<>(), sonarComponents);
  }

  public VisitorsBridgeForTests(Iterable visitors, SonarComponents sonarComponents) {
    super(visitors, new ArrayList<>(), sonarComponents, SymbolicExecutionMode.DISABLED);
    enableSemantic = false;
  }

  public VisitorsBridgeForTests(Iterable visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents) {
    super(visitors, projectClasspath, sonarComponents, SymbolicExecutionMode.getMode(Iterables.<CodeVisitor>toArray(visitors, CodeVisitor.class), true));
  }

  @Override
  protected JavaFileScannerContext createScannerContext(CompilationUnitTree tree, SemanticModel semanticModel,
                                                        SonarComponents sonarComponents, boolean failedParsing) {
    SemanticModel model = enableSemantic ? semanticModel : null;
    testContext = new TestJavaFileScannerContext(tree, currentFile, model, sonarComponents, javaVersion, failedParsing);
    return testContext;
  }

  public TestJavaFileScannerContext lastCreatedTestContext() {
    return testContext;
  }

  public static class TestJavaFileScannerContext extends DefaultJavaFileScannerContext {

    private final Set<AnalyzerMessage> issues = new HashSet<>();

    public TestJavaFileScannerContext(CompilationUnitTree tree, File file, SemanticModel semanticModel,
                                      @Nullable SonarComponents sonarComponents, JavaVersion javaVersion, boolean failedParsing) {
      super(tree, file, semanticModel, sonarComponents, javaVersion, failedParsing);
    }

    public Set<AnalyzerMessage> getIssues() {
      return issues;
    }

    @Override
    public void addIssue(int line, JavaCheck javaCheck, String message, @Nullable Integer cost) {
      issues.add(new AnalyzerMessage(javaCheck, getFile(), line, message, cost != null ? cost.intValue() : 0));
    }

    @Override
    public void reportIssue(JavaCheck javaCheck, Tree syntaxNode, String message, List<Location> secondary, @Nullable Integer cost) {
      List<List<Location>> flows = secondary.stream().map(Collections::singletonList).collect(Collectors.toList());
      issues.add(createAnalyzerMessage(getFile(), javaCheck, syntaxNode, null, message, flows, cost));
    }

    @Override
    public void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message) {
      issues.add(createAnalyzerMessage(javaCheck, startTree, endTree, message, ImmutableList.of(), null));
    }

    @Override
    public void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message, List<Location> secondary, @Nullable Integer cost) {
      issues.add(createAnalyzerMessage(javaCheck, startTree, endTree, message, secondary, cost));
    }

    @Override
    public void reportIssueWithFlow(JavaCheck javaCheck, Tree syntaxNode, String message, Iterable<List<Location>> flows, @Nullable Integer cost) {
      issues.add(createAnalyzerMessage(getFile(), javaCheck, syntaxNode, null, message, flows, cost));
    }

    private AnalyzerMessage createAnalyzerMessage(JavaCheck javaCheck, Tree startTree, @Nullable Tree endTree, String message, List<Location> secondary, @Nullable Integer cost) {
      return createAnalyzerMessage(getFile(), javaCheck, startTree, endTree, message, Collections.singletonList(secondary), cost);
    }
  }
}
