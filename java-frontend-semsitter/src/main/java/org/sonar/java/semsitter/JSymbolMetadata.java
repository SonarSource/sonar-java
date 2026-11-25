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
package org.sonar.java.semsitter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.java.model.JNullabilityData;
import org.sonar.java.model.JSymbolMetadataNullabilityHelper;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

/**
 * Annotations of one symbol, taken from its declaration tree when the file contains it and from
 * the graph fact otherwise. Nullability walks those annotations and then overridden methods.
 */
final class JSymbolMetadata implements SymbolMetadata {

  private final JSymbol symbol;

  private List<AnnotationInstance> annotations;

  private final Map<NullabilityTarget, NullabilityData> nullabilityCache = new EnumMap<>(NullabilityTarget.class);

  JSymbolMetadata(JSymbol symbol) {
    this.symbol = symbol;
  }

  public static NullabilityData noNullabilityAnnotationAt(NullabilityLevel level) {
    return JNullabilityData.noNullabilityAnnotationAt(level);
  }

  public static NullabilityData unknownNullabilityAt(NullabilityLevel level) {
    return JNullabilityData.unknownNullabilityAt(level);
  }

  @Override
  public List<AnnotationInstance> annotations() {
    if (annotations == null) {
      annotations = loadAnnotations();
    }
    return annotations;
  }

  @Override
  public List<AnnotationInstance> symbolAnnotations() {
    return annotations();
  }

  private List<AnnotationInstance> loadAnnotations() {
    Tree declaration = symbol.declaration();
    if (declaration != null) {
      List<AnnotationInstance> fromTree = new ArrayList<>();
      TypeTree written = declaredType(declaration);
      boolean array = written instanceof ArrayTypeTree;
      for (AnnotationTree annotation : ModifiersUtils.getAnnotations(declaration)) {
        // an annotation only a type can carry, written before an array type, is on its elements
        if (!(array && isTypeUseOnly(annotation)) && !namesDeclaredNonAnnotation(annotation)) {
          fromTree.add(new TreeAnnotation(symbol.sema, annotation));
        }
      }
      if (written != null) {
        for (AnnotationTree annotation : outermostTypeAnnotations(written)) {
          fromTree.add(new TreeAnnotation(symbol.sema, annotation));
        }
      }
      return fromTree;
    }
    List<AnnotationInstance> fromFacts = new ArrayList<>();
    List<DeclaredAnnotation> declared = symbol instanceof JPackageSymbol
      ? symbol.sema.annotationFacts().packageAnnotations(symbol.name())
      : symbol.sema.annotationFacts().declaredAnnotations(symbol.id);
    for (DeclaredAnnotation annotation : declared) {
      fromFacts.add(new FactAnnotation(symbol.sema, SemaFacts.erase(annotation.name()), annotation.values(), annotation.valuesKnown()));
    }
    return fromFacts;
  }

  /** A class or interface declared in the sources is not an annotation, whatever the code writes after the {@code @}. */
  private static boolean namesDeclaredNonAnnotation(AnnotationTree annotation) {
    return annotation.annotationType().symbolType().symbol().declaration() instanceof ClassTree declared && !declared.is(Tree.Kind.ANNOTATION_TYPE);
  }

  @Nullable
  private static TypeTree declaredType(Tree declaration) {
    if (declaration instanceof MethodTree method) {
      return method.returnType();
    }
    return declaration instanceof VariableTree variable ? variable.type() : null;
  }

  /** The annotations written on the type itself: on an array type those of its first dimension. */
  private static List<AnnotationTree> outermostTypeAnnotations(TypeTree type) {
    TypeTree current = type;
    while (current instanceof ArrayTypeTree array && array.type() instanceof ArrayTypeTree inner) {
      current = inner;
    }
    if (current instanceof ArrayTypeTree array) {
      return array.annotations();
    }
    if (current instanceof IdentifierTree identifier) {
      return identifier.annotations();
    }
    if (current instanceof MemberSelectExpressionTree select) {
      return select.identifier().annotations();
    }
    return current instanceof ParameterizedTypeTree parameterized ? outermostTypeAnnotations(parameterized.type()) : List.of();
  }

  private static boolean isTypeUseOnly(AnnotationTree annotation) {
    List<AnnotationValue> targets = annotation.annotationType().symbolType().symbol().metadata().valuesForAnnotation("java.lang.annotation.Target");
    if (targets == null) {
      return false;
    }
    Set<String> names = new HashSet<>();
    for (AnnotationValue target : targets) {
      collectTargets(target.value(), names);
    }
    return names.contains("TYPE_USE") && Stream.of("METHOD", "FIELD", "PARAMETER", "LOCAL_VARIABLE").noneMatch(names::contains);
  }

  private static void collectTargets(@Nullable Object value, Set<String> names) {
    if (value instanceof Object[] values) {
      for (Object item : values) {
        collectTargets(item, names);
      }
    } else if (value instanceof Symbol symbol) {
      names.add(symbol.name());
    }
  }

  @Override
  public boolean isAnnotatedWith(String fullyQualifiedName) {
    for (AnnotationInstance annotation : annotations()) {
      if (annotation.symbol().type().is(fullyQualifiedName)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  @Override
  public List<AnnotationValue> valuesForAnnotation(String fullyQualifiedNameOfAnnotation) {
    for (AnnotationInstance annotation : annotations()) {
      if (annotation.symbol().type().is(fullyQualifiedNameOfAnnotation)) {
        return annotation.values();
      }
    }
    return null;
  }

  @Override
  public NullabilityData nullabilityData() {
    NullabilityTarget target = target(symbol);
    if (target == null) {
      return unknownNullabilityAt(NullabilityLevel.UNKNOWN);
    }
    return nullabilityData(target);
  }

  @Override
  public NullabilityData nullabilityData(NullabilityTarget target) {
    return nullabilityCache.computeIfAbsent(target, this::resolveNullability);
  }

  @Nullable
  @Override
  public AnnotationTree findAnnotationTree(AnnotationInstance annotationInstance) {
    Tree declaration = symbol.declaration();
    if (declaration == null) {
      return null;
    }
    for (AnnotationTree annotation : ModifiersUtils.getAnnotations(declaration)) {
      if (annotation.annotationType().symbolType().symbol().equals(annotationInstance.symbol())
        && annotation.arguments().size() == annotationInstance.values().size()) {
        return annotation;
      }
    }
    return null;
  }

  @Override
  public SymbolMetadata[] parametersMetadata() {
    Tree declaration = symbol.declaration();
    return declaration == null ? new SymbolMetadata[0] : typeArgumentsMetadata(symbol.sema, declaredType(declaration));
  }

  private static SymbolMetadata[] typeArgumentsMetadata(JSema sema, @Nullable TypeTree written) {
    if (!(written instanceof ParameterizedTypeTree parameterized)) {
      return new SymbolMetadata[0];
    }
    return parameterized.typeArguments().stream()
      .map(argument -> new TypeTreeMetadata(sema, argument))
      .toArray(SymbolMetadata[]::new);
  }

  static boolean isAnnotatedType(TypeTree type) {
    return !outermostTypeAnnotations(type).isEmpty();
  }

  static SymbolMetadata ofTypeTree(JSema sema, TypeTree type) {
    return new TypeTreeMetadata(sema, type);
  }

  static SymbolMetadata ofDeclared(JSema sema, List<DeclaredAnnotation> declared) {
    return new DeclaredTypeMetadata(sema, declared);
  }

  /** The annotations written on a type, such as an argument of a parameterized type. */
  private abstract static class AnnotatedType implements SymbolMetadata {

    private List<AnnotationInstance> annotations;

    abstract List<AnnotationInstance> loadAnnotations();

    @Override
    public List<AnnotationInstance> annotations() {
      if (annotations == null) {
        annotations = loadAnnotations();
      }
      return annotations;
    }

    @Override
    public List<AnnotationInstance> symbolAnnotations() {
      return annotations();
    }

    @Override
    public boolean isAnnotatedWith(String fullyQualifiedName) {
      return annotations().stream().anyMatch(annotation -> annotation.symbol().type().is(fullyQualifiedName));
    }

    @Nullable
    @Override
    public List<AnnotationValue> valuesForAnnotation(String fullyQualifiedNameOfAnnotation) {
      return annotations().stream()
        .filter(annotation -> annotation.symbol().type().is(fullyQualifiedNameOfAnnotation))
        .map(AnnotationInstance::values)
        .findFirst()
        .orElse(null);
    }

    @Override
    public NullabilityData nullabilityData() {
      return nullabilityData(NullabilityTarget.PARAMETER);
    }

    @Override
    public NullabilityData nullabilityData(NullabilityTarget target) {
      return JSymbolMetadataNullabilityHelper.getNullabilityDataAtLevel(this, target, NullabilityLevel.VARIABLE);
    }
  }

  private static final class TypeTreeMetadata extends AnnotatedType {

    private final JSema sema;
    private final TypeTree type;

    private TypeTreeMetadata(JSema sema, TypeTree type) {
      this.sema = sema;
      this.type = type;
    }

    @Override
    List<AnnotationInstance> loadAnnotations() {
      return outermostTypeAnnotations(type).stream().<AnnotationInstance>map(annotation -> new TreeAnnotation(sema, annotation)).toList();
    }

    @Nullable
    @Override
    public AnnotationTree findAnnotationTree(AnnotationInstance annotationInstance) {
      return outermostTypeAnnotations(type).stream()
        .filter(annotation -> annotation.annotationType().symbolType().symbol().equals(annotationInstance.symbol()))
        .findFirst()
        .orElse(null);
    }

    @Override
    public SymbolMetadata[] parametersMetadata() {
      return typeArgumentsMetadata(sema, type);
    }
  }

  /** The annotations of a type a server answer states, such as a type argument of a supertype. */
  private static final class DeclaredTypeMetadata extends AnnotatedType {

    private final JSema sema;
    private final List<DeclaredAnnotation> declared;

    private DeclaredTypeMetadata(JSema sema, List<DeclaredAnnotation> declared) {
      this.sema = sema;
      this.declared = declared;
    }

    @Override
    List<AnnotationInstance> loadAnnotations() {
      return declared.stream()
        .<AnnotationInstance>map(annotation -> new FactAnnotation(sema, SemaFacts.erase(annotation.name()), annotation.values(), annotation.valuesKnown()))
        .toList();
    }

    @Nullable
    @Override
    public AnnotationTree findAnnotationTree(AnnotationInstance annotationInstance) {
      return null;
    }

    @Override
    public SymbolMetadata[] parametersMetadata() {
      return new SymbolMetadata[0];
    }
  }

  private NullabilityData resolveNullability(NullabilityTarget target) {
    NullabilityLevel level = level(symbol);
    NullabilityData atLevel = JSymbolMetadataNullabilityHelper.getNullabilityDataAtLevel(this, target, level);
    if (atLevel.type() != NullabilityType.NO_ANNOTATION) {
      return atLevel;
    }
    if (symbol.isMethodSymbol()) {
      NullabilityData inherited = inheritedNullability((Symbol.MethodSymbol) symbol, target);
      if (inherited.type() != NullabilityType.NO_ANNOTATION) {
        return inherited;
      }
    }
    if (symbol.isPackageSymbol()) {
      return noNullabilityAnnotationAt(level);
    }
    Symbol owner = symbol.owner();
    // a local or anonymous class is declared in a method or initializer; the contract of that member is not its own
    while (symbol.isTypeSymbol() && owner != null && (owner.isMethodSymbol() || owner.isVariableSymbol())) {
      owner = owner.owner();
    }
    if (owner == Symbol.ROOT_PACKAGE) {
      return noNullabilityAnnotationAt(NullabilityLevel.PACKAGE);
    }
    if (owner == null || owner.isUnknown()) {
      return unknownNullabilityAt(level);
    }
    return owner.metadata().nullabilityData(target);
  }

  private static NullabilityData inheritedNullability(Symbol.MethodSymbol method, NullabilityTarget target) {
    for (Symbol.MethodSymbol overridden : method.overriddenSymbols()) {
      NullabilityData data = JSymbolMetadataNullabilityHelper.getNullabilityDataAtLevel(overridden.metadata(), target, NullabilityLevel.METHOD);
      if (data.type() != NullabilityType.NO_ANNOTATION && !data.equals(unknownNullabilityAt(NullabilityLevel.METHOD))) {
        return data;
      }
    }
    return noNullabilityAnnotationAt(NullabilityLevel.METHOD);
  }

  static NullabilityLevel level(Symbol symbol) {
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

  @CheckForNull
  private static NullabilityTarget target(Symbol symbol) {
    if (symbol.isMethodSymbol()) {
      return NullabilityTarget.METHOD;
    }
    if (!symbol.isVariableSymbol()) {
      return null;
    }
    Symbol owner = symbol.owner();
    if (owner != null && owner.isTypeSymbol()) {
      return NullabilityTarget.FIELD;
    }
    Tree declaration = symbol.declaration();
    if (declaration == null || declaration.parent() == null || declaration.parent() instanceof MethodTree) {
      return NullabilityTarget.PARAMETER;
    }
    if (declaration.parent().is(Tree.Kind.LAMBDA_EXPRESSION)) {
      return null;
    }
    return NullabilityTarget.LOCAL_VARIABLE;
  }
}
