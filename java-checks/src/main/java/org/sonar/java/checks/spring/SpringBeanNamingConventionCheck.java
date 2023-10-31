/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.spring;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6830")
public class SpringBeanNamingConventionCheck extends IssuableSubscriptionVisitor {

  private final List<String> annotationsToCheck = List.of(
    "org.springframework.beans.factory.annotation.Qualifier",
    "org.springframework.context.annotation.Bean",
    "org.springframework.context.annotation.Configuration",
    "org.springframework.stereotype.Controller",
    "org.springframework.stereotype.Component",
    "org.springframework.stereotype.Repository",
    "org.springframework.stereotype.Service",
    "org.springframework.web.bind.annotation.RestController"
  );

  Pattern namingConvention = Pattern.compile("^[a-z][a-zA-Z0-9]*$");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    var annotation = (AnnotationTree) tree;
    annotationsToCheck.stream().filter(a -> annotation.symbolType().is(a)).findFirst()
      .map(a -> getNoncompliantNameArgument(annotation))
      .ifPresent(n -> reportIssue(n, "Rename this bean to match the regular expression '" + namingConvention.pattern() + "'."));
  }

  @CheckForNull
  private ExpressionTree getNoncompliantNameArgument(AnnotationTree annotation) {
    return annotation.arguments().stream()
      .map(arg -> {
        ExpressionTree nameValue = null;
        if (arg.is(Tree.Kind.STRING_LITERAL)) {
          nameValue = arg;
        } else {
          var assignment = (AssignmentExpressionTree) arg;
          var argName = ((IdentifierTree) assignment.variable()).name();
          var argValue = assignment.expression();
          if (argName.equals("name") || argName.equals("value")) {
            nameValue = argValue;
          }
        }

        if (nameValue != null) {
          var resolvedNameValue = ExpressionsHelper.getConstantValueAsString(nameValue).value();
          if (resolvedNameValue != null && !namingConvention.matcher(resolvedNameValue).matches()) {
            return arg;
          }
        }
        return null;

      }).filter(Objects::nonNull).findFirst().orElse(null);
  }
}
