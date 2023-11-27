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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.ml.feature_engineering.CommentPreparation;
import org.sonar.ml.feature_engineering.VocabularyAndSemicolonFeatures;
import org.sonar.ml.model.LinearRegressionModel;
import org.sonar.ml.model.LogisticRegressionModel;
import org.sonar.ml.tokenization.RoBERTaBPEEncoder;
import org.sonar.ml.tokenization.RoBERTaTokenizer;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.ml.feature_engineering.CommentPreparation.CommentType.JAVADOC;
import static org.sonar.ml.feature_engineering.VocabularyAndSemicolonFeatures.loadVocabulary;
import static org.sonar.ml.model.LinearRegressionModel.loadParams;

@Rule(key = "S125")
public class CommentedOutCodeLineMLCheck extends IssuableSubscriptionVisitor {

  private static final String MODEL_LR_100_FILENAME = "/Users/angelo.buono/IdeaProjects/sonar-java/java-checks/src/main/resources/ml/S125/model-lr-100.json";
  private static final String MERGES_TXT = "/Users/angelo.buono/IdeaProjects/sonar-java/java-checks/src/main/resources/ml/S125/merges.txt";
  private static final String VOCAB_100_FILENAME = "/Users/angelo.buono/IdeaProjects/sonar-java/java-checks/src/main/resources/ml/S125/vocab-100.json";
  private static final double DECISION_THRESHOLD = 0.83d;
  public static final int MAX_TOKENS_PER_STRING = 500;

  private final CommentPreparation commentPreparation;
  private final RoBERTaTokenizer tokenizer;
  private final VocabularyAndSemicolonFeatures featureExtractor;
  private final LogisticRegressionModel model;

  public CommentedOutCodeLineMLCheck() {
    commentPreparation = CommentPreparation.newInstance();

    try {
      tokenizer = new RoBERTaTokenizer(RoBERTaBPEEncoder.from(new FileInputStream(MERGES_TXT)));
      VocabularyAndSemicolonFeatures.Vocabulary vocabulary = loadVocabulary(new FileInputStream(VOCAB_100_FILENAME));
      featureExtractor = new VocabularyAndSemicolonFeatures(vocabulary, MAX_TOKENS_PER_STRING);
      LinearRegressionModel.ModelParams linearRegressionParams = loadParams(new FileInputStream(MODEL_LR_100_FILENAME));
      model = new LogisticRegressionModel(new LinearRegressionModel(linearRegressionParams), DECISION_THRESHOLD);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TOKEN);
  }

  @Override
  public void visitToken(SyntaxToken syntaxToken) {
    for (SyntaxTrivia syntaxTrivia : syntaxToken.trivias()) {

      String[] lines = syntaxTrivia.comment().split("\r\n?|\n");

      if (containsCode(lines)) {
        reportCommentedOutCode(syntaxTrivia, lines);
      }
    }
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

  private void reportCommentedOutCode(SyntaxTrivia syntaxTrivia, String[] lines) {
    for (int lineOffset = 0; lineOffset < lines.length; lineOffset++) {

      // todo figure out the logic to identify where the commented out code starts! O.O -> Binary Search?

//      String line = lines[lineOffset];
//
//      int startLine = LineUtils.startLine(syntaxTrivia) + lineOffset;
//      int startColumnOffset = (lineOffset == 0 ? Position.startOf(syntaxTrivia).columnOffset() : 0);
//
//      ((DefaultJavaFileScannerContext) this.context)
//        .reportIssue(createAnalyzerMessage(startLine, startColumnOffset, line, "Code"));
    }
  }

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
