/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
import org.sonar.java.checks.methods.MethodMatcher;
import org.sonar.java.checks.methods.TypeCriteria;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.List;

@Rule(
  key = "S2109",
  name = "Reflection should not be used to check non-runtime annotations",
  tags = {"bug"},
  priority = Priority.BLOCKER)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("15min")
public class ReflectionOnNonRuntimeAnnotationCheck extends AbstractMethodDetection {

  @Override
  protected List<MethodMatcher> getMethodInvocationMatchers() {
    return ImmutableList.of(MethodMatcher.create()
      .typeDefinition(TypeCriteria.subtypeOf("java.lang.reflect.AnnotatedElement"))
      .name("isAnnotationPresent").withNoParameterConstraint());
  }

  @Override
  protected void onMethodInvocationFound(MethodInvocationTree mit) {
    ExpressionTree expressionTree = mit.arguments().get(0);
    // For now ignore everything that is not a .class expression
    if (expressionTree.is(Tree.Kind.MEMBER_SELECT)) {
      Type symbolType = ((MemberSelectExpressionTree) expressionTree).expression().symbolType();
      if (!symbolType.isUnknown() && isNotRuntimeAnnotation(symbolType)) {
        addIssue(mit, "\"@" + symbolType.name() + "\" is not available at runtime and cannot be seen with reflection.");
      }
    }
  }

  private static boolean isNotRuntimeAnnotation(Type symbolType) {
    List<SymbolMetadata.AnnotationValue> valuesFor = symbolType.symbol().metadata().valuesForAnnotation("java.lang.annotation.Retention");
    // default policy is CLASS
    if (valuesFor == null) {
      return true;
    }
    String retentionValue = getRetentionValue(valuesFor.get(0).value());
    return !"RUNTIME".equals(retentionValue);
  }

  @Nullable
  private static String getRetentionValue(Object value) {
    String retentionValue = null;
    if (value instanceof Tree) {
      Tree tree = (Tree) value;
      if (tree.is(Tree.Kind.MEMBER_SELECT)) {
        retentionValue = ((MemberSelectExpressionTree) tree).identifier().name();
      } else if (tree.is(Tree.Kind.IDENTIFIER)) {
        retentionValue = ((IdentifierTree) tree).name();
      }
    } else if (value instanceof Symbol.VariableSymbol) {
      retentionValue = ((Symbol.VariableSymbol) value).name();
    }
    return retentionValue;
  }
}
