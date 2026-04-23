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
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3706")
public class StreamForeachCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers STREAM_METHOD = MethodMatchers.create()
    .ofSubTypes("java.util.Collection")
    .names("stream")
    .withAnyParameters()
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
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (STREAM_FOREACH_METHOD.matches(mit)) {
      checkUnnecessaryForEach(mit);
    }
  }

  private void checkUnnecessaryForEach(MethodInvocationTree mit) {
    ExpressionTree methodSelect = mit.methodSelect();
    if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      ExpressionTree receiver = ((MemberSelectExpressionTree) methodSelect).expression();
      if (receiver.is(Tree.Kind.METHOD_INVOCATION) && STREAM_METHOD.matches((MethodInvocationTree) receiver)) {
        reportIssue(mit, "Replace unnecessary call to .stream().forEach() by .forEach()");
      }
    }
  }
}
