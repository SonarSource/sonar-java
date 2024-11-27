/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.sonar.check.Rule;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodReferenceTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S1170")
public class ConstantsShouldBeStaticFinalCheck extends IssuableSubscriptionVisitor {

  private int nestedClassesLevel;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.CLASS);
  }

  @Override
  public void setContext(JavaFileScannerContext context) {
    nestedClassesLevel = 0;
    super.setContext(context);
  }

  @Override
  public void visitNode(Tree tree) {
    nestedClassesLevel++;
    for (Tree member : ((ClassTree) tree).members()) {
      if (member.is(Tree.Kind.VARIABLE)) {
        VariableTree variableTree = (VariableTree) member;
        if (staticNonFinal(variableTree) && hasConstantInitializer(variableTree) && !isObjectInInnerClass(variableTree)) {
          reportIssue(variableTree.simpleName(), "Make this final field static too.");
        }
      }
    }
  }

  private boolean isObjectInInnerClass(VariableTree variableTree) {
    if (nestedClassesLevel > 1) {
      ExpressionTree initializer = variableTree.initializer();
      return !((variableTree.type().is(Tree.Kind.PRIMITIVE_TYPE) || variableTree.symbol().type().is("java.lang.String"))
        && initializer != null && initializer.asConstant().isPresent());
    }
    return false;
  }

  private static boolean staticNonFinal(VariableTree variableTree) {
    return isFinal(variableTree) && !isStatic(variableTree);
  }

  @Override
  public void leaveNode(Tree tree) {
    nestedClassesLevel--;
  }

  private static boolean hasConstantInitializer(VariableTree variableTree) {
    ExpressionTree init = variableTree.initializer();
    if (init == null) {
      return false;
    }

    var deparenthesized = ExpressionUtils.skipParentheses(init);

    if (deparenthesized instanceof MethodReferenceTree methodRef && isInstanceIdentifier(methodRef.expression())) {
      return false;
    }
    return !containsChildMatchingPredicate((JavaTree) deparenthesized,
      (ConstantsShouldBeStaticFinalCheck::isNonStaticOrFinal));
  }

  private static boolean isNonStaticOrFinal(Tree tree) {
    return switch (tree.kind()) {
      case METHOD_INVOCATION, NEW_CLASS, NEW_ARRAY, ARRAY_ACCESS_EXPRESSION -> true;
      case IDENTIFIER -> {
        String name = ((IdentifierTree) tree).name();
        if ("super".equals(name) || "this".equals(name)) {
          yield true;
        } else {
          var symbol = ((IdentifierTree) tree).symbol();
          yield symbol.isVariableSymbol() && !(symbol.isStatic() && symbol.isFinal());
        }
      }
      default -> false;
    };
  }

  private static boolean isInstanceIdentifier(Tree expression) {
    return expression.is(Tree.Kind.IDENTIFIER) && !((IdentifierTree) expression).symbol().isStatic();
  }

  private static boolean containsChildMatchingPredicate(JavaTree tree, Predicate<Tree> predicate) {
    if (predicate.test(tree)) {
      return true;
    }
    if (!tree.isLeaf()) {
      for (Tree javaTree : tree.getChildren()) {
        if (javaTree != null && containsChildMatchingPredicate((JavaTree) javaTree, predicate)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isFinal(VariableTree variableTree) {
    return ModifiersUtils.hasModifier(variableTree.modifiers(), Modifier.FINAL);
  }

  private static boolean isStatic(VariableTree variableTree) {
    return ModifiersUtils.hasModifier(variableTree.modifiers(), Modifier.STATIC);
  }
}
