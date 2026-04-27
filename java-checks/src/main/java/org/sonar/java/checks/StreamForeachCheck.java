/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.java.checks.helpers.QuickFixHelper;

@Rule(key = "S3706")
public class StreamForeachCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers STREAM_METHOD = MethodMatchers.create()
    .ofSubTypes("java.util.Collection")
    .names("stream")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers STREAM_FOREACH_METHOD = MethodMatchers.create()
    .ofSubTypes("java.util.stream.Stream")
    .names("forEach")
    .withAnyParameters()
    .build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof MethodInvocationTree mit && STREAM_FOREACH_METHOD.matches(mit)) {
      checkUnnecessaryForEach(mit);
    }
  }

  private void checkUnnecessaryForEach(MethodInvocationTree mitForEach) {
    if (mitForEach.methodSelect() instanceof MemberSelectExpressionTree msetForEach
      && msetForEach.expression() instanceof MethodInvocationTree mitStream
      && STREAM_METHOD.matches(mitStream)
      && mitStream.methodSelect() instanceof MemberSelectExpressionTree msetStream) {
      QuickFixHelper.newIssue(context)
        .forRule(this)
        .onRange(msetStream.identifier(), msetForEach.identifier())
        .withMessage("Simplify the code by replacing .stream().forEach() with .forEach().")
        .withQuickFix(() -> computeQuickFix(msetStream, mitStream))
        .report();
    }
  }

  private static JavaQuickFix computeQuickFix(MemberSelectExpressionTree msetStream, MethodInvocationTree mitStream) {
    return JavaQuickFix.newQuickFix("Remove the call to \".stream()\"")
        .addTextEdit(JavaTextEdit.removeBetweenTree(msetStream.operatorToken(), mitStream))
        .build();
  }
}
