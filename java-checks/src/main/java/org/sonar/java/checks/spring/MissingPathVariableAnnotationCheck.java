/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S6856")
public class MissingPathVariableAnnotationCheck extends IssuableSubscriptionVisitor {
  private static final String PATH_VARIABLE_ANNOTATION = "org.springframework.web.bind.annotation.PathVariable";
  private static final String MAP = "java.util.Map";
  private static final String MODEL_ATTRIBUTE_ANNOTATION = "org.springframework.web.bind.annotation.ModelAttribute";
  private static final String REQUEST_MAPPING_ANNOTATION = "org.springframework.web.bind.annotation.RequestMapping";
  private static final Pattern EXTRACT_PATH_VARIABLE = Pattern.compile("([^:}/]*)(:.*)?}.*");
  private static final Predicate<String> CONTAINS_PLACEHOLDER = Pattern.compile("\\$\\{.*}").asPredicate();
  private static final Predicate<String> PATH_ARG_REGEX = Pattern.compile("\\{([^{}:]+:.*)}").asPredicate();
  private static final Pattern PATH_REGEX = Pattern.compile("\\{([^{}]+)}");

  private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
    REQUEST_MAPPING_ANNOTATION,
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.DeleteMapping");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree clazzTree = (ClassTree) tree;

    List<MethodTree> methods = clazzTree.members().stream()
      .filter(member -> member.is(Tree.Kind.METHOD))
      .map(MethodTree.class::cast)
      .toList();

    // we find path variable annotations on method annotated with @ModelAttribute and extract the name
    Set<String> modelAttributeMethodParameter = new HashSet<>();
    for (var method : methods) {
      if (!method.symbol().metadata().isAnnotatedWith(MODEL_ATTRIBUTE_ANNOTATION)) {
        continue;
      }
      for (var parameter : method.parameters()) {
        SymbolMetadata metadata = parameter.symbol().metadata();
        var arguments = metadata.valuesForAnnotation(PATH_VARIABLE_ANNOTATION);
        if (arguments != null) {
          modelAttributeMethodParameter.add(extractPathMethodParameters(parameter, arguments).value);
        }
      }
    }

    for (var method : methods) {
      if (!method.symbol().metadata().isAnnotatedWith(MODEL_ATTRIBUTE_ANNOTATION)) {
        try {
          checkParametersAndPathTemplate(method, modelAttributeMethodParameter);
        } catch (DoNotReportOnMethod ignored) {
        }
      }
    }
  }

  private void checkParametersAndPathTemplate(MethodTree method, Set<String> modelAttributePathVariable) {
    // we find path variable annotations and extract the name
    // example find : @PathVariable() String id and extract id
    List<ParameterInfo> methodParameters = new ArrayList<>();
    for (var parameter : method.parameters()) {
      SymbolMetadata metadata = parameter.symbol().metadata();

      if (metadata.annotations().stream().anyMatch(ann -> ann.symbol().isUnknown())) {
        throw new DoNotReportOnMethod();
      }

      var arguments = metadata.valuesForAnnotation(PATH_VARIABLE_ANNOTATION);
      if (arguments != null) {
        methodParameters.add(extractPathMethodParameters(parameter, arguments));
      }
    }


    // we find mapping annotation and extract path
    // example find @GetMapping("/{id}") and extract "/{id}"
    List<UriInfo<Set<String>>> templateVariables = new ArrayList<>();
    if (method.modifiers().annotations().stream().anyMatch(ann -> ann.symbolType().isUnknown())) {
      throw new DoNotReportOnMethod();
    }
    for(var ann : method.modifiers().annotations()){
      String fullyQualifiedName = ann.annotationType().symbolType().fullyQualifiedName();
      if(!MAPPING_ANNOTATIONS.contains(fullyQualifiedName)){
        continue;
      }
      // valuesForAnnotation cannot be null from previous filter
      Map<String, Object> nameToValue = method.symbol().metadata().valuesForAnnotation(fullyQualifiedName).stream()
        .collect(Collectors.toMap(SymbolMetadata.AnnotationValue::name, SymbolMetadata.AnnotationValue::value));
      List<String> path = arrayOrString(nameToValue.get("path"));
      List<String> value = arrayOrString(nameToValue.get("value"));

      if (path != null || value!=null) {
        List<String> paths = path!=null ? path : value;
        templateVariables.add(new UriInfo<>(ann, paths.stream()
          .map(MissingPathVariableAnnotationCheck::extractTemplateVariables)
          .flatMap(Collection::stream)
          .collect(Collectors.toSet()))
        );
      }
    }

    // we handle the case where a path variable doesn't match to an uri parameter (/{aParam}/)
    Set<String> allTemplateVariables = templateVariables.stream()
      .flatMap(uri -> uri.value().stream())
      .collect(Collectors.toSet());
    methodParameters.stream()
      .filter(v -> !allTemplateVariables.contains(v.value()))
      .filter(v -> !v.parameter().symbol().type().is(MAP))
      .forEach(v -> reportIssue(v.parameter(), String.format("Bind method parameter \"%s\" to a template variable.", v.value())));


    if (containsTypeMapAsParameter(method)) {
      /*
       * If any of the method parameters is a map, we assume all path variables are captured
       * and there is no mismatch with path variables in the request mapping.
       */
      return;
    }

    Set<String> allPathVariables = methodParameters.stream()
      .map(ParameterInfo::value)
      .collect(Collectors.toSet());
    allPathVariables.addAll(modelAttributePathVariable);

    templateVariables.stream()
      .filter(uri -> !allPathVariables.containsAll(uri.value()))
      .forEach(uri -> {
        Set<String> unbind = new HashSet<>(uri.value());
        unbind.removeAll(allPathVariables);
        reportIssue(
          uri.request(),
          "Bind template variable \"" + String.join("\", \"", unbind) + "\" to a method parameter.");
      });

  }

  private static boolean containsTypeMapAsParameter(MethodTree method) {
    return method.parameters().stream()
      .filter(parameter -> parameter.symbol().metadata().isAnnotatedWith(PATH_VARIABLE_ANNOTATION))
      .anyMatch(parameter -> {
        Type type = parameter.type().symbolType();
        return type.isSubtypeOf(MAP);
      });
  }

  // missing create custom parser
  private static Set<String> extractTemplateVariables(String path) {
    if (CONTAINS_PLACEHOLDER.test(path)) {
      return new HashSet<>();
    }

    if (PATH_ARG_REGEX.test(path)) {
      return PATH_REGEX.matcher(path).results()
        .map(MatchResult::group)
        .map(s -> s.substring(1))
        .filter(s -> s.contains(":"))
        .map(s -> s.split(":")[0])
        .collect(Collectors.toSet());
    }

    return Stream.of(path.split("\\{"))
      .map(EXTRACT_PATH_VARIABLE::matcher)
      .filter(Matcher::matches)
      .map(matcher -> matcher.group(1))
      .collect(Collectors.toSet());
  }

  private static ParameterInfo extractPathMethodParameters(VariableTree parameter, List<SymbolMetadata.AnnotationValue> arguments) {
    Map<String, Object> nameToValue = arguments.stream().collect(
      Collectors.toMap(SymbolMetadata.AnnotationValue::name, SymbolMetadata.AnnotationValue::value));

    String value = (String) nameToValue.get("value");
    String name = (String) nameToValue.get("name");
    if (value != null) {
      return new ParameterInfo(parameter, value);
    } else if (name != null) {
      return new ParameterInfo(parameter, name);
    } else {
      return new ParameterInfo(parameter, parameter.simpleName().name());
    }
  }

  @Nullable
  private static List<String> arrayOrString(@Nullable Object value) {
    if (value == null) {
      return null;
    }

    Object[] array = (Object[]) value;
    return Stream.of(array)
      .map(el -> (String) el)
      .toList();
  }

  private static class DoNotReportOnMethod extends RuntimeException {
  }

  private record ParameterInfo(VariableTree parameter, String value) {
  }
  private record UriInfo<A>(AnnotationTree request, A value) {
  }
}
