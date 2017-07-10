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

import org.sonar.check.Rule;
import org.sonar.java.JavaVersionAwareVisitor;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.resolve.JavaType;
import org.sonar.java.resolve.MethodJavaType;
import org.sonar.java.resolve.ParametrizedTypeJavaType;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaVersion;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

@Rule(key = "S3552")
public class HashMapComparableKeyCheck extends BaseTreeVisitor implements JavaFileScanner, JavaVersionAwareVisitor {

  private static final String JAVA_UTIL_HASH_MAP = "java.util.HashMap";
  private JavaFileScannerContext context;

  @Override
  public boolean isCompatibleWithJavaVersion(JavaVersion version) {
    return version.isJava8Compatible();
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitVariable(VariableTree tree) {
    if (checkNewMap(tree.initializer(), tree.type().symbolType())) {
      return;
    }
    super.visitVariable(tree);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    if (checkNewMap(tree.expression(), tree.variable().symbolType())) {
      return;
    }
    super.visitAssignmentExpression(tree);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    checkNewMap(tree, tree.identifier().symbolType());
    super.visitNewClass(tree);
  }

  private boolean checkNewMap(@Nullable ExpressionTree expr, Type parentExpressionType) {
    if (expr == null) {
      return false;
    }
    ExpressionTree expression = ExpressionUtils.skipParentheses(expr);
    if (!expression.is(Tree.Kind.NEW_CLASS)) {
      return false;
    }
    NewClassTree newClassTree = (NewClassTree) expression;
    Type newClassType = newClassTree.symbolType();
    if (newClassType.isSubtypeOf(JAVA_UTIL_HASH_MAP)) {
      String name = nonComparableKeyTypeName(newClassType);
      if (name == null) {
        name = nonComparableKeyTypeName(parentExpressionType);
      }
      if (name != null) {
        reportIssue(newClassTree, name);
        return true;
      }
    }
    return false;
  }

  @CheckForNull
  private static String nonComparableKeyTypeName(Type symbolType) {
    Type type = symbolType;
    if (type instanceof MethodJavaType) {
      type = ((MethodJavaType) type).resultType();
    }
    if (type instanceof ParametrizedTypeJavaType) {
      ParametrizedTypeJavaType parametrizedTypeJavaType = (ParametrizedTypeJavaType) type;
      JavaType keyType = parametrizedTypeJavaType.substitution(parametrizedTypeJavaType.typeParameters().get(0));
      if (!keyType.isTagged(JavaType.TYPEVAR) && !keyType.isSubtypeOf("java.lang.Comparable")) {
        return keyType.name();
      }
    }
    return null;
  }

  private void reportIssue(NewClassTree newClassTree, String typeName) {
    context.reportIssue(this, reportTree(newClassTree),
      String.format("Implement \"Comparable\" in \"%s\" or change the key type of this map.%s",
        typeName,
        context.getJavaVersion().java8CompatibilityMessage()));
  }

  private static Tree reportTree(NewClassTree newClassTree) {
    TypeTree typeTree = newClassTree.identifier();
    if (!typeTree.is(Tree.Kind.PARAMETERIZED_TYPE)) {
      return typeTree;
    }
    TypeArguments typeArguments = ((ParameterizedTypeTree) typeTree).typeArguments();
    if (typeArguments.isEmpty()) {
      return typeTree;
    }
    return typeArguments.get(0);
  }

}
