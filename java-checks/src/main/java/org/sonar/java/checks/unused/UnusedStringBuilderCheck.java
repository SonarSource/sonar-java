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
package org.sonar.java.checks.unused;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.InitializerListTreeImpl;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Check that `StringBuilder` and `StringBuffer` instances are consumed by calling `toString()` or in a similar way.
 */
@Rule(key = "S3063")
public class UnusedStringBuilderCheck extends IssuableSubscriptionVisitor {
  private static final Set<String> TERMINAL_METHODS = Set.of("charAt", "getChars", "substring", "toString");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.VARIABLE);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree instanceof VariableTree variableTree) {
      Symbol symbol = variableTree.symbol();
      String typeName = getStringBuilderOrStringBuffer(symbol.type());

      // Exclude non-local variables, because they can be changed in a way that is hard to track.
      if (typeName != null && isInitializedByConstructor(variableTree.initializer()) &&
        symbol.isLocalVariable() &&
        symbol.usages().stream().noneMatch(UnusedStringBuilderCheck::isUsedInAssignment) &&
        symbol.usages().stream().noneMatch(UnusedStringBuilderCheck::isConsumed)) {
        reportIssue(variableTree.simpleName(), "Consume or remove this unused %s".formatted(typeName));
      }
    }
  }

  /**
   * Returns `"StringBuilder"` or `"StringBuffer"` if the variable is one of these types.
   * This is to both check the type and extract the name for use in the issue message.
   */
  private static @Nullable String getStringBuilderOrStringBuffer(Type type) {
    if (type.is("java.lang.StringBuilder")) {
      return "StringBuilder";
    } else if (type.is("java.lang.StringBuffer")) {
      return "StringBuffer";
    }
    return null;
  }

  /**
   * Verify that the initializer is a call to a constructor, for example `new StringBuilder()`,
   * and not a method call or missing, which happens when the variable is a parameter.
   */
  private static boolean isInitializedByConstructor(@Nullable ExpressionTree initializer) {
    return initializer instanceof NewClassTree;
  }

  /**
   * True if the argument is the LHS of an assignment.
   *
   * Ideally, we should also check the RHS, to account for variables escaping the analysis.
   */
  private static boolean isUsedInAssignment(@Nullable Tree tree) {
    return Optional.ofNullable(tree)
      .map(Tree::parent)
      .filter(AssignmentExpressionTree.class::isInstance)
      .isPresent();
  }

  /**
   * True if a tree, representing a variable, is consumed by calling `toString()`, returning it,
   * passing it as an argument, etc.
   */
  private static boolean isConsumed(Tree tree) {
    Tree parent = tree.parent();
    if (parent instanceof MemberSelectExpressionTree mset) {
      if (TERMINAL_METHODS.contains(mset.identifier().name())) {
        return true;
      } else {
        // Handle chained method calls, for example `return sb.append("text")`.
        return Optional.ofNullable(parent.parent())
          .filter(UnusedStringBuilderCheck::isConsumed)
          .isPresent();
      }
    } else if (parent instanceof ReturnStatementTree || parent instanceof ArgumentListTreeImpl
      || parent instanceof BinaryExpressionTree || parent instanceof MethodReferenceTree
      || parent instanceof InitializerListTreeImpl) {
      return true;
    }
    return false;
  }
}
