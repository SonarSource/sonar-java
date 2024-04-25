/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6816")
public class NullableInjectedFieldsHaveDefaultValueCheck extends IssuableSubscriptionVisitor {

  private static final String VALUE_ANNOTATION = "org.springframework.beans.factory.annotation.Value";

  private static final String MESSAGE_FOR_FIELDS = "Provide a default null value for this field.";
  private static final String MESSAGE_FOR_PARAMETERS = "Provide a default null value for this parameter.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS, Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    final AnnotationTree methodLevelValueAnnotation;
    final boolean isClass = tree.is(Tree.Kind.CLASS);
    Stream<VariableTree> variables;
    if (isClass) {
      methodLevelValueAnnotation = null;
      variables = ((ClassTree) tree).members().stream()
        .filter(member -> member.is(Tree.Kind.VARIABLE))
        .map(VariableTree.class::cast);
    } else {
      var method = ((MethodTree) tree);
      variables = method.parameters().stream();
      methodLevelValueAnnotation = extractValueAnnotationOnSetter(method);
    }
    String issueMessage = isClass ? MESSAGE_FOR_FIELDS : MESSAGE_FOR_PARAMETERS;
    variables.map(variable -> mapToAnnotationsOfInterest(variable, methodLevelValueAnnotation))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .forEach(trees ->
        QuickFixHelper.newIssue(context)
          .forRule(this)
          .onTree(trees.valueAnnotation)
          .withMessage(issueMessage)
          .withSecondaries(new JavaFileScannerContext.Location("The nullable annotation", trees.nullableAnnotation))
          .withQuickFixes(() -> computeQuickFix(trees.valueAnnotation))
          .report()
      );
  }

  private static List<JavaQuickFix> computeQuickFix(AnnotationTree annotation) {
    ExpressionTree expression = extractExpressionTree(annotation.arguments().get(0));
    // We provide at most 2 quickfixes
    List<JavaQuickFix> quickFixes = new ArrayList<>(2);
    // Compute replacement value
    String originalValue = ExpressionsHelper.getConstantValueAsString(expression).value();
    if (originalValue == null) {
      // Unlikely since we are computing a quickfix, then the value must have been resolved
      return List.of();
    }
    String currentValue = originalValue.strip();
    String replacementValue = "\"" +
      originalValue.strip().substring(0, currentValue.lastIndexOf('}'))
      + ":#{null}}"
      + "\"";
    String quickFixMessage = "Set null as default value";
    // Test if the value is defined in a constant that can be fixed as an alternative
    if (!expression.is(Tree.Kind.STRING_LITERAL)) {
      quickFixMessage = "Set null as default value locally";
      computeQuickFixOnOriginalDefinition(expression, replacementValue).ifPresent(quickFixes::add);
    }
    // Insert local replacement
    quickFixes.add(
      JavaQuickFix.newQuickFix(quickFixMessage)
        .addTextEdit(AnalyzerMessage.replaceTree(expression, replacementValue))
        .build()
    );
    return quickFixes;
  }

  private static Optional<JavaQuickFix> computeQuickFixOnOriginalDefinition(ExpressionTree expression, String replacementValue) {
    Symbol symbol;
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      symbol = ((MemberSelectExpressionTree) expression).identifier().symbol();
    } else {
      symbol = ((IdentifierTree) expression).symbol();
    }
    Tree declaration = symbol.declaration();
    if (declaration != null && declaration.is(Tree.Kind.VARIABLE)) {
      ExpressionTree assignedExpression = ((VariableTree) declaration).initializer();
      if (assignedExpression != null) {
        return Optional.of(
          JavaQuickFix.newQuickFix("Set null as default value")
            .addTextEdit(AnalyzerMessage.replaceTree(assignedExpression, replacementValue))
            .build());
      }
    }
    return Optional.empty();
  }

  /**
   * Maps a variable to an {@link AnnotationsOfInterest} if it is annotated as nullable and injected with a Spring value annotation missing a default value.
   *
   * @param variable A field or a parameter
   * @param valueAnnotationOnParent In the case of a parameter, a possible value annotation without default on the parent method
   * @return An Optional with a {@link AnnotationsOfInterest} if the member matches. Optional.empty() otherwise.
   */
  private static Optional<AnnotationsOfInterest> mapToAnnotationsOfInterest(VariableTree variable, @Nullable AnnotationTree valueAnnotationOnParent) {
    final AnnotationTree valueAnnotation;
    if (valueAnnotationOnParent == null) {
      Optional<AnnotationTree> annotationOnVariable = getValueAnnotationWithoutDefault(variable);
      if (annotationOnVariable.isEmpty()) {
        return Optional.empty();
      }
      valueAnnotation = annotationOnVariable.get();
    } else {
      valueAnnotation = valueAnnotationOnParent;
    }
    Optional<AnnotationTree> nullableAnnotation = getNullableAnnotation(variable);
    return nullableAnnotation.map(annotationTree -> new AnnotationsOfInterest(valueAnnotation, annotationTree));
  }

  @Nullable
  private static AnnotationTree extractValueAnnotationOnSetter(MethodTree method) {
    if (MethodTreeUtils.isSetterMethod(method)) {
      return method.modifiers().annotations().stream()
        .filter(annotation -> annotation.symbolType().is(VALUE_ANNOTATION) &&
          !hasDefaultValue(annotation))
        .findFirst()
        .orElse(null);
    }
    return null;
  }

  private static Optional<AnnotationTree> getNullableAnnotation(VariableTree field) {
    SymbolMetadata.NullabilityData nullabilityData = field.symbol().metadata().nullabilityData(SymbolMetadata.NullabilityTarget.FIELD);
    SymbolMetadata.AnnotationInstance instance = nullabilityData.annotation();
    if (instance == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(field.symbol().metadata().findAnnotationTree(instance));
  }

  private static Optional<AnnotationTree> getValueAnnotationWithoutDefault(VariableTree field) {
    return field.modifiers().annotations().stream()
      .filter(annotation -> annotation.symbolType().is(VALUE_ANNOTATION) && !hasDefaultValue(annotation))
      .findFirst();
  }

  /**
   * We consider that an annotation value has a default value if:
   * - it cannot be resolved using ${@link ExpressionTree#asConstant()}
   * - it does not look like a SpEL property access
   * - it looks like a SpEL property access and contains the default separator ':'
   */
  private static boolean hasDefaultValue(AnnotationTree valueAnnotation) {
    ExpressionTree expression = valueAnnotation.arguments().get(0);
    String value = extractLiteralValue(expression);
    String argument = value.strip();
    if (argument.startsWith("${") && argument.endsWith("}")) {
      return argument.contains(":");
    }
    return true;
  }

  private static String extractLiteralValue(ExpressionTree annotationArgument) {
    ExpressionTree expressionTree = extractExpressionTree(annotationArgument);
    String value = ExpressionsHelper.getConstantValueAsString(expressionTree).value();
    return value != null ? value : "";
  }

  private static ExpressionTree extractExpressionTree(ExpressionTree annotationArgument) {
    if (annotationArgument.is(Tree.Kind.ASSIGNMENT)) {
      return ((AssignmentExpressionTree) annotationArgument).expression();
    }
    return annotationArgument;
  }

  private static class AnnotationsOfInterest {
    public final AnnotationTree valueAnnotation;
    public final AnnotationTree nullableAnnotation;

    public AnnotationsOfInterest(AnnotationTree valueAnnotation, AnnotationTree nullableAnnotation) {
      this.valueAnnotation = valueAnnotation;
      this.nullableAnnotation = nullableAnnotation;
    }
  }
}
