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

import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;

@Rule(key = "S9130")
public class StreamReadResultCastCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers READ_MATCHERS = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("java.io.InputStream")
      .names("read")
      .addWithoutParametersMatcher()
      .build(),
    MethodMatchers.create()
      .ofSubTypes("java.io.Reader")
      .names("read")
      .addWithoutParametersMatcher()
      .build());

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.TYPE_CAST);
  }

  @Override
  public void visitNode(Tree tree) {
    TypeCastTree castTree = (TypeCastTree) tree;
    var castToType = castTree.type().symbolType();
    if (castToType.is("byte") || castToType.is("char")) {
      ExpressionTree expression = ExpressionUtils.skipParentheses(castTree.expression());
      if (expression.is(Tree.Kind.METHOD_INVOCATION) && READ_MATCHERS.matches((MethodInvocationTree) expression)) {
        reportIssue(castTree, "Store the return value of \"read()\" in an \"int\" variable and check for -1 before casting.");
      }
    }
  }

}
