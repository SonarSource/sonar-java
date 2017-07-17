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
package org.sonar.java.checks.naming;

import com.google.common.collect.ImmutableList;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.RspecKey;
import org.sonar.java.checks.serialization.SerializableContract;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;

import java.util.List;
import java.util.regex.Pattern;

@Rule(key = "S00115")
@RspecKey("S115")
public class BadConstantNameCheck extends IssuableSubscriptionVisitor {

  private static final String DEFAULT_FORMAT = "^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$";
  @RuleProperty(
    key = "format",
    description = "Regular expression used to check the constant names against.",
    defaultValue = "" + DEFAULT_FORMAT)
  public String format = DEFAULT_FORMAT;

  private Pattern pattern = null;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE, Tree.Kind.METHOD);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    if (pattern == null) {
      pattern = Pattern.compile(format, Pattern.DOTALL);
    }
    super.scanFile(context);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD)) {
      checkMethodForLocalConstants((MethodTree) tree);
    } else {
      ClassTree classTree = (ClassTree) tree;
      for (Tree member : classTree.members()) {
        if (member.is(Tree.Kind.VARIABLE)) {
          checkVariableTree(classTree, (VariableTree) member);
        } else if (member.is(Tree.Kind.ENUM_CONSTANT)) {
          checkName((VariableTree) member);
        }
      }
    }
  }

  private void checkVariableTree(Tree owner, VariableTree variableTree) {
    if (!hasSemantic()) {
      return;
    }
    Type symbolType = variableTree.type().symbolType();
    if (isConstantType(symbolType) && (owner.is(Tree.Kind.INTERFACE, Tree.Kind.ANNOTATION_TYPE) || isStaticFinal(variableTree.symbol()))) {
      checkName(variableTree);
    }
  }

  private void checkMethodForLocalConstants(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    if (block != null) {
      block.accept(new VariableFromMethodVisitor(methodTree));
    }
  }

  private static boolean isConstantType(Type symbolType) {
    return symbolType.isPrimitive() || symbolType.is("java.lang.String") || ((JavaType) symbolType).isPrimitiveWrapper();
  }

  private void checkName(VariableTree variableTree) {
    if (!SerializableContract.SERIAL_VERSION_UID_FIELD.equals(variableTree.simpleName().name()) && !pattern.matcher(variableTree.simpleName().name()).matches()) {
      reportIssue(variableTree.simpleName(), "Rename this constant name to match the regular expression '" + format + "'.");
    }
  }

  private static boolean isStaticFinal(Symbol variable) {
    return (variable.isStatic() && variable.isFinal())
      || (variable.owner().isMethodSymbol() && variable.isFinal());
  }

  private class VariableFromMethodVisitor extends BaseTreeVisitor {

    private final MethodTree method;

    public VariableFromMethodVisitor(MethodTree method) {
      this.method = method;
    }

    @Override
    public void visitVariable(VariableTree tree) {
      if (hasLiteralInitializer(tree.initializer())) {
        checkVariableTree(method, tree);
      }
      super.visitVariable(tree);
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // skip lambdas
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip inner classes and anonymous classes - will be explored later, starting from the class tree
    }

    private boolean hasLiteralInitializer(@Nullable ExpressionTree initializer) {
      return initializer != null && ExpressionUtils.skipParentheses(initializer).is(
        Tree.Kind.BOOLEAN_LITERAL,
        Tree.Kind.CHAR_LITERAL,
        Tree.Kind.DOUBLE_LITERAL,
        Tree.Kind.FLOAT_LITERAL,
        Tree.Kind.INT_LITERAL,
        Tree.Kind.LONG_LITERAL,
        Tree.Kind.NULL_LITERAL,
        Tree.Kind.STRING_LITERAL);
    }
  }

}
