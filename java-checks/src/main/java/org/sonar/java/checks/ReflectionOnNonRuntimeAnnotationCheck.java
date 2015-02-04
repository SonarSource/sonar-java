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
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.checks.methods.MethodInvocationMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.resolve.AnnotationValue;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2109",
  name = "Reflection should not be used to check non-runtime annotations",
  tags = {"bug"},
  priority = Priority.BLOCKER)
@BelongsToProfile(title = "Sonar way", priority = Priority.BLOCKER)
@SqaleSubCharacteristic(value = RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation(value = "15min")
public class ReflectionOnNonRuntimeAnnotationCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodInvocationMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(MethodInvocationMatcher.create()
        .typeDefinition(TypeCriteria.subtypeOf("java.lang.reflect.AnnotatedElement"))
        .name("isAnnotationPresent").withNoParameterConstraint());
  }

  @Override
  protected void onMethodFound(MethodInvocationTree mit) {
    ExpressionTree expressionTree = mit.arguments().get(0);
    //For now ignore everything that is not a .class expression
    if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      Type symbolType = ((AbstractTypedTree) ((MemberSelectExpressionTree) expressionTree).expression()).getSymbolType();
      if (isNotRuntimeAnnotation(symbolType)) {
        addIssue(mit, "\"@" + symbolType.getSymbol().getName() + "\" is not available at runtime and cannot be seen with reflection.");
      }
    }
  }

  private boolean isNotRuntimeAnnotation(Type symbolType) {
    List<AnnotationValue> valuesFor = symbolType.getSymbol().metadata().getValuesFor("java.lang.annotation.Retention");
    //default policy is CLASS
    return valuesFor == null || !"RUNTIME".equals(((Symbol.VariableSymbol) valuesFor.get(0).value()).getName());
  }
}
