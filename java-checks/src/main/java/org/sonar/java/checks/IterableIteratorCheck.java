/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4348")
public class IterableIteratorCheck extends IssuableSubscriptionVisitor {

  private static final MethodMatchers ITERATOR = MethodMatchers.create()
    .ofSubTypes("java.lang.Iterable").names("iterator").addWithoutParametersMatcher().build();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ReturnThis returnThis = new ReturnThis();
    ClassTree classTree = (ClassTree) tree;
    Type classType = classTree.symbol().type();
    if(!(classType.isSubtypeOf("java.util.Iterator") && classType.isSubtypeOf("java.lang.Iterable"))) {
      return;
    }
    classTree.members().stream()
      .filter(m -> m.is(Tree.Kind.METHOD))
      .filter(m -> ITERATOR.matches(((MethodTree) m)))
      .forEach(t -> t.accept(returnThis));
    if (!returnThis.issueLocations.isEmpty()) {
      reportIssue(returnThis.issueLocations.get(0), "Refactor this code so that the Iterator supports multiple traversal",
        returnThis.issueLocations.stream().skip(1)
          .map(t -> new JavaFileScannerContext.Location("", t)).toList(),
        null);
    }
  }

  private static class ReturnThis extends BaseTreeVisitor {
    private List<Tree> issueLocations = new ArrayList<>();

    @Override
    public void visitClass(ClassTree tree) {
      // cut the visit for inner classes
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      ExpressionTree returnedExpression = tree.expression();
      if (ExpressionUtils.isThis(returnedExpression)) {
        issueLocations.add(returnedExpression);
      }
      super.visitReturnStatement(tree);
    }
  }
}
