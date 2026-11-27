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
import javax.annotation.Nullable;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.LiteralUtils;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.ConditionalExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.InstanceOfTreeImpl;
import org.sonar.java.model.expression.InternalPostfixUnaryExpression;
import org.sonar.java.model.expression.InternalPrefixUnaryExpression;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.NewArrayTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.expression.TypeCastExpressionTreeImpl;
import org.sonar.java.model.pattern.TypePatternTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

import static org.sonar.java.semsitter.Bindings.argumentTypes;
import static org.sonar.java.semsitter.Bindings.ascribe;
import static org.sonar.java.semsitter.Bindings.assignmentKind;
import static org.sonar.java.semsitter.Bindings.binaryKind;
import static org.sonar.java.semsitter.Bindings.binaryType;
import static org.sonar.java.semsitter.Bindings.bind;
import static org.sonar.java.semsitter.Bindings.keepsArguments;
import static org.sonar.java.semsitter.Bindings.literal;
import static org.sonar.java.semsitter.Bindings.sameErasure;
import static org.sonar.java.semsitter.Bindings.unaryKind;
import static org.sonar.java.semsitter.Bindings.unboxed;
import static org.sonar.java.semsitter.Bindings.variableType;
import static org.sonar.java.semsitter.CstNodes.field;
import static org.sonar.java.semsitter.CstNodes.firstNamed;
import static org.sonar.java.semsitter.CstNodes.lastNamed;
import static org.sonar.java.semsitter.CstNodes.required;
import static org.sonar.java.semsitter.CstNodes.requiredField;

/** Translates expressions and operators, and ascribes target types. */
final class ExpressionTranslator extends TranslatorPart {

  private static final String OPERATOR = "operator";
  private static final String RIGHT = "right";
  private static final String BOOLEAN = "boolean";
  private static final String SHORT = "short";
  private static final String INSTANCEOF = "instanceof";

  ExpressionTranslator(TranslationState state) {
    super(state);
  }

  ExpressionTree expression(CstNode node) {
    return switch (node.k()) {
      case "decimal_integer_literal", "hex_integer_literal", "octal_integer_literal", "binary_integer_literal" -> names().integerLiteral(node);
      case "decimal_floating_point_literal", "hex_floating_point_literal" -> names().floatingLiteral(node);
      case "string_literal", "string_fragment" ->
        literal(node.text().startsWith("\"\"\"") ? Tree.Kind.TEXT_BLOCK : Tree.Kind.STRING_LITERAL, token(node), sema.getClassType("java.lang.String"));
      case "character_literal" -> literal(Tree.Kind.CHAR_LITERAL, token(node), sema.getClassType("char"));
      case "scoped_identifier", "scoped_type_identifier", "field_access" -> names().memberSelect(node);
      case "method_invocation" -> invocations().invocation(node);
      case "object_creation_expression" -> invocations().creation(node);
      case "array_creation_expression" -> invocations().arrayCreation(node);
      case "array_initializer", "element_value_array_initializer" -> invocations().arrayInitializer(node);
      case "element_value_pair" -> members().elementValuePair(node);
      case "class_literal" -> invocations().classLiteral(node);
      case "annotation", "marker_annotation" -> members().annotation(node);
      case "array_access" -> invocations().arrayAccess(node);
      case "assignment_expression" -> assignment(node);
      case "binary_expression" -> binary(node);
      case "unary_expression" -> unary(node);
      case "update_expression" -> update(node);
      case "parenthesized_expression" -> parenthesized(node);
      case "ternary_expression" -> ternary(node);
      case "cast_expression" -> cast(node);
      case "instanceof_expression" -> instanceOf(node);
      case "lambda_expression" -> lambdas().lambda(node);
      case "method_reference" -> lambdas().methodReference(node);
      case "switch_expression" -> switches().switchExpression(node);
      case "generic_type", "array_type", "integral_type", "floating_point_type", "boolean_type", "void_type", "wildcard", "annotated_type" -> invocations().typeExpression(node);
      default -> names().primary(node);
    };
  }

  /** Types a lambda, a method reference or an array initializer from the type its context expects. */
  void ascribeTarget(ExpressionTree expression, Type target) {
    ascribeTargetTyped(expression, target);
    ascribeArrayElements(expression, target);
    if (expression instanceof NewClassTreeImpl newClass) {
      inferDiamond(newClass, target);
    }
    ascribePolyExpression(expression, target);
    ascribeGenericCall(expression, target);
    ascribeCollectorCall(expression, target);
    if (expression instanceof LambdaExpressionTreeImpl lambda) {
      lambdas().bindLambdaParameterOwner(lambda, lambda.symbolType());
    }
  }

  private void ascribeTargetTyped(ExpressionTree expression, Type target) {
    boolean targetTyped = expression.is(Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.METHOD_REFERENCE) || (expression instanceof NewArrayTreeImpl array && array.type() == null);
    // an erased type variable (Object) is not a functional interface: wait for the inferred parameter
    if (targetTyped && expression.symbolType().isUnknown() && !target.isUnknown() && !target.is("java.lang.Object")) {
      ascribe(expression, expression instanceof NewArrayTreeImpl ? target : sema.inference().groundType(target));
    }
  }

  private void ascribeArrayElements(ExpressionTree expression, Type target) {
    if (expression instanceof NewArrayTreeImpl array && array.type() == null && target instanceof Type.ArrayType arrayType) {
      array.initializers().forEach(element -> ascribeTarget(element, arrayType.elementType()));
    }
  }

  private static void ascribePolyExpression(ExpressionTree expression, Type target) {
    // a switch expression is a poly expression: in an assignment context it has the target type
    if (expression.is(Tree.Kind.SWITCH_EXPRESSION) && !target.isUnknown() && !target.isPrimitive() && !expression.symbolType().isPrimitive()) {
      ascribe(expression, target);
    }
    // a reference conditional is a poly expression: in an assignment context it has the target type
    if (expression instanceof ConditionalExpressionTree && keepsArguments(target) && sameErasure(target, expression.symbolType())) {
      ascribe(expression, target);
    }
  }

  private void ascribeGenericCall(ExpressionTree expression, Type target) {
    if (expression instanceof MethodInvocationTreeImpl call
      && call.methodSymbol() instanceof JMethodSymbol generic
      && !generic.isUnknown()
      && !call.symbolType().isParameterized()) {
      Type inferred = sema.inference().targetedReturn(generic, target);
      if (inferred != null) {
        ascribe(call, inferred);
        List<Type> parameters = sema.functionalInference().targetedParameters(generic, target);
        if (parameters != null && parameters.size() == generic.parameterTypes().size()) {
          ascribeArguments(generic, parameters, call.arguments());
        }
      }
    }
  }

  private void ascribeCollectorCall(ExpressionTree expression, Type target) {
    if (expression instanceof MethodInvocationTreeImpl call
      && call.methodSymbol() instanceof JMethodSymbol generic
      && !generic.isUnknown()
      && call.symbolType().isParameterized()
      && target.isParameterized()
      && call.arguments().stream().anyMatch(argument -> argument.is(Tree.Kind.METHOD_INVOCATION) && argument.symbolType().is("java.util.stream.Collector"))) {
      Type retargeted = sema.inference().retargetedElement(generic, generic.owner().type(), call.symbolType(), target);
      if (retargeted != null) {
        ascribe(call, retargeted);
      }
    }
  }

  /** A diamond takes the type arguments its target gives the created class, and its constructor is seen from that type. */
  void inferDiamond(NewClassTreeImpl tree, Type target) {
    if (!(tree.identifier() instanceof JavaTree.ParameterizedTypeTreeImpl generic) || !generic.typeArguments().isEmpty() || !target.isParameterized()) {
      return;
    }
    boolean anonymous = tree.classBody() != null && tree.classBody().symbol() instanceof JTypeSymbol body && !body.isUnknown();
    // an anonymous class keeps its own type; the diamond parameterizes the supertype it names
    Type inferred = sema.inference().inferDiamond(anonymous ? generic.symbolType() : tree.symbolType(), sema.inference().groundType(target));
    if (!inferred.isParameterized()) {
      return;
    }
    ascribe(generic, inferred);
    if (anonymous) {
      ((JTypeSymbol) tree.classBody().symbol()).declaredSupertype(inferred);
      return;
    }
    ascribe(tree, inferred);
    IdentifierTreeImpl identifier = (IdentifierTreeImpl) tree.getConstructorIdentifier();
    ascribe(identifier, inferred);
    if (identifier.symbol() instanceof JMethodSymbol constructor && !constructor.isUnknown()) {
      List<Type> parameters = sema.substitution().invocationParameters(constructor, inferred, argumentTypes(tree.arguments()));
      bind(identifier, sema.substitution().invocationView(constructor, inferred, parameters, constructor.returnType().type()));
    }
  }

  void ascribeArguments(Symbol.MethodSymbol method, List<ExpressionTree> arguments) {
    ascribeArguments(method, method.parameterTypes(), arguments);
  }

  /** Types the arguments from the declared parameters, substituted for the receiver's type arguments when the call is generic. */
  void ascribeArguments(Symbol.MethodSymbol method, List<Type> parameters, List<ExpressionTree> arguments) {
    for (int i = 0; i < arguments.size() && !parameters.isEmpty(); i++) {
      Type parameter = parameters.get(Math.min(i, parameters.size() - 1));
      if (method.isVarArgsMethod() && i >= parameters.size() - 1 && parameter.isArray()
        && (arguments.size() != parameters.size() || arguments.get(i).is(Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.METHOD_REFERENCE))) {
        parameter = ((Type.ArrayType) parameter).elementType();
      }
      if (i < parameters.size() || method.isVarArgsMethod()) {
        ascribeTarget(arguments.get(i), parameter);
      }
    }
  }

  private AssignmentExpressionTreeImpl assignment(CstNode node) {
    CstNode operator = requiredField(node, OPERATOR, null);
    AssignmentExpressionTreeImpl tree = new AssignmentExpressionTreeImpl(
      assignmentKind(operator.text()),
      expression(requiredField(node, "left", firstNamed(node))),
      token(operator),
      expression(requiredField(node, RIGHT, lastNamed(node))));
    ascribe(tree, tree.variable().symbolType());
    if (tree.is(Tree.Kind.ASSIGNMENT)) {
      ascribeTarget(tree.expression(), tree.variable().symbolType());
    }
    return tree;
  }

  private BinaryExpressionTreeImpl binary(CstNode node) {
    CstNode operator = requiredField(node, OPERATOR, null);
    String text = operator.text();
    ExpressionTree left = expression(requiredField(node, "left", firstNamed(node)));
    ExpressionTree right = expression(requiredField(node, RIGHT, lastNamed(node)));
    BinaryExpressionTreeImpl tree = new BinaryExpressionTreeImpl(binaryKind(text), left, token(operator), right);
    ascribe(tree, binaryType(text, left.symbolType(), right.symbolType()));
    return tree;
  }

  private ExpressionTree unary(CstNode node) {
    CstNode operator = requiredField(node, OPERATOR, null);
    String text = operator.text();
    ExpressionTree value = expression(requiredField(node, "operand", firstNamed(node)));
    InternalPrefixUnaryExpression tree = new InternalPrefixUnaryExpression(unaryKind(text, true), token(operator), value);
    ascribe(tree, "!".equals(text) ? sema.getClassType(BOOLEAN) : unaryPromoted(value.symbolType()));
    return tree;
  }

  /** {@code +}, {@code -} and {@code ~} promote a byte, short or char operand to int. */
  private Type unaryPromoted(Type operand) {
    Type primitive = unboxed(operand);
    if (!primitive.isPrimitive()) {
      return operand;
    }
    boolean narrow = primitive.is("byte") || primitive.is(SHORT) || primitive.is("char");
    return narrow ? sema.getClassType("int") : primitive;
  }

  private ExpressionTree update(CstNode node) {
    List<CstNode> children = node.children();
    CstNode first = children.get(0);
    boolean prefix = "++".equals(first.text()) || "--".equals(first.text());
    CstNode operator = prefix ? first : children.get(children.size() - 1);
    ExpressionTree operand = expression(firstNamed(node));
    String text = operator.text();
    InternalSyntaxToken token = token(operator);
    ExpressionTree tree = prefix
      ? new InternalPrefixUnaryExpression(unaryKind(text, true), token, operand)
      : new InternalPostfixUnaryExpression(unaryKind(text, false), operand, token);
    ascribe(tree, operand.symbolType());
    return tree;
  }

  private ParenthesizedTreeImpl parenthesized(CstNode node) {
    Parens parens = parens(node);
    ParenthesizedTreeImpl tree = new ParenthesizedTreeImpl(parens.open(), parens.expr(), parens.close());
    ascribe(tree, parens.expr().symbolType());
    return tree;
  }

  private ConditionalExpressionTreeImpl ternary(CstNode node) {
    ExpressionTree condition = expression(requiredField(node, "condition", firstNamed(node)));
    ExpressionTree then = expression(requiredField(node, "consequence", firstNamed(node)));
    ExpressionTree otherwise = expression(requiredField(node, "alternative", lastNamed(node)));
    ConditionalExpressionTreeImpl tree = new ConditionalExpressionTreeImpl(condition, token(required(node, "?")), then, token(required(node, ":")), otherwise);
    ascribe(tree, conditionalType(then, otherwise));
    return tree;
  }

  /** The type of a conditional: operands of numeric or boolean types are unboxed and promoted, others keep the more general operand type. */
  private Type conditionalType(ExpressionTree then, ExpressionTree otherwise) {
    Type first = then.symbolType();
    Type second = otherwise.symbolType();
    if (first.isUnknown() || second.isUnknown()) {
      return first.isUnknown() ? second : first;
    }
    if (first.equals(second)) {
      return first;
    }
    Type left = primitiveOperand(first);
    Type right = primitiveOperand(second);
    if (isUnboxedConditional(first, second, left, right)) {
      return unboxedConditionalType(first, left, right, then, otherwise);
    }
    if (first.isNullType() || second.isNullType()) {
      return nullConditionalType(first, second);
    }
    // a type variable is compared by its bound: erasing it would make the variable look like its own bound
    if ((first.isTypeVar() || second.isTypeVar()) && !first.isPrimitive() && !second.isPrimitive()) {
      return typeVariableConditionalType(first, second);
    }
    return referenceConditionalType(first, second);
  }

  private static Type primitiveOperand(Type operand) {
    return operand.isPrimitive() ? operand : operand.primitiveType();
  }

  private static boolean isUnboxedConditional(Type first, Type second, @Nullable Type left, @Nullable Type right) {
    boolean oneIsPrimitive = first.isPrimitive() || second.isPrimitive();
    return left != null && right != null && (oneIsPrimitive || (first.isPrimitiveWrapper() && second.isPrimitiveWrapper() && !left.equals(right)));
  }

  private Type unboxedConditionalType(Type first, Type left, Type right, ExpressionTree then, ExpressionTree otherwise) {
    if (left.equals(right)) {
      return left;
    }
    if (left.is(BOOLEAN) || right.is(BOOLEAN)) {
      return first;
    }
    return numericPromotion(left, right, then, otherwise);
  }

  private static Type nullConditionalType(Type first, Type second) {
    if (first.isNullType()) {
      return second.isPrimitive() ? second.primitiveWrapperType() : second;
    }
    return first.isPrimitive() ? first.primitiveWrapperType() : first;
  }

  private Type typeVariableConditionalType(Type first, Type second) {
    if (second.isSubtypeOf(first)) {
      return first;
    }
    if (first.isSubtypeOf(second)) {
      return second;
    }
    Type firstBound = typeVariableBound(first);
    Type secondBound = typeVariableBound(second);
    return !secondBound.isSubtypeOf(firstBound.erasure()) && firstBound.isSubtypeOf(secondBound.erasure()) ? secondBound : firstBound;
  }

  private Type typeVariableBound(Type type) {
    return type instanceof JType variable && variable.isTypeVar() ? sema.memberFactory().typeVariableBound(variable.typeVarId) : type;
  }

  private static Type referenceConditionalType(Type first, Type second) {
    if (!first.isPrimitive() && !second.isPrimitive()) {
      if (second.isSubtypeOf(first.erasure())) {
        return first;
      }
      if (first.isSubtypeOf(second.erasure())) {
        return second;
      }
    }
    return first;
  }

  private Type numericPromotion(Type left, Type right, ExpressionTree then, ExpressionTree otherwise) {
    List<String> order = List.of("byte", SHORT, "char", "int", "long", "float", "double");
    int a = order.indexOf(left.fullyQualifiedName());
    int b = order.indexOf(right.fullyQualifiedName());
    boolean byteAndShort = (a == 0 && b == 1) || (a == 1 && b == 0);
    if (byteAndShort) {
      return sema.getClassType(SHORT);
    }
    // a constant int that fits the narrower operand keeps that type
    if (a < 3 && right.is("int") && fitsIn(otherwise, left)) {
      return left;
    }
    if (b < 3 && left.is("int") && fitsIn(then, right)) {
      return right;
    }
    return sema.getClassType(order.get(Math.max(3, Math.max(a, b))));
  }

  static boolean fitsIn(ExpressionTree constant, Type narrow) {
    Integer value = LiteralUtils.intLiteralValue(constant);
    if (value == null) {
      return false;
    }
    return switch (narrow.fullyQualifiedName()) {
      case "byte" -> value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE;
      case SHORT -> value >= Short.MIN_VALUE && value <= Short.MAX_VALUE;
      default -> value >= Character.MIN_VALUE && value <= Character.MAX_VALUE;
    };
  }

  private TypeCastExpressionTreeImpl cast(CstNode node) {
    List<CstNode> typeNodes = node.children().stream().filter(child -> "type".equals(child.f())).toList();
    TypeTree type = typeTrees().type(typeNodes.get(0));
    List<CstNode> ands = node.children().stream().filter(child -> "&".equals(child.k())).toList();
    QualifiedIdentifierListTreeImpl bounds = QualifiedIdentifierListTreeImpl.emptyList();
    for (int i = 1; i < typeNodes.size(); i++) {
      bounds.add(typeTrees().type(typeNodes.get(i)));
      if (i < ands.size()) {
        bounds.separators().add(token(ands.get(i)));
      }
    }
    ExpressionTree value = expression(requiredField(node, "value", lastNamed(node)));
    InternalSyntaxToken andToken = bounds.isEmpty() ? null : token(ands.get(0));
    TypeCastExpressionTreeImpl tree = new TypeCastExpressionTreeImpl(token(required(node, "(")), type, andToken, bounds, token(required(node, ")")), value);
    if (bounds.isEmpty()) {
      ascribe(tree, type.symbolType());
      // The cast type is the target of a lambda operand (JLS 15.16). Only its parameters are rebound: typing the lambda itself would make the cast look redundant.
      // The rebinding mirrors the ECJ-based analysis, which suppresses S1117 for a lambda parameter whose target is a nested, implicitly static interface.
      if (value instanceof LambdaExpressionTreeImpl lambda && !type.symbolType().isUnknown()) {
        lambdas().bindLambdaParameterOwner(lambda, sema.inference().groundType(type.symbolType()));
      }
    } else {
      List<JType> parts = new ArrayList<>();
      parts.add(sema.asJType(type.symbolType()));
      bounds.forEach(bound -> parts.add(sema.asJType(bound.symbolType())));
      ascribe(tree, JType.intersection(sema, parts));
    }
    return tree;
  }

  private InstanceOfTreeImpl instanceOf(CstNode node) {
    ExpressionTree left = expression(requiredField(node, "left", firstNamed(node)));
    CstNode name = field(node, "name");
    CstNode patternNode = field(node, "pattern");
    InstanceOfTreeImpl tree;
    if (patternNode != null) {
      tree = new InstanceOfTreeImpl(left, token(required(node, INSTANCEOF)), lambdas().pattern(patternNode));
    } else if (name != null) {
      VariableTreeImpl variable = new VariableTreeImpl(members().modifiers(node), typeTrees().type(requiredField(node, RIGHT, null)), names().identifier(name));
      bindDeclaration(variable, name, (IdentifierTreeImpl) variable.simpleName());
      TypePatternTreeImpl pattern = new TypePatternTreeImpl(variable, variableType(variable));
      tree = new InstanceOfTreeImpl(left, token(required(node, INSTANCEOF)), pattern);
    } else {
      tree = new InstanceOfTreeImpl(left, token(required(node, INSTANCEOF)), typeTrees().type(requiredField(node, RIGHT, null)));
    }
    ascribe(tree, sema.getClassType(BOOLEAN));
    return tree;
  }

  ArgumentListTreeImpl arguments(CstNode node) {
    ArgumentListTreeImpl list = ArgumentListTreeImpl.emptyList();
    InternalSyntaxToken open = null;
    InternalSyntaxToken close = null;
    for (CstNode child : node.children()) {
      if ("(".equals(child.text())) {
        open = token(child);
      } else if (")".equals(child.text())) {
        close = token(child);
      } else if (",".equals(child.text())) {
        list.separators().add(token(child));
      } else {
        list.add(expression(child));
      }
    }
    return list.complete(open, close);
  }
}
