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
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.SonarComponents;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VisitorsBridgeForTests extends VisitorsBridge {

  private TestJavaFileScannerContext testContext;

  @VisibleForTesting
  public VisitorsBridgeForTests(JavaFileScanner visitor) {
    this(Collections.singletonList(visitor), new ArrayList<File>(), null);
  }

  public VisitorsBridgeForTests(Iterable visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents) {
    this(visitors, projectClasspath, sonarComponents, true);
  }

  public VisitorsBridgeForTests(Iterable visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents, boolean symbolicExecutionEnabled) {
    super(visitors, projectClasspath, sonarComponents, symbolicExecutionEnabled);
  }

  @Override
  protected JavaFileScannerContext createScannerContext(CompilationUnitTree tree, SemanticModel semanticModel, boolean analyseAccessors,
                                                        SonarComponents sonarComponents, boolean failedParsing) {
    testContext = new TestJavaFileScannerContext(tree, currentFile, semanticModel, analyseAccessors, sonarComponents, javaVersion, failedParsing);
    return testContext;
  }

  public TestJavaFileScannerContext lastCreatedTestContext() {
    return testContext;
  }

  public static class TestJavaFileScannerContext extends DefaultJavaFileScannerContext {

    private final Set<AnalyzerMessage> issues = new HashSet<>();

    public TestJavaFileScannerContext(CompilationUnitTree tree, File file, SemanticModel semanticModel,
                                      boolean analyseAccessors, @Nullable SonarComponents sonarComponents, JavaVersion javaVersion, boolean failedParsing) {
      super(tree, file, semanticModel, analyseAccessors, sonarComponents, javaVersion, failedParsing);
    }

    public Set<AnalyzerMessage> getIssues() {
      return issues;
    }

    @Override
    public void addIssue(int line, JavaCheck javaCheck, String message, @Nullable Double cost) {
      issues.add(new AnalyzerMessage(javaCheck, getFile(), line, message, cost != null ? cost.intValue() : 0));
    }

    @Override
    public void reportIssue(JavaCheck javaCheck, Tree syntaxNode, String message, List<Location> secondary, @Nullable Integer cost) {
      File file = getFile();
      AnalyzerMessage analyzerMessage = new AnalyzerMessage(javaCheck, file, AnalyzerMessage.textSpanFor(syntaxNode), message, cost != null ? cost : 0);
      for (Location location : secondary) {
        AnalyzerMessage secondaryLocation = new AnalyzerMessage(javaCheck, file, AnalyzerMessage.textSpanFor(location.syntaxNode), location.msg, 0);
        analyzerMessage.secondaryLocations.add(secondaryLocation);
      }
      issues.add(analyzerMessage);
    }

    @Override
    public void reportIssue(JavaCheck javaCheck, Tree startTree, Tree endTree, String message) {
      issues.add(new AnalyzerMessage(javaCheck, getFile(), AnalyzerMessage.textSpanBetween(startTree, endTree), message, 0));
    }
  }
}
