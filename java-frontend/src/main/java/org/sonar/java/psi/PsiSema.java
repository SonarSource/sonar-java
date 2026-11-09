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
package org.sonar.java.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.LambdaUtil;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiCapturedWildcardType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiDisjunctionType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIntersectionType;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiMethodReferenceExpression;
import com.intellij.psi.PsiMethodReferenceUtil;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiRecordComponent;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypeParameterListOwner;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.PsiWildcardType;
import com.intellij.psi.impl.light.LightDefaultConstructor;
import com.intellij.psi.infos.MethodCandidateInfo;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.Tree;

public final class PsiSema implements Sema {
  final Project project;
  final PsiJavaFile file;
  final boolean explicitRuntime;
  private final JavaPsiEnvironment environment;
  private final Runnable cleaner;
  private final Map<String, PsiType> types = new HashMap<>();
  private final Map<String, Symbol> symbols = new HashMap<>();
  private final Map<String, Type> namedTypes = new HashMap<>();
  final Map<String, Tree> declarations = new LinkedHashMap<>();
  final Map<String, List<IdentifierTree>> usages = new LinkedHashMap<>();
  final Map<PsiElement, Boolean> skippedFlowAnalysis = new java.util.IdentityHashMap<>();
  private final Map<String, PsiInitializerBlockSymbol> initializers = new HashMap<>();

  PsiSema(JavaPsiEnvironment environment, Project project, PsiJavaFile file, Runnable cleaner, boolean explicitRuntime) {
    this.environment = environment;
    this.project = project;
    this.file = file;
    this.cleaner = cleaner;
    this.explicitRuntime = explicitRuntime;
  }

  <T> T read(Supplier<T> action) {
    return PsiParserConfig.cancellable(() -> environment.read((unused, files) -> action.get()));
  }

  public Runnable getEnvironmentCleaner() {
    return cleaner;
  }

  @Override
  public Type getClassType(String name) {
    return read(() -> {
      Type cached = namedTypes.get(name);
      if (cached != null) {
        return cached;
      }
      com.intellij.psi.PsiType resolved = resolveType(name);
      Type result = resolved == null ? Type.UNKNOWN : type(resolved);
      namedTypes.put(name, result);
      return result;
    });
  }

  com.intellij.psi.PsiType resolveType(String name) {
    if (name.isEmpty()) {
      return null;
    }
    if (name.endsWith("[]")) {
      var component = resolveType(name.substring(0, name.length() - 2));
      return component == null ? null : component.createArrayType();
    }
    var primitive = switch (name) {
      case "boolean" -> PsiTypes.booleanType();
      case "byte" -> PsiTypes.byteType();
      case "short" -> PsiTypes.shortType();
      case "char" -> PsiTypes.charType();
      case "int" -> PsiTypes.intType();
      case "long" -> PsiTypes.longType();
      case "float" -> PsiTypes.floatType();
      case "double" -> PsiTypes.doubleType();
      case "void" -> PsiTypes.voidType();
      default -> null;
    };
    if (primitive != null) {
      return primitive;
    }
    var cls = JavaPsiFacade.getInstance(project).findClass(name.replace('$', '.'), GlobalSearchScope.allScope(project));
    return cls == null ? null : declarationType(cls);
  }

  PsiClassType declarationType(PsiClass cls) {
    var factory = JavaPsiFacade.getElementFactory(project);
    PsiSubstitutor substitutor = PsiSubstitutor.EMPTY;
    for (var current = cls; current != null; current = current.getContainingClass()) {
      for (var parameter : current.getTypeParameters()) {
        substitutor = substitutor.put(parameter, factory.createType(parameter));
      }
    }
    return factory.createType(cls, substitutor);
  }

  Type type(com.intellij.psi.PsiType value) {
    if (value == null) {
      return Type.UNKNOWN;
    }
    if (value instanceof PsiDisjunctionType disjunction) {
      return type(disjunction.getLeastUpperBound());
    }
    String key = typeKey(value);
    PsiType cached = types.get(key);
    if (cached == null) {
      cached = new PsiType(this, value, key);
      types.put(key, cached);
    }
    return cached;
  }

  List<Type> types(com.intellij.psi.PsiType[] values) {
    return Arrays.stream(values).map(this::type).toList();
  }

  Type recoveredType(com.intellij.psi.PsiType value, String name) {
    String recovered = "Recovered#typeBindingL" + name.replace('.', '/') + ";0";
    return types.computeIfAbsent("recoveredSyntax:" + recovered, unused -> new PsiType(this, value, "recoveredSyntax:" + recovered, recovered));
  }

  /** Structural keys terminate at type-parameter declarations rather than following their recursive bounds. */
  String typeKey(com.intellij.psi.PsiType type) {
    if (type instanceof PsiArrayType array) {
      return "[" + typeKey(array.getComponentType());
    }
    if (type instanceof PsiCapturedWildcardType captured) {
      return "capture:" + key(captured.getContext()) + ":" + key(captured.getTypeParameter()) + ":" + typeKey(captured.getWildcard());
    }
    if (type instanceof PsiWildcardType wildcard) {
      return "wild:" + wildcard.isExtends() + ":" + (wildcard.getBound() == null ? "" : typeKey(wildcard.getBound()));
    }
    if (type instanceof PsiIntersectionType intersection) {
      return "intersection:" + Arrays.stream(intersection.getConjuncts()).map(this::typeKey).sorted().toList();
    }
    if (type instanceof PsiClassType cls) {
      var result = cls.resolveGenerics();
      var declaration = result.getElement();
      if (declaration == null) {
        return "recovered:" + cls.getCanonicalText(false);
      }
      if (declaration instanceof PsiTypeParameter) {
        return "parameter:" + key(declaration);
      }
      return "class:" + key(declaration) + ":" + substitutionKey(result.getSubstitutor(), declaration);
    }
    return type.getCanonicalText(false);
  }

  String substitutionKey(PsiSubstitutor substitutor, PsiTypeParameterListOwner owner) {
    List<String> arguments = new ArrayList<>();
    for (var parameter : owner.getTypeParameters()) {
      var argument = substitutor.substitute(parameter);
      arguments.add(argument == null ? "raw" : typeKey(argument));
    }
    if (owner instanceof PsiClass cls && cls.getContainingClass() != null && !cls.hasModifierProperty(PsiModifier.STATIC)) {
      arguments.add(substitutionKey(substitutor, cls.getContainingClass()));
    }
    return arguments.toString();
  }

  String key(PsiElement element) {
    if (element == null) {
      return "root";
    }
    if (element instanceof PsiPackage pkg) {
      return "package:" + pkg.getQualifiedName();
    }
    if (element instanceof PsiClass cls && cls.getQualifiedName() != null) {
      return "class:" + cls.getQualifiedName();
    }
    if (element instanceof PsiMethod method) {
      return key(method.getContainingClass()) + "#" + method.getName() + ":"
        + Arrays.stream(method.getParameterList().getParameters()).map(p -> p.getType().getCanonicalText(false)).toList();
    }
    if (element instanceof PsiField field) {
      return key(field.getContainingClass()) + ":field:" + field.getName();
    }
    if (element instanceof PsiRecordComponent component) {
      return key(component.getContainingClass()) + ":field:" + component.getName();
    }
    if (element instanceof PsiParameter parameter && parameter.getDeclarationScope() instanceof PsiMethod method) {
      return key(method) + ":argument:" + method.getParameterList().getParameterIndex(parameter);
    }
    if (element instanceof PsiTypeParameter parameter) {
      return key((PsiElement) parameter.getOwner()) + ":parameter:" + parameter.getIndex();
    }
    var containingFile = element.getContainingFile() == null ? null : element.getContainingFile().getOriginalFile();
    String path = containingFile == null ? "" : containingFile.getVirtualFile() == null ? containingFile.getName() : containingFile.getVirtualFile().getPath();
    return path + ":" + element.getTextRange().getStartOffset() + ":" + (element instanceof PsiVariable ? "variable" : element.getClass().getSimpleName());
  }

  Symbol symbol(PsiElement element) {
    if (element == null) {
      return Symbol.UNKNOWN_SYMBOL;
    }
    if (element instanceof PsiParameter parameter && parameter.getDeclarationScope() instanceof PsiLambdaExpression lambda
      && LambdaUtil.getFunctionalInterfaceMethod(lambda) == null) {
      return Symbol.UNKNOWN_SYMBOL;
    }
    if (element instanceof PsiClass cls) {
      return type(declarationType(cls)).symbol();
    }
    if (element instanceof PsiMethod method) {
      return method(method, PsiSubstitutor.EMPTY);
    }
    String key = key(element);
    var cached = symbols.get(key);
    if (cached == null) {
      if (element instanceof PsiVariable variable) {
        return variable(variable, PsiSubstitutor.EMPTY);
      } else if (element instanceof PsiPackage pkg) {
        cached = new PsiPackageSymbol(this, pkg, key);
      } else {
        return Symbol.UNKNOWN_SYMBOL;
      }
      symbols.put(key, cached);
    }
    return cached;
  }

  PsiVariableSymbol variable(PsiVariable variable, PsiSubstitutor substitutor) {
    if (variable instanceof PsiField field) {
      substitutor = completeSubstitutor(substitutor, field.getContainingClass());
    } else if (variable instanceof PsiRecordComponent component) {
      substitutor = completeSubstitutor(substitutor, component.getContainingClass());
    }
    String key = key(variable) + (variable instanceof PsiField field ? ":" + substitutionKey(substitutor, field.getContainingClass())
      : variable instanceof PsiRecordComponent component ? ":" + substitutionKey(substitutor, component.getContainingClass()) : "");
    String cacheKey = variable instanceof PsiRecordComponent ? "component:" + key : key;
    var cached = symbols.get(cacheKey);
    if (cached == null) {
      cached = new PsiVariableSymbol(this, variable, substitutor, key);
      symbols.put(cacheKey, cached);
    }
    return (PsiVariableSymbol) cached;
  }

  PsiTypeSymbol typeSymbol(PsiType type) {
    String key = "type:" + type.key;
    var cached = symbols.get(key);
    if (cached == null) {
      cached = new PsiTypeSymbol(this, type, key);
      symbols.put(key, cached);
    }
    return (PsiTypeSymbol) cached;
  }

  Symbol.MethodSymbol method(JavaResolveResult result) {
    return result.isValidResult() && result.getElement() instanceof PsiMethod method
      ? method(method, result.getSubstitutor()) : Symbol.MethodSymbol.UNKNOWN_METHOD;
  }

  Symbol.MethodSymbol method(PsiMethodReferenceExpression reference) {
    var targetType = reference.getFunctionalInterfaceType();
    if (targetType == null || !reference.isPotentiallyCompatible(targetType)) {
      return Symbol.MethodSymbol.UNKNOWN_METHOD;
    }
    var result = reference.advancedResolve(false);
    var candidates = result.isValidResult() ? new JavaResolveResult[] {result} : reference.multiResolve(false);
    if (candidates.length == 1 && candidates[0].getElement() instanceof PsiMethod method
      && PsiMethodReferenceUtil.isReturnTypeCompatible(reference, candidates[0], targetType)) {
      return method(method, candidates[0].getSubstitutor());
    }
    return Symbol.MethodSymbol.UNKNOWN_METHOD;
  }

  Symbol.MethodSymbol method(PsiMethodCallExpression call) {
    var qualifier = call.getMethodExpression().getQualifierExpression();
    if (qualifier != null && (qualifier.getType() != null && type(qualifier.getType()).isUnknown() && !(qualifier instanceof PsiClassObjectAccessExpression)
      || qualifier instanceof PsiMethodCallExpression nested && method(nested).isUnknown())) {
      return Symbol.MethodSymbol.UNKNOWN_METHOD;
    }
    var resolved = call.resolveMethodGenerics();
    if (resolved.isValidResult()) {
      return method(resolved);
    }
    var candidates = call.getMethodExpression().multiResolve(false);
    if (candidates.length == 1 && candidates[0].getElement() instanceof PsiMethod method
      && (method.isVarArgs() || method.getParameterList().getParametersCount() == call.getArgumentList().getExpressionCount())) {
      return method(method, candidates[0].getSubstitutor());
    }
    var applicable = Arrays.stream(candidates)
      .filter(result -> result.isAccessible() && result.isStaticsScopeCorrect()
        && result instanceof MethodCandidateInfo candidate && (candidate.isApplicable() || compatibleKnownArguments(call, candidate)))
      .min(Comparator.comparingInt(result -> result.getElement() instanceof PsiCompiledElement ? Integer.MAX_VALUE : result.getElement().getTextOffset()))
      .map(result -> (Symbol.MethodSymbol) method((PsiMethod) result.getElement(), result.getSubstitutor()))
      .orElse(Symbol.MethodSymbol.UNKNOWN_METHOD);
    if (!applicable.isUnknown()) {
      return applicable;
    }
    return Arrays.stream(candidates)
      .filter(result -> result.isAccessible() && result.isStaticsScopeCorrect() && result.getElement() instanceof PsiMethod method
        && method.getParameterList().getParametersCount() == call.getArgumentList().getExpressionCount())
      .min(Comparator.comparingInt(result -> declarationOrder((PsiMethod) result.getElement())))
      .map(result -> (Symbol.MethodSymbol) method((PsiMethod) result.getElement(), result.getSubstitutor()))
      .orElse(Symbol.MethodSymbol.UNKNOWN_METHOD);
  }

  private static int declarationOrder(PsiMethod method) {
    return Arrays.asList(method.getContainingClass().getMethods()).indexOf(method);
  }

  private boolean compatibleKnownArguments(com.intellij.psi.PsiCall call, JavaResolveResult result) {
    if (!(result.getElement() instanceof PsiMethod method)) {
      return false;
    }
    var arguments = call.getArgumentList().getExpressions();
    var parameters = method.getParameterList().getParameters();
    if (arguments.length != parameters.length || Arrays.stream(arguments).noneMatch(argument -> type(argument.getType()).isUnknown())) {
      return false;
    }
    for (int i = 0; i < arguments.length; i++) {
      var actual = arguments[i].getType();
      if (!type(actual).isUnknown() && !com.intellij.psi.util.TypeConversionUtil.isAssignable(result.getSubstitutor().substitute(parameters[i].getType()), actual)) {
        return false;
      }
    }
    return true;
  }

  Symbol.MethodSymbol constructor(PsiNewExpression expression) {
    var result = expression.resolveMethodGenerics();
    if (result.isValidResult()) {
      return method(result);
    }
    var recovered = Arrays.stream(JavaPsiFacade.getInstance(project).getResolveHelper().getReferencedMethodCandidates(expression, false))
      .filter(candidate -> candidate.getElement() instanceof PsiMethod method && expression.getArgumentList() != null
        && method.getParameterList().getParametersCount() == expression.getArgumentList().getExpressionCount())
      .filter(candidate -> compatibleKnownArguments(expression, candidate))
      .min(Comparator.comparingInt(candidate -> declarationOrder((PsiMethod) candidate.getElement())))
      .map(candidate -> (Symbol.MethodSymbol) method((PsiMethod) candidate.getElement(), candidate.getSubstitutor()))
      .orElse(Symbol.MethodSymbol.UNKNOWN_METHOD);
    if (!recovered.isUnknown()) {
      return recovered;
    }
    if (expression.getAnonymousClass() != null && expression.getAnonymousClass().getBaseClassType().resolve() instanceof PsiClass base
      && base.isInterface()) {
      var object = getClassType("java.lang.Object").symbol();
      return object.lookupSymbols("<init>").stream().filter(Symbol.MethodSymbol.class::isInstance)
        .map(Symbol.MethodSymbol.class::cast).filter(method -> method.parameterTypes().isEmpty()).findFirst().orElse(Symbol.MethodSymbol.UNKNOWN_METHOD);
    }
    var reference = expression.getClassReference();
    var resolved = reference == null ? JavaResolveResult.EMPTY : reference.advancedResolve(false);
    if (resolved.isValidResult() && resolved.getElement() instanceof PsiClass cls && expression.getArgumentList() != null
      && expression.getArgumentList().isEmpty()) {
      var constructor = LightDefaultConstructor.create(cls);
      if (constructor != null) {
        return method(constructor, resolved.getSubstitutor());
      }
    }
    return Symbol.MethodSymbol.UNKNOWN_METHOD;
  }

  PsiMethodSymbol method(PsiMethod method, PsiSubstitutor substitutor) {
    substitutor = completeSubstitutor(substitutor, method);
    String key = key(method) + ":" + substitutionKey(substitutor, method) + ":" + substitutionKey(substitutor, method.getContainingClass());
    var cached = symbols.get(key);
    if (cached == null) {
      cached = new PsiMethodSymbol(this, method, substitutor, null, key);
      symbols.put(key, cached);
    }
    return (PsiMethodSymbol) cached;
  }

  private PsiSubstitutor completeSubstitutor(PsiSubstitutor substitutor, PsiTypeParameterListOwner owner) {
    var factory = JavaPsiFacade.getElementFactory(project);
    for (var parameter : owner.getTypeParameters()) {
      if (!substitutor.getSubstitutionMap().containsKey(parameter)) {
        substitutor = substitutor.put(parameter, factory.createType(parameter));
      }
    }
    var parent = owner instanceof PsiMethod method ? method.getContainingClass() : ((PsiClass) owner).getContainingClass();
    return parent == null ? substitutor : completeSubstitutor(substitutor, parent);
  }

  Symbol.MethodSymbol lambda(PsiLambdaExpression lambda) {
    String key = "lambda:" + key(lambda);
    var cached = symbols.get(key);
    if (cached == null) {
      var method = LambdaUtil.getFunctionalInterfaceMethod(lambda);
      if (method == null) {
        return Symbol.MethodSymbol.UNKNOWN_METHOD;
      }
      var functionalType = LambdaUtil.normalizeFunctionalType(lambda.getFunctionalInterfaceType());
      var substitutor = LambdaUtil.getSubstitutor(method, ((PsiClassType) functionalType).resolveGenerics());
      cached = new PsiMethodSymbol(this, method, substitutor, lambda, key);
      symbols.put(key, cached);
    }
    return (Symbol.MethodSymbol) cached;
  }

  Symbol owner(PsiElement element) {
    if (element instanceof PsiParameter parameter) {
      var scope = parameter.getDeclarationScope();
      if (scope instanceof PsiLambdaExpression lambda) {
        return lambda(lambda);
      }
      if (scope instanceof PsiMethod method) {
        return symbol(method);
      }
    }
    for (var parent = element.getParent(); parent != null; parent = parent.getParent()) {
      if (parent instanceof PsiLambdaExpression lambda) {
        var owner = lambda(lambda);
        if (!owner.isUnknown()) {
          return owner;
        }
        continue;
      }
      if (parent instanceof PsiMethod || parent instanceof PsiClass) {
        return symbol(parent);
      }
      if (parent instanceof PsiClassInitializer initializer) {
        var cls = (Symbol.TypeSymbol) symbol(initializer.getContainingClass());
        boolean isStatic = initializer.hasModifierProperty(PsiModifier.STATIC);
        return initializer(cls, isStatic);
      }
      if (parent instanceof PsiJavaFile javaFile) {
        return symbol(JavaPsiFacade.getInstance(project).findPackage(javaFile.getPackageName()));
      }
    }
    return Symbol.ROOT_PACKAGE;
  }

  PsiInitializerBlockSymbol initializer(Symbol.TypeSymbol owner, boolean isStatic) {
    return initializers.computeIfAbsent(((PsiSymbol) owner).key + isStatic, unused -> new PsiInitializerBlockSymbol(owner, isStatic));
  }

  String binaryName(PsiClass cls) {
    if (cls.getContainingClass() != null) {
      return binaryName(cls.getContainingClass()) + "$" + cls.getName();
    }
    if (cls.getQualifiedName() != null) {
      return cls.getQualifiedName();
    }
    var outer = PsiTreeUtil.getParentOfType(cls, PsiClass.class, true);
    if (outer == null) {
      return cls.getName() == null ? "" : cls.getName();
    }
    int ordinal = 0;
    for (var candidate : PsiTreeUtil.findChildrenOfType(outer, PsiClass.class)) {
      if (candidate instanceof PsiAnonymousClass || candidate.getQualifiedName() == null) {
        ordinal++;
      }
      if (candidate == cls) {
        break;
      }
    }
    return binaryName(outer) + "$" + ordinal + (cls.getName() == null ? "" : cls.getName());
  }
}
