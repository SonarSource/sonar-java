/*
 * SonarQube Java
 * Copyright (C) 2012-2018 SonarSource SA
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
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S4973")
public class CompareStringsBoxedTypesWithEqualsCheck extends CompareWithEqualsVisitor {

  private static final String JAVA_LANG_STRING = "java.lang.String";

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    super.visitBinaryExpression(tree);
    if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      Type leftOpType = tree.leftOperand().symbolType();
      Type rightOpType = tree.rightOperand().symbolType();
      if (!isNullComparison(leftOpType, rightOpType) && (isString(leftOpType, rightOpType) || isBoxedType(leftOpType, rightOpType))) {
        reportIssue(this, tree.operatorToken());
      }
    }
  }

  private static boolean isString(Type leftOpType, Type rightOpType) {
    return leftOpType.is(JAVA_LANG_STRING) && rightOpType.is(JAVA_LANG_STRING);
  }

  private static boolean isBoxedType(Type leftOpType, Type rightOpType) {
    return ((JavaType)leftOpType).isPrimitiveWrapper() && ((JavaType)rightOpType).isPrimitiveWrapper();
  }

}
