/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.ml;

import com.sonar.ml.feature_engineering.CommentPreparation;
import com.sonar.ml.feature_engineering.VocabularyAndSemicolonFeatures;
import com.sonar.ml.model.LinearRegressionModel;
import com.sonar.ml.model.LogisticRegressionModel;
import com.sonar.ml.tokenization.RoBERTaBPEEncoder;
import com.sonar.ml.tokenization.RoBERTaTokenizer;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.LineUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import static com.sonar.ml.feature_engineering.CommentPreparation.CommentType.JAVADOC;
import static com.sonar.ml.feature_engineering.VocabularyAndSemicolonFeatures.loadVocabulary;
import static com.sonar.ml.model.LinearRegressionModel.loadParams;

@Rule(key = "S125-ML")
public class CommentedOutCodeLineMLCheck extends IssuableSubscriptionVisitor {
  private static final String MESSAGE = "This block of commented-out lines of code should be removed.";

  private final CommentPreparation commentPreparation;
  private final RoBERTaTokenizer tokenizer;
  private final VocabularyAndSemicolonFeatures featureExtractor;
  private final LogisticRegressionModel model;

  public CommentedOutCodeLineMLCheck() {
    commentPreparation = CommentPreparation.newInstance();

    try {
      tokenizer = new RoBERTaTokenizer(RoBERTaBPEEncoder.from(loadResource("/ml/S125/merges.txt")));
      featureExtractor = new VocabularyAndSemicolonFeatures(loadVocabulary(loadResource("/ml/S125/vocab-100.json")), 500);
      model = new LogisticRegressionModel(new LinearRegressionModel(loadParams(loadResource("/ml/S125/model-lr-100.json"))), 0.83d);

    } catch (IOException e) {
      throw new IllegalArgumentException("Could setup ML Model.", e);
    }
  }

  private InputStream loadResource(String resourceName) {
    return getClass().getResourceAsStream(resourceName);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TOKEN);
  }

  @Override
  public void visitToken(SyntaxToken syntaxToken) {

    List<AnalyzerMessage> issues = new ArrayList<>();
    AnalyzerMessage previousRelatedIssue = null;
    int previousCommentLine = -1;

    for (SyntaxTrivia syntaxTrivia : syntaxToken.trivias()) {
      int currentCommentLine = LineUtils.startLine(syntaxTrivia);

      if (currentCommentLine != previousCommentLine + 1 &&
        currentCommentLine != previousCommentLine) {
        previousRelatedIssue = null;
      }
      String[] lines = syntaxTrivia.comment().split("\r\n?|\n");

      if (containsCode(lines)) {
        previousRelatedIssue = collectIssues(issues, syntaxTrivia, previousRelatedIssue);
        previousCommentLine = currentCommentLine;
      }
    }

    DefaultJavaFileScannerContext scannerContext = (DefaultJavaFileScannerContext) this.context;
    issues.forEach(scannerContext::reportIssue);
  }

  public AnalyzerMessage collectIssues(List<AnalyzerMessage> issues, SyntaxTrivia syntaxTrivia, @Nullable AnalyzerMessage previousRelatedIssue) {
    String[] lines = syntaxTrivia.comment().split("\r\n?|\n");
    AnalyzerMessage issue = previousRelatedIssue;

    for (int lineOffset = 0; lineOffset < lines.length; lineOffset++) {
      String line = lines[lineOffset];

      if (containsCode(List.of(line).toArray(String[]::new))) {
        int startLine = LineUtils.startLine(syntaxTrivia) + lineOffset;
        int startColumnOffset = (lineOffset == 0 ? Position.startOf(syntaxTrivia).columnOffset() : 0);

        if (issue != null) {
          issue.flows.add(Collections.singletonList(createAnalyzerMessage(startLine, startColumnOffset, line, "Code")));
        } else {
          issue = createAnalyzerMessage(startLine, startColumnOffset, line, MESSAGE);
          issues.add(issue);
        }
      }
    }

    return issue;
  }

  private boolean containsCode(String[] lines) {
    return commentPreparation.toComment(List.of(lines))
      .filter(comment -> comment.getType() != JAVADOC)
      .map(comment -> {
        String[] tokens = tokenizer.tokenize(String.join("\n", comment.getCommentedOutText()));
        double[] features = featureExtractor.extractFrom(tokens);
        LogisticRegressionModel.Prediction prediction = model.predict(features);
        return prediction.getDecision();
      })
      .orElse(false);
  }

  // methods below are a copy of the methods in CommentedOutCodeLineCheck, should be refactored to a common place!
  private AnalyzerMessage createAnalyzerMessage(int startLine, int startColumn, String line, String message) {
    String lineWithoutCommentPrefix = line.replaceFirst("^(//|/\\*\\*?|[ \t]*\\*)?[ \t]*+", "");
    int prefixSize = line.length() - lineWithoutCommentPrefix.length();
    String lineWithoutCommentPrefixAndSuffix = removeCommentSuffix(lineWithoutCommentPrefix);

    AnalyzerMessage.TextSpan textSpan = new AnalyzerMessage.TextSpan(
      startLine,
      startColumn + prefixSize,
      startLine,
      startColumn + prefixSize + lineWithoutCommentPrefixAndSuffix.length());

    return new AnalyzerMessage(this, context.getInputFile(), textSpan, message, 0);
  }

  private static String removeCommentSuffix(String line) {
    // We do not use a regex for this task, to avoid ReDoS.
    if (line.endsWith("*/")) {
      line = line.substring(0, line.length() - 2);
    }
    return line.stripTrailing();
  }
}
