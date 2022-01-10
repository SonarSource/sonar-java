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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.model.JUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Symbol.TypeSymbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3254")
public class AnnotationDefaultArgumentCheck extends IssuableSubscriptionVisitor {
  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.ANNOTATION);
  }

  @Override
  public void visitNode(Tree tree) {
    AnnotationTree annotationTree = (AnnotationTree) tree;
    TypeSymbol annotationSymbol = annotationTree.symbolType().symbol();
    if (annotationSymbol.isUnknown()) {
      return;
    }

    Map<String, Object> defaultValueByName = annotationSymbol.memberSymbols().stream()
      .filter(Symbol::isMethodSymbol)
      .map(Symbol.MethodSymbol.class::cast)
      .filter(s -> JUtils.defaultValue(s) != null)
      .collect(Collectors.toMap(Symbol::name, JUtils::defaultValue));

    for (ExpressionTree argument : annotationTree.arguments()) {
      ExpressionTree valueSet = argument;
      // Single element annotation : JLS8 9.7.3 : one param must be named value.
      String paramName = "value";
      if (argument.is(Tree.Kind.ASSIGNMENT)) {
        AssignmentExpressionTree assignmentTree = (AssignmentExpressionTree) argument;
        IdentifierTree nameTree = (IdentifierTree) assignmentTree.variable();
        paramName = nameTree.name();
        valueSet = assignmentTree.expression();
      }
      if (setValueIsSameAsDefaultValue(defaultValueByName.get(paramName), valueSet)) {
        reportIssue(argument, String.format("Remove this default value assigned to parameter \"%s\".", paramName));
      }
    }
  }

  private static boolean setValueIsSameAsDefaultValue(@Nullable Object defaultValue, ExpressionTree valueSet) {
    Optional<String> valueAsStringConstant = valueSet.asConstant(String.class);
    if (valueAsStringConstant.isPresent()) {
      return valueAsStringConstant.get().equals(defaultValue);
    }
    Optional<Integer> valueAsIntConstant = valueSet.asConstant(Integer.class);
    return valueAsIntConstant.map(integer -> integer.equals(defaultValue)).orElse(false);
  }

}
