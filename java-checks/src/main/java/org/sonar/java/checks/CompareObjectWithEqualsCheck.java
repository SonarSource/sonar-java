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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = CompareObjectWithEqualsCheck.RULE_KEY,
  name = "Objects should be compared with \"equals()\"",
  tags = {"cert", "cwe"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("2min")
public class CompareObjectWithEqualsCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String RULE_KEY = "S1698";
  private final RuleKey ruleKey = RuleKey.of(CheckList.REPOSITORY_KEY, RULE_KEY);

  private static final String DEFAULT_EXCLUSIONS = "java.lang.Class";
  private static final String SEPARATOR = ";";

  private JavaFileScannerContext context;

  @RuleProperty(
      key = "exclusions",
      defaultValue = "" + DEFAULT_EXCLUSIONS)
  private String exclusions = DEFAULT_EXCLUSIONS;
  
  private Set<String> exclusionSet;
  
  private synchronized Set<String> getExclusionSet() {
	  if (exclusionSet == null) {
		  exclusionSet = new HashSet<String>();
		  exclusionSet.addAll(Arrays.asList(exclusions.split(SEPARATOR)));
	  }
	  
	  return exclusionSet;
  }
  
  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    if (context.getSemanticModel() != null) {
      scan(context.getTree());
    }
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (!isEquals(tree)) {
      super.visitMethod(tree);
    }
  }

  private boolean isEquals(MethodTree tree) {
    return ((MethodTreeImpl) tree).isEqualsMethod();
  }

  @Override
  public void visitBinaryExpression(BinaryExpressionTree tree) {
    super.visitBinaryExpression(tree);
    if (tree.is(Tree.Kind.EQUAL_TO, Tree.Kind.NOT_EQUAL_TO)) {
      Type leftOpType = tree.leftOperand().symbolType();
      Type rightOpType = tree.rightOperand().symbolType();
      if (!isExcluded(leftOpType, rightOpType) && hasObjectOperand(leftOpType, rightOpType)) {
        context.addIssue(tree, ruleKey, "Change this comparison to use the equals method.");
      }
    }
  }

  private boolean hasObjectOperand(Type leftOpType, Type rightOpType) {
    return isObject(leftOpType) || isObject(rightOpType);
  }

  private boolean isExcluded(Type leftOpType, Type rightOpType) {
    return isNullComparison(leftOpType, rightOpType) || isNumericalComparison(leftOpType, rightOpType) || isExcludedComparison(leftOpType, rightOpType);
  }

  private boolean isObject(Type operandType) {
    return operandType.erasure().isClass() && !operandType.symbol().isEnum();
  }

  private boolean isNullComparison(Type leftOpType, Type rightOpType) {
    return isBot(leftOpType) || isBot(rightOpType);
  }

  private boolean isNumericalComparison(Type leftOperandType, Type rightOperandType) {
    return leftOperandType.isNumerical() || rightOperandType.isNumerical();
  }

  private boolean isExcludedComparison(Type leftOpType, Type rightOpType) {
    return getExclusionSet().contains(leftOpType.toString()) || getExclusionSet().contains(rightOpType.toString());
  }

  private boolean isBot(Type type) {
    return ((JavaType) type).isTagged(JavaType.BOT);

  }
}
