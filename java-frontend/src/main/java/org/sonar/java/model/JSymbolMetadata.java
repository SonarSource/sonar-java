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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
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
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

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
    List<JSema.DeclaredAnnotation> declared = symbol instanceof JPackageSymbol ? symbol.sema.packageAnnotations(symbol.name()) : symbol.sema.declaredAnnotations(symbol.id);
    for (JSema.DeclaredAnnotation annotation : declared) {
      fromFacts.add(new FactAnnotation(symbol.sema, JSema.erase(annotation.name()), annotation.values(), annotation.valuesKnown()));
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
      .filter(TypeTree.class::isInstance)
      .map(argument -> new TypeTreeMetadata(sema, (TypeTree) argument))
      .toArray(SymbolMetadata[]::new);
  }

  static boolean isAnnotatedType(TypeTree type) {
    return !outermostTypeAnnotations(type).isEmpty();
  }

  static SymbolMetadata ofTypeTree(JSema sema, TypeTree type) {
    return new TypeTreeMetadata(sema, type);
  }

  static SymbolMetadata ofDeclared(JSema sema, List<JSema.DeclaredAnnotation> declared) {
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
    private final List<JSema.DeclaredAnnotation> declared;

    private DeclaredTypeMetadata(JSema sema, List<JSema.DeclaredAnnotation> declared) {
      this.sema = sema;
      this.declared = declared;
    }

    @Override
    List<AnnotationInstance> loadAnnotations() {
      return declared.stream().<AnnotationInstance>map(annotation -> new FactAnnotation(sema, JSema.erase(annotation.name()), annotation.values(), annotation.valuesKnown())).toList();
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
      return NO_ANNOTATION_NULLABILITY[level.ordinal()];
    }
    Symbol owner = symbol.owner();
    // a local or anonymous class is declared in a method or initializer; the contract of that member is not its own
    while (symbol.isTypeSymbol() && owner != null && (owner.isMethodSymbol() || owner.isVariableSymbol())) {
      owner = owner.owner();
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

  static final class TreeAnnotation implements AnnotationInstance {

    private final JSema sema;
    private final AnnotationTree tree;

    private TreeAnnotation(JSema sema, AnnotationTree tree) {
      this.sema = sema;
      this.tree = tree;
    }

    @Override
    public Symbol symbol() {
      Type type = tree.annotationType().symbolType();
      if (type.isUnknown() && tree.symbolType().isUnknown() && !"!Unknown!".equals(tree.symbolType().fullyQualifiedName())) {
        // an unresolved annotation still carries its written name
        type = tree.symbolType();
      }
      return type.symbol();
    }

    @Override
    public List<AnnotationValue> values() {
      List<AnnotationValue> values = new ArrayList<>();
      if (symbol().isUnknown()) {
        // the members of an annotation nothing declares cannot be told apart from other expressions
        return values;
      }
      for (ExpressionTree argument : tree.arguments()) {
        if (argument instanceof AssignmentExpressionTree assignment) {
          String name = assignment.variable().toString();
          values.add(new AnnotationValueImpl(name, value(name, assignment.expression())));
        } else {
          values.add(new AnnotationValueImpl("value", value("value", argument)));
        }
      }
      return values;
    }

    /**
     * The value of the member {@code name}, wrapped in an array when the member is array typed, as the value of an
     * array typed member is always an array no matter how many elements the annotation spells out.
     */
    private Object value(String name, ExpressionTree expression) {
      Object value = literal(expression);
      return value instanceof Object[] || !isArrayTyped(name) ? value : new Object[] {value};
    }

    private boolean isArrayTyped(String name) {
      return symbol() instanceof Symbol.TypeSymbol annotation
        && annotation.lookupSymbols(name).stream()
          .anyMatch(member -> member instanceof Symbol.MethodSymbol method && method.returnType().type().isArray());
    }

    private Object literal(ExpressionTree expression) {
      return literal(sema, expression);
    }

    static Object literal(JSema sema, ExpressionTree expression) {
      if (expression instanceof LiteralTree literal) {
        return LiteralUtils.constantValue(literal);
      }
      if (expression instanceof AnnotationTree annotation) {
        return new TreeAnnotation(sema, annotation);
      }
      if (expression instanceof NewArrayTree array) {
        return array.initializers().stream().map(initializer -> literal(sema, initializer)).toArray();
      }
      if (expression instanceof MemberSelectExpressionTree select && "class".equals(select.identifier().name())) {
        return select.expression().symbolType().symbol();
      }
      Symbol symbol = expression instanceof MemberSelectExpressionTree select ? select.identifier().symbol()
        : (expression instanceof IdentifierTree identifier ? identifier.symbol() : null);
      if (symbol instanceof Symbol.VariableSymbol variable && !variable.isUnknown()) {
        return variable.constantValue().orElse(variable);
      }
      if (symbol != null && symbol.isUnknown()) {
        return Symbol.UNKNOWN_SYMBOL;
      }
      return expression.toString();
    }

  }

  private static final class FactAnnotation implements AnnotationInstance {

    private final Symbol symbol;
    private final List<AnnotationValue> values;

    private FactAnnotation(JSema sema, String fqn, List<AnnotationValue> values, boolean valuesKnown) {
      this.symbol = sema.typeSymbol(fqn);
      // An annotation known by name only keeps one unknown value, so a consumer that reads a value never meets an empty list.
      this.values = valuesKnown ? values : List.of(new AnnotationValueImpl("value", null));
    }

    @Override
    public Symbol symbol() {
      return symbol;
    }

    @Override
    public List<AnnotationValue> values() {
      return values;
    }
  }
}
