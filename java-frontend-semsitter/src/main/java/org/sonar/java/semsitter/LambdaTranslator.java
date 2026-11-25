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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.MethodReferenceTreeImpl;
import org.sonar.java.model.pattern.RecordPatternTreeImpl;
import org.sonar.java.model.pattern.TypePatternTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.InferedTypeTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.semsitter.Bindings.ascribe;
import static org.sonar.java.semsitter.Bindings.bind;
import static org.sonar.java.semsitter.Bindings.namesType;
import static org.sonar.java.semsitter.Bindings.variableType;
import static org.sonar.java.semsitter.CstNodes.firstOf;
import static org.sonar.java.semsitter.CstNodes.firstType;
import static org.sonar.java.semsitter.CstNodes.lastNamed;
import static org.sonar.java.semsitter.CstNodes.lastOf;
import static org.sonar.java.semsitter.CstNodes.required;
import static org.sonar.java.semsitter.CstNodes.requiredField;

/** Translates lambdas, method references and patterns. */
final class LambdaTranslator extends TranslatorPart {
  private static final String RECORD_PATTERN = "record_pattern";

  LambdaTranslator(TranslationState state) {
    super(state);
  }

  void bindLambdaParameterOwner(LambdaExpressionTreeImpl lambda, Type target) {
    JMethodSymbol functional = sema.lookup().functionalMethod(target);
    if (functional == null || functional.parameterTypes().size() != lambda.parameters().size()) {
      return;
    }
    List<Type> substituted = sema.substitution().invocationParameters(functional, target, List.of());
    List<Type> types = lambdaParameterTypes(lambda, functional, substituted);
    JMethodSymbol shared = sema.substitution().invocationView(functional, target, types, sema.inference().invocationReturn(functional, target, List.of()));
    var arrow = lambda.arrowToken().range().start();
    JMethodSymbol view = shared.lambdaView(functional.id + "$lambda$" + arrow.line() + ":" + arrow.column() + "()");
    lambda.setSymbol(view);
    for (VariableTree parameter : lambda.parameters()) {
      if (parameter.symbol() instanceof JVariableSymbol symbol) {
        symbol.owner(view);
      }
    }
    ascribeLambdaResults(lambda, view.returnType().type());
    ownLambdaVariables(lambda, view);
  }

  /** Types the untyped lambda parameters from the functional method and returns the type each parameter ends up with. */
  private static List<Type> lambdaParameterTypes(LambdaExpressionTreeImpl lambda, JMethodSymbol functional, List<Type> substituted) {
    List<Type> types = new ArrayList<>();
    for (int i = 0; i < lambda.parameters().size(); i++) {
      VariableTree parameter = lambda.parameters().get(i);
      Type type = parameter.symbol().type();
      if ((type == null || type.isUnknown()) && parameter.symbol() instanceof JVariableSymbol symbol && !substituted.get(i).isUnknown()) {
        type = substituted.get(i);
        symbol.type(type);
        ascribe(parameter.simpleName(), type);
      }
      if (parameter.type() instanceof InferedTypeTree inferred && inferred.symbolType().isUnknown() && type != null && !type.isUnknown()) {
        ascribe(inferred, type);
      }
      types.add(type == null || type.isUnknown() ? functional.parameterTypes().get(i) : type);
    }
    return types;
  }

  private static void ownLambdaVariables(LambdaExpressionTreeImpl lambda, JMethodSymbol view) {
    lambda.body().accept(new BaseTreeVisitor() {
      @Override
      public void visitVariable(VariableTree tree) {
        if (tree.symbol() instanceof JVariableSymbol symbol && symbol.owner() instanceof JSymbol owner && owner.id != null && owner.id.contains("$lambda$")) {
          symbol.owner(view);
        }
        super.visitVariable(tree);
      }

      @Override
      public void visitIdentifier(IdentifierTree tree) {
        if (tree instanceof IdentifierTreeImpl use && use.symbolType().isUnknown() && use.symbol() instanceof JVariableSymbol variable
          && lambda.parameters().stream().anyMatch(parameter -> parameter.symbol().equals(variable)) && variable.type() != null && !variable.type().isUnknown()) {
          ascribe(use, variable.type());
        }
      }

      @Override
      public void visitLambdaExpression(LambdaExpressionTree tree) {
        // nested lambdas own their variables
      }

      @Override
      public void visitClass(ClassTree tree) {
        // local and anonymous classes own their variables
      }
    });
  }

  /** The values a lambda returns take the return type of the functional method as their target. */
  private void ascribeLambdaResults(LambdaExpressionTreeImpl lambda, Type returned) {
    if (returned.isUnknown() || returned.isVoid()) {
      return;
    }
    if (lambda.body() instanceof ExpressionTree value) {
      expressions().ascribeTarget(value, returned);
      return;
    }
    lambda.body().accept(new BaseTreeVisitor() {
      @Override
      public void visitReturnStatement(ReturnStatementTree tree) {
        if (tree.expression() != null) {
          expressions().ascribeTarget(tree.expression(), returned);
        }
      }

      @Override
      public void visitLambdaExpression(LambdaExpressionTree tree) {
        // nested lambdas have their own results
      }

      @Override
      public void visitClass(ClassTree tree) {
        // local and anonymous classes have their own methods
      }
    });
  }

  /**
   * The type a method reference yields, or null when it is a constructor reference or the method is generic or unknown.
   * An unbound {@code Type::method} returning a class type variable takes it from the function's first parameter.
   */
  @Nullable
  Type referenceResult(MethodReferenceTreeImpl reference, Type function) {
    if (!(reference.method().symbol() instanceof JMethodSymbol method) || method.isUnknown() || "new".equals(method.name()) || "<init>".equals(method.name())) {
      return null;
    }
    Type returned = method.returnType().type();
    if (sema.functionalInference().returnsClassVariable(method) && namesType(reference.expression())) {
      return sema.functionalInference().unboundReferenceResult(method, function);
    }
    return returned == null || returned.isUnknown() || returned.isTypeVar() || returned.isVoid() ? null : returned;
  }

  PatternTree pattern(CstNode node) {
    if (RECORD_PATTERN.equals(node.k())) {
      return recordPattern(node);
    }
    CstNode nested = firstOf(node, RECORD_PATTERN, "type_pattern", "pattern", "underscore_pattern");
    if (nested != null) {
      return pattern(nested);
    }
    if ("underscore_pattern".equals(node.k())) {
      return new TypePatternTreeImpl(new VariableTreeImpl(new IdentifierTreeImpl(token(node), true)), Type.UNKNOWN);
    }
    CstNode name = lastOf(node, "identifier");
    VariableTreeImpl variable = new VariableTreeImpl(members().modifiers(node), typeTrees().type(firstType(node)), names().identifier(name == null ? node : name));
    bindDeclaration(variable, name, (IdentifierTreeImpl) variable.simpleName());
    if (variable.symbol().type() != null && !variable.symbol().type().isUnknown()) {
      ascribe(variable.type(), variable.symbol().type());
    }
    return new TypePatternTreeImpl(variable, variableType(variable));
  }

  private RecordPatternTreeImpl recordPattern(CstNode node) {
    CstNode body = required(node, "record_pattern_body");
    List<PatternTree> patterns = new ArrayList<>();
    for (CstNode child : body.children()) {
      if ("record_pattern_component".equals(child.k()) || RECORD_PATTERN.equals(child.k())) {
        patterns.add(pattern(child));
      }
    }
    TypeTree recordType = typeTrees().type(firstType(node));
    state.deferred.add(() -> {
      if (recordType.symbolType().symbol().declaration() instanceof ClassTree recordClass && recordClass.recordComponents().size() == patterns.size()) {
        for (int i = 0; i < patterns.size(); i++) {
          if (patterns.get(i) instanceof TypePatternTreeImpl unnamed && unnamed.patternVariable().symbol().isUnknown()
            && unnamed.patternVariable() instanceof VariableTreeImpl variable) {
            patterns.set(i, typeUnnamedPattern(variable, recordClass.recordComponents().get(i).type().symbolType()));
          }
        }
      }
    });
    return new RecordPatternTreeImpl(recordType, token(required(body, "(")), patterns, token(required(body, ")")));
  }

  /** An unnamed pattern variable has the type of the record component it stands for. */
  private TypePatternTreeImpl typeUnnamedPattern(VariableTreeImpl variable, Type type) {
    var start = ((IdentifierTreeImpl) variable.simpleName()).identifierToken().range().start();
    JVariableSymbol symbol = new JVariableSymbol(sema, "_@" + start.line() + ":" + start.column(), "_", "local");
    symbol.type(type);
    sema.declare(symbol.id, variable);
    variable.setSymbol(symbol);
    ascribe(variable.type(), type);
    return new TypePatternTreeImpl(variable, type);
  }

  LambdaExpressionTreeImpl lambda(CstNode node) {
    state.scopes.addFirst(new HashMap<>());
    state.returnTargets.push(Type.UNKNOWN);
    try {
      return lambdaInScope(node);
    } finally {
      state.scopes.removeFirst();
      state.returnTargets.pop();
    }
  }

  private LambdaExpressionTreeImpl lambdaInScope(CstNode node) {
    List<VariableTree> parameters = new ArrayList<>();
    CstNode formals = requiredField(node, "parameters", null);
    InternalSyntaxToken open = null;
    InternalSyntaxToken close = null;
    if ("formal_parameters".equals(formals.k()) || "inferred_parameters".equals(formals.k())) {
      open = optionalToken(formals, "(");
      close = optionalToken(formals, ")");
      if ("formal_parameters".equals(formals.k())) {
        parameters.addAll(members().parameters(formals));
      } else {
        parameters.addAll(inferredParameters(formals));
      }
    } else {
      parameters.add(untypedParameter(formals));
    }
    CstNode body = requiredField(node, "body", lastNamed(node));
    Tree lambdaBody = "block".equals(body.k()) ? statements().block(body) : expressions().expression(body);
    LambdaExpressionTreeImpl tree = new LambdaExpressionTreeImpl(open, parameters, close, token(required(node, "->")), lambdaBody);
    state.deferred.add(() -> parameters.forEach(parameter -> {
      if (parameter.type() instanceof InferedTypeTree inferred && inferred.symbolType().isUnknown() && !parameter.symbol().type().isUnknown()) {
        ascribe(inferred, parameter.symbol().type());
      }
    }));
    state.deferred.add(() -> unbindUntypedParameters(tree));
    for (VariableTree parameter : parameters) {
      if (parameter.symbol().owner() instanceof JMethodSymbol method) {
        method.lambda(true);
        tree.setSymbol(method);
        break;
      }
    }
    return tree;
  }

  private List<VariableTreeImpl> inferredParameters(CstNode formals) {
    List<VariableTreeImpl> inferred = new ArrayList<>();
    for (CstNode child : formals.children()) {
      if ("identifier".equals(child.k())) {
        inferred.add(untypedParameter(child));
      } else if (",".equals(child.text())) {
        inferred.get(inferred.size() - 1).setEndToken(token(child));
      }
    }
    return inferred;
  }

  private VariableTreeImpl untypedParameter(CstNode node) {
    IdentifierTreeImpl name = names().identifier(node);
    VariableTreeImpl parameter = new VariableTreeImpl(name);
    bindDeclaration(parameter, node, name);
    return parameter;
  }

  /** The compiler leaves the receivers of a lambda without a target unresolved: its inferred parameters are not bound where they qualify a member. */
  private static void unbindUntypedParameters(LambdaExpressionTreeImpl lambda) {
    if (!lambda.symbolType().isUnknown()) {
      return;
    }
    Set<Symbol> untyped = new HashSet<>();
    for (VariableTree parameter : lambda.parameters()) {
      if (parameter.type() instanceof InferedTypeTree && parameter.symbol().type().isUnknown()) {
        untyped.add(parameter.symbol());
      }
    }
    if (untyped.isEmpty()) {
      return;
    }
    lambda.body().accept(new BaseTreeVisitor() {
      @Override
      public void visitMemberSelectExpression(MemberSelectExpressionTree select) {
        if (select.expression() instanceof IdentifierTreeImpl use && untyped.contains(use.symbol())) {
          use.symbol().usages().remove(use);
          bind(use, Symbol.UNKNOWN_SYMBOL);
        }
        super.visitMemberSelectExpression(select);
      }
    });
  }

  MethodReferenceTreeImpl methodReference(CstNode node) {
    List<CstNode> children = node.children();
    ExpressionTree qualifier = expressions().expression(children.get(0));
    CstNode nameNode = children.get(children.size() - 1);
    IdentifierTreeImpl name = names().identifier(nameNode);
    if (!"new".equals(name.name()) || !qualifier.is(Tree.Kind.ARRAY_TYPE)) {
      bindUse(name, nameNode, true);
    }
    MethodReferenceTreeImpl tree = new MethodReferenceTreeImpl(qualifier, token(required(node, "::")));
    CstNode typeArguments = firstOf(node, "type_arguments");
    tree.complete(typeArguments == null ? null : typeTrees().typeArguments(typeArguments), name);
    ascribe(tree, Type.UNKNOWN);
    return tree;
  }
}
