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

    // request @RequestMapping can be put on top of a class, path template inside it will affect all the class methods
    var requestMappingArguments = clazzTree.symbol().metadata().valuesForAnnotation(REQUEST_MAPPING_ANNOTATION);
    Set<String> requestMappingTemplateVariables = new HashSet<>();
    if (requestMappingArguments != null) {
      Set<String> templateVars;
      try {
        templateVars = templateVariablesFromMapping(requestMappingArguments);
      } catch (DoNotReportOnMethod ignored) {
        return;
      }

      if (templateVars != null) {
        requestMappingTemplateVariables = templateVars;
      }
    }

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
          checkParametersAndPathTemplate(method, modelAttributeMethodParameter, requestMappingTemplateVariables);
        } catch (DoNotReportOnMethod ignored) {
        }
      }
    }
  }

  private void checkParametersAndPathTemplate(MethodTree method, Set<String> modelAttributeMethodParameters, Set<String> requestMappingTemplateVars) {
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
    for (var ann : method.modifiers().annotations()) {
      String fullyQualifiedName = ann.annotationType().symbolType().fullyQualifiedName();
      if (!MAPPING_ANNOTATIONS.contains(fullyQualifiedName)) {
        continue;
      }

      // valuesForAnnotation cannot be null from previous filter
      var values = method.symbol().metadata().valuesForAnnotation(fullyQualifiedName);
      var templatesVars = templateVariablesFromMapping(values);

      if (templatesVars != null) {
        templateVariables.add(new UriInfo<>(ann, templatesVars));
      }
    }

    // we handle the case where a path variable doesn't match to an uri parameter (/{aParam}/)
    Set<String> allTemplateVariables = templateVariables.stream()
      .flatMap(uri -> uri.value().stream())
      .collect(Collectors.toSet());
    allTemplateVariables.addAll(requestMappingTemplateVars);

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
    allPathVariables.addAll(modelAttributeMethodParameters);

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

  @Nullable
  private static Set<String> templateVariablesFromMapping(List<SymbolMetadata.AnnotationValue> values) {
    Map<String, Object> nameToValue = values.stream()
      .collect(Collectors.toMap(SymbolMetadata.AnnotationValue::name, SymbolMetadata.AnnotationValue::value));
    List<String> path = arrayOrString(nameToValue.get("path"));
    List<String> value = arrayOrString(nameToValue.get("value"));

    if (path != null || value != null) {
      List<String> paths = path != null ? path : value;
      return paths.stream()
        .map(MissingPathVariableAnnotationCheck::extractTemplateVariables)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
    } else {
      return null;
    }
  }

  private static Set<String> extractTemplateVariables(String path) {
    return PathPatternParser.parsePathVariables(path);
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

  static class DoNotReportOnMethod extends RuntimeException {
  }

  private record ParameterInfo(VariableTree parameter, String value) {
  }
  private record UriInfo<A>(AnnotationTree request, A value) {
  }

  static class PathPatternParser {
    private static final String REST_PATH_WILDCARD = "{**}";
    private static final String PREFIX_REST_PATH_VARIABLE = "{*";
    private static final String PREFIX_REGEX_PATH_VARIABLE = "{";

    private static int stringPos;
    private static String path;
    private static Set<String> vars;

    public static Set<String> parsePathVariables(String urlPath) {
      stringPos = 0;
      path = urlPath;
      vars = new HashSet<>();

      while (stringPos < path.length()) {

        if (!matchPrefix("{")) {
          consumeCurrentChar();
        }else if(!ifMatchConsumeRestPathWildcard() &&
          !ifMatchConsumeRestPathVariable()) {
          // for now it is impossible to cover this line, regex path variable consume everything
          consumeRegexPathVariable();
        }
      }
      return vars;
    }

    // match and consume exactly "{**}"
    private static boolean ifMatchConsumeRestPathWildcard() {
      if(matchPrefix(REST_PATH_WILDCARD)){
        consumePrefix(REST_PATH_WILDCARD);
        return true;
      }else{
        return false;
      }
    }

    // match and consume "{*name}"
    private static boolean ifMatchConsumeRestPathVariable() {
      if(!matchPrefix(PREFIX_REST_PATH_VARIABLE)){
        return false;
      }

      consumePrefix(PREFIX_REST_PATH_VARIABLE);
      int startTemplateVar = stringPos;

      while (stringPos < path.length()) {
        char current = consumeCurrentChar();
        if (current == '}') {
          vars.add(substringToCurrentChar(startTemplateVar));
          return true;
        }
      }
      throw new DoNotReportOnMethod();
    }

    // consume "{name}" or "{name:regex}"
    private static void consumeRegexPathVariable() {

      if(!matchPrefix(PREFIX_REGEX_PATH_VARIABLE)){
        throw new DoNotReportOnMethod();
      }

      if(matchPrefix("{}")){
        throw new DoNotReportOnMethod();
      }

      consumePrefix(PREFIX_REGEX_PATH_VARIABLE);
      int startTemplateVar = stringPos;

      while (stringPos < path.length()) {
        char current = consumeCurrentChar();

        if (current == '}') {
          vars.add(substringToCurrentChar(startTemplateVar));
          return;
        } else if (current == ':') {
          vars.add(substringToCurrentChar(startTemplateVar));
          consumeRegex();
          return;
        }

      }
      throw new DoNotReportOnMethod();
    }

    // consume "regex}"
    // the regular expression can be written as regex = "([^{}]*regex)*}"
    // it is recursive we use regex in regex
    private static void consumeRegex() {
      while (stringPos < path.length()) {
        char current = consumeCurrentChar();

        if (current == '}') {
          return;
        } else if (current == '{') {
          consumeRegex();
        }
      }
      throw new DoNotReportOnMethod();
    }

    private static boolean matchPrefix(String prefix) {
      int endPosPrefix = stringPos + prefix.length();
      if (endPosPrefix <= path.length()) {
        return prefix.equals(path.substring(stringPos, endPosPrefix));
      } else {
        return false;
      }
    }

    // we assume bound check on the path were done
    private static char consumeCurrentChar() {
      ++stringPos;
      return path.charAt(stringPos-1);
    }
    private static void consumePrefix(String prefix){
      stringPos+=prefix.length();
    }
    // current char is excluded from the substring
    private static String substringToCurrentChar(int start){
      return path.substring(start, stringPos-1);
    }

  }
}
