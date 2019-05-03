/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
package org.sonar.java;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.sonar.api.SonarProduct;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.java.ast.visitors.CognitiveComplexityVisitor;
import org.sonar.java.ast.visitors.CommentLinesVisitor;
import org.sonar.java.ast.visitors.LinesOfCodeVisitor;
import org.sonar.java.ast.visitors.StatementVisitor;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

public class Measurer extends SubscriptionVisitor {

  private final SensorContext sensorContext;
  private final NoSonarFilter noSonarFilter;
  private InputFile sonarFile;
  private int methods;
  private final Deque<ClassTree> classTrees = new LinkedList<>();
  private int classes;

  public Measurer(SensorContext context, NoSonarFilter noSonarFilter) {
    this.sensorContext = context;
    this.noSonarFilter = noSonarFilter;
  }

  public class TestFileMeasurer implements JavaFileScanner {
    @Override
    public void scanFile(JavaFileScannerContext context) {
      sonarFile = context.getInputFile();
      createCommentLineVisitorAndFindNoSonar(context);
    }
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM, Tree.Kind.ANNOTATION_TYPE,
        Tree.Kind.NEW_CLASS, Tree.Kind.ENUM_CONSTANT,
        Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }


  @Override
  public void scanFile(JavaFileScannerContext context) {
    sonarFile = context.getInputFile();
    CommentLinesVisitor commentLinesVisitor = createCommentLineVisitorAndFindNoSonar(context);
    if(isSonarLintContext()) {
      // No need to compute metrics on SonarLint side, but the no sonar filter is still required
      return;
    }
    classTrees.clear();
    methods = 0;
    classes = 0;
    super.setContext(context);
    scanTree(context.getTree());
    //leave file.
    int fileComplexity = context.getComplexityNodes(context.getTree()).size();
    saveMetricOnFile(CoreMetrics.CLASSES, classes);
    saveMetricOnFile(CoreMetrics.FUNCTIONS, methods);
    saveMetricOnFile(CoreMetrics.COMPLEXITY, fileComplexity);
    saveMetricOnFile(CoreMetrics.COMMENT_LINES, commentLinesVisitor.commentLinesMetric());
    saveMetricOnFile(CoreMetrics.STATEMENTS, new StatementVisitor().numberOfStatements(context.getTree()));
    saveMetricOnFile(CoreMetrics.NCLOC, new LinesOfCodeVisitor().linesOfCode(context.getTree()));

    saveMetricOnFile(CoreMetrics.COGNITIVE_COMPLEXITY, CognitiveComplexityVisitor.compilationUnitComplexity(context.getTree()));
  }

  private boolean isSonarLintContext() {
    return sensorContext.runtime().getProduct() == SonarProduct.SONARLINT;
  }

  private CommentLinesVisitor createCommentLineVisitorAndFindNoSonar(JavaFileScannerContext context) {
    CommentLinesVisitor commentLinesVisitor = new CommentLinesVisitor();
    commentLinesVisitor.analyzeCommentLines(context.getTree());
    noSonarFilter.noSonarInFile(sonarFile, commentLinesVisitor.noSonarLines());
    return commentLinesVisitor;
  }

  @Override
  public void visitNode(Tree tree) {
    if (isClassTree(tree)) {
      classes++;
      classTrees.push((ClassTree) tree);
    }
    if (tree.is(Tree.Kind.NEW_CLASS) && ((NewClassTree) tree).classBody() != null) {
      classes--;
    }
    if (tree.is(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR) && classTrees.peek().simpleName() != null) {
      //don't count methods in anonymous classes.
      methods++;
    }

  }

  @Override
  public void leaveNode(Tree tree) {
    if (isClassTree(tree)) {
      classTrees.pop();
    }
  }

  private static boolean isClassTree(Tree tree) {
    return tree.is(Tree.Kind.CLASS) || tree.is(Tree.Kind.INTERFACE) || tree.is(Tree.Kind.ENUM) || tree.is(Tree.Kind.ANNOTATION_TYPE);
  }

  private <T extends Serializable> void saveMetricOnFile(Metric<T> metric, T value) {
    sensorContext.<T>newMeasure().forMetric(metric).on(sonarFile).withValue(value).save();
  }
}
