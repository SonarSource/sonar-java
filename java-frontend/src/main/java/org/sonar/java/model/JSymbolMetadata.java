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

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.file.impl.JavaFileManager;
import com.intellij.psi.search.GlobalSearchScope;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.model.JSymbolMetadataNullabilityHelper.getNullabilityDataAtLevel;

final class JSymbolMetadata implements SymbolMetadata {

  private static final NullabilityData[] NO_ANNOTATION_NULLABILITY =
    forEachLevel(level -> new JNullabilityData(NullabilityType.NO_ANNOTATION, level, null, null, false));

  private static final NullabilityData[] UNKNOWN_NULLABILITY =
    forEachLevel(level -> new JNullabilityData(NullabilityType.UNKNOWN, level, null, null, false));

  private final Symbol symbol;
  private final Supplier<List<AnnotationInstance>> annotationSource;
  private final Supplier<List<AnnotationInstance>> allAnnotationsSource;
  private final SymbolMetadata[] parameterMetas;

  /**
   * Cache for {@link #annotations()}.
   */
  private List<AnnotationInstance> annotations;

  /**
   * Cache for {@link #symbolAnnotations()}.
   */
  private List<AnnotationInstance> symbolAnnotations;

  private final Map<NullabilityTarget, NullabilityData> nullabilityCache = new EnumMap<>(NullabilityTarget.class);

  private final boolean isTypeArgMetadata;

  JSymbolMetadata(Symbol symbol, List<AnnotationInstance> annotations) {
    this(symbol, () -> annotations, new JSymbolMetadata[0], false);
  }

  JSymbolMetadata(Symbol symbol, Supplier<List<AnnotationInstance>> annotations,
    JSymbolMetadata[] parameterMetas, boolean isTypeArgMetadata) {
    this.symbol = symbol;
    this.annotationSource = annotations;
    this.parameterMetas = parameterMetas;
    this.isTypeArgMetadata = isTypeArgMetadata;
    List<Supplier<List<AnnotationInstance>>> parameterSources = Arrays.stream(parameterMetas)
      .map(metadata -> metadata.allAnnotationsSource)
      .toList();
    this.allAnnotationsSource = () -> {
      List<AnnotationInstance> all = new ArrayList<>(annotationSource.get());
      parameterSources.forEach(source -> all.addAll(source.get()));
      return all;
    };
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
      annotations = allAnnotationsSource.get();
    }
    return annotations;
  }

  @Override
  public List<AnnotationInstance> symbolAnnotations() {
    if (symbolAnnotations == null) {
      symbolAnnotations = annotationSource.get();
    }
    return symbolAnnotations;
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
  public List<AnnotationValue> valuesForAnnotation(String fullyQualifiedNameOfAnnotation) {
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
    if (declaration != null) {
      return findAnnotationTree(ModifiersUtils.getAnnotations(declaration), annotationInstance);
    }
    return null;
  }

  @Override
  public SymbolMetadata[] parametersMetadata() {
    return parameterMetas;
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
    NullabilityData nullabilityDataAtLevel = getNullabilityDataAtLevel(this, target, currentLevel);
    if (nullabilityDataAtLevel.type() != NullabilityType.NO_ANNOTATION) {
      return nullabilityDataAtLevel;
    }

    // Check nullability from the inheritance hierarchy.
    // Skipped for type argument metadata: type arg nullability must not inherit from the enclosing
    // method's return type annotations, which belong to the outer type, not the type argument.
    if (symbol.isMethodSymbol() && !isTypeArgMetadata) {
      NullabilityData nullabilityDataFromInheritance = getNullabilityDataFromInheritance((Symbol.MethodSymbol) symbol, target);
      if (nullabilityDataFromInheritance.type() != NullabilityType.NO_ANNOTATION) {
        return nullabilityDataFromInheritance;
      }
    }

    // Not annotated or meta annotated, check upper level in the ownership hierarchy...
    if (symbol.isPackageSymbol()) {
      return NO_ANNOTATION_NULLABILITY[currentLevel.ordinal()];
    }
    Symbol owner = getEffectiveOwner(symbol, currentLevel);
    if (owner == null) return unknownNullabilityAt(currentLevel);
    var metadata = owner.metadata();
    return metadata.nullabilityData(target);
  }

  private static NullabilityData getNullabilityDataFromInheritance(Symbol.MethodSymbol methodSymbol, NullabilityTarget target) {
    List<Symbol.MethodSymbol> overriddenSymbols = methodSymbol.overriddenSymbols();
    NullabilityLevel level = NullabilityLevel.METHOD;
    for (Symbol.MethodSymbol overriddenSymbol : overriddenSymbols) {
      SymbolMetadata metadata = overriddenSymbol.metadata();
      NullabilityData nullabilityData = getNullabilityDataAtLevel(metadata, target, level);
      if (nullabilityData.type() != NullabilityType.NO_ANNOTATION && !nullabilityData.equals(unknownNullabilityAt(level))) {
        return nullabilityData;
      }
    }
    return NO_ANNOTATION_NULLABILITY[level.ordinal()];
  }

  @CheckForNull
  private static Symbol getEffectiveOwner(Symbol symbol, NullabilityLevel currentLevel) {
    Symbol owner = symbol.owner();
    if (owner != null && owner.isMethodSymbol() && currentLevel == NullabilityLevel.CLASS) {
      // Owner of anonymous class can be a method symbol, but the nullability of the method is unrelated to the ones from the anonymous classes.
      return getEffectiveOwner(owner, currentLevel);
    }
    return owner;
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
      // If variableTreeParent is not a lambda, we consider that the target is a local variable.
      if (!variableTreeParent.is(Tree.Kind.LAMBDA_EXPRESSION)) {
        return NullabilityTarget.LOCAL_VARIABLE;
      }
      // When variableTreeParent is an instance of LambdaExpressionTree we intentionally return null
      // because parameters of lambdas are not supported.
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
      if (type == NullabilityType.NO_ANNOTATION) {
        return false;
      } else if (type == NullabilityType.UNKNOWN || (ignoreMetaAnnotation && metaAnnotation)) {
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

  static JSymbolMetadata of(JSema sema, Symbol owner, PsiElement element) {
    var annotations = element instanceof PsiPackage pkg ? packageAnnotations(sema, pkg)
      : element instanceof PsiModifierListOwner declaration ? declaration.getAnnotations() : new PsiAnnotation[0];
    com.intellij.psi.PsiType type = element instanceof PsiVariable variable ? variable.getType()
      : element instanceof PsiMethod method ? method.getReturnType() : null;
    if (type != null) {
      type = owner instanceof JMethodSymbol method ? method.substitutor.substitute(type)
        : owner instanceof JVariableSymbol variable ? variable.substitutor.substitute(type) : type;
    }
    return metadata(sema, owner, annotations, type, false);
  }

  private static PsiAnnotation[] packageAnnotations(JSema sema, PsiPackage pkg) {
    var name = pkg.getQualifiedName();
    var classes = JavaFileManager.getInstance(sema.project).findClasses(name.isEmpty() ? "package-info" : name + ".package-info",
      GlobalSearchScope.allScope(sema.project));
    var binary = Arrays.stream(classes).filter(PsiCompiledElement.class::isInstance).findFirst().orElse(null);
    var statement = binary == null ? null : ((PsiJavaFile) binary.getContainingFile()).getPackageStatement();
    return statement == null || statement.getAnnotationList() == null ? new PsiAnnotation[0] : statement.getAnnotationList().getAnnotations();
  }

  private static JSymbolMetadata metadata(JSema sema, Symbol owner, PsiAnnotation[] declarations, com.intellij.psi.PsiType type, boolean argument) {
    List<PsiAnnotation> annotations = new ArrayList<>(Arrays.asList(declarations));
    if (type != null) {
      for (var annotation : type.getAnnotations()) {
        if (!annotations.contains(annotation)) {
          annotations.add(annotation);
        }
      }
    }
    var parameters = type instanceof PsiClassType cls ? Arrays.stream(cls.getParameters())
      .map(t -> metadata(sema, owner, new PsiAnnotation[0], t, true)).toArray(JSymbolMetadata[]::new) : new JSymbolMetadata[0];
    return new JSymbolMetadata(owner, () -> sema.read(() -> annotations.stream()
      .filter(a -> a.getNameReferenceElement() == null || !(a.getNameReferenceElement().resolve() instanceof PsiClass cls) || cls.isAnnotationType())
      .map(a -> (SymbolMetadata.AnnotationInstance) new Annotation(sema, a)).toList()), parameters, argument);
  }

  static Object value(JSema sema, PsiAnnotationMemberValue value) {
    if (value instanceof PsiAnnotation annotation) {
      return new Annotation(sema, annotation);
    }
    if (value instanceof PsiArrayInitializerMemberValue array) {
      return Arrays.stream(array.getInitializers()).map(v -> value(sema, v)).toArray();
    }
    if (value instanceof PsiClassObjectAccessExpression cls) {
      return sema.type(cls.getOperand().getType()).symbol();
    }
    if (value instanceof PsiReferenceExpression reference && reference.resolve() instanceof PsiEnumConstant constant) {
      return sema.symbol(constant);
    }
    return value == null ? null : JavaPsiFacade.getInstance(sema.project).getConstantEvaluationHelper().computeConstantExpression(value);
  }

  static Object value(JSema sema, PsiAnnotationMemberValue value, com.intellij.psi.PsiType expectedType) {
    var result = value(sema, value);
    return result != null && expectedType instanceof PsiArrayType && !(result instanceof Object[]) ? new Object[] {result} : result;
  }

  private static final class Annotation implements SymbolMetadata.AnnotationInstance {
    private final JSema sema;
    private final PsiAnnotation annotation;

    Annotation(JSema sema, PsiAnnotation annotation) {
      this.sema = sema;
      this.annotation = annotation;
    }

    @Override
    public Symbol symbol() {
      return sema.read(() -> {
        var cls = annotation.resolveAnnotationType();
        return cls == null ? annotation.getNameReferenceElement() == null ? Symbol.UNKNOWN_SYMBOL
          : sema.type(JavaPsiFacade.getElementFactory(sema.project).createTypeByFQClassName(annotation.getQualifiedName(), annotation.getResolveScope())).symbol()
          : sema.symbol(cls);
      });
    }

    @Override
    public List<SymbolMetadata.AnnotationValue> values() {
      return sema.read(() -> {
        var declaration = annotation.resolveAnnotationType();
        return Arrays.stream(annotation.getParameterList().getAttributes()).map(attribute -> {
          var name = attribute.getName() == null ? "value" : attribute.getName();
          var methods = declaration == null ? new PsiMethod[0] : declaration.findMethodsByName(name, false);
          var expectedType = methods.length == 0 ? null : methods[0].getReturnType();
          return (SymbolMetadata.AnnotationValue) new AnnotationValueImpl(name, value(sema, attribute.getValue(), expectedType));
        }).toList();
      });
    }
  }
}
