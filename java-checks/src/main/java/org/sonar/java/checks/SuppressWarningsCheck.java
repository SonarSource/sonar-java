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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import java.util.List;

@Rule(
  key = "S1309",
  priority = Priority.INFO,
  tags = {})
public class SuppressWarningsCheck extends SubscriptionBaseVisitor {

  @RuleProperty(
    key = "listOfWarnings",
    defaultValue = "")
  public String warningsCommaSeparated = "";

  private List<String> warnings;

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    AnnotationTree at = (AnnotationTree) tree;
    List<String> ruleWarnings = getRuleWarnings();

    if (isSuppressWarningsNode(at)) {
      if (ruleWarnings.isEmpty()) {
        addIssue(at, "Suppressing the warnings is not allowed");
      } else {
        List<String> annotationWarnings = getAnnotationArguments(at.arguments().get(0));
        List<String> issues = Lists.newArrayList();
        for (String currentWarning : annotationWarnings) {
          if (ruleWarnings.contains(currentWarning)) {
            issues.add(new StringBuilder().append("'").append(currentWarning).append("'").toString());
          }
        }
        if (!issues.isEmpty()) {
          StringBuilder sb = new StringBuilder("Suppressing the ").append(Joiner.on(", ").join(issues))
            .append(" warning").append(issues.size() > 1 ? "s" : "").append(" is not allowed");
          addIssue(at, sb.toString());
        }
      }
    }
  }

  private boolean isSuppressWarningsNode(AnnotationTree tree) {
    return ((AbstractTypedTree) tree).getSymbolType().is("java.lang.SuppressWarnings");
  }

  private List<String> getRuleWarnings() {
    if (warnings != null) {
      return warnings;
    }

    warnings = Lists.newArrayList();
    Iterable<String> listOfWarnings = Splitter.on(",").trimResults().split(warningsCommaSeparated);
    for (String warning : listOfWarnings) {
      if (StringUtils.isNotBlank(warning)) {
        warnings.add(warning);
      }
    }

    return warnings;
  }

  private List<String> getAnnotationArguments(ExpressionTree argument) {
    List<String> result = Lists.newArrayList();
    if (argument.is(Tree.Kind.STRING_LITERAL)) {
      result.add(getValue(argument));
    } else if (argument.is(Tree.Kind.NEW_ARRAY)) {
      NewArrayTree array = (NewArrayTree) argument;
      for (ExpressionTree expressionTree : array.initializers()) {
        if (expressionTree.is(Kind.STRING_LITERAL)) {
          result.add(getValue(expressionTree));
        }
      }
    }
    return result;
  }

  private String getValue(ExpressionTree stringLiteral) {
    return LiteralUtils.trimQuotes(((LiteralTree) stringLiteral).value());
  }
}
