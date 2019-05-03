/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.TypeCriteria;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

@Rule(key = "S2133")
public class ObjectCreatedOnlyToCallGetClassCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return Collections.singletonList(
      MethodMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.lang.Object")).name("getClass").withoutParameter());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    if (hasSemantic() && mit.methodSelect().is(Kind.MEMBER_SELECT)) {
      ExpressionTree expressionTree = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
      if (expressionTree.is(Kind.NEW_CLASS)) {
        reportIssue(expressionTree);
      } else if (expressionTree.is(Kind.IDENTIFIER) && variableUsedOnlyToGetClass((IdentifierTree) expressionTree)) {
        reportIssue(getInitializer((IdentifierTree) expressionTree));
      }
    }
  }

  @CheckForNull
  private static ExpressionTree getInitializer(IdentifierTree tree) {
    Symbol symbol = tree.symbol();
    if (symbol.isVariableSymbol()) {
      VariableTree declaration = ((Symbol.VariableSymbol) symbol).declaration();
      if (declaration != null) {
        return declaration.initializer();
      }
    }
    return null;
  }

  private static boolean variableUsedOnlyToGetClass(IdentifierTree tree) {
    if ("this".equals(tree.name()) || "super".equals(tree.name())) {
      return false;
    }
    Symbol symbol = tree.symbol();
    return symbol.usages().size() == 1 && hasBeenInitialized(tree);
  }

  private static boolean hasBeenInitialized(IdentifierTree tree) {
    ExpressionTree initializer = getInitializer(tree);
    return initializer != null && initializer.is(Kind.NEW_CLASS);
  }

  private void reportIssue(@Nullable ExpressionTree expressionTree) {
    if (expressionTree != null) {
      reportIssue(expressionTree, "Remove this object instantiation and use \"" + getTypeName(expressionTree) + ".class\" instead.");
    }
  }

  private static String getTypeName(ExpressionTree tree) {
    Type type = tree.symbolType();
    String name = getTypeName(type);
    if (name.isEmpty()) {
      name = getAnonymousClassTypeName(type.symbol());
    }
    return name;
  }

  private static String getAnonymousClassTypeName(Symbol.TypeSymbol symbol) {
    if (symbol.interfaces().isEmpty()) {
      return getTypeName(symbol.superClass());
    }
    return getTypeName(symbol.interfaces().get(0));
  }

  private static String getTypeName(Type type) {
    return type.symbol().name();
  }

}
