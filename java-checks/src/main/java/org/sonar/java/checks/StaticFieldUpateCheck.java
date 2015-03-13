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
import com.google.common.collect.Lists;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.VariableSymbol;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2696",
  name = "Instance methods should not write to \"static\" fields",
  tags = {"bug", "multi-threading"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.SYNCHRONIZATION_RELIABILITY)
@SqaleConstantRemediation("20min")
public class StaticFieldUpateCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!hasSemantic()) {
      return;
    }

    ClassTree classTree = (ClassTree) tree;
    if (isNonStatic(classTree)) {
      List<MethodTree> nonStaticMethods = Lists.newArrayList();
      List<VariableSymbol> staticNonFinalFields = Lists.newArrayList();

      extractMembers(classTree, staticNonFinalFields, nonStaticMethods);
      checkMethods(staticNonFinalFields, nonStaticMethods);
    }
  }

  private boolean isNonStatic(ClassTree classTree) {
    return !classTree.modifiers().modifiers().contains(Modifier.STATIC);
  }

  private void extractMembers(ClassTree classTree, List<VariableSymbol> staticNonFinalFields, List<MethodTree> nonStaticMethods) {
    extractMembers(classTree.members(), staticNonFinalFields, nonStaticMethods, true);
  }

  private void extractMembers(List<Tree> members, List<VariableSymbol> staticNonFinalFields, List<MethodTree> nonStaticMethods, boolean lookForStaticFields) {
    for (Tree member : members) {
      if (member.is(Kind.VARIABLE) && lookForStaticFields) {
        VariableTreeImpl variable = (VariableTreeImpl) member;
        if (isStaticNonFinalField(variable.modifiers().modifiers())) {
          staticNonFinalFields.add(variable.getSymbol());
        }
      } else if (member.is(Kind.METHOD, Kind.CONSTRUCTOR)) {
        MethodTree method = (MethodTree) member;
        if (isNonStaticMethod(method)) {
          nonStaticMethods.add(method);
        }
      } else if (member.is(Kind.CLASS)) {
        // don't look for static fields of the inner classes, as inner class will be explored later
        extractMembers(((ClassTree) member).members(), staticNonFinalFields, nonStaticMethods, false);
      }
    }
  }

  private void checkMethods(List<VariableSymbol> staticNonFinalFields, List<MethodTree> nonStaticMethods) {
    for (MethodTree method : nonStaticMethods) {
      BlockTree block = method.block();
      if (block != null) {
        block.accept(new AssignmentVisitor(staticNonFinalFields));
      }
    }
  }

  private boolean isNonStaticMethod(MethodTree method) {
    return !method.modifiers().modifiers().contains(Modifier.STATIC);
  }

  private boolean isStaticNonFinalField(List<Modifier> modifiers) {
    return modifiers.contains(Modifier.STATIC) && !modifiers.contains(Modifier.FINAL);
  }

  private class AssignmentVisitor extends BaseTreeVisitor {
    private final List<VariableSymbol> targets;

    public AssignmentVisitor(List<VariableSymbol> staticNonFinalFields) {
      this.targets = staticNonFinalFields;
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      checkFieldAssignement(tree.variable());
    }

    @Override
    public void visitUnaryExpression(UnaryExpressionTree tree) {
      if (tree.is(Kind.POSTFIX_DECREMENT, Kind.POSTFIX_INCREMENT, Kind.PREFIX_DECREMENT, Kind.PREFIX_INCREMENT)) {
        checkFieldAssignement(tree.expression());
      }
    }

    private void checkFieldAssignement(ExpressionTree expression) {
      if (expression.is(Kind.IDENTIFIER)) {
        Symbol symbol = getSemanticModel().getReference((IdentifierTree) expression);
        if (targets.contains(symbol)) {
          addIssue(expression, "Make the enclosing method \"static\" or remove this set.");
        }
      } else if (expression.is(Kind.MEMBER_SELECT)) {
        checkFieldAssignement(((MemberSelectExpressionTree) expression).identifier());
      } else if (expression.is(Kind.ARRAY_ACCESS_EXPRESSION)) {
        checkFieldAssignement(((ArrayAccessExpressionTree) expression).expression());
      }
    }
  }
}
