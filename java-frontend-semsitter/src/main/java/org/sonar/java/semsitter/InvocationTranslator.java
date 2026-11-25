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
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.InitializerListTreeImpl;
import org.sonar.java.model.ArrayDimensionTreeImpl;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.expression.ArrayAccessExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.MethodReferenceTreeImpl;
import org.sonar.java.model.expression.NewArrayTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.semsitter.Bindings.argumentTypes;
import static org.sonar.java.semsitter.Bindings.ascribe;
import static org.sonar.java.semsitter.Bindings.bind;
import static org.sonar.java.semsitter.Bindings.bindMethod;
import static org.sonar.java.semsitter.Bindings.functionArguments;
import static org.sonar.java.semsitter.CstNodes.childKind;
import static org.sonar.java.semsitter.CstNodes.field;
import static org.sonar.java.semsitter.CstNodes.firstNamed;
import static org.sonar.java.semsitter.CstNodes.firstType;
import static org.sonar.java.semsitter.CstNodes.lastKeyword;
import static org.sonar.java.semsitter.CstNodes.lastNamed;
import static org.sonar.java.semsitter.CstNodes.required;
import static org.sonar.java.semsitter.CstNodes.requiredField;

/** Translates method invocations, instance and array creations, and array accesses. */
final class InvocationTranslator extends TranslatorPart {

  private static final String JAVA_LANG_OBJECT = "java.lang.Object";
  private static final Pattern ARITY_ONLY = Pattern.compile("[^#]+#[^#(]+\\(\\d+\\)");

  InvocationTranslator(TranslationState state) {
    super(state);
  }

  MethodInvocationTreeImpl invocation(CstNode node) {
    CstNode name = requiredField(node, "name", null);
    CstNode object = field(node, "object");
    IdentifierTreeImpl identifier = names().identifier(name);
    bindUse(identifier, name, object != null);
    ExpressionTree select;
    ExpressionTree qualifier = null;
    if (object == null) {
      select = identifier;
    } else {
      qualifier = names().superQualifier(node, object);
      select = new MemberSelectExpressionTreeImpl(qualifier, token(lastKeyword(node, ".")), identifier);
    }
    CstNode typeArguments = field(node, "type_arguments");
    MethodInvocationTreeImpl tree = new MethodInvocationTreeImpl(
      select,
      typeArguments == null ? null : typeTrees().typeArguments(typeArguments),
      expressions().arguments(requiredField(node, "arguments", childKind(node, "argument_list"))));
    Type receiver = qualifier == null ? typeTrees().currentType() : qualifier.symbolType();
    String target = name.valueTarget();
    boolean arityOnly = target != null && ARITY_ONLY.matcher(target).matches();
    List<Type> arguments = argumentTypes(tree.arguments());
    bindStaticImport(identifier, qualifier, arguments);
    bindLibraryOrOverload(tree, identifier, qualifier, arityOnly ? target : null, arguments);
    refineMethodBinding(identifier, qualifier, receiver, arguments);
    if (identifier.symbol() instanceof JMethodSymbol method) {
      bindResolvedInvocation(tree, identifier, method, qualifier, receiver, arguments);
    } else if (tree.methodSymbol().isUnknown()) {
      // An unresolved call keeps its written name, as a recovered binding does.
      JMethodSymbol unresolved = new JMethodSymbol(sema, null, identifier.name());
      unresolved.unknown(true);
      bindMethod(tree, unresolved);
    }
    return tree;
  }

  /** An unqualified call to a statically imported name binds the imported method unless an enclosing type declares it. */
  private void bindStaticImport(IdentifierTreeImpl identifier, @Nullable ExpressionTree qualifier, List<Type> arguments) {
    if (qualifier == null && state.imports.staticImports.containsKey(identifier.name())) {
      boolean declaredHere = state.types.stream().filter(type -> !type.isUnknown())
        .anyMatch(type -> sema.lookup().declaredMethod(type.type().fullyQualifiedName(), identifier.name(), arguments) != null);
      if (!declaredHere) {
        Symbol.MethodSymbol imported = sema.lookup().declaredMethod(state.imports.staticImports.get(identifier.name()), identifier.name(), arguments);
        if (imported != null) {
          bind(identifier, imported);
        }
      }
    }
  }

  /** An unresolved or arity-only call is looked up, a bound one prefers its best overload. */
  private void bindLibraryOrOverload(MethodInvocationTreeImpl tree, IdentifierTreeImpl identifier, @Nullable ExpressionTree qualifier,
    @Nullable String arityTarget, List<Type> arguments) {
    if (identifier.symbol().isUnknown() || arityTarget != null) {
      Symbol.MethodSymbol method = arityTarget != null
        ? sema.lookup().declaredMethod(SemaFacts.erase(arityTarget.substring(0, arityTarget.indexOf('#'))), identifier.name(), arguments)
        : names().libraryMethod(qualifier, identifier.name(), arguments, functionArguments(tree.arguments()));
      if (method != null) {
        bind(identifier, method);
      }
    } else if (identifier.symbol() instanceof Symbol.MethodSymbol bound) {
      Set<Integer> functions = functionArguments(tree.arguments());
      Symbol.MethodSymbol preferred = sema.overloads().preferOverload(bound, names().libraryMethod(qualifier, identifier.name(), arguments, functions), arguments, functions);
      if (preferred != bound && bound instanceof JSymbol from && preferred instanceof JSymbol to) {
        sema.moveUse(from.id, to.id, identifier);
      }
      bind(identifier, preferred);
    }
  }

  /** A method whose arity does not fit is dropped, a varargs one becomes its most specific form. */
  private void refineMethodBinding(IdentifierTreeImpl identifier, @Nullable ExpressionTree qualifier, Type receiver, List<Type> arguments) {
    if (identifier.symbol() instanceof JMethodSymbol method && !method.isUnknown() && !MemberLookup.arityMatches(method, arguments.size())) {
      bind(identifier, Symbol.MethodSymbol.UNKNOWN_METHOD);
    }
    if (identifier.symbol() instanceof JMethodSymbol chosen) {
      JMethodSymbol specific = OverloadRanking.mostSpecificVarArgs(chosen, arguments);
      if (specific != chosen) {
        sema.moveUse(chosen.id, specific.id, identifier);
        bind(identifier, specific);
      }
    }
    if (qualifier != null && receiver.isUnknown() && !receiver.symbol().isUnknown()) {
      // a member of a parameterization with an undeclared argument is not bound
      bind(identifier, Symbol.MethodSymbol.UNKNOWN_METHOD);
    }
  }

  /** Binds and types an invocation whose method is resolved. */
  private void bindResolvedInvocation(MethodInvocationTreeImpl tree, IdentifierTreeImpl identifier, JMethodSymbol method, @Nullable ExpressionTree qualifier,
    Type receiver, List<Type> arguments) {
    List<Type> explicit = tree.typeArguments() == null ? List.of() : tree.typeArguments().stream().map(TypeTree::symbolType).toList();
    List<Type> parameters = sema.substitution().withExplicitTypeArguments(explicit, () -> sema.substitution().invocationParameters(method, receiver, arguments));
    expressions().ascribeArguments(method, parameters, tree.arguments());
    List<Type> typed = typedArguments(tree, arguments, parameters);
    Type returned = sema.substitution().withExplicitTypeArguments(explicit, () -> sema.inference().invocationReturn(method, receiver, typed));
    Type erased = qualifier != null && receiver.isRawType() && isWrittenRaw(qualifier) ? sema.inference().rawReturn(method) : null;
    if (erased != null) {
      // a member of a raw type has the erasure of its declared type
      returned = erased;
      state.rawResults.add(tree);
    }
    if ("getClass".equals(method.name()) && arguments.isEmpty() && method.owner().type().is(JAVA_LANG_OBJECT)
      && !receiver.isUnknown() && receiver.erasure() instanceof JType erasedReceiver) {
      // the static type of the call is Class<? extends |T|> where T is the static type of the receiver
      returned = JType.named(sema, "java.lang.Class", List.of(JType.wildcard(sema, erasedReceiver)));
    }
    List<Type> thrownArguments = withCreatedTypes(tree.arguments(), typed);
    List<Type> thrown = sema.substitution().withExplicitTypeArguments(explicit, () -> sema.inference().invocationThrown(method, receiver, thrownArguments));
    JMethodSymbol view = sema.substitution().invocationView(method, receiver, parameters, returned, thrown);
    bindMethod(tree, view);
    bind(identifier, view);
    ascribe(identifier, returned);
    ascribe(tree, returned);
  }

  /** The argument types, with nested calls, method references and expression lambdas typed by their result. */
  private List<Type> typedArguments(MethodInvocationTreeImpl tree, List<Type> arguments, List<Type> parameters) {
    List<Type> typed = new ArrayList<>(arguments);
    for (int i = 0; i < typed.size(); i++) {
      if (tree.arguments().get(i).is(Tree.Kind.METHOD_INVOCATION)) {
        typed.set(i, tree.arguments().get(i).symbolType());
      } else if (tree.arguments().get(i) instanceof MethodReferenceTreeImpl reference && i < parameters.size()
        && lambdas().referenceResult(reference, parameters.get(i)) instanceof Type produced) {
        Type function = sema.functionalInference().functionProducing(parameters.get(i), produced);
        if (function != null) {
          typed.set(i, function);
        }
      } else if (tree.arguments().get(i) instanceof LambdaExpressionTreeImpl lambda && lambda.body() instanceof ExpressionTree value && i < parameters.size()) {
        Type function = sema.functionalInference().functionProducing(parameters.get(i), value.symbolType());
        if (function != null) {
          typed.set(i, function);
        }
      }
    }
    return typed;
  }

  /**
   * The argument types with each supplier of a created object, {@code Type::new} or {@code () -> new Type(..)},
   * typed as {@code Supplier<Type>}: the type a generic throws clause is inferred from.
   */
  private List<Type> withCreatedTypes(List<ExpressionTree> expressions, List<Type> types) {
    List<Type> result = new ArrayList<>(types);
    for (int i = 0; i < result.size(); i++) {
      Type created = null;
      ExpressionTree argument = expressions.get(i);
      if (argument instanceof MethodReferenceTreeImpl reference && "new".equals(reference.method().name()) && reference.expression() instanceof TypeTree qualifier) {
        created = qualifier.symbolType();
      } else if (argument instanceof LambdaExpressionTreeImpl lambda && lambda.parameters().isEmpty() && lambda.body() instanceof NewClassTreeImpl creation) {
        created = creation.symbolType();
      }
      if (created != null && !created.isUnknown() && created.isClass()) {
        result.set(i, JType.named(sema, "java.util.function.Supplier", List.of(sema.asJType(created))));
      }
    }
    return result;
  }

  ExpressionTree typeExpression(CstNode node) {
    return (ExpressionTree) typeTrees().type(node);
  }

  NewClassTreeImpl creation(CstNode node) {
    CstNode typeNode = requiredField(node, "type", null);
    TypeTree type = typeTrees().annotate(names().recoverQualifiedType(typeTrees().type(typeNode)), node);
    ArgumentListTreeImpl args = expressions().arguments(requiredField(node, "arguments", null));
    CstNode body = childKind(node, "class_body");
    ClassTreeImpl classBody = null;
    if (body != null) {
      classBody = new ClassTreeImpl(Tree.Kind.CLASS, token(required(body, "{")), declarations().membersOf(body), token(required(body, "}")));
      // An anonymous class is declared at its creation expression, whatever its members.
      bindType(classBody, node);
    }
    NewClassTreeImpl tree = new NewClassTreeImpl(type, args, classBody).completeWithNewKeyword(token(required(node, "new")));
    CstNode enclosing = node.children().get(0);
    if (!"new".equals(enclosing.text())) {
      tree.completeWithEnclosingExpression(expressions().expression(enclosing));
      tree.completeWithDotToken(token(required(node, ".")));
    }
    CstNode typeArguments = field(node, "type_arguments");
    if (typeArguments != null) {
      tree.completeWithTypeArguments(typeTrees().typeArguments(typeArguments));
    }
    bindConstructor(tree, typeNode);
    ascribe(tree, classBody != null && !classBody.symbol().isUnknown() ? classBody.symbol().type() : type.symbolType());
    return tree;
  }

  private void bindConstructor(NewClassTreeImpl tree, CstNode typeNode) {
    IdentifierTreeImpl identifier = (IdentifierTreeImpl) tree.getConstructorIdentifier();
    TypeTree written = tree.identifier() instanceof ParameterizedTypeTree parameterized ? parameterized.type() : tree.identifier();
    String imported = written instanceof MemberSelectExpressionTree ? null : state.imports.singleImports.get(identifier.name());
    String constructor = typeNode.valueTarget();
    if (constructor != null && bindResolvedConstructor(tree, identifier, imported, constructor)) {
      return;
    }
    Type type = tree.identifier().symbolType().erasure();
    if (imported != null && sema.declaresType(imported) && !imported.equals(type.fullyQualifiedName())) {
      type = sema.getClassType(imported);
      bindType(identifier, imported);
    }
    String owner = tree.classBody() != null && type.symbol().isInterface() ? JAVA_LANG_OBJECT : type.fullyQualifiedName();
    Symbol.MethodSymbol declared = type.isClass() ? sema.lookup().declaredMethod(owner, "<init>", argumentTypes(tree.arguments()), functionArguments(tree.arguments())) : null;
    if (declared != null && MemberLookup.arityMatches(declared, tree.arguments().size())) {
      bind(identifier, declared);
      recordUse(declared, identifier);
      expressions().ascribeArguments(declared, tree.arguments());
      viewWithInferredParameters(tree, identifier);
    } else if (!(identifier.symbol() instanceof Symbol.MethodSymbol)) {
      bind(identifier, Symbol.MethodSymbol.UNKNOWN_METHOD);
    }
  }

  /** Binds the constructor SemSitter resolved unless another import declares the type; whether it did. */
  private boolean bindResolvedConstructor(NewClassTreeImpl tree, IdentifierTreeImpl identifier, @Nullable String imported, String constructor) {
    Symbol resolved = sema.resolve(constructor);
    String owner = resolved.owner() == null || resolved.owner().type() == null ? "" : resolved.owner().type().erasure().fullyQualifiedName();
    boolean importedElsewhere = imported != null && sema.declaresType(imported) && !imported.equals(owner);
    if (resolved instanceof Symbol.MethodSymbol method && !importedElsewhere && MemberLookup.arityMatches(method, tree.arguments().size())) {
      bind(identifier, resolved);
      recordUse(resolved, identifier);
      expressions().ascribeArguments(method, tree.arguments());
      viewWithInferredParameters(tree, identifier);
      return true;
    }
    return false;
  }

  /** A diamond without a target takes the class arguments its constructor arguments fix. */
  private void inferDiamondFromArguments(NewClassTreeImpl tree, JMethodSymbol constructor) {
    if (!(tree.identifier() instanceof JavaTree.ParameterizedTypeTreeImpl generic) || !generic.typeArguments().isEmpty()) {
      return;
    }
    Type inferred = sema.inference().inferDiamondFromArguments(constructor, generic.symbolType(), argumentTypes(tree.arguments()));
    if (inferred.isParameterized()) {
      ascribe(generic, inferred);
    }
  }

  /** A generic constructor is seen with the type arguments its call infers for the parameters. */
  private void viewWithInferredParameters(NewClassTreeImpl tree, IdentifierTreeImpl identifier) {
    if (!(identifier.symbol() instanceof JMethodSymbol constructor) || constructor.isUnknown()) {
      return;
    }
    inferDiamondFromArguments(tree, constructor);
    List<Type> parameters = sema.substitution().invocationParameters(constructor, tree.identifier().symbolType(), argumentTypes(tree.arguments()));
    if (!parameters.equals(constructor.parameterTypes())) {
      JMethodSymbol view = sema.substitution().invocationView(constructor, tree.identifier().symbolType(), parameters, constructor.returnType().type());
      if (view != constructor && !constructor.isParametrizedMethod()) {
        // the class arguments written at the call do not make the constructor generic
        view.parametrized(false);
      }
      bind(identifier, view);
    }
  }

  /** Whether a receiver is raw because of what is written: a variable declared with a raw type, or the erased result of such a call. */
  private boolean isWrittenRaw(ExpressionTree qualifier) {
    if (state.rawResults.contains(qualifier)) {
      return true;
    }
    Symbol symbol = qualifier instanceof IdentifierTree identifier ? identifier.symbol() : Symbol.UNKNOWN_SYMBOL;
    return symbol.isVariableSymbol() && symbol.declaration() instanceof VariableTree variable
      && variable.type() instanceof IdentifierTree variableType && !"var".equals(variableType.name());
  }

  NewArrayTreeImpl arrayCreation(CstNode node) {
    CstNode initializer = field(node, "value");
    List<ArrayDimensionTree> dims = new ArrayList<>();
    for (CstNode child : node.children()) {
      // sized dimensions, then unsized ones: new byte[n][]
      if ("dimensions_expr".equals(child.k()) || "dimensions".equals(child.k())) {
        dims.addAll(typeTrees().dimensions(child));
      }
    }
    InitializerListTreeImpl initializers = initializer == null ? InitializerListTreeImpl.emptyList() : initializers(initializer);
    NewArrayTreeImpl tree = new NewArrayTreeImpl(dims, initializers)
      .complete(typeTrees().annotate(typeTrees().type(requiredField(node, "type", firstType(node))), node))
      .completeWithNewKeyword(token(required(node, "new")));
    if (initializer != null) {
      tree.completeWithCurlyBraces(token(required(initializer, "{")), token(required(initializer, "}")));
    }
    ascribe(tree, typeTrees().arrayTypeOf(tree.type(), dims.size()));
    return tree;
  }

  NewArrayTreeImpl arrayInitializer(CstNode node) {
    return new NewArrayTreeImpl(List.of(), initializers(node)).completeWithCurlyBraces(token(required(node, "{")), token(required(node, "}")));
  }

  MemberSelectExpressionTreeImpl classLiteral(CstNode node) {
    ExpressionTree type = (ExpressionTree) names().recoverQualifiedType(typeTrees().type(firstNamed(node)));
    IdentifierTreeImpl className = new IdentifierTreeImpl(token(required(node, "class")));
    MemberSelectExpressionTreeImpl tree = new MemberSelectExpressionTreeImpl(type, token(required(node, ".")), className);
    Type literal = type.symbolType().isPrimitive() ? type.symbolType().primitiveWrapperType() : type.symbolType();
    JType argument = literal instanceof JType jType ? jType : JType.unknown(sema);
    ascribe(tree, JType.inferred(sema, "java.lang.Class", List.of(argument)));
    return tree;
  }

  private InitializerListTreeImpl initializers(CstNode node) {
    InitializerListTreeImpl list = InitializerListTreeImpl.emptyList();
    for (CstNode child : node.children()) {
      if ("{".equals(child.text()) || "}".equals(child.text())) {
        continue;
      }
      if (",".equals(child.text())) {
        list.separators().add(token(child));
      } else {
        list.add(expressions().expression(child));
      }
    }
    return list;
  }

  ArrayAccessExpressionTreeImpl arrayAccess(CstNode node) {
    ExpressionTree array = expressions().expression(requiredField(node, "array", firstNamed(node)));
    ExpressionTree index = expressions().expression(requiredField(node, "index", lastNamed(node)));
    ArrayDimensionTreeImpl dimension = new ArrayDimensionTreeImpl(token(required(node, "[")), index, token(required(node, "]")));
    ArrayAccessExpressionTreeImpl tree = new ArrayAccessExpressionTreeImpl(array, dimension);
    Type arrayType = array.symbolType();
    ascribe(tree, arrayType.isArray() ? ((Type.ArrayType) arrayType).elementType() : Type.UNKNOWN);
    return tree;
  }
}
