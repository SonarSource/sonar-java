/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.model;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.JSymbolMetadataNullabilityHelper.getNullabilityDataAtLevel;

final class JSymbolMetadata implements SymbolMetadata {

  public static final NullabilityData UNKNOWN_NULLABILITY = new JNullabilityData(
    NullabilityType.UNKNOWN, NullabilityLevel.UNKNOWN, null, null, false);

  private final JSema sema;
  private final Symbol symbol;
  private final IAnnotationBinding[] annotationBindings;

  /**
   * Cache for {@link #annotations()}.
   */
  private List<AnnotationInstance> annotations;

  private final Map<NullabilityTarget, NullabilityData> nullabilityCache = new EnumMap<>(NullabilityTarget.class);

  JSymbolMetadata(JSema sema, Symbol symbol, IAnnotationBinding[] annotationBindings) {
    this.sema = Objects.requireNonNull(sema);
    this.symbol = symbol;
    this.annotationBindings = annotationBindings;
  }

  JSymbolMetadata(JSema sema, Symbol symbol, IAnnotationBinding[] typeAnnotationBindings, IAnnotationBinding[] annotationBindings) {
    this.sema = Objects.requireNonNull(sema);
    this.symbol = symbol;
    this.annotationBindings = new IAnnotationBinding[typeAnnotationBindings.length + annotationBindings.length];
    System.arraycopy(typeAnnotationBindings, 0, this.annotationBindings, 0, typeAnnotationBindings.length);
    System.arraycopy(annotationBindings, 0, this.annotationBindings, typeAnnotationBindings.length, annotationBindings.length);
  }

  @Override
  public List<AnnotationInstance> annotations() {
    if (annotations == null) {
      annotations = Arrays.stream(annotationBindings)
        .map(sema::annotation)
        .collect(Collectors.toList());
    }
    return annotations;
  }

  @Override
  public final boolean isAnnotatedWith(String fullyQualifiedName) {
    for (AnnotationInstance a : annotations()) {
      if (a.symbol().type().is(fullyQualifiedName)) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  @Override
  public final List<AnnotationValue> valuesForAnnotation(String fullyQualifiedNameOfAnnotation) {
    for (AnnotationInstance a : annotations()) {
      if (a.symbol().type().is(fullyQualifiedNameOfAnnotation)) {
        // TODO what about repeating annotations?
        return a.values();
      }
    }
    return null;
  }

  @Override
  public NullabilityData nullabilityData() {
    NullabilityTarget target = getTarget(symbol);
    if (target == null) {
      return UNKNOWN_NULLABILITY;
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
    if (declaration != null) {
      return findAnnotationTree(ModifiersUtils.getAnnotations(declaration), annotationInstance);
    }
    return null;
  }

  @Nullable
  private static AnnotationTree findAnnotationTree(List<AnnotationTree> annotations, AnnotationInstance annotationInstance) {
    for (AnnotationTree annotationTree : annotations) {
      if (annotationTree.symbolType().symbol() == annotationInstance.symbol() &&
        annotationTree.arguments().size() == annotationInstance.values().size()) {
        return annotationTree;
      }
    }
    return null;
  }

  private NullabilityData resolveNullability(NullabilityTarget target) {
    NullabilityLevel currentLevel = getLevel(symbol);
    Optional<NullabilityData> nullabilityDataAtLevel = getNullabilityDataAtLevel(this, target, currentLevel);
    if (nullabilityDataAtLevel.isPresent()) {
      return nullabilityDataAtLevel.get();
    }

    // Not annotated or meta annotated, check upper level...
    if (symbol.isPackageSymbol()) {
      return UNKNOWN_NULLABILITY;
    }
    Symbol owner = symbol.owner();
    return owner == null ? UNKNOWN_NULLABILITY : owner.metadata().nullabilityData(target);
  }

  private static NullabilityLevel getLevel(Symbol symbol) {
    if (symbol.isVariableSymbol()) {
      return NullabilityLevel.VARIABLE;
    } else if (symbol.isMethodSymbol()) {
      return NullabilityLevel.METHOD;
    } else if (symbol.isTypeSymbol()) {
      return NullabilityLevel.CLASS;
    } else if (symbol.isPackageSymbol()) {
      return NullabilityLevel.PACKAGE;
    }
    return NullabilityLevel.UNKNOWN;
  }

  @CheckForNull
  private static NullabilityTarget getTarget(Symbol symbol) {
    if (symbol.isMethodSymbol()) {
      return NullabilityTarget.METHOD;
    } else if (symbol.isVariableSymbol()) {
      Symbol owner = symbol.owner();
      if (owner != null && owner.isTypeSymbol()) {
        return NullabilityTarget.FIELD;
      }
      Tree variableTree = symbol.declaration();
      if (variableTree == null) {
        return NullabilityTarget.PARAMETER;
      }
      Tree variableTreeParent = variableTree.parent();
      if (variableTreeParent == null || variableTreeParent instanceof MethodTree) {
        return NullabilityTarget.PARAMETER;
      }
      // when variableTreeParent is instance of LambdaExpressionTree we intentionally do not return PARAMETER
      // because parameters of lambda are not supported and if it's not a lambda parameter, then it's a local
      // variable which is also not supported
    }
    return null;
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
      return testNullabilityType(minLevel, ignoreMetaAnnotation, defaultValue, t -> t == NullabilityType.NON_NULL);
    }

    @Override
    public boolean isNullable(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue) {
      return testNullabilityType(minLevel, ignoreMetaAnnotation, defaultValue,
        t -> t == NullabilityType.STRONG_NULLABLE || t == NullabilityType.WEAK_NULLABLE);
    }

    @Override
    public boolean isStrongNullable(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue) {
      return testNullabilityType(minLevel, ignoreMetaAnnotation, defaultValue, t -> t == NullabilityType.STRONG_NULLABLE);
    }

    private boolean testNullabilityType(NullabilityLevel minLevel, boolean ignoreMetaAnnotation, boolean defaultValue, Predicate<NullabilityType> typePredicate) {
      if (type == NullabilityType.UNKNOWN || (ignoreMetaAnnotation && metaAnnotation)) {
        return defaultValue;
      } else if (typePredicate.test(type)) {
        return minLevel.ordinal() <= level.ordinal();
      }
      return false;
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

  static final class JAnnotationInstance implements AnnotationInstance {
    private final JSema sema;
    private final IAnnotationBinding annotationBinding;

    /**
     * Cache for {@link #values()}.
     */
    private List<AnnotationValue> values;

    JAnnotationInstance(JSema sema, IAnnotationBinding annotationBinding) {
      this.sema = sema;
      this.annotationBinding = annotationBinding;
    }

    @Override
    public Symbol symbol() {
      return sema.typeSymbol(annotationBinding.getAnnotationType());
    }

    @Override
    public List<AnnotationValue> values() {
      if (values == null) {
        values = Arrays.stream(annotationBinding.getDeclaredMemberValuePairs())
          .map(p -> new AnnotationValueImpl(p.getName(), convertAnnotationValue(p.getValue())))
          .collect(Collectors.toList());
      }
      return values;
    }

    private Object convertAnnotationValue(Object value) {
      if (value instanceof IVariableBinding) {
        return sema.variableSymbol((IVariableBinding) value);
      } else if (value instanceof ITypeBinding) {
        return sema.typeSymbol((ITypeBinding) value);
      } else if (value instanceof IAnnotationBinding) {
        return sema.annotation((IAnnotationBinding) value);
      } else if (value instanceof Object[]) {
        // Godin: probably better to not modify original array
        Object[] a = (Object[]) value;
        Object[] result = new Object[a.length];
        for (int i = 0; i < a.length; i++) {
          result[i] = convertAnnotationValue(a[i]);
        }
        return result;
      } else {
        return value;
      }
    }
  }

}
