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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.InternalSyntaxTrivia;
import org.sonar.java.model.LineUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.ml.feature_engineering.CommentPreparation;
import org.sonar.ml.feature_engineering.VocabularyAndSemicolonFeatures;
import org.sonar.ml.model.LinearRegressionModel;
import org.sonar.ml.model.LogisticRegressionModel;
import org.sonar.ml.tokenization.RoBERTaBPEEncoder;
import org.sonar.ml.tokenization.RoBERTaTokenizer;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.location.Position;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.ml.feature_engineering.CommentPreparation.CommentType.JAVADOC;
import static org.sonar.ml.feature_engineering.VocabularyAndSemicolonFeatures.loadVocabulary;
import static org.sonar.ml.model.LinearRegressionModel.loadParams;

@Rule(key = "S125-ML")
public class CommentedOutCodeLineMLCheck extends IssuableSubscriptionVisitor {

  private static final String MODEL_LR_100_FILENAME = "ml/S125/model-lr-100.json";
  private static final String MERGES_TXT = "ml/S125/merges.txt";
  private static final String VOCAB_100_FILENAME = "ml/S125/vocab-100.json";
  private static final double DECISION_THRESHOLD = 0.83d;
  public static final int MAX_TOKENS_PER_STRING = 500;

  private final CommentPreparation commentPreparation;
  private final RoBERTaTokenizer tokenizer;
  private final VocabularyAndSemicolonFeatures featureExtractor;
  private final LogisticRegressionModel model;

  public CommentedOutCodeLineMLCheck() {
    commentPreparation = CommentPreparation.newInstance();

    try {
      tokenizer = new RoBERTaTokenizer(RoBERTaBPEEncoder.from(loadResource(MERGES_TXT)));
      VocabularyAndSemicolonFeatures.Vocabulary vocabulary = loadVocabulary(loadResource(VOCAB_100_FILENAME));
      featureExtractor = new VocabularyAndSemicolonFeatures(vocabulary, MAX_TOKENS_PER_STRING);
      LinearRegressionModel.ModelParams linearRegressionParams = loadParams(loadResource(MODEL_LR_100_FILENAME));
      model = new LogisticRegressionModel(new LinearRegressionModel(linearRegressionParams), DECISION_THRESHOLD);

    } catch (IOException e) {
      throw new IllegalArgumentException("Could setup ML Model.", e);
    }
  }

  private static FileInputStream loadResource(String resourceName) throws FileNotFoundException {
    var resourcePath = CommentedOutCodeLineMLCheck.class.getClassLoader().getResource(resourceName);
    return new FileInputStream(Objects.requireNonNull(resourcePath).getFile());
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TOKEN);
  }

  @Override
  public void visitToken(SyntaxToken syntaxToken) {
    for (SyntaxTrivia syntaxTrivia : mergeAdjacentSingleLineComments(syntaxToken.trivias())) {

      String[] lines = syntaxTrivia.comment().split("\r\n?|\n");

      if (containsCode(lines)) {
        reportCommentedOutCode(syntaxTrivia, lines);
      }
    }
  }

  private static List<SyntaxTrivia> mergeAdjacentSingleLineComments(List<SyntaxTrivia> syntaxTrivias) {
    var mergedTrivias = new LinkedList<SyntaxTrivia>();
    var triviaToMerge = new LinkedList<SyntaxTrivia>();

    for (SyntaxTrivia syntaxTrivia : syntaxTrivias) {
      if (triviaToMerge.isEmpty()
        || (sameCommentType(triviaToMerge.getLast(), syntaxTrivia) && contiguousComments(triviaToMerge.getLast(), syntaxTrivia))) {
        triviaToMerge.add(syntaxTrivia);
      } else {
        mergedTrivias.add(mergeSyntaxTrivias(triviaToMerge));
        triviaToMerge.clear();
        triviaToMerge.add(syntaxTrivia);
      }
    }

    if(!triviaToMerge.isEmpty()) {
      mergedTrivias.add(mergeSyntaxTrivias(triviaToMerge));
    }

    return mergedTrivias;
  }

  private static boolean sameCommentType(SyntaxTrivia syntaxTrivia1, SyntaxTrivia syntaxTrivia2) {
    return syntaxTrivia1.comment().startsWith("//") && syntaxTrivia2.comment().startsWith("//")
      || (syntaxTrivia1.comment().startsWith("/*") && syntaxTrivia1.comment().endsWith("*/")
        && syntaxTrivia2.comment().startsWith("/*") && syntaxTrivia2.comment().endsWith("*/"));
  }

  private static boolean contiguousComments(SyntaxTrivia syntaxTrivia1, SyntaxTrivia syntaxTrivia2) {
    return syntaxTrivia1.range().end().line() + 1 == syntaxTrivia2.range().start().line();
  }

  private static SyntaxTrivia mergeSyntaxTrivias(List<SyntaxTrivia> syntaxTrivias) {
    var mergedComments = syntaxTrivias.stream().map(SyntaxTrivia::comment).collect(Collectors.joining("\n"));
    var firstCommentRange = syntaxTrivias.get(0).range();
    return new InternalSyntaxTrivia(mergedComments, firstCommentRange.start().line(), firstCommentRange.start().columnOffset());
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
    // todo figure out the logic to identify where the commented out code starts! O.O -> Binary Search?
    // for now just reporting at the beginning of the comment
    int lineOffset = 0;
    String line = lines[lineOffset];

    int startLine = LineUtils.startLine(syntaxTrivia) + lineOffset;
    int startColumnOffset = (lineOffset == 0 ? Position.startOf(syntaxTrivia).columnOffset() : 0);

    ((DefaultJavaFileScannerContext) this.context)
      .reportIssue(createAnalyzerMessage(startLine, startColumnOffset, line, "This block of commented-out lines of code should be removed."));
  }

  // todo: methods below are a copy of the methods in CommentedOutCodeLineCheck, should be refactored to a common place!
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
