/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.DefaultModuleScannerContext;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.DependencyVersionAware;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.ModuleScannerContext;
import org.sonar.plugins.java.api.Version;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.ExpressionsHelper.isStandardDataType;
import static org.sonar.java.checks.helpers.MethodTreeUtils.isSetterLike;

@Rule(key = "S6856")
public class MissingPathVariableAnnotationCheck extends IssuableSubscriptionVisitor implements DependencyVersionAware, EndOfAnalysis {
  private static final String PATH_VARIABLE_ANNOTATION = "org.springframework.web.bind.annotation.PathVariable";
  private static final String MAP = "java.util.Map";
  private static final String MODEL_ATTRIBUTE_ANNOTATION = "org.springframework.web.bind.annotation.ModelAttribute";
  private static final String REQUEST_MAPPING_ANNOTATION = "org.springframework.web.bind.annotation.RequestMapping";
  private static final String MVC_API_VERSION_CONFIGURER = "org.springframework.web.servlet.config.annotation.ApiVersionConfigurer";
  private static final String WEB_MVC_CONFIGURER = "org.springframework.web.servlet.config.annotation.WebMvcConfigurer";
  private static final String WEB_MVC_CONFIGURATION_SUPPORT = "org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport";
  private static final String PROPERTY_PLACEHOLDER_PATTERN = "\\$\\{[^{}]*\\}";

  private static final Set<String> AMBIGUOUS_API_VERSION_CONFIGURATION_METHODS = Set.of(
    "useRequestHeader",
    "useQueryParam",
    "useMediaTypeParameter",
    "useVersionResolver",
    "setDefaultVersion");

  private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
    REQUEST_MAPPING_ANNOTATION,
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.DeleteMapping",
    "org.springframework.web.bind.annotation.PatchMapping");

  private static final Set<String> LOMBOK_SETTER_ANNOTATIONS = Set.of(
    "lombok.Data",
    "lombok.Setter");

  private static final String BIND_PARAM_ANNOTATION = "org.springframework.web.bind.annotation.BindParam";

  private SpringWebVersion springWebVersion;
  private final Set<Integer> apiVersionPathSegments = new HashSet<>();
  private boolean hasAmbiguousApiVersionConfiguration;
  private final List<AnalyzerMessage> pendingIssues = new ArrayList<>();
  private final List<PendingMappingIssue> pendingMappingIssues = new ArrayList<>();

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

    collectApiVersionPathSegments(methods);

    // request @RequestMapping can be put on top of a class, path template inside it will affect all the class methods
    var requestMappingArguments = clazzTree.symbol().metadata().valuesForAnnotation(REQUEST_MAPPING_ANNOTATION);
    MappingInfo classMapping = MappingInfo.empty();
    if (requestMappingArguments != null) {
      try {
        classMapping = mappingInfoFromMapping(requestMappingArguments);
      } catch (DoNotReport ignored) {
        return;
      }
    }

    Set<String> modelAttributeMethodParameter = extractModelAttributeMethodParameter(methods);
    modelAttributeMethodParameter.addAll(addInheritedModelAttributeMethodParameter(modelAttributeMethodParameter, clazzTree));

    checkParametersAndPathTemplate(methods, modelAttributeMethodParameter, classMapping);
  }

  private void collectApiVersionPathSegments(List<MethodTree> methods) {
    if (springWebVersion != SpringWebVersion.START_FROM_7_0) {
      return;
    }

    methods.stream()
      .filter(MissingPathVariableAnnotationCheck::isApiVersionConfigurationMethod)
      .forEach(method -> {
        Symbol configurerParameter = method.parameters().get(0).symbol();
        method.accept(new BaseTreeVisitor() {
          @Override
          public void visitIdentifier(IdentifierTree identifier) {
            if (configurerParameter.equals(identifier.symbol())
              && !isConfigurerParameterDeclaration(identifier)
              && !isDirectConfigurerReceiver(identifier)) {
              hasAmbiguousApiVersionConfiguration = true;
            }
            super.visitIdentifier(identifier);
          }

          @Override
          public void visitMethodInvocation(MethodInvocationTree invocation) {
            collectApiVersionConfiguration(invocation, configurerParameter);
            super.visitMethodInvocation(invocation);
          }
        });
      });
  }

  private static boolean isApiVersionConfigurationMethod(MethodTree method) {
    if (!"configureApiVersioning".equals(method.simpleName().name()) || method.parameters().size() != 1
      || !method.parameters().get(0).type().symbolType().is(MVC_API_VERSION_CONFIGURER)) {
      return false;
    }

    Type ownerType = method.symbol().owner().type();
    return ownerType.isSubtypeOf(WEB_MVC_CONFIGURER) || ownerType.isSubtypeOf(WEB_MVC_CONFIGURATION_SUPPORT);
  }

  private void collectApiVersionConfiguration(MethodInvocationTree invocation, Symbol configurerParameter) {
    Symbol.MethodSymbol method = invocation.methodSymbol();
    if (!method.owner().type().is(MVC_API_VERSION_CONFIGURER) || !isInvokedOnParameter(invocation, configurerParameter)) {
      return;
    }

    if ("usePathSegment".equals(method.name()) && !invocation.arguments().isEmpty()) {
      Optional<Integer> index = invocation.arguments().get(0).asConstant(Integer.class).filter(value -> value >= 0);
      if (index.isPresent()) {
        apiVersionPathSegments.add(index.get());
      } else {
        hasAmbiguousApiVersionConfiguration = true;
      }
    } else if (AMBIGUOUS_API_VERSION_CONFIGURATION_METHODS.contains(method.name())
      || isPossiblyOptionalVersionConfiguration(invocation, method)) {
      hasAmbiguousApiVersionConfiguration = true;
    }
  }

  private static boolean isInvokedOnParameter(MethodInvocationTree invocation, Symbol configurerParameter) {
    if (!(invocation.methodSelect() instanceof MemberSelectExpressionTree memberSelect)) {
      return false;
    }
    ExpressionTree receiver = ExpressionUtils.skipParentheses(memberSelect.expression());
    while (receiver instanceof MethodInvocationTree receiverInvocation
      && receiverInvocation.methodSelect() instanceof MemberSelectExpressionTree receiverMethodSelect) {
      receiver = ExpressionUtils.skipParentheses(receiverMethodSelect.expression());
    }
    return receiver instanceof IdentifierTree identifier && configurerParameter.equals(identifier.symbol());
  }

  private static boolean isConfigurerParameterDeclaration(IdentifierTree identifier) {
    return identifier.parent() instanceof VariableTree variable && variable.simpleName() == identifier;
  }

  private static boolean isDirectConfigurerReceiver(IdentifierTree identifier) {
    return identifier.parent() instanceof MemberSelectExpressionTree memberSelect
      && memberSelect.expression() == identifier
      && memberSelect.parent() instanceof MethodInvocationTree invocation
      && invocation.methodSelect() == memberSelect
      && invocation.methodSymbol().owner().type().is(MVC_API_VERSION_CONFIGURER);
  }

  private static boolean isPossiblyOptionalVersionConfiguration(MethodInvocationTree invocation, Symbol.MethodSymbol method) {
    return "setVersionRequired".equals(method.name())
      && (invocation.arguments().isEmpty() || !invocation.arguments().get(0).asConstant(Boolean.class).orElse(false));
  }

  private static Set<String> extractModelAttributeMethodParameter(List<MethodTree> methods){
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
    return modelAttributeMethodParameter;
  }

  private static Set<String> addInheritedModelAttributeMethodParameter(Set<String> modelAttributeMethodParameters, ClassTree clazz){
    if(clazz.superClass() == null){
      return modelAttributeMethodParameters;
    }
    Type superClass = clazz.superClass().symbolType();
    ClassTree declaration = superClass.symbol().declaration();
    if(declaration != null){
      List<MethodTree> methods = declaration.members().stream()
        .filter(member -> member.is(Tree.Kind.METHOD))
        .map(MethodTree.class::cast)
        .toList();
      modelAttributeMethodParameters.addAll(extractModelAttributeMethodParameter(methods));
      modelAttributeMethodParameters.addAll(addInheritedModelAttributeMethodParameter(modelAttributeMethodParameters, declaration));
    }
    return modelAttributeMethodParameters;
  }

  private void checkParametersAndPathTemplate(List<MethodTree> methods, Set<String> modelAttributeMethodParameter, MappingInfo classMapping) {
    for (var method : methods) {
      if (!method.symbol().metadata().isAnnotatedWith(MODEL_ATTRIBUTE_ANNOTATION)) {
        try {
          checkParametersAndPathTemplate(method, modelAttributeMethodParameter, classMapping);
        } catch (DoNotReport ignored) {
          // We don't want to report when semantics is broken or we were unable to parse the path template
        }
      }
    }
  }

  private void checkParametersAndPathTemplate(MethodTree method, Set<String> modelAttributeMethodParameters, MappingInfo classMapping) {
    // we find path variable annotations and extract the name
    // example find : @PathVariable() String id and extract id
    List<ParameterInfo> methodParameters = new ArrayList<>();
    for (var parameter : method.parameters()) {
      SymbolMetadata metadata = parameter.symbol().metadata();

      if (metadata.annotations().stream().anyMatch(ann -> ann.symbol().isUnknown())) {
        throw new DoNotReport();
      }

      var arguments = metadata.valuesForAnnotation(PATH_VARIABLE_ANNOTATION);
      if (arguments != null) {
        methodParameters.add(extractPathMethodParameters(parameter, arguments));
      }

    }

    // we find mapping annotation and extract path
    // example find @GetMapping("/{id}") and extract "/{id}"
    List<UriInfo> templateVariables = new ArrayList<>();
    for (var ann : method.modifiers().annotations()) {
      if (ann.symbolType().isUnknown()) {
        throw new DoNotReport();
      }

      String fullyQualifiedName = ann.annotationType().symbolType().fullyQualifiedName();
      var values = method.symbol().metadata().valuesForAnnotation(fullyQualifiedName);
      if (values == null || !MAPPING_ANNOTATIONS.contains(fullyQualifiedName)) {
        continue;
      }

      templateVariables.add(new UriInfo(ann, mappingInfoFromMapping(values)));
    }

    // we handle the case where a path variable doesn't match to uri parameter (/{aParam}/)
    Set<String> allTemplateVariables = templateVariables.stream()
      .flatMap(uri -> uri.mapping().templateVariables().stream())
      .collect(Collectors.toSet());
    allTemplateVariables.addAll(classMapping.templateVariables());

    methodParameters.stream()
      .filter(v -> !allTemplateVariables.contains(v.value()))
      .filter(v -> !v.parameter().symbol().type().is(MAP))
      .forEach(v -> addPendingIssue(v.parameter(), String.format("Bind method parameter \"%s\" to a template variable.", v.value())));

    if (containsTypeMapAsParameter(method)) {
      /*
       * If any of the method parameters is a map, we assume all path variables are captured
       * and there is no mismatch with path variables in the request mapping.
       */
      return;
    }

    // finally, we handle the case where a uri parameter (/{aParam}/) doesn't match to path-, ModelAttribute-, or class / record inherited variables
    Set<String> allPathVariables = methodParameters.stream()
      .map(ParameterInfo::value)
      .collect(Collectors.toSet());
    // Add properties inherited from @ModelAttribute methods
    allPathVariables.addAll(modelAttributeMethodParameters);
    // Add properties inherited from class and record parameters
    allPathVariables.addAll(extractClassAndRecordProperties(method));

    templateVariables.stream()
      .filter(uri -> !allPathVariables.containsAll(uri.mapping().templateVariables()))
      .forEach(uri -> {
        Set<String> unbind = new HashSet<>(uri.mapping().templateVariables());
        unbind.removeAll(allPathVariables);
        reportOrDeferMissingTemplateVariables(uri, classMapping, unbind);
      });

  }

  private void reportOrDeferMissingTemplateVariables(UriInfo uri, MappingInfo classMapping, Set<String> unboundVariables) {
    if (springWebVersion != SpringWebVersion.START_FROM_7_0) {
      addPendingIssue(uri.request(), missingTemplateVariablesMessage(unboundVariables));
      return;
    }

    pendingMappingIssues.add(new PendingMappingIssue(
      context.getInputFile(),
      AnalyzerMessage.textSpanFor(uri.request()),
      Set.copyOf(unboundVariables),
      combinePaths(classMapping.paths(), uri.mapping().paths()),
      classMapping.versioned() || uri.mapping().versioned()));
  }

  private void addPendingIssue(Tree tree, String message) {
    pendingIssues.add(new AnalyzerMessage(this, context.getInputFile(), AnalyzerMessage.textSpanFor(tree), message, 0));
  }

  private static String missingTemplateVariablesMessage(Set<String> variables) {
    return "Bind template variable \"" + String.join("\", \"", variables) + "\" to a method parameter.";
  }

  private static List<String> combinePaths(List<String> classPaths, List<String> methodPaths) {
    List<String> combinedPaths = new ArrayList<>();
    for (String classPath : classPaths) {
      for (String methodPath : methodPaths) {
        combinedPaths.add(combinePath(classPath, methodPath));
      }
    }
    return combinedPaths;
  }

  private static String combinePath(String classPath, String methodPath) {
    String left = classPath.replaceAll("/+$", "");
    String right = methodPath.replaceAll("^/+", "");
    if (left.isEmpty()) {
      return right.isEmpty() ? "" : "/" + right;
    }
    return right.isEmpty() ? left : left + "/" + right;
  }

  @Override
  public boolean scanWithoutParsing(InputFileScannerContext inputFileScannerContext) {
    // Cross-file configuration discovery is only needed for Spring Web 7 and later.
    return springWebVersion != SpringWebVersion.START_FROM_7_0;
  }

  @Override
  public void endOfAnalysis(ModuleScannerContext moduleContext) {
    DefaultModuleScannerContext defaultContext = (DefaultModuleScannerContext) moduleContext;
    pendingIssues.forEach(defaultContext::reportIssue);
    for (PendingMappingIssue pendingIssue : pendingMappingIssues) {
      Set<String> unboundVariables = new HashSet<>(pendingIssue.unboundVariables());
      if (pendingIssue.versioned() && !hasAmbiguousApiVersionConfiguration && apiVersionPathSegments.size() == 1) {
        unboundVariables.removeIf(variable -> isReadByPathVersionResolver(variable, pendingIssue.paths(), apiVersionPathSegments));
      }
      if (!unboundVariables.isEmpty()) {
        defaultContext.reportIssue(new AnalyzerMessage(
          this,
          pendingIssue.inputComponent(),
          pendingIssue.textSpan(),
          missingTemplateVariablesMessage(unboundVariables),
          0));
      }
    }

    apiVersionPathSegments.clear();
    hasAmbiguousApiVersionConfiguration = false;
    pendingIssues.clear();
    pendingMappingIssues.clear();
  }

  private static boolean isReadByPathVersionResolver(String variable, List<String> paths, Set<Integer> configuredSegments) {
    if (configuredSegments.isEmpty()) {
      return false;
    }

    boolean found = false;
    for (String path : paths) {
      if (path.contains("${")) {
        return false;
      }
      List<String> segments = pathSegments(path);
      for (int index = 0; index < segments.size(); index++) {
        String segment = segments.get(index);
        if (PathPatternParser.parsePathVariables(segment).contains(variable)) {
          found = true;
          if (!configuredSegments.contains(index) || segment.contains("{*" + variable + "}")) {
            return false;
          }
        }
      }
    }
    return found;
  }

  private static List<String> pathSegments(String path) {
    List<String> segments = new ArrayList<>();
    int segmentStart = 0;
    int braces = 0;
    boolean escaped = false;
    for (int i = 0; i < path.length(); i++) {
      char current = path.charAt(i);
      if (escaped) {
        escaped = false;
        continue;
      }
      if (current == '\\') {
        escaped = true;
      } else if (current == '{') {
        braces++;
      } else if (current == '}' && braces > 0) {
        braces--;
      } else if (current == '/' && braces == 0) {
        addPathSegment(path, segmentStart, i, segments);
        segmentStart = i + 1;
      }
    }
    addPathSegment(path, segmentStart, path.length(), segments);
    return segments;
  }

  private static void addPathSegment(String path, int start, int end, List<String> segments) {
    if (start < end) {
      segments.add(path.substring(start, end));
    }
  }

  private static boolean containsTypeMapAsParameter(MethodTree method) {
    return method.parameters().stream()
      .filter(parameter -> parameter.symbol().metadata().isAnnotatedWith(PATH_VARIABLE_ANNOTATION))
      .anyMatch(parameter -> {
        Type type = parameter.type().symbolType();
        return type.isSubtypeOf(MAP);
      });
  }

  private static MappingInfo mappingInfoFromMapping(List<SymbolMetadata.AnnotationValue> values) {
    Map<String, Object> nameToValue = values.stream()
      .collect(Collectors.toMap(SymbolMetadata.AnnotationValue::name, SymbolMetadata.AnnotationValue::value));
    List<String> path = arrayOrString(nameToValue.get("path"));
    List<String> value = arrayOrString(nameToValue.get("value"));
    List<String> paths = path != null ? path : value;
    if (paths == null || paths.isEmpty()) {
      paths = List.of("");
    }

    Set<String> templateVariables = paths.stream()
      .map(MissingPathVariableAnnotationCheck::removePropertyPlaceholder)
      .map(PathPatternParser::parsePathVariables)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
    boolean versioned = nameToValue.get("version") instanceof String version && !version.isBlank();
    return new MappingInfo(List.copyOf(paths), templateVariables, versioned);
  }

  private static ParameterInfo extractPathMethodParameters(VariableTree parameter, List<SymbolMetadata.AnnotationValue> arguments) {
    Map<String, Object> argNameToValue = arguments.stream().collect(
      Collectors.toMap(SymbolMetadata.AnnotationValue::name, SymbolMetadata.AnnotationValue::value));

    String value = (String) argNameToValue.get("value");
    String name = (String) argNameToValue.get("name");
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

  static class DoNotReport extends RuntimeException {
  }

  private record ParameterInfo(VariableTree parameter, String value) {
  }

  private record UriInfo(AnnotationTree request, MappingInfo mapping) {
  }

  private record MappingInfo(List<String> paths, Set<String> templateVariables, boolean versioned) {
    private static MappingInfo empty() {
      return new MappingInfo(List.of(""), Set.of(), false);
    }
  }

  private record PendingMappingIssue(
    InputComponent inputComponent,
    AnalyzerMessage.TextSpan textSpan,
    Set<String> unboundVariables,
    List<String> paths,
    boolean versioned) {
  }

  private static String removePropertyPlaceholder(String path){
    return path.replaceAll(PROPERTY_PLACEHOLDER_PATTERN, "");
  }

  private boolean requiresModelAttributeAnnotation(SymbolMetadata metadata) {
    // for spring-web < 5.3 we need to use ModelAttribute annotation to extract properties from classes / records
    return springWebVersion == SpringWebVersion.LESS_THAN_5_3 && !metadata.isAnnotatedWith(MODEL_ATTRIBUTE_ANNOTATION);
  }

  private Set<String> extractClassAndRecordProperties(MethodTree method) {
    Set<String> properties = new HashSet<>();

    for (var parameter : method.parameters()) {
      Type parameterType = parameter.type().symbolType();
      if (parameterType.isUnknown()
        || isStandardDataType(parameterType) || parameterType.isSubtypeOf(MAP)
        || requiresModelAttributeAnnotation(parameter.symbol().metadata())) {
        continue;
      }

      if (parameterType.isSubtypeOf("java.lang.Record") && springWebVersion != SpringWebVersion.LESS_THAN_5_3) {
        // Extract record's components
        properties.addAll(extractRecordProperties(parameterType));
      } else if (parameterType.isClass()) {
        // Extract setter properties from the class
        properties.addAll(extractSetterProperties(parameterType));
      }
    }

    return properties;
  }

  @VisibleForTesting
  static Set<String> extractSetterProperties(Type type) {
    Symbol.TypeSymbol typeSymbol = type.symbol();

    // Extract properties from Lombok-generated setters
    Set<String> properties = new HashSet<>(checkForLombokSetters(typeSymbol));

    // Extract properties from explicit setter methods
    for (Symbol member : typeSymbol.memberSymbols()) {
      if (!member.isMethodSymbol()) {
        continue;
      }

      Symbol.MethodSymbol method = (Symbol.MethodSymbol) member;

      // Check if it's a setter and extract a property name
      isSetterLike(method).ifPresent(properties::add);
    }

    return properties;
  }

  private static Set<String> checkForLombokSetters(Symbol.TypeSymbol typeSymbol) {
    Set<String> properties = new HashSet<>();

    // Check if the class has Lombok annotations that generate setters
    boolean hasLombokSetters = typeSymbol.metadata().annotations().stream()
      .anyMatch(annotation -> LOMBOK_SETTER_ANNOTATIONS.contains(annotation.symbol().type().fullyQualifiedName()));

    // Extract properties from fields if Lombok generates setters
    for (Symbol.VariableSymbol field : typeSymbol.memberSymbols().stream().filter(Symbol::isVariableSymbol).map(Symbol.VariableSymbol.class::cast).toList()) {
      if (field.isStatic() || field.isFinal()) {
        continue;
      }

      // Check if field has @Setter annotation at field level
      boolean hasFieldLevelSetter = field.metadata().annotations().stream()
        .anyMatch(annotation -> "lombok.Setter".equals(annotation.symbol().type().fullyQualifiedName()));

      // Add property if class-level or field-level Lombok setter exists
      if (hasLombokSetters || hasFieldLevelSetter) {
        properties.add(field.name());
      }
    }

    return properties;
  }

  @VisibleForTesting
  static Set<String> extractRecordProperties(Type type) {
    Set<String> properties = new HashSet<>();
    // For records, extract component names from the record components
    // Records automatically generate accessor methods for their components
    type.symbol().memberSymbols().stream()
      .filter(Symbol::isVariableSymbol)
      .map(Symbol.VariableSymbol.class::cast)
      .filter(f -> !f.isStatic())
      .forEach(field -> properties.add(getComponentName(field)));

    return properties;
  }

  private static String getComponentName(Symbol.VariableSymbol field) {
    // Check if the component has @BindParam annotation for custom binding name
    String componentName = field.name();
    var bindParamValues = field.metadata().valuesForAnnotation(BIND_PARAM_ANNOTATION);
    if (bindParamValues != null) {
      Object value = bindParamValues.get(0).value();
      if (value instanceof String bindParamName && !bindParamName.isEmpty()) {
        componentName = bindParamName;
      }
    }
    return componentName;
  }

  static class PathPatternParser {
    private PathPatternParser() {
    }

    private static final String REST_PATH_WILDCARD = "{**}";
    private static final String PREFIX_REST_PATH_VARIABLE = "{*";
    private static final String PREFIX_REGEX_PATH_VARIABLE = "{";

    private static int pos;
    private static String path;
    private static Set<String> vars;

    public static Set<String> parsePathVariables(String urlPath) {
      pos = 0;
      path = urlPath;
      vars = new HashSet<>();

      while (pos < path.length()) {

        if (!matchPrefix("{")) {
          consumeCurrentChar();
        } else if (!ifMatchConsumeRestPathWildcard() && !ifMatchConsumeRestPathVariable()) {

          consumeRegexPathVariable();
        }
      }
      return vars;
    }

    // match and consume exactly "{**}"
    private static boolean ifMatchConsumeRestPathWildcard() {
      if (matchPrefix(REST_PATH_WILDCARD)) {
        consumePrefix(REST_PATH_WILDCARD);
        return true;
      } else {
        return false;
      }
    }

    // match and consume "{*name}"
    private static boolean ifMatchConsumeRestPathVariable() {
      if (!matchPrefix(PREFIX_REST_PATH_VARIABLE)) {
        return false;
      }

      consumePrefix(PREFIX_REST_PATH_VARIABLE);
      int startTemplateVar = pos;

      while (pos < path.length()) {
        char current = consumeCurrentChar();
        if (current == '}') {
          vars.add(substringToCurrentChar(startTemplateVar));
          return true;
        }
      }
      throw new DoNotReport();
    }

    // consume "{name}" or "{name:regex}"
    private static void consumeRegexPathVariable() {

      if (!matchPrefix(PREFIX_REGEX_PATH_VARIABLE)) {
        throw new DoNotReport();
      }

      if (matchPrefix("{}")) {
        throw new DoNotReport();
      }

      consumePrefix(PREFIX_REGEX_PATH_VARIABLE);
      int startTemplateVar = pos;

      while (pos < path.length()) {
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
      throw new DoNotReport();
    }

    // consume "regex}"
    // the regular expression can be written as regex = "([^{}]*regex)*}"
    // remark it is a recursive definition
    private static void consumeRegex() {
      while (pos < path.length()) {
        char current = consumeCurrentChar();

        if (current == '}') {
          return;
        } else if (current == '{') {
          consumeRegex();
        }
      }
      throw new DoNotReport();
    }

    private static boolean matchPrefix(String prefix) {
      int endPosPrefix = pos + prefix.length();
      if (endPosPrefix <= path.length()) {
        return prefix.equals(path.substring(pos, endPosPrefix));
      } else {
        return false;
      }
    }

    // for consumeCurrentChar, consumePrefix. We assume bound check on the path were done. We may add assert to ensure it.
    private static char consumeCurrentChar() {
      ++pos;
      return path.charAt(pos - 1);
    }

    private static void consumePrefix(String prefix) {
      pos += prefix.length();
    }

    // return substring from start up to the last consumed character (excluded)
    private static String substringToCurrentChar(int start) {
      return path.substring(start, pos - 1);
    }

  }

  @Override
  public boolean isCompatibleWithDependencies(Function<String, Optional<Version>> dependencyFinder) {
    Optional<Version> springWebCurrentVersion = dependencyFinder.apply("spring-web");
    if (springWebCurrentVersion.isEmpty()) {
      return false;
    }
    springWebVersion = getSpringWebVersion(springWebCurrentVersion.get());
    return true;
  }

  private static SpringWebVersion getSpringWebVersion(Version springWebVersion) {
    if (springWebVersion.isLowerThan("5.3")) {
      return SpringWebVersion.LESS_THAN_5_3;
    }
    return springWebVersion.isLowerThan("7.0") ? SpringWebVersion.FROM_5_3_TO_6_X : SpringWebVersion.START_FROM_7_0;
  }

  private enum SpringWebVersion {
    LESS_THAN_5_3,
    FROM_5_3_TO_6_X,
    START_FROM_7_0;
  }
}
