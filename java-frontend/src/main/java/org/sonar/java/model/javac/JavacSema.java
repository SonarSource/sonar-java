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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.sonar.java.model.JProblem;
import org.sonar.java.model.Symbols;
import org.sonar.plugins.java.api.semantic.Sema;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.Tree;

public final class JavacSema implements Sema {

  private final Trees trees;
  private final JavacParser.Result result;
  private final Elements elements;
  private final Types types;
  private final JavacSymbols symbols;
  private final Map<Tree, TreePath> paths = new IdentityHashMap<>();
  private final Map<Tree, Symbol.VariableSymbol> keywordSymbols = new IdentityHashMap<>();

  public JavacSema(JavacParser.Result result) {
    this.result = result;
    this.trees = result.trees();
    this.elements = result.task().getElements();
    this.types = result.task().getTypes();
    this.symbols = new JavacSymbols(types, elements, trees, result.unit());
  }

  @Override
  public Type getClassType(String fullyQualifiedName) {
    if (fullyQualifiedName.isEmpty()) {
      return Type.UNKNOWN;
    }
    int dimensions = 0;
    String name = fullyQualifiedName;
    while (name.endsWith("[]")) {
      name = name.substring(0, name.length() - 2);
      dimensions++;
    }
    TypeMirror mirror = primitiveType(name);
    if (mirror == null) {
      TypeElement element = elements.getTypeElement(name.replace('$', '.'));
      if (element == null) {
        return Type.UNKNOWN;
      }
      mirror = element.asType();
    }
    for (int i = 0; i < dimensions; i++) {
      mirror = types.getArrayType(mirror);
    }
    return symbols.type(mirror);
  }

  public Type type(TreePath path) {
    TypeMirror mirror = trees.getTypeMirror(path);
    return mirror == null || mirror.getKind() == TypeKind.ERROR ? Type.UNKNOWN : symbols.type(mirror);
  }

  public Symbol symbol(TreePath path) {
    return symbols.symbol(trees.getElement(path));
  }

  public Type typeFor(Tree tree) {
    TreePath path = paths.get(tree);
    if (path == null) {
      return Type.UNKNOWN;
    }
    Type resolved = type(path);
    if (resolved.isUnknown() && tree instanceof org.sonar.plugins.java.api.tree.AnnotationTree
      && path.getLeaf() instanceof com.sun.source.tree.AnnotationTree annotation) {
      return type(new TreePath(path, annotation.getAnnotationType()));
    }
    return resolved;
  }

  public Symbol symbolFor(Tree tree) {
    TreePath path = paths.get(tree);
    if (path == null) {
      return Symbol.UNKNOWN_SYMBOL;
    }
    if (tree instanceof org.sonar.plugins.java.api.tree.ImportTree
      && path.getLeaf() instanceof com.sun.source.tree.ImportTree importTree) {
      return symbol(new TreePath(path, importTree.getQualifiedIdentifier()));
    }
    if (tree instanceof IdentifierTree
      && tree.parent() instanceof org.sonar.plugins.java.api.tree.MemberSelectExpressionTree memberSelect
      && memberSelect.identifier() == tree
      && memberSelect.parent() instanceof org.sonar.plugins.java.api.tree.MethodInvocationTree invocation
      && invocation.methodSelect() == memberSelect) {
      Symbol invoked = symbolFor(invocation);
      if (!invoked.isUnknown()) {
        return invoked;
      }
    }
    if (tree instanceof org.sonar.plugins.java.api.tree.MethodInvocationTree
      && path.getLeaf() instanceof com.sun.source.tree.MethodInvocationTree invocation
      && invocation.getMethodSelect() instanceof com.sun.source.tree.MemberSelectTree select
      && trees.getElement(path) instanceof ExecutableElement method) {
      TreePath selection = new TreePath(path, invocation.getMethodSelect());
      TypeMirror receiver = trees.getTypeMirror(new TreePath(selection, select.getExpression()));
      if (receiver instanceof DeclaredType declared) {
        TypeMirror selectedType = trees.getTypeMirror(selection);
        return symbols.methodAt(method, declared,
          selectedType instanceof ExecutableType executable ? executable : null);
      }
    }
    if (tree instanceof org.sonar.plugins.java.api.tree.NewClassTree
      && path.getLeaf() instanceof com.sun.source.tree.NewClassTree
      && trees.getElement(path) instanceof ExecutableElement constructor
      && trees.getTypeMirror(path) instanceof DeclaredType declared) {
      return symbols.methodAt(constructor, declared, null);
    }
    Symbol resolved = symbol(path);
    if (resolved.isUnknown() && path.getLeaf() instanceof com.sun.source.tree.MethodInvocationTree invocation) {
      resolved = symbol(new TreePath(path, invocation.getMethodSelect()));
      if (resolved.isUnknown() && invocation.getMethodSelect() instanceof com.sun.source.tree.MemberSelectTree select) {
        TypeMirror receiver = trees.getTypeMirror(new TreePath(new TreePath(path, invocation.getMethodSelect()), select.getExpression()));
        Element owner = receiver == null ? null : types.asElement(receiver);
        if (owner instanceof TypeElement type) {
          return elements.getAllMembers(type).stream()
            .filter(member -> member.getKind() == ElementKind.METHOD)
            .map(ExecutableElement.class::cast)
            .filter(method -> method.getSimpleName().contentEquals(select.getIdentifier()))
            .filter(method -> method.getParameters().size() == invocation.getArguments().size())
            .map(symbols::symbol)
            .findFirst().orElse(Symbol.UNKNOWN_SYMBOL);
        }
      }
    }
    return resolved;
  }

  public Symbol keywordSymbolFor(Tree tree, String name) {
    if (!paths.containsKey(tree)) {
      return Symbol.UNKNOWN_SYMBOL;
    }
    if (tree.parent() instanceof org.sonar.plugins.java.api.tree.MethodInvocationTree invocation
      && invocation.methodSelect() == tree) {
      return symbolFor(invocation);
    }
    return keywordSymbols.computeIfAbsent(tree, unused -> new KeywordSymbol(name, typeFor(tree)));
  }

  public Symbol.MethodSymbol lambdaMethodSymbolFor(Tree tree) {
    TreePath path = paths.get(tree);
    if (path == null) {
      return Symbol.MethodSymbol.UNKNOWN_METHOD;
    }
    TypeMirror target = trees.getTypeMirror(path);
    if (target == null || target.getKind() == TypeKind.ERROR) {
      return Symbol.MethodSymbol.UNKNOWN_METHOD;
    }
    var owner = types.asElement(target);
    if (!(owner instanceof TypeElement typeElement)) {
      return Symbol.MethodSymbol.UNKNOWN_METHOD;
    }
    return elements.getAllMembers(typeElement).stream()
      .filter(member -> member.getKind() == ElementKind.METHOD)
      .map(ExecutableElement.class::cast)
      .filter(method -> method.getModifiers().contains(Modifier.ABSTRACT))
      .map(method -> target instanceof DeclaredType declared
        ? symbols.methodAt(method, declared, null) : symbols.symbol(method))
      .filter(Symbol.MethodSymbol.class::isInstance)
      .map(Symbol.MethodSymbol.class::cast)
      .findFirst()
      .orElse(Symbol.MethodSymbol.UNKNOWN_METHOD);
  }

  public boolean hasDuplicateDeclarationError(Tree tree) {
    TreePath path = paths.get(tree);
    if (path == null) {
      return false;
    }
    long start = result.sourcePositions().getStartPosition(result.unit(), path.getLeaf());
    long end = result.sourcePositions().getEndPosition(result.unit(), path.getLeaf());
    return result.diagnostics().stream()
      .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
      .filter(diagnostic -> diagnostic.getCode().contains("already.defined"))
      .anyMatch(diagnostic -> diagnostic.getStartPosition() >= start && diagnostic.getStartPosition() <= end);
  }

  public Set<JProblem> undefinedTypes() {
    return result.diagnostics().stream()
      .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
      .filter(this::isUndefinedType)
      .map(diagnostic -> diagnostic.getMessage(Locale.ROOT))
      .map(message -> new JProblem(message, JProblem.Type.UNDEFINED_TYPE))
      .collect(Collectors.toSet());
  }

  private boolean isUndefinedType(Diagnostic<?> diagnostic) {
    String code = diagnostic.getCode();
    if (code.contains("doesnt.exist") || code.contains("cant.access")) {
      return true;
    }
    if (!code.contains("cant.resolve")) {
      return false;
    }
    String message = diagnostic.getMessage(Locale.ROOT);
    return message.contains("class ") || message.contains("interface ") || message.contains("enum ")
      || message.contains("record ");
  }

  public Runnable getEnvironmentCleaner() {
    return () -> {
      try {
        result.close();
      } catch (IOException e) {
        throw new IllegalStateException("Could not close the javac file manager", e);
      }
    };
  }

  public void registerDeclaration(TreePath path, Tree declaration) {
    var element = trees.getElement(path);
    if (element != null) {
      symbols.registerDeclaration(element, declaration);
      IdentifierTree name = JavacSymbols.declarationName(declaration);
      if (name != null) {
        paths.put(name, path);
      }
      if (declaration instanceof org.sonar.plugins.java.api.tree.VariableTree variable
        && variable.type() instanceof org.sonar.plugins.java.api.tree.InferedTypeTree) {
        paths.put(variable.type(), path);
      }
    }
  }

  public void registerUsage(TreePath path, IdentifierTree usage) {
    var element = trees.getElement(path);
    if (element != null) {
      symbols.registerUsage(element, usage);
    }
  }

  public void register(CompilationUnitTree unit, com.sun.source.tree.Tree javacTree, Tree sonarTree) {
    TreePath path = TreePath.getPath(unit, javacTree);
    if (path == null) {
      return;
    }
    paths.put(sonarTree, path);
    if (sonarTree instanceof org.sonar.java.model.expression.NewClassTreeImpl creation) {
      IdentifierTree constructor = creation.getConstructorIdentifier();
      paths.put(constructor, path);
      registerUsage(path, constructor);
    }
    if (sonarTree instanceof org.sonar.plugins.java.api.tree.ClassTree
      || sonarTree instanceof org.sonar.plugins.java.api.tree.MethodTree
      || sonarTree instanceof org.sonar.plugins.java.api.tree.VariableTree) {
      registerDeclaration(path, sonarTree);
      if (sonarTree instanceof EnumConstantTree constant
        && javacTree instanceof com.sun.source.tree.VariableTree variable
        && variable.getInitializer() instanceof com.sun.source.tree.NewClassTree initializer) {
        registerUsage(new TreePath(path, initializer), constant.simpleName());
      }
    } else if (sonarTree instanceof IdentifierTree identifier) {
      registerUsage(path, identifier);
    }
  }

  private TypeMirror primitiveType(String name) {
    return switch (name) {
      case "boolean" -> types.getPrimitiveType(TypeKind.BOOLEAN);
      case "byte" -> types.getPrimitiveType(TypeKind.BYTE);
      case "char" -> types.getPrimitiveType(TypeKind.CHAR);
      case "double" -> types.getPrimitiveType(TypeKind.DOUBLE);
      case "float" -> types.getPrimitiveType(TypeKind.FLOAT);
      case "int" -> types.getPrimitiveType(TypeKind.INT);
      case "long" -> types.getPrimitiveType(TypeKind.LONG);
      case "short" -> types.getPrimitiveType(TypeKind.SHORT);
      case "void" -> types.getNoType(TypeKind.VOID);
      default -> null;
    };
  }

  private static final class KeywordSymbol extends Symbols.DefaultSymbol implements Symbol.VariableSymbol {
    private final String name;
    private final Type type;

    private KeywordSymbol(String name, Type type) {
      this.name = name;
      this.type = type;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public Symbol owner() {
      return type.symbol();
    }

    @Override
    public Type type() {
      return type;
    }

    @Override
    public boolean isVariableSymbol() {
      return true;
    }

    @Override
    public boolean isUnknown() {
      return type.isUnknown();
    }

    @Override
    public Symbol.TypeSymbol enclosingClass() {
      return type.symbol();
    }

    @Override
    public List<IdentifierTree> usages() {
      return List.of();
    }

    @Override
    public VariableTree declaration() {
      return null;
    }

    @Override
    public boolean isEffectivelyFinal() {
      return true;
    }

    @Override
    public Optional<Object> constantValue() {
      return Optional.empty();
    }
  }
}
