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
import com.google.common.collect.Lists;
import org.sonar.api.utils.AnnotationUtils;
import org.sonar.java.AnalyzerMessage;
import org.sonar.java.SonarComponents;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleLinearRemediation;
import org.sonar.squidbridge.annotations.SqaleLinearWithOffsetRemediation;
import org.sonar.squidbridge.api.CheckMessage;
import org.sonar.squidbridge.api.SourceFile;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Original VisitorsBridge renamed to InternalVisitorsBridge because VisitorsBridge
 * is the recommended way for custom rules.
 * We need to customize VisitorsBridge to keep compatibility with CheckMessageVerifier.
 * InternalVisitorsBridge can be refactored once CheckMessageVerifier support will be dropped.
 */
public class VisitorsBridge extends InternalVisitorsBridge {

  private TestJavaFileScannerContext testContext;

  @VisibleForTesting
  public VisitorsBridge(JavaFileScanner visitor) {
    this(Arrays.asList(visitor), Lists.<File>newArrayList(), null);
  }

  @VisibleForTesting
  public VisitorsBridge(JavaFileScanner visitor, List<File> projectClasspath) {
    this(Arrays.asList(visitor), projectClasspath, null, false);
  }

  public VisitorsBridge(Iterable visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents) {
    super(visitors, projectClasspath, sonarComponents, true);
  }

  public VisitorsBridge(Iterable visitors, List<File> projectClasspath, @Nullable SonarComponents sonarComponents, boolean symbolicExecutionEnabled) {
    super(visitors, projectClasspath, sonarComponents, symbolicExecutionEnabled);
  }

  @Override
  protected JavaFileScannerContext createScannerContext(CompilationUnitTree tree, SemanticModel semanticModel, boolean analyseAccessors, SonarComponents sonarComponents,
    boolean failedParsing) {
    testContext = new TestJavaFileScannerContext(tree, (SourceFile) getContext().peekSourceCode(), getContext().getFile(), semanticModel, analyseAccessors, sonarComponents,
      getJavaVersion(), failedParsing);
    return testContext;
  }

  public TestJavaFileScannerContext lastCreatedTestContext() {
    return testContext;
  }

  public static class TestJavaFileScannerContext extends DefaultJavaFileScannerContext {

    private final Set<AnalyzerMessage> issues = new HashSet<>();

    public TestJavaFileScannerContext(
      CompilationUnitTree tree, SourceFile sourceFile, File file, SemanticModel semanticModel, boolean analyseAccessors, @Nullable SonarComponents sonarComponents,
      JavaVersion javaVersion, boolean failedParsing) {
      super(tree, sourceFile, file, semanticModel, analyseAccessors, sonarComponents, javaVersion, failedParsing);
    }

    public Set<AnalyzerMessage> getIssues() {
      return issues;
    }

    @Override
    public void addIssue(int line, JavaCheck javaCheck, String message, @Nullable Double cost) {
      issues.add(new AnalyzerMessage(javaCheck, getFile(), line, message, cost != null ? cost.intValue() : 0));
      addIssueForCheckMessageVerifier(line, javaCheck, message, cost);
    }

    /**
     * @deprecated should be removed once CheckMessageVerifier support is dropped
     */
    @Deprecated
    private void addIssueForCheckMessageVerifier(int line, JavaCheck javaCheck, String message, @Nullable Double cost) {
      // compatibility with CheckMessageVerifier
      CheckMessage checkMessage = new CheckMessage(javaCheck, message);
      if (line > 0) {
        checkMessage.setLine(line);
      }
      if (cost == null) {
        Annotation linear = AnnotationUtils.getAnnotation(javaCheck, SqaleLinearRemediation.class);
        Annotation linearWithOffset = AnnotationUtils.getAnnotation(javaCheck, SqaleLinearWithOffsetRemediation.class);
        if ((linear != null) || (linearWithOffset != null)) {
          throw new IllegalStateException("A check annotated with a linear sqale function should provide an effort to fix");
        }
      } else {
        checkMessage.setCost(cost);
      }
      sourceFile.log(checkMessage);
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
      addIssueForCheckMessageVerifier(analyzerMessage.getLine(), javaCheck, message, (double) (cost != null ? cost : 0));
    }
  }
}
