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
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.java.resolve.Symbol.TypeSymbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

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

  private ExpressionTree getInitializer(IdentifierTree tree) {
    Symbol symbol = getSemanticModel().getReference(tree);
    return ((VariableTree) getSemanticModel().getTree(symbol)).initializer();
  }

  private boolean variableUsedOnlyToGetClass(IdentifierTree tree) {
    if ("this".equals(tree.name()) || "super".equals(tree.name())) {
      return false;
    }
    Symbol symbol = getSemanticModel().getReference(tree);
    return getSemanticModel().getUsages(symbol).size() == 1 && hasBeenInitialized(symbol);
  }

  private boolean hasBeenInitialized(Symbol symbol) {
    ExpressionTree initializer = ((VariableTree) getSemanticModel().getTree(symbol)).initializer();
    return initializer != null && initializer.is(Kind.NEW_CLASS);
  }

  private void reportIssue(ExpressionTree expressionTree) {
    addIssue(expressionTree, "Remove this object instantiation and use \"" + getTypeName(expressionTree) + ".class\" instead.");
  }

  private String getTypeName(Tree tree) {
    Type type = ((AbstractTypedTree) tree).getSymbolType();
    String name = getTypeName(type);
    if (name.isEmpty()) {
      name = getAnonymousClassTypeName(type.getSymbol());
    }
    return name;
  }

  private String getAnonymousClassTypeName(TypeSymbol symbol) {
    String name = "";
    if (symbol.getInterfaces().isEmpty()) {
      name = getTypeName(symbol.getSuperclass());
    } else {
      name = getTypeName(symbol.getInterfaces().get(0));
    }
    return name;
  }

  private String getTypeName(Type type) {
    return type.getSymbol().getName();
  }

}
