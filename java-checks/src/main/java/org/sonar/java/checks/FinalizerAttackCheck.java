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
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

@Rule(key = "S9345")
public class FinalizerAttackCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return Collections.singletonList(Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree classTree = (ClassTree) tree;
    if (classTree.simpleName() == null ||
      ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.FINAL) ||
      ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.ABSTRACT) ||
      ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.SEALED)) {
      return;
    }
    List<JavaFileScannerContext.Location> secondaryLocations = Collections.singletonList(
      new JavaFileScannerContext.Location("Non-final class", classTree.simpleName()));
    for (Tree member : classTree.members()) {
      if (member.is(Kind.CONSTRUCTOR) && isVulnerableConstructor((MethodTree) member)) {
        MethodTree constructor = (MethodTree) member;
        reportIssue(constructor.simpleName(),
          "Make this class \"final\" or make this throwing constructor \"private\".",
          secondaryLocations, null);
      }
    }
  }

  private static boolean isVulnerableConstructor(MethodTree constructor) {
    if (ModifiersUtils.hasModifier(constructor.modifiers(), Modifier.PRIVATE)) {
      return false;
    }
    return !constructor.throwsClauses().isEmpty() || containsThrowStatement(constructor);
  }

  private static boolean containsThrowStatement(MethodTree constructor) {
    BlockTree block = constructor.block();
    if (block == null) {
      return false;
    }
    ThrowStatementVisitor visitor = new ThrowStatementVisitor();
    block.accept(visitor);
    return visitor.hasThrow;
  }

  private static class ThrowStatementVisitor extends BaseTreeVisitor {
    boolean hasThrow;

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      hasThrow = true;
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip nested classes
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree tree) {
      // skip lambdas
    }
  }
}
