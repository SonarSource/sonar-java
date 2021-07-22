package org.sonar.java.reporting;

import java.util.List;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.java.AnalyzerMessage.TextSpan;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Tree;

public class JavaIssueBuilderImpl implements FluentReporting.JavaIssueBuilder {

  private final InputFile inputFile;
  private final SensorContext sensorContext;

  public JavaIssueBuilderImpl(JavaFileScannerContext context) {
    this.inputFile = context.getInputFile();
    this.sensorContext = ((DefaultJavaFileScannerContext) context).sensorContext();
  }

  @Override
  public JavaIssueBuilderImpl forRule(JavaCheck rule) {
    // TODO
    return this;
  }

  @Override
  public JavaIssueBuilderImpl onTree(Tree tree) {
    // TODO
    return this;
  }

  @Override
  public JavaIssueBuilderImpl onRange(Tree from, Tree to) {
    // TODO
    return this;
  }

  @Override
  public JavaIssueBuilderImpl onRange(TextSpan range) {
    // TODO
    return this;
  }

  @Override
  public JavaIssueBuilderImpl withMessage(String message) {
    // TODO
    return this;
  }

  @Override
  public JavaIssueBuilderImpl withMessage(String message, Object... args) {
    // TODO
    return this;
  }

  @Override
  public JavaIssueBuilderImpl withSecondaries(JavaFileScannerContext.Location... secondaries) {
    // TODO
    return this;
  }

  @Override
  public JavaIssueBuilderImpl withSecondaries(List<JavaFileScannerContext.Location> secondaries) {
    // TODO
    return this;
  }

  @Override
  public JavaIssueBuilderImpl withFlows(List<List<JavaFileScannerContext.Location>> flows) {
    // TODO
    return this;
  }

  @Override
  public JavaIssueBuilderImpl withCost(int cost) {
    // TODO
    return this;
  }

  @Override
  public JavaIssueBuilderImpl withQuickFix() {
    // TODO
    return this;
  }

  @Override
  public void build() {
    // TODO
  }

}
