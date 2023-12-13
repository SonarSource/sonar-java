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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.model.InternalSyntaxTrivia;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
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

    for (SyntaxTrivia syntaxTrivia : mergeAdjacentSingleLineComments(syntaxToken.trivias())) {
      String[] lines = syntaxTrivia.comment().split("\r\n?|\n");

      if (containsCode(lines)) {
        reportCommentedOutCode(syntaxTrivia);
      }
    }
  }

  private static List<SyntaxTrivia> mergeAdjacentSingleLineComments(List<SyntaxTrivia> syntaxTrivias) {
    var mergedTrivia = new LinkedList<SyntaxTrivia>();
    var triviaToMerge = new LinkedList<SyntaxTrivia>();

    for (SyntaxTrivia syntaxTrivia : syntaxTrivias) {

      if (triviaToMerge.isEmpty()
        || (sameCommentType(triviaToMerge.getLast(), syntaxTrivia) && contiguousComments(triviaToMerge.getLast(), syntaxTrivia))) {
        triviaToMerge.add(syntaxTrivia);
      } else {
        mergedTrivia.add(mergeSyntaxTrivia(triviaToMerge));
        triviaToMerge.clear();
        // start a new list of comments to merge
        triviaToMerge.add(syntaxTrivia);
      }
    }

    if (!triviaToMerge.isEmpty()) {
      mergedTrivia.add(mergeSyntaxTrivia(triviaToMerge));
    }

    return mergedTrivia;
  }

  private static boolean sameCommentType(SyntaxTrivia syntaxTrivia1, SyntaxTrivia syntaxTrivia2) {
    return syntaxTrivia1.comment().startsWith("//") && syntaxTrivia2.comment().startsWith("//");
  }

  private static boolean contiguousComments(SyntaxTrivia syntaxTrivia1, SyntaxTrivia syntaxTrivia2) {
    return syntaxTrivia1.range().end().line() + 1 == syntaxTrivia2.range().start().line();
  }

  private static SyntaxTrivia mergeSyntaxTrivia(List<SyntaxTrivia> syntaxTrivia) {
    var mergedComments = syntaxTrivia.stream().map(SyntaxTrivia::comment).collect(Collectors.joining("\n"));
    var firstCommentRange = syntaxTrivia.get(0).range();
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

  private void reportCommentedOutCode(SyntaxTrivia syntaxTrivia) {
    AnalyzerMessage.TextSpan textSpan = new AnalyzerMessage.TextSpan(
      syntaxTrivia.range().start().line(),
      syntaxTrivia.range().start().columnOffset(),
      syntaxTrivia.range().end().line(),
      syntaxTrivia.range().end().columnOffset());

    AnalyzerMessage message = new AnalyzerMessage(this, context.getInputFile(), textSpan, CommentedOutCodeLineMLCheck.MESSAGE, 0);

    ((DefaultJavaFileScannerContext) this.context).reportIssue(message);
  }
}
