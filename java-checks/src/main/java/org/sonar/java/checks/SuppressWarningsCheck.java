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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

@Rule(
  key = "S1309",
  name = "The @SuppressWarnings annotation should not be used",
  tags = {},
  priority = Priority.INFO)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.UNDERSTANDABILITY)
@SqaleConstantRemediation("10min")
public class SuppressWarningsCheck extends SubscriptionBaseVisitor {

  @RuleProperty(
    key = "listOfWarnings",
    description = "Comma separated list of warnings that can be suppressed (example: unchecked, cast, boxing). An empty list means that no warning can be suppressed.",
    defaultValue = "")
  public String warningsCommaSeparated = "";

  private List<String> allowedWarnings;

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    AnnotationTree annotationTree = (AnnotationTree) tree;
    List<String> ruleWarnings = getAllowedWarnings();

    if (isJavaLangSuppressWarnings(annotationTree)) {
      if (ruleWarnings.isEmpty()) {
        addIssue(annotationTree, "Suppressing warnings is not allowed");
      } else {
        List<String> suppressedWarnings = getSuppressedWarnings(annotationTree.arguments().get(0));
        List<String> issues = Lists.newArrayList();
        for (String currentWarning : suppressedWarnings) {
          if (!ruleWarnings.contains(currentWarning)) {
            issues.add(currentWarning);
          }
        }
        if (!issues.isEmpty()) {
          StringBuilder sb = new StringBuilder("Suppressing the '").append(Joiner.on(", ").join(issues))
            .append("' warning").append(issues.size() > 1 ? "s" : "").append(" is not allowed");
          addIssue(annotationTree, sb.toString());
        }
      }
    }
  }

  private static boolean isJavaLangSuppressWarnings(AnnotationTree tree) {
    return tree.symbolType().is("java.lang.SuppressWarnings");
  }

  private List<String> getAllowedWarnings() {
    if (allowedWarnings != null) {
      return allowedWarnings;
    }

    allowedWarnings = Lists.newArrayList();
    Iterable<String> listOfWarnings = Splitter.on(",").trimResults().split(warningsCommaSeparated);
    for (String warning : listOfWarnings) {
      if (StringUtils.isNotBlank(warning)) {
        allowedWarnings.add(warning);
      }
    }

    return allowedWarnings;
  }

  private static List<String> getSuppressedWarnings(ExpressionTree argument) {
    List<String> result = Lists.newArrayList();
    if (argument.is(Tree.Kind.STRING_LITERAL)) {
      result.add(LiteralUtils.trimQuotes(((LiteralTree) argument).value()));
    } else if (argument.is(Tree.Kind.NEW_ARRAY)) {
      NewArrayTree array = (NewArrayTree) argument;
      for (ExpressionTree expressionTree : array.initializers()) {
        if (expressionTree.is(Kind.STRING_LITERAL)) {
          result.add(LiteralUtils.trimQuotes(((LiteralTree) expressionTree).value()));
        }
      }
    }
    return result;
  }

}
