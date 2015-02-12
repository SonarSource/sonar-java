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
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.Symbol;
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
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.FAULT_TOLERANCE)
@SqaleConstantRemediation("2min")
public class ObjectCreatedOnlyToCallGetClassCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (hasSemantic() && mit.methodSelect().is(Kind.MEMBER_SELECT)) {
      ExpressionTree expressionTree = ((MemberSelectExpressionTree) mit.methodSelect()).expression();
      boolean calledFromNewObject = expressionTree.is(Kind.NEW_CLASS);
      boolean calledFromVariable = expressionTree.is(Kind.IDENTIFIER);
      if (isGetClassInvocation(mit) && (calledFromNewObject || calledFromVariable)) {
        Tree issueTarget = null;
        if (calledFromVariable && isInstantiationOnlyOtherOperation(expressionTree)) {
          issueTarget = getInstantiationTree(expressionTree);
        } else if (calledFromNewObject) {
          issueTarget = expressionTree;
        }
        if (issueTarget != null) {
          addIssue(issueTarget, "Remove this object instantiation and use \"" + getTypeName(issueTarget) + ".class\" instead.");
        }
      }
    }
  }

  private String getTypeName(Tree tree) {
    Type type = ((AbstractTypedTree) tree).getSymbolType();
    TypeSymbol symbol = type.getSymbol();
    String name = symbol.getName();
    if (name.isEmpty()) {
      name = getAnonymousClassTypeName(symbol);
    }
    return name;
  }

  private String getAnonymousClassTypeName(TypeSymbol symbol) {
    String name = "";
    if (!symbol.getSuperclass().is("java.lang.Object")) {
      // abstract method
      name = symbol.getSuperclass().getSymbol().getName();
    } else {
      // interface
      name = symbol.getInterfaces().get(0).getSymbol().getName();
    }
    return name;
  }

  private boolean isInstantiationOnlyOtherOperation(ExpressionTree tree) {
    Symbol symbol = getSemanticModel().getReference((IdentifierTree) tree);
    String symbolName = symbol.getName();
    if ("this".equals(symbolName) || "super".equals(symbolName)) {
      return false;
    }
    VariableTree source = (VariableTree) getSemanticModel().getTree(symbol);
    return getSemanticModel().getUsages(symbol).size() == 1 && source.initializer() != null;
  }

  private Tree getInstantiationTree(ExpressionTree tree) {
    Symbol symbol = getSemanticModel().getReference((IdentifierTree) tree);
    return ((VariableTree) getSemanticModel().getTree(symbol)).initializer();
  }

  private boolean isGetClassInvocation(MethodInvocationTree tree) {
    return tree.arguments().isEmpty() && "getClass".equals(((MemberSelectExpressionTree) tree.methodSelect()).identifier().name());
  }
}
