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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6856")
public class PathVariableAnnotationShouldBePresentIfPathVariableIsUsedCheck extends IssuableSubscriptionVisitor {
  private static final String PATH_VARIABLE_ANNOTATION = "org.springframework.web.bind.annotation.PathVariable";
  private static final Pattern EXTRACT_PATH_VARIABLE = Pattern.compile("([^:}/]*)(:.*)?\\}.*");
  private static final Predicate<String> CONTAINS_PLACEHOLDER = Pattern.compile("\\$\\{.*\\}").asPredicate();
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

    MAPPING_ANNOTATIONS
      .forEach(annotation -> reportIssueOnParameters(method, annotation));
  }

  private void reportIssueOnParameters(MethodTree method, String annotation) {
    boolean containsMap = method.parameters().stream()
      .filter(parameter -> parameter.symbol().metadata().isAnnotatedWith(PATH_VARIABLE_ANNOTATION))
      .anyMatch(parameter -> {
        Type type = parameter.type().symbolType();
        // if the type is not Map<String,String>, Spring will throw a ClassCastException exception at runtime
        boolean stringToString = type.typeArguments().stream().allMatch(typeArgument -> typeArgument.is("java.lang.String"));
        return type.isSubtypeOf("java.util.Map") && stringToString;
      });

    if (containsMap) {
      /*
       * If any of the method parameters is a map, we assume all path variables are captured
       * and there is no mismatch with path variables in the request mapping.
       */
      return;
    }

    Set<String> pathVariablesNames = method.parameters().stream()
      .map(variable -> pathVariableName(variable))
      .flatMap(Optional::stream)
      .collect(Collectors.toSet());

    extractPathArgumentFromMappingAnnotations(method, annotation)
      .map(path -> extractPathVariables(path))
      .map(pathVariables -> {
        pathVariables.removeAll(pathVariablesNames);
        return pathVariables;
      })
      .filter(pathVariables -> !pathVariables.isEmpty())
      .forEach(pathVariables -> reportIssue(
        annotation(method, annotation),
        "Bind path variable \"" + String.join("\", \"", pathVariables) + "\" to a method parameter."));
  }

  private static ExpressionTree annotation(MethodTree method, String name) {
    return method.modifiers().annotations().stream()
      .filter(annotation -> annotation.symbolType().is(name))
      .findFirst()
      // it will never be null because we are filtering on the annotation before.
      .orElse(null);
  }

  private static Set<String> extractPathVariables(String path) {
    if (CONTAINS_PLACEHOLDER.test(path)) {
      return new HashSet<>();
    }

    return Stream.of(path.split("\\{"))
      .map(EXTRACT_PATH_VARIABLE::matcher)
      .filter(Matcher::matches)
      .map(matcher -> matcher.group(1))
      .collect(Collectors.toSet());
  }

  private static Optional<String> pathVariableName(VariableTree parameter) {
    SymbolMetadata metadata = parameter.symbol().metadata();

    return Optional.ofNullable(metadata.valuesForAnnotation(PATH_VARIABLE_ANNOTATION)).flatMap(arguments -> {
      Map<String, Object> nameToValue = arguments.stream().collect(
        Collectors.toMap(SymbolMetadata.AnnotationValue::name, SymbolMetadata.AnnotationValue::value));

      return Optional.ofNullable((String) nameToValue.get("value"))
        .or(() -> Optional.ofNullable((String) nameToValue.get("name")))
        .or(() -> Optional.of(parameter.simpleName().name()));
    });

  }

  private static Stream<String> extractPathArgumentFromMappingAnnotations(MethodTree method, String annotation) {
    SymbolMetadata metadata = method.symbol().metadata();
    return Optional.ofNullable(metadata.valuesForAnnotation(annotation)).flatMap(arguments -> {
      Map<String, Object> nameToValue = arguments.stream().collect(
        Collectors.toMap(SymbolMetadata.AnnotationValue::name, SymbolMetadata.AnnotationValue::value));

      return arrayOrString(nameToValue.get("path"))
        .or(() -> arrayOrString(nameToValue.get("value")));
    }).orElseGet(Stream::empty);
  }

  private static Optional<Stream<String>> arrayOrString(Object value) {
    if (value == null) {
      return Optional.empty();
    }

    Object[] array = (Object[]) value;
    return Optional.of(Stream.of(array)
      .map(x -> (String) x));
  }

}
