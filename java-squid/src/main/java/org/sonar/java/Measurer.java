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
package org.sonar.java;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PersistenceMode;
import org.sonar.api.measures.RangeDistributionBuilder;
import org.sonar.java.ast.visitors.AccessorsUtils;
import org.sonar.java.ast.visitors.CommentLinesVisitor;
import org.sonar.java.ast.visitors.LinesOfCodeVisitor;
import org.sonar.java.ast.visitors.PublicApiChecker;
import org.sonar.java.ast.visitors.StatementVisitor;
import org.sonar.java.ast.visitors.SubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class Measurer extends SubscriptionVisitor implements CharsetAwareVisitor {

  private static final Number[] LIMITS_COMPLEXITY_METHODS = {1, 2, 4, 6, 8, 10, 12};
  private static final Number[] LIMITS_COMPLEXITY_FILES = {0, 5, 10, 20, 30, 60, 90};

  private final FileSystem fs;
  private final SensorContext sensorContext;
  private final boolean separateAccessorsFromMethods;
  private InputFile sonarFile;
  private int methods;
  private int accessors;
  private int complexityInMethods;
  private RangeDistributionBuilder methodComplexityDistribution;

  private final Deque<ClassTree> classTrees = new LinkedList<>();
  private Charset charset;
  private double classes;

  public Measurer(FileSystem fs, SensorContext context, boolean separateAccessorsFromMethods) {
    this.fs = fs;
    this.sensorContext = context;
    this.separateAccessorsFromMethods = separateAccessorsFromMethods;
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM, Tree.Kind.ANNOTATION_TYPE,
        Tree.Kind.NEW_CLASS, Tree.Kind.ENUM_CONSTANT,
        Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }


  @Override
  public void scanFile(JavaFileScannerContext context) {
    sonarFile = fs.inputFile(fs.predicates().is(context.getFile()));
    classTrees.clear();
    methods = 0;
    complexityInMethods = 0;
    accessors = 0;
    classes = 0;
    PublicApiChecker publicApiChecker = PublicApiChecker.newInstanceWithAccessorsHandledAsMethods();
    if (separateAccessorsFromMethods) {
      publicApiChecker = PublicApiChecker.newInstanceWithAccessorsSeparatedFromMethods();
    }
    publicApiChecker.scan(context.getTree());
    methodComplexityDistribution = new RangeDistributionBuilder(CoreMetrics.FUNCTION_COMPLEXITY_DISTRIBUTION, LIMITS_COMPLEXITY_METHODS);
    CommentLinesVisitor commentLinesVisitor = new CommentLinesVisitor();
    commentLinesVisitor.analyzeCommentLines(context.getTree());
    context.addNoSonarLines(commentLinesVisitor.noSonarLines());
    super.scanFile(context);
    //leave file.
    int fileComplexity = context.getComplexity(context.getTree());
    saveMetricOnFile(CoreMetrics.CLASSES, classes);
    saveMetricOnFile(CoreMetrics.FUNCTIONS, methods);
    saveMetricOnFile(CoreMetrics.ACCESSORS, accessors);
    saveMetricOnFile(CoreMetrics.COMPLEXITY_IN_FUNCTIONS, complexityInMethods);
    saveMetricOnFile(CoreMetrics.COMPLEXITY, fileComplexity);
    saveMetricOnFile(CoreMetrics.PUBLIC_API, publicApiChecker.getPublicApi());
    saveMetricOnFile(CoreMetrics.PUBLIC_DOCUMENTED_API_DENSITY, publicApiChecker.getDocumentedPublicApiDensity());
    saveMetricOnFile(CoreMetrics.PUBLIC_UNDOCUMENTED_API, publicApiChecker.getUndocumentedPublicApi());
    saveMetricOnFile(CoreMetrics.COMMENT_LINES, commentLinesVisitor.commentLinesMetric());
    saveMetricOnFile(CoreMetrics.STATEMENTS, new StatementVisitor().numberOfStatements(context.getTree()));
    saveMetricOnFile(CoreMetrics.NCLOC, new LinesOfCodeVisitor().linesOfCode(context.getTree()));

    sensorContext.saveMeasure(sonarFile, methodComplexityDistribution.build(true).setPersistenceMode(PersistenceMode.MEMORY));

    RangeDistributionBuilder fileComplexityDistribution = new RangeDistributionBuilder(CoreMetrics.FILE_COMPLEXITY_DISTRIBUTION, LIMITS_COMPLEXITY_FILES);
    sensorContext.saveMeasure(sonarFile, fileComplexityDistribution.add(fileComplexity).build(true).setPersistenceMode(PersistenceMode.MEMORY));
    saveLinesMetric();

  }

  private void saveLinesMetric() {
    try {
      String content = Files.toString(context.getFile(), charset);
      saveMetricOnFile(CoreMetrics.LINES, content.split("(\r)?\n|\r", -1).length);
    } catch (IOException e) {
      Throwables.propagate(e);
    }
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
      MethodTree methodTree = (MethodTree) tree;
      if (separateAccessorsFromMethods && AccessorsUtils.isAccessor(classTrees.peek(), methodTree)) {
        accessors++;
      } else {
        methods++;
        int methodComplexity = context.getMethodComplexity(classTrees.peek(), methodTree);
        methodComplexityDistribution.add(methodComplexity);
        complexityInMethods += methodComplexity;
      }
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

  private void saveMetricOnFile(Metric metric, double value) {
    sensorContext.saveMeasure(sonarFile, new Measure(metric, value));
  }

  @Override
  public void setCharset(Charset charset) {
    this.charset = charset;
  }
}
