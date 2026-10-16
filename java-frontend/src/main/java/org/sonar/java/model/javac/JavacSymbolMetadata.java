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
package org.sonar.java.model.javac;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.sonar.java.model.JSymbolMetadataNullabilityHelper;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.model.Nullability;
import org.sonar.java.model.Symbols;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

/** Adapts javac declaration and type-use annotations to symbol metadata. */
final class JavacSymbolMetadata implements SymbolMetadata {

  private final JavacSymbols symbols;
  private final Symbol symbol;
  private final List<AnnotationInstance> directAnnotations;
  private final List<String> sourceAnnotationNames;
  private final List<String> unknownSourceAnnotationNames;
  private final SymbolMetadata[] parametersMetadata;
  private final boolean typeArgument;
  private final Map<NullabilityTarget, NullabilityData> nullability = new EnumMap<>(NullabilityTarget.class);

  JavacSymbolMetadata(JavacSymbols symbols, Symbol symbol, Element element) {
    this(symbols, symbol, annotations(element),
      element instanceof javax.lang.model.element.ExecutableElement method ? method.getReturnType() : element.asType(), false);
  }

  private static List<AnnotationMirror> annotations(Element element) {
    Map<String, AnnotationMirror> result = new LinkedHashMap<>();
    for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
      result.put(annotation.toString(), annotation);
    }
    TypeMirror type = element instanceof javax.lang.model.element.ExecutableElement method ? method.getReturnType() : element.asType();
    for (AnnotationMirror annotation : type.getAnnotationMirrors()) {
      result.putIfAbsent(annotation.toString(), annotation);
    }
    return List.copyOf(result.values());
  }

  private JavacSymbolMetadata(JavacSymbols symbols, Symbol symbol, List<? extends AnnotationMirror> annotations,
    TypeMirror type, boolean typeArgument) {
    this.symbols = symbols;
    this.symbol = symbol;
    this.typeArgument = typeArgument;
    sourceAnnotationNames = !typeArgument && symbol.declaration() != null
      ? ModifiersUtils.getAnnotations(symbol.declaration()).stream().map(annotation -> annotationName(annotation.annotationType())).toList()
      : List.of();
    unknownSourceAnnotationNames = !typeArgument && symbol.declaration() != null
      ? ModifiersUtils.getAnnotations(symbol.declaration()).stream()
        .filter(annotation -> annotation.symbolType().isUnknown())
        .map(annotation -> annotationName(annotation.annotationType())).toList()
      : List.of();
    List<AnnotationInstance> resolvedAnnotations = new ArrayList<>(annotations.stream().map(this::adapt).map(this::sourceAnnotation).toList());
    if (!typeArgument && symbol.declaration() != null) {
      for (AnnotationTree annotation : ModifiersUtils.getAnnotations(symbol.declaration())) {
        Symbol annotationSymbol = annotation.symbolType().symbol();
        if (!annotationSymbol.isUnknown() && resolvedAnnotations.stream().noneMatch(instance -> instance.symbol().equals(annotationSymbol))) {
          resolvedAnnotations.add(new Instance(annotationSymbol, List.of()));
        }
      }
    }
    directAnnotations = List.copyOf(resolvedAnnotations);
    if (type instanceof DeclaredType declared) {
      parametersMetadata = declared.getTypeArguments().stream()
        .map(argument -> new JavacSymbolMetadata(symbols, symbol, argument.getAnnotationMirrors(), argument, true))
        .toArray(SymbolMetadata[]::new);
    } else {
      parametersMetadata = new SymbolMetadata[0];
    }
  }

  @Override
  public boolean isAnnotatedWith(String fullyQualifiedName) {
    return unknownSourceAnnotationNames.contains(fullyQualifiedName)
      || annotations().stream().anyMatch(annotation -> !annotation.symbol().isUnknown() && annotation.symbol().type().is(fullyQualifiedName));
  }

  @Nullable
  @Override
  public List<AnnotationValue> valuesForAnnotation(String fullyQualifiedNameOfAnnotation) {
    return annotations().stream()
      .filter(annotation -> annotation.symbol().isUnknown()
        ? unknownSourceAnnotationNames.contains(fullyQualifiedNameOfAnnotation)
          && annotation.symbol().type().fullyQualifiedName().equals(fullyQualifiedNameOfAnnotation)
        : annotation.symbol().type().is(fullyQualifiedNameOfAnnotation))
      .map(AnnotationInstance::values)
      .findFirst().orElse(null);
  }

  @Override
  public List<AnnotationInstance> annotations() {
    List<AnnotationInstance> all = new ArrayList<>(directAnnotations);
    for (SymbolMetadata parameter : parametersMetadata) {
      all.addAll(parameter.annotations());
    }
    return List.copyOf(all);
  }

  @Override
  public List<AnnotationInstance> symbolAnnotations() {
    return directAnnotations;
  }

  @Override
  public NullabilityData nullabilityData() {
    NullabilityTarget target = target();
    return target == null ? Nullability.unknownAt(NullabilityLevel.UNKNOWN) : nullabilityData(target);
  }

  @Override
  public NullabilityData nullabilityData(NullabilityTarget level) {
    return nullability.computeIfAbsent(level, this::resolveNullability);
  }

  @Nullable
  @Override
  public AnnotationTree findAnnotationTree(AnnotationInstance annotationInstance) {
    Tree declaration = symbol.declaration();
    if (declaration != null) {
      for (AnnotationTree annotation : ModifiersUtils.getAnnotations(declaration)) {
        if (annotation.symbolType().symbol().equals(annotationInstance.symbol())
          && annotation.arguments().size() == annotationInstance.values().size()) {
          return annotation;
        }
      }
    }
    return null;
  }

  @Override
  public SymbolMetadata[] parametersMetadata() {
    return parametersMetadata.clone();
  }

  private NullabilityData resolveNullability(NullabilityTarget target) {
    NullabilityLevel level = level();
    NullabilityData direct = JSymbolMetadataNullabilityHelper.getNullabilityDataAtLevel(this, target, level);
    if (direct.type() != NullabilityType.NO_ANNOTATION) {
      return direct;
    }
    if (symbol instanceof Symbol.MethodSymbol method && !typeArgument) {
      for (Symbol.MethodSymbol overridden : method.overriddenSymbols()) {
        NullabilityData inherited = JSymbolMetadataNullabilityHelper.getNullabilityDataAtLevel(overridden.metadata(), target, NullabilityLevel.METHOD);
        if (inherited.type() != NullabilityType.NO_ANNOTATION && inherited.type() != NullabilityType.UNKNOWN) {
          return inherited;
        }
      }
    }
    if (symbol.isPackageSymbol()) {
      return Nullability.noAnnotationAt(level);
    }
    Symbol owner = symbol.owner();
    while (owner != null && owner.isMethodSymbol() && level == NullabilityLevel.CLASS) {
      owner = owner.owner();
    }
    return owner == null ? Nullability.unknownAt(level) : owner.metadata().nullabilityData(target);
  }

  private NullabilityLevel level() {
    if (symbol.isVariableSymbol()) {
      return NullabilityLevel.VARIABLE;
    }
    if (symbol.isMethodSymbol()) {
      return NullabilityLevel.METHOD;
    }
    if (symbol.isTypeSymbol()) {
      return NullabilityLevel.CLASS;
    }
    if (symbol.isPackageSymbol()) {
      return NullabilityLevel.PACKAGE;
    }
    return NullabilityLevel.UNKNOWN;
  }

  @Nullable
  private NullabilityTarget target() {
    if (symbol.isMethodSymbol()) {
      return NullabilityTarget.METHOD;
    }
    if (symbol.isVariableSymbol()) {
      Symbol owner = symbol.owner();
      if (owner != null && owner.isTypeSymbol()) {
        return NullabilityTarget.FIELD;
      }
      Tree declaration = symbol.declaration();
      if (declaration == null || declaration.parent() == null || declaration.parent() instanceof MethodTree) {
        return NullabilityTarget.PARAMETER;
      }
      if (!declaration.parent().is(Tree.Kind.LAMBDA_EXPRESSION)) {
        return NullabilityTarget.LOCAL_VARIABLE;
      }
    }
    return null;
  }

  private AnnotationInstance adapt(AnnotationMirror mirror) {
    Symbol annotationSymbol = symbols.symbol(mirror.getAnnotationType().asElement());
    List<AnnotationValue> values = mirror.getElementValues().entrySet().stream()
      .map(entry -> {
        String name = entry.getKey().getSimpleName().toString();
        Object value = convertAnnotationValue(entry.getValue());
        return (AnnotationValue) new Value(name, value == null ? unresolvedClassLiteral(mirror, name) : value);
      })
      .toList();
    return new Instance(annotationSymbol, values);
  }

  private AnnotationInstance sourceAnnotation(AnnotationInstance annotation) {
    if (!annotation.symbol().isUnknown()) {
      return annotation;
    }
    String name = annotation.symbol().type().fullyQualifiedName();
    if (sourceAnnotationNames.contains(name)) {
      return annotation;
    }
    String sourceName = sourceAnnotationNames.stream()
      .filter(candidate -> name.endsWith("." + candidate))
      .findFirst().orElse("!Unknown!");
    return new Instance(new JavacSyntheticTypeSymbol(new Symbols.UnknownType(sourceName), sourceName), annotation.values());
  }

  private static String annotationName(Tree expression) {
    if (expression instanceof IdentifierTree identifier) {
      return identifier.name();
    }
    if (expression instanceof MemberSelectExpressionTree memberSelect) {
      return annotationName(memberSelect.expression()) + "." + memberSelect.identifier().name();
    }
    return "";
  }

  @Nullable
  private Symbol.TypeSymbol unresolvedClassLiteral(AnnotationMirror mirror, String valueName) {
    Tree declaration = symbol.declaration();
    if (declaration == null) {
      return null;
    }
    String annotationName = symbols.type(mirror.getAnnotationType()).fullyQualifiedName();
    for (AnnotationTree annotation : ModifiersUtils.getAnnotations(declaration)) {
      if (!annotation.symbolType().is(annotationName)) {
        continue;
      }
      for (ExpressionTree argument : annotation.arguments()) {
        String name = "value";
        ExpressionTree expression = argument;
        if (argument instanceof AssignmentExpressionTree assignment) {
          if (!(assignment.variable() instanceof IdentifierTree identifier)) {
            continue;
          }
          name = identifier.name();
          expression = assignment.expression();
        }
        if (!valueName.equals(name) || !(expression instanceof MemberSelectExpressionTree classLiteral)
          || !"class".equals(classLiteral.identifier().name())) {
          continue;
        }
        ExpressionTree selected = classLiteral.expression();
        if (selected instanceof IdentifierTree identifier) {
          return new JavacSyntheticTypeSymbol(Type.UNKNOWN, identifier.name());
        }
        if (selected instanceof MemberSelectExpressionTree memberSelect) {
          return new JavacSyntheticTypeSymbol(Type.UNKNOWN, memberSelect.identifier().name());
        }
      }
    }
    return null;
  }

  private Object convertAnnotationValue(javax.lang.model.element.AnnotationValue value) {
    return value.toString().equals("<error>") ? null : convert(value.getValue());
  }

  private Object convert(Object value) {
    if (value instanceof VariableElement variable) {
      return symbols.symbol(variable);
    }
    if (value instanceof TypeMirror type) {
      return symbols.type(type).symbol();
    }
    if (value instanceof AnnotationMirror annotation) {
      return adapt(annotation);
    }
    if (value instanceof List<?> list) {
      return list.stream()
        .map(item -> convertAnnotationValue((javax.lang.model.element.AnnotationValue) item))
        .toArray();
    }
    return value;
  }

  private record Instance(Symbol symbol, List<AnnotationValue> values) implements AnnotationInstance {
  }

  private record Value(String name, Object value) implements AnnotationValue {
  }
}
