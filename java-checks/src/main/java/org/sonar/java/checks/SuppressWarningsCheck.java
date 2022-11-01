/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S1309")
public class SuppressWarningsCheck extends IssuableSubscriptionVisitor {

  private static final Pattern FORMER_REPOSITORY_PREFIX = Pattern.compile("^squid:");
  private static final String NEW_REPOSITORY_PREFIX = "java:";

  @RuleProperty(
    key = "listOfWarnings",
    description = "Comma separated list of warnings that can be suppressed (example: unchecked, cast, boxing). An empty list means that no warning can be suppressed.",
    defaultValue = "")
  public String warningsCommaSeparated = "";

  private Set<String> allowedWarnings;

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    AnnotationTree annotationTree = (AnnotationTree) tree;
    Set<String> ruleWarnings = getAllowedWarnings();

    if (isJavaLangSuppressWarnings(annotationTree)) {
      if (ruleWarnings.isEmpty()) {
        reportIssue(annotationTree.annotationType(), "Suppressing warnings is not allowed");
      } else {
        List<String> suppressedWarnings = getSuppressedWarnings(annotationTree.arguments().get(0));
        List<String> issues = suppressedWarnings.stream()
          .filter(currentWarning -> !ruleWarnings.contains(currentWarning))
          .collect(Collectors.toList());
        if (!issues.isEmpty()) {
          StringBuilder sb = new StringBuilder("Suppressing the '").append(String.join(", ", issues))
            .append("' warning").append(issues.size() > 1 ? "s" : "").append(" is not allowed");
          reportIssue(annotationTree.annotationType(), sb.toString());
        }
      }
    }
  }

  private static boolean isJavaLangSuppressWarnings(AnnotationTree tree) {
    return tree.symbolType().is("java.lang.SuppressWarnings");
  }

  private Set<String> getAllowedWarnings() {
    if (allowedWarnings != null) {
      return allowedWarnings;
    }

    allowedWarnings = Arrays.stream(warningsCommaSeparated.split(","))
      .filter(StringUtils::isNotBlank)
      .map(SuppressWarningsCheck::replaceFormerRepositoryPrefix)
      .collect(Collectors.toSet());

    return allowedWarnings;
  }

  private static List<String> getSuppressedWarnings(ExpressionTree argument) {
    List<String> result = new ArrayList<>();
    if (argument.is(Tree.Kind.STRING_LITERAL)) {
      result.add(getAnnotationArgument((LiteralTree) argument));
    } else if (argument.is(Tree.Kind.NEW_ARRAY)) {
      NewArrayTree array = (NewArrayTree) argument;
      for (ExpressionTree expressionTree : array.initializers()) {
        if (expressionTree.is(Tree.Kind.STRING_LITERAL)) {
          result.add(getAnnotationArgument((LiteralTree) expressionTree));
        }
      }
    }
    return result;
  }

  private static String getAnnotationArgument(LiteralTree argument) {
    return replaceFormerRepositoryPrefix(LiteralUtils.trimQuotes(argument.value()));
  }

  private static String replaceFormerRepositoryPrefix(String value) {
    return FORMER_REPOSITORY_PREFIX.matcher(value.trim()).replaceFirst(NEW_REPOSITORY_PREFIX);
  }

}
