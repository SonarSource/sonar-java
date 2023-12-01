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
package org.sonar.java.checks;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;


@Rule(key = "S6856")
public class PathVariableAnnotationShouldBePresentIfPathVariableIsUsedCheck extends IssuableSubscriptionVisitor {
  private static final List<String> MAPPING_ANNOTATION_PATH_ARGUMENTS = List.of("path", "value");
  private static final String PATH_VARIABLE_ANNOTATION = "org.springframework.web.bind.annotation.PathVariable";
  private static final List<String> MAPPING_ANNOTATIONS = List.of(
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.DeleteMapping");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;

    method.modifiers()
      .annotations()
      .stream()
      .filter(annotation -> MAPPING_ANNOTATIONS.stream().anyMatch(name -> annotation.symbolType().is(name)))
      .forEach(annotation -> reportIssueOnParameters(annotation, method));
  }

  private void reportIssueOnParameters(AnnotationTree annotation, MethodTree method) {
    boolean containsMap = method.parameters().stream()
      .anyMatch(parameter -> {
        Type type = parameter.type().symbolType();
        boolean stringToString = type.typeArguments().stream().allMatch(typeArgument -> typeArgument.is("java.lang.String"));
        return type.isSubtypeOf("java.util.Map") && stringToString;
      });

    if (containsMap) {
      return;
    }

    Set<String> pathVariablesNames = method.parameters().stream()
      .map(variable -> pathVariableName(variable))
      .flatMap(Optional::stream)
      .collect(Collectors.toSet());

    extractPathArgumentFromMappingAnnotations(annotation)
      .map(path -> extractPathVariables(path))
      .map(pathVariables -> {
        pathVariables.removeAll(pathVariablesNames);
        return pathVariables;
      })
      .filter(pathVariables -> !pathVariables.isEmpty())
      .forEach(pathVariables -> reportIssue(
        annotation.arguments(),
        "Bind path variable \"" + String.join("\", \"", pathVariables) + "\" to a method parameter."));
  }

  private static Set<String> extractPathVariables(String path) {
    return List.of(path.split("/"))
      .stream()
      .filter(part -> part.startsWith("{") && part.endsWith("}"))
      .map(part -> cropFirstAndLast(part))
      .map(part -> part.split(":")[0])
      .collect(Collectors.toSet());
  }

  private static String cropFirstAndLast(String str) {
    return str.substring(1, str.length() - 1);
  }

  private static Optional<String> pathVariableName(VariableTree parameter) {
    return getAnnotation(parameter.modifiers(), PATH_VARIABLE_ANNOTATION)
      .flatMap(annotation -> getArgumentNamed(annotation, "name", expression -> extractLiteral(expression))
        .or(() -> getArgumentNamed(annotation, "value", expression -> extractLiteral(expression)))
        .or(() -> getDefaultArgument(annotation, expression -> extractLiteral(expression)))
        .or(() -> Optional.of(parameter.simpleName().name())));
  }

  private static Optional<AnnotationTree> getAnnotation(ModifiersTree modifiers, String annotationName) {
    return modifiers.annotations()
      .stream()
      .filter(annotation -> annotation.symbolType().is(annotationName))
      .findAny();
  }

  private static Stream<String> extractPathArgumentFromMappingAnnotations(AnnotationTree annotation){
    Optional<Stream<String>> defaultPath = getDefaultArgument(annotation, expression -> extractLiteralOrArray(expression));

    Stream<String> assignedPaths = MAPPING_ANNOTATION_PATH_ARGUMENTS.stream()
      .map(name -> getArgumentNamed(annotation, name, expression -> extractLiteralOrArray(expression)))
      .flatMap(Optional::stream)
      .flatMap(Function.identity());

    return Stream.concat(defaultPath.orElse(Stream.of()), assignedPaths);
  }

  private static <A> Optional<A> getDefaultArgument(AnnotationTree annotation, Function<ExpressionTree, A> readValue) {
    return annotation.arguments().stream()
      .filter(argument -> argument.is(Tree.Kind.STRING_LITERAL) || argument.is(Tree.Kind.ARRAY_TYPE))
      .findFirst()
      .map(readValue);
  }

  private static <A> Optional<A> getArgumentNamed(AnnotationTree annotation, String name, Function<ExpressionTree, A> readValue) {
    return annotation.arguments().stream()
      .filter(argument -> argument.is(Tree.Kind.ASSIGNMENT))
      .map(AssignmentExpressionTree.class::cast)
      .filter(assignment -> ((IdentifierTree) assignment.variable()).name().equals(name))
      .findFirst()
      .map(assignment -> readValue.apply(assignment.expression()));
  }

  private static String extractLiteral(ExpressionTree expression) {
    LiteralTree literal = (LiteralTree) expression;
    return LiteralUtils.trimQuotes((literal).value());
  }

  private static Stream<String> extractLiteralOrArray(ExpressionTree expression) {
    if (expression.is(Tree.Kind.STRING_LITERAL)) {
      return Stream.of(extractLiteral(expression));
    }

    NewArrayTree array = (NewArrayTree) expression;
    return array.initializers().stream().map(lit -> extractLiteral(lit));
  }

}
