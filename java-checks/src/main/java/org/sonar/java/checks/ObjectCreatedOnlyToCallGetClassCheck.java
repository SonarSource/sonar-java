/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.List;

@Rule(
  key = "S2133",
  name = "Objects should not be created only to \"getClass\"",
  tags = {"performance"},
  priority = Priority.MAJOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.MEMORY_EFFICIENCY)
@SqaleConstantRemediation("5min")
public class ObjectCreatedOnlyToCallGetClassCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(
      MethodInvocationMatcher.create().typeDefinition(TypeCriteria.subtypeOf("java.lang.Object")).name("getClass"));
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
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
  private ExpressionTree getInitializer(IdentifierTree tree) {
    Symbol symbol = tree.symbol();
    if(symbol.isVariableSymbol()) {
      VariableTree declaration = ((Symbol.VariableSymbol) symbol).declaration();
      if(declaration != null) {
        return declaration.initializer();
      }
    }
    return null;
  }

  private boolean variableUsedOnlyToGetClass(IdentifierTree tree) {
    if ("this".equals(tree.name()) || "super".equals(tree.name())) {
      return false;
    }
    Symbol symbol = tree.symbol();
    return symbol.usages().size() == 1 && hasBeenInitialized(tree);
  }

  private boolean hasBeenInitialized(IdentifierTree tree) {
    ExpressionTree initializer = getInitializer(tree);
    return initializer != null && initializer.is(Kind.NEW_CLASS);
  }

  private void reportIssue(@Nullable ExpressionTree expressionTree) {
    if(expressionTree != null) {
      addIssue(expressionTree, "Remove this object instantiation and use \"" + getTypeName(expressionTree) + ".class\" instead.");
    }
  }

  private String getTypeName(ExpressionTree tree) {
    Type type = tree.symbolType();
    String name = getTypeName(type);
    if (name.isEmpty()) {
      name = getAnonymousClassTypeName(type.symbol());
    }
    return name;
  }

  private String getAnonymousClassTypeName(Symbol.TypeSymbol symbol) {
    String name = "";
    if (symbol.interfaces().isEmpty()) {
      name = getTypeName(symbol.superClass());
    } else {
      name = getTypeName(symbol.interfaces().get(0));
    }
    return name;
  }

  private String getTypeName(Type type) {
    return type.symbol().name();
  }

}
