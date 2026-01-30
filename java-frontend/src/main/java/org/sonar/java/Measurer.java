/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java;

import java.io.Serializable;
import java.util.ArrayList;
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
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.java.metrics.MetricsScannerContext;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

public class Measurer extends SubscriptionVisitor {

  private static final Tree.Kind[] CLASS_KINDS = new Tree.Kind[]{
    Tree.Kind.CLASS,
    Tree.Kind.INTERFACE,
    Tree.Kind.ENUM,
    Tree.Kind.ANNOTATION_TYPE,
    Tree.Kind.RECORD,
    Tree.Kind.IMPLICIT_CLASS
  };

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
      var metricsComputer = ((MetricsScannerContext)context).getMetricsComputer();
      noSonarFilter.noSonarInFile(sonarFile, metricsComputer.getNoSonarLines(context.getTree()));
    }
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    List<Tree.Kind> nodes = new ArrayList<>(Arrays.asList(CLASS_KINDS));
    nodes.addAll(Arrays.asList(
      Tree.Kind.NEW_CLASS,
      Tree.Kind.ENUM_CONSTANT,
      Tree.Kind.METHOD,
      Tree.Kind.CONSTRUCTOR));
    return nodes;
  }


  @Override
  public void scanFile(JavaFileScannerContext context) {
    sonarFile = context.getInputFile();
    var metricsComputer = ((MetricsScannerContext)context).getMetricsComputer();
    noSonarFilter.noSonarInFile(sonarFile, metricsComputer.getNoSonarLines(context.getTree()));
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
    saveMetricOnFile(CoreMetrics.CLASSES, classes);
    saveMetricOnFile(CoreMetrics.FUNCTIONS, methods);
    saveMetricOnFile(CoreMetrics.COMPLEXITY, metricsComputer.getComplexityNodes(context.getTree()).size());
    saveMetricOnFile(CoreMetrics.COMMENT_LINES, metricsComputer.getNumberOfCommentedLines(context.getTree()));
    saveMetricOnFile(CoreMetrics.STATEMENTS, metricsComputer.getNumberOfStatements(context.getTree()));
    saveMetricOnFile(CoreMetrics.NCLOC, metricsComputer.getLinesOfCode(context.getTree()));

    saveMetricOnFile(CoreMetrics.COGNITIVE_COMPLEXITY, CognitiveComplexityVisitor.compilationUnitComplexity(context.getTree()));
  }

  private boolean isSonarLintContext() {
    return sensorContext.runtime().getProduct() == SonarProduct.SONARLINT;
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
    return tree.is(CLASS_KINDS);
  }

  private <T extends Serializable> void saveMetricOnFile(Metric<T> metric, T value) {
    sensorContext.<T>newMeasure().forMetric(metric).on(sonarFile).withValue(value).save();
  }
}
