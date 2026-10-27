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
package org.sonar.java.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationInstance;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityData;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityLevel;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityTarget;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.NullabilityType;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

/**
 * Annotations of one symbol, taken from its declaration tree when the file contains it and from
 * the graph fact otherwise. Nullability walks those annotations and then overridden methods.
 */
final class JSymbolMetadata implements SymbolMetadata {

  private static final NullabilityData[] NO_ANNOTATION_NULLABILITY =
    forEachLevel(level -> new JNullabilityData(NullabilityType.NO_ANNOTATION, level, null, null, false));
  private static final NullabilityData[] UNKNOWN_NULLABILITY =
    forEachLevel(level -> new JNullabilityData(NullabilityType.UNKNOWN, level, null, null, false));

  private final JSymbol symbol;
  private List<AnnotationInstance> annotations;
  private final Map<NullabilityTarget, NullabilityData> nullabilityCache = new EnumMap<>(NullabilityTarget.class);

  JSymbolMetadata(JSymbol symbol) {
    this.symbol = symbol;
  }

  private static NullabilityData[] forEachLevel(Function<NullabilityLevel, NullabilityData> initializer) {
    return Arrays.stream(NullabilityLevel.values()).map(initializer).toArray(NullabilityData[]::new);
  }

  public static NullabilityData noNullabilityAnnotationAt(NullabilityLevel level) {
    return NO_ANNOTATION_NULLABILITY[level.ordinal()];
  }

  public static NullabilityData unknownNullabilityAt(NullabilityLevel level) {
    return UNKNOWN_NULLABILITY[level.ordinal()];
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
      for (AnnotationTree annotation : ModifiersUtils.getAnnotations(declaration)) {
        fromTree.add(new TreeAnnotation(symbol.sema, annotation));
      }
      return fromTree;
    }
    List<AnnotationInstance> fromFacts = new ArrayList<>();
    for (String name : symbol.sema.annotationNames(symbol.id)) {
      fromFacts.add(new FactAnnotation(symbol.sema, JSema.erase(name)));
    }
    return fromFacts;
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
    if (symbol.isMethodSymbol() && symbol.declaration() instanceof MethodTree method) {
      return method.parameters().stream().map(parameter -> parameter.symbol().metadata()).toArray(SymbolMetadata[]::new);
    }
    return new SymbolMetadata[0];
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
      return NO_ANNOTATION_NULLABILITY[level.ordinal()];
    }
    Symbol owner = symbol.owner();
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
    return NO_ANNOTATION_NULLABILITY[NullabilityLevel.METHOD.ordinal()];
  }

  private static NullabilityLevel level(Symbol symbol) {
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

  static final class JNullabilityData implements NullabilityData {

    private final NullabilityType type;
    private final NullabilityLevel level;
    @Nullable
    private final AnnotationInstance annotation;
    @Nullable
    private final AnnotationTree annotationTree;
    private final boolean metaAnnotation;

    public JNullabilityData(NullabilityType type, NullabilityLevel level, @Nullable AnnotationInstance annotation,
      @Nullable AnnotationTree annotationTree, boolean metaAnnotation) {
      this.type = type;
      this.level = level;
      this.annotation = annotation;
      this.annotationTree = annotationTree;
      this.metaAnnotation = metaAnnotation;
    }

    @Override
    public NullabilityType type() {
      return type;
    }

    @Override
    public NullabilityLevel level() {
      return level;
    }

    @Override
    public boolean metaAnnotation() {
      return metaAnnotation;
    }

    @Override
    public boolean isNonNull(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue) {
      return test(minLevel, ignoreMetaAnnotation, defaultValue, candidate -> candidate == NullabilityType.NON_NULL);
    }

    @Override
    public boolean isNullable(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue) {
      return test(minLevel, ignoreMetaAnnotation, defaultValue,
        candidate -> candidate == NullabilityType.STRONG_NULLABLE || candidate == NullabilityType.WEAK_NULLABLE);
    }

    @Override
    public boolean isStrongNullable(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue) {
      return test(minLevel, ignoreMetaAnnotation, defaultValue, candidate -> candidate == NullabilityType.STRONG_NULLABLE);
    }

    private boolean test(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue, Predicate<NullabilityType> typePredicate) {
      if (type == NullabilityType.NO_ANNOTATION) {
        return false;
      }
      if (type == NullabilityType.UNKNOWN || (ignoreMetaAnnotation && metaAnnotation)) {
        return defaultValue;
      }
      return typePredicate.test(type) && minLevel.ordinal() <= level.ordinal();
    }

    @Nullable
    @Override
    public AnnotationInstance annotation() {
      return annotation;
    }

    @Nullable
    @Override
    public Tree declaration() {
      return annotationTree;
    }
  }

  private static final class TreeAnnotation implements AnnotationInstance {

    private final JSema sema;
    private final AnnotationTree tree;

    private TreeAnnotation(JSema sema, AnnotationTree tree) {
      this.sema = sema;
      this.tree = tree;
    }

    @Override
    public Symbol symbol() {
      Type type = tree.annotationType().symbolType();
      return type.symbol();
    }

    @Override
    public List<AnnotationValue> values() {
      List<AnnotationValue> values = new ArrayList<>();
      for (ExpressionTree argument : tree.arguments()) {
        if (argument instanceof AssignmentExpressionTree assignment) {
          values.add(new AnnotationValueImpl(assignment.variable().toString(), literal(assignment.expression())));
        } else {
          values.add(new AnnotationValueImpl("value", literal(argument)));
        }
      }
      return values;
    }

    private Object literal(ExpressionTree expression) {
      if (expression instanceof LiteralTree literal) {
        return literal.value();
      }
      if (expression instanceof AnnotationTree annotation) {
        return new TreeAnnotation(sema, annotation);
      }
      return expression.toString();
    }
  }

  private static final class FactAnnotation implements AnnotationInstance {

    private final Symbol symbol;

    private FactAnnotation(JSema sema, String fqn) {
      this.symbol = sema.typeSymbol(fqn);
    }

    @Override
    public Symbol symbol() {
      return symbol;
    }

    @Override
    public List<AnnotationValue> values() {
      return List.of();
    }
  }
}
