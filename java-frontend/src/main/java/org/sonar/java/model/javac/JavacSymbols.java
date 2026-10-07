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

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ArrayType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

/** Creates SonarJava symbol views over javac elements from one compilation task. */
public final class JavacSymbols {

  private final Types types;
  private final Elements elements;
  private final Map<Element, Symbol> symbols = new IdentityHashMap<>();
  private final Map<String, Element> recoveredTypes = new HashMap<>();
  private final Map<TypeMirror, JavacType> typeViews = new IdentityHashMap<>();
  private final Map<TypeMirror, Symbol.TypeSymbol> syntheticTypes = new IdentityHashMap<>();
  private final Map<Element, Tree> declarations = new IdentityHashMap<>();
  private final Map<Element, List<IdentifierTree>> usages = new IdentityHashMap<>();
  private final Map<Element, Boolean> effectivelyFinal = new IdentityHashMap<>();

  public JavacSymbols(Types types, Elements elements) {
    this.types = Objects.requireNonNull(types);
    this.elements = Objects.requireNonNull(elements);
  }

  JavacSymbols(Types types, Elements elements, Trees trees, CompilationUnitTree unit) {
    this(types, elements);
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitAssignment(AssignmentTree assignment, Void unused) {
        markWrite(assignment.getVariable());
        return super.visitAssignment(assignment, unused);
      }

      @Override
      public Void visitCompoundAssignment(CompoundAssignmentTree assignment, Void unused) {
        markWrite(assignment.getVariable());
        return super.visitCompoundAssignment(assignment, unused);
      }

      @Override
      public Void visitUnary(UnaryTree unary, Void unused) {
        switch (unary.getKind()) {
          case PREFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_INCREMENT, POSTFIX_DECREMENT -> markWrite(unary.getExpression());
          default -> { }
        }
        return super.visitUnary(unary, unused);
      }

      private void markWrite(ExpressionTree expression) {
        Element element = trees.getElement(new TreePath(getCurrentPath(), expression));
        if (element != null) {
          effectivelyFinal.put(element, false);
        }
      }
    }.scan(unit, null);
  }

  public Symbol symbol(@Nullable Element element) {
    if (element == null) {
      return Symbol.UNKNOWN_SYMBOL;
    }
    if (element instanceof PackageElement packageElement && packageElement.isUnnamed()) {
      return Symbol.ROOT_PACKAGE;
    }
    element = canonical(element);
    return symbols.computeIfAbsent(element, this::create);
  }

  private Element canonical(Element element) {
    if (element instanceof TypeElement typeElement && element.asType().getKind() == TypeKind.ERROR) {
      String qualifiedName = typeElement.getQualifiedName().toString();
      String key = qualifiedName.isEmpty() ? typeElement.getSimpleName().toString() : qualifiedName;
      return recoveredTypes.computeIfAbsent(key, unused -> typeElement);
    }
    return element;
  }

  public Type type(TypeMirror mirror) {
    return typeViews.computeIfAbsent(mirror, key -> new JavacType(key, types, elements, this));
  }

  Symbol.TypeSymbol syntheticType(TypeMirror mirror) {
    return syntheticTypes.computeIfAbsent(mirror, unused -> new JavacSyntheticTypeSymbol(type(mirror)));
  }

  Symbol.TypeSymbol instantiatedType(DeclaredType mirror) {
    return new TypeView((TypeElement) mirror.asElement(), mirror);
  }

  public void registerDeclaration(Element element, Tree declaration) {
    element = canonical(element);
    declarations.put(element, declaration);
    IdentifierTree name = declarationName(declaration);
    if (name != null) {
      usages.computeIfPresent(element, (unused, references) -> {
        references.removeIf(reference -> reference == name);
        return references;
      });
    }
  }

  public void registerUsage(Element element, IdentifierTree usage) {
    element = canonical(element);
    if (usage != declarationName(declarations.get(element))) {
      List<IdentifierTree> references = usages.computeIfAbsent(element, unused -> new ArrayList<>());
      if (references.stream().noneMatch(reference -> reference == usage)) {
        references.add(usage);
      }
    }
  }

  @Nullable
  static IdentifierTree declarationName(@Nullable Tree declaration) {
    if (declaration instanceof ClassTree type) {
      return type.simpleName();
    }
    if (declaration instanceof MethodTree method) {
      return method.simpleName();
    }
    if (declaration instanceof VariableTree variable) {
      return variable.simpleName();
    }
    return null;
  }

  @Nullable
  public static Object defaultValue(Symbol.MethodSymbol method) {
    if (method instanceof JavacSymbols.MethodView view) {
      javax.lang.model.element.AnnotationValue value = view.method.getDefaultValue();
      return value == null ? null : value.getValue();
    }
    return null;
  }

  private Symbol create(Element element) {
    if (element instanceof TypeElement typeElement) {
      return new TypeView(typeElement);
    }
    if (element instanceof TypeParameterElement typeParameter) {
      return new TypeParameterView(typeParameter);
    }
    if (element instanceof ExecutableElement executableElement) {
      return new MethodView(executableElement);
    }
    if (element instanceof VariableElement variableElement) {
      return new VariableView(variableElement);
    }
    if (element instanceof RecordComponentElement recordComponentElement) {
      return new VariableView(recordComponentElement);
    }
    if (element instanceof PackageElement packageElement) {
      return new PackageView(packageElement);
    }
    return Symbol.UNKNOWN_SYMBOL;
  }

  private abstract class BaseView implements Symbol {
    final Element element;
    private SymbolMetadata metadata;
    private Type type;

    BaseView(Element element) {
      this.element = element;
    }

    @Override
    public boolean equals(Object object) {
      return object instanceof JavacSymbols.BaseView view && element.equals(view.element);
    }

    @Override
    public int hashCode() {
      return element.hashCode();
    }

    @Override
    public String name() {
      return element.getSimpleName().toString();
    }

    @Nullable
    @Override
    public Symbol owner() {
      Element owner = element.getEnclosingElement();
      return owner == null ? null : symbol(owner);
    }

    @Override
    public Type type() {
      if (type == null) {
        type = JavacSymbols.this.type(element.asType());
      }
      return type;
    }

    @Override
    public boolean isVariableSymbol() {
      return this instanceof VariableView;
    }

    @Override
    public boolean isTypeSymbol() {
      return this instanceof TypeView || this instanceof TypeParameterView;
    }

    @Override
    public boolean isMethodSymbol() {
      return this instanceof MethodView;
    }

    @Override
    public boolean isPackageSymbol() {
      return this instanceof PackageView;
    }

    @Override
    public boolean isStatic() {
      return element.getModifiers().contains(Modifier.STATIC);
    }

    @Override
    public boolean isFinal() {
      return element.getModifiers().contains(Modifier.FINAL);
    }

    @Override
    public boolean isEnum() {
      return element.getKind() == ElementKind.ENUM || element.getKind() == ElementKind.ENUM_CONSTANT;
    }

    @Override
    public boolean isInterface() {
      return element.getKind() == ElementKind.INTERFACE;
    }

    @Override
    public boolean isAbstract() {
      return element.getModifiers().contains(Modifier.ABSTRACT);
    }

    @Override
    public boolean isPublic() {
      return element.getModifiers().contains(Modifier.PUBLIC);
    }

    @Override
    public boolean isPrivate() {
      return element.getModifiers().contains(Modifier.PRIVATE);
    }

    @Override
    public boolean isProtected() {
      return element.getModifiers().contains(Modifier.PROTECTED);
    }

    @Override
    public boolean isPackageVisibility() {
      return !(isPublic() || isPrivate() || isProtected());
    }

    @Override
    public boolean isDeprecated() {
      return elements.isDeprecated(element);
    }

    @Override
    public boolean isVolatile() {
      return element.getModifiers().contains(Modifier.VOLATILE);
    }

    @Override
    public boolean isUnknown() {
      return element.asType().getKind() == TypeKind.ERROR;
    }

    @Override
    public SymbolMetadata metadata() {
      if (metadata == null) {
        metadata = new JavacSymbolMetadata(JavacSymbols.this, this, element);
      }
      return metadata;
    }

    @Nullable
    @Override
    public TypeSymbol enclosingClass() {
      if (this instanceof TypeView typeView) {
        return typeView;
      }
      for (Element current = element.getEnclosingElement(); current != null; current = current.getEnclosingElement()) {
        if (current instanceof TypeElement) {
          return (TypeSymbol) symbol(current);
        }
      }
      return null;
    }

    @Override
    public List<IdentifierTree> usages() {
      return List.copyOf(usages.getOrDefault(element, List.of()));
    }

    @Nullable
    @Override
    public org.sonar.plugins.java.api.tree.Tree declaration() {
      return declarations.get(element);
    }
  }

  private final class PackageView extends BaseView {
    PackageView(PackageElement element) {
      super(element);
    }

    @Override
    public String name() {
      return ((PackageElement) element).getQualifiedName().toString();
    }

    @Nullable
    @Override
    public Symbol owner() {
      return Symbol.ROOT_PACKAGE;
    }

    @Override
    public Type type() {
      return Type.UNKNOWN;
    }
  }

  private final class TypeParameterView extends BaseView implements Symbol.TypeSymbol {
    TypeParameterView(TypeParameterElement element) {
      super(element);
    }

    @Override
    public Type superClass() {
      return type().isUnknown() ? null : type().erasure();
    }

    @Override
    public List<Type> interfaces() {
      return List.of();
    }

    @Override
    public Collection<Symbol> memberSymbols() {
      return List.of();
    }

    @Override
    public Collection<Symbol> lookupSymbols(String name) {
      return List.of();
    }

    @Nullable
    @Override
    public ClassTree declaration() {
      return null;
    }

    @Override
    public Set<Type> superTypes() {
      return superClass() == null ? Set.of() : Set.of(superClass());
    }

    @Override
    public Symbol.TypeSymbol outermostClass() {
      Symbol.TypeSymbol enclosing = enclosingClass();
      return enclosing == null ? this : enclosing.outermostClass();
    }

    @Override
    public boolean isAnnotation() {
      return false;
    }
  }

  private final class TypeView extends BaseView implements Symbol.TypeSymbol {
    private final TypeElement typeElement;
    private final TypeMirror mirror;

    TypeView(TypeElement element) {
      this(element, element.asType());
    }

    TypeView(TypeElement element, TypeMirror mirror) {
      super(element);
      typeElement = element;
      this.mirror = mirror;
    }

    @Override
    public Type type() {
      return JavacSymbols.this.type(mirror);
    }

    @Nullable
    @Override
    public Type superClass() {
      TypeMirror superclass = typeElement.getSuperclass();
      return superclass.getKind() == TypeKind.NONE ? null : JavacSymbols.this.type(superclass);
    }

    @Override
    public List<Type> interfaces() {
      return typeElement.getInterfaces().stream().map(JavacSymbols.this::type).toList();
    }

    @Override
    public Collection<Symbol> memberSymbols() {
      var members = typeElement.getEnclosedElements().stream();
      if (typeElement.getKind() == ElementKind.RECORD) {
        members = members.sorted(Comparator.comparingInt(member -> {
          if (member.getKind() == ElementKind.METHOD && !declarations.containsKey(member)) {
            return 0;
          }
          return member.getKind() == ElementKind.METHOD ? 2 : 1;
        }));
      }
      return members.map(JavacSymbols.this::symbol).toList();
    }

    @Override
    public Collection<Symbol> lookupSymbols(String name) {
      return typeElement.getEnclosedElements().stream()
        .filter(member -> member.getSimpleName().contentEquals(name))
        .map(JavacSymbols.this::symbol)
        .toList();
    }

    @Nullable
    @Override
    public ClassTree declaration() {
      return (ClassTree) declarations.get(element);
    }

    @Override
    public Set<Type> superTypes() {
      Set<Type> result = new LinkedHashSet<>();
      collectSuperTypes(mirror, result);
      return result;
    }

    private void collectSuperTypes(TypeMirror current, Set<Type> result) {
      for (Type resolved : ((JavacType) JavacSymbols.this.type(current)).directSupertypes()) {
        if (result.add(resolved)) {
          if (resolved instanceof JavacType javacType && !resolved.isUnknown()) {
            collectSuperTypes(javacType.mirror(), result);
          }
        }
      }
    }

    @Override
    public Symbol.TypeSymbol outermostClass() {
      TypeView current = this;
      while (current.owner() instanceof TypeView parent) {
        current = parent;
      }
      return current;
    }

    @Override
    public boolean isAnnotation() {
      return typeElement.getKind() == ElementKind.ANNOTATION_TYPE;
    }
  }

  private final class VariableView extends BaseView implements Symbol.VariableSymbol {
    @Nullable
    private final VariableElement variableElement;

    VariableView(Element element) {
      super(element);
      variableElement = element instanceof VariableElement variable ? variable : null;
    }

    @Nullable
    @Override
    public VariableTree declaration() {
      return (VariableTree) declarations.get(element);
    }

    @Override
    public boolean isUnknown() {
      return false;
    }

    @Override
    public boolean isEffectivelyFinal() {
      return isFinal() || (declaration() != null && effectivelyFinal.getOrDefault(element, true));
    }

    @Override
    public Optional<Object> constantValue() {
      if (!isFinal() || !isStatic()) {
        return Optional.empty();
      }
      Object value = variableElement == null ? null : variableElement.getConstantValue();
      if (value instanceof Character character) {
        return Optional.of((int) character);
      }
      if (value instanceof Byte number) {
        return Optional.of(number.intValue());
      }
      if (value instanceof Short number) {
        return Optional.of(number.intValue());
      }
      return Optional.ofNullable(value);
    }

    @Override
    public boolean isLocalVariable() {
      return switch (element.getKind()) {
        case LOCAL_VARIABLE, RESOURCE_VARIABLE, EXCEPTION_PARAMETER, BINDING_VARIABLE, PARAMETER -> true;
        default -> false;
      };
    }

    @Override
    public boolean isParameter() {
      return element.getKind() == ElementKind.PARAMETER;
    }
  }

  private final class MethodView extends BaseView implements Symbol.MethodSymbol {
    private final ExecutableElement method;

    MethodView(ExecutableElement element) {
      super(element);
      method = element;
    }

    @Override
    public Type type() {
      return Type.UNKNOWN;
    }

    @Override
    public List<Type> parameterTypes() {
      return method.getParameters().stream().map(Element::asType).map(JavacSymbols.this::type).toList();
    }

    @Override
    public List<Symbol> declarationParameters() {
      return method.getParameters().stream().map(JavacSymbols.this::symbol).toList();
    }

    @Override
    public TypeSymbol returnType() {
      if (method.getReturnType().getKind() == TypeKind.DECLARED) {
        Element returnElement = types.asElement(method.getReturnType());
        if (returnElement instanceof TypeElement) {
          return (TypeSymbol) symbol(returnElement);
        }
      }
      return syntheticType(method.getReturnType());
    }

    @Override
    public List<Type> thrownTypes() {
      return method.getThrownTypes().stream().map(JavacSymbols.this::type).toList();
    }

    @Override
    public List<Symbol.MethodSymbol> overriddenSymbols() {
      if (!(method.getEnclosingElement() instanceof TypeElement ownerType)) {
        return List.of();
      }
      List<Symbol.MethodSymbol> result = new ArrayList<>();
      collectOverridden(ownerType.asType(), ownerType, result, new LinkedHashSet<>());
      return result;
    }

    private void collectOverridden(TypeMirror current, TypeElement ownerType, List<Symbol.MethodSymbol> result,
      Set<TypeMirror> visited) {
      if (!visited.add(current)) {
        return;
      }
      for (TypeMirror supertype : types.directSupertypes(current)) {
        Element parent = types.asElement(supertype);
        if (parent instanceof TypeElement parentType) {
          for (Element candidate : parentType.getEnclosedElements()) {
            if (candidate instanceof ExecutableElement executable && elements.overrides(method, executable, ownerType)) {
              result.add((Symbol.MethodSymbol) symbol(executable));
            }
          }
          collectOverridden(supertype, ownerType, result, visited);
        }
      }
    }

    @Nullable
    @Override
    public MethodTree declaration() {
      return (MethodTree) declarations.get(element);
    }

    @Override
    public String signature() {
      String parameters = method.getParameters().stream()
        .map(parameter -> descriptor(parameter.asType()))
        .collect(java.util.stream.Collectors.joining());
      return owner().type().fullyQualifiedName() + "#" + name() + "(" + parameters + ")"
        + descriptor(method.getReturnType());
    }

    private String descriptor(TypeMirror mirror) {
      return switch (mirror.getKind()) {
        case BOOLEAN -> "Z";
        case BYTE -> "B";
        case SHORT -> "S";
        case INT -> "I";
        case LONG -> "J";
        case CHAR -> "C";
        case FLOAT -> "F";
        case DOUBLE -> "D";
        case VOID, NONE -> "V";
        case ARRAY -> "[" + descriptor(((ArrayType) mirror).getComponentType());
        default -> {
          Element typeElement = types.asElement(types.erasure(mirror));
          String name = typeElement instanceof TypeElement declared
            ? elements.getBinaryName(declared).toString()
            : "java.lang.Object";
          yield "L" + name.replace('.', '/') + ";";
        }
      };
    }

    @Override
    public boolean isOverridable() {
      return !isPrivate() && !isStatic() && !isFinal() && !owner().isFinal();
    }

    @Override
    public boolean isParametrizedMethod() {
      return !method.getTypeParameters().isEmpty();
    }

    @Override
    public boolean isDefaultMethod() {
      return method.isDefault();
    }

    @Override
    public boolean isSynchronizedMethod() {
      return method.getModifiers().contains(Modifier.SYNCHRONIZED);
    }

    @Override
    public boolean isVarArgsMethod() {
      return method.isVarArgs();
    }

    @Override
    public boolean isNativeMethod() {
      return method.getModifiers().contains(Modifier.NATIVE);
    }
  }
}
