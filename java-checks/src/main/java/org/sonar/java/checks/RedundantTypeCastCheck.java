/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.sonar.check.Rule;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.MethodJavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import java.util.List;
import java.util.Set;

@Rule(key = "S1905")
public class RedundantTypeCastCheck extends IssuableSubscriptionVisitor {

  private Set<Tree> excluded = Sets.newHashSet();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    super.scanFile(context);
    excluded.clear();
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.TYPE_CAST);
  }

  @Override
  public void visitNode(Tree tree) {
    if(!hasSemantic()) {
      return;
    }
    TypeCastTree typeCastTree = (TypeCastTree) tree;
    Type cast = typeCastTree.type().symbolType();
    Type target = targetType(typeCastTree);
    Type expressionType = typeCastTree.expression().symbolType();
    if(isPrimitiveWrapperInConditional(expressionType, typeCastTree)) {
      // Excluded because covered by S2154
      return;
    }
    if(target != null && (isRedundantNumericalCast(cast, expressionType) || isSubtype(expressionType, target))) {
      reportIssue(typeCastTree.type(), "Remove this unnecessary cast to \"" + cast + "\".");
    }
  }

  private static boolean isPrimitiveWrapperInConditional(Type expressionType, TypeCastTree typeCastTree) {
    Tree parent = skipParentheses(typeCastTree.parent());
    return parent.is(Tree.Kind.CONDITIONAL_EXPRESSION) && (((JavaType) expressionType).isPrimitiveWrapper() || expressionType.isPrimitive());
  }

  @CheckForNull
  private static Type targetType(TypeCastTree tree) {
    Tree parent = skipParentheses(tree.parent());
    Type target = null;
    if(parent.is(Tree.Kind.RETURN_STATEMENT)) {
      Tree method = parent;
      while (!method.is(Tree.Kind.METHOD, Tree.Kind.LAMBDA_EXPRESSION)) {
        method = method.parent();
      }
      target = method.is(Tree.Kind.LAMBDA_EXPRESSION) ? null : ((MethodJavaType) ((MethodTree) method).symbol().type()).resultType();
    } else if (parent.is(Tree.Kind.VARIABLE)) {
      VariableTree variableTree = (VariableTree) parent;
      target = variableTree.symbol().type();
    } else if (parent.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree mit = (MethodInvocationTree) parent;
      if(mit.symbol().isMethodSymbol()) {
        JavaSymbol.MethodJavaSymbol sym = (JavaSymbol.MethodJavaSymbol) mit.symbol();
        int castArgIndex = mit.arguments().indexOf(tree);
        target = sym.parameterTypes().get(castArgIndex);
      }
    } else if(parent.is(Tree.Kind.MEMBER_SELECT, Tree.Kind.CONDITIONAL_EXPRESSION)) {
      target = tree.type().symbolType();
    } else if(parent instanceof ExpressionTree) {
      target = ((ExpressionTree) parent).symbolType();
    }
    return target;
  }

  private static Tree skipParentheses(Tree parent) {
    Tree skip = parent;
    while (skip.is(Tree.Kind.PARENTHESIZED_EXPRESSION)) {
      skip = skip.parent();
    }
    return skip;
  }

  private static boolean isSubtype(Type expression, Type target) {
    return expression.isSubtypeOf(target);
  }
  private static boolean isRedundantNumericalCast(Type cast, Type expressionType) {
    return cast.isNumerical() && cast.equals(expressionType);
  }

}
