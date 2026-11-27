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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.java.model.AbstractTypedTree;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CaseGroupTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.YieldStatementTree;

/** Stateless helpers that bind symbols and types to trees. */
final class Bindings {

  private static final String BOOLEAN = "boolean";
  private static final String JAVA_LANG_STRING = "java.lang.String";
  private static final Set<String> BOOLEAN_OPERATORS = Set.of("&&", "||", "==", "!=", "<", ">", "<=", ">=", "instanceof");

  private Bindings() {
  }

  static Type switchResultType(SwitchExpressionTreeImpl tree) {
    List<Type> results = new ArrayList<>();
    for (CaseGroupTree group : tree.cases()) {
      group.body().forEach(statement -> statement.accept(new BaseTreeVisitor() {
        @Override
        public void visitYieldStatement(YieldStatementTree yieldStatement) {
          results.add(yieldStatement.expression().symbolType());
        }

        @Override
        public void visitSwitchExpression(SwitchExpressionTree nested) {
          // nested switch expressions yield their own results
        }

        @Override
        public void visitLambdaExpression(LambdaExpressionTree lambda) {
          // lambdas cannot yield to the enclosing switch
        }

        @Override
        public void visitClass(ClassTree type) {
          // classes cannot yield to the enclosing switch
        }
      }));
    }
    if (results.isEmpty() || results.stream().anyMatch(Type::isUnknown) || results.stream().distinct().count() != 1) {
      return Type.UNKNOWN;
    }
    return results.get(0);
  }

  static Set<Integer> functionArguments(List<ExpressionTree> arguments) {
    Set<Integer> functions = new HashSet<>();
    for (int i = 0; i < arguments.size(); i++) {
      if (arguments.get(i).is(Tree.Kind.LAMBDA_EXPRESSION, Tree.Kind.METHOD_REFERENCE)) {
        functions.add(i);
      }
    }
    return functions;
  }

  static List<Type> argumentTypes(List<ExpressionTree> arguments) {
    return arguments.stream().map(ExpressionTree::symbolType).toList();
  }

  static LiteralTreeImpl literal(Tree.Kind kind, InternalSyntaxToken token, Type type) {
    LiteralTreeImpl tree = new LiteralTreeImpl(kind, token);
    ascribe(tree, type);
    return tree;
  }

  static boolean namesType(Tree expression) {
    if (expression instanceof IdentifierTree identifier) {
      return identifier.symbol().isTypeSymbol();
    }
    return (expression instanceof MemberSelectExpressionTree select) ? select.identifier().symbol().isTypeSymbol() : (expression instanceof ParameterizedTypeTree);
  }

  private static boolean isTypeVariable(Type type) {
    Type base = type;
    while (base.isArray()) {
      base = ((Type.ArrayType) base).elementType();
    }
    return base.isTypeVar();
  }

  /** Whether a declared type, or the element type of a declared array, spells type arguments. */
  static boolean keepsArguments(Type type) {
    return type.isParameterized() || (type.isArray() && keepsArguments(((Type.ArrayType) type).elementType()));
  }

  static boolean sameErasure(Type left, Type right) {
    return left.erasure().fullyQualifiedName().equals(right.erasure().fullyQualifiedName());
  }

  /** The scan records erased parameter types, while the declared parameters keep their type arguments. */
  static void declaredParameterTypes(JMethodSymbol method, List<VariableTree> parameters) {
    List<Type> erased = method.parameterTypes();
    if (erased.size() != parameters.size()) {
      return;
    }
    List<Type> declared = new ArrayList<>();
    for (int i = 0; i < parameters.size(); i++) {
      Type type = parameters.get(i).type().symbolType();
      if (type.isUnknown() || !(isTypeVariable(type) || sameErasure(type, erased.get(i)))) {
        return;
      }
      declared.add(type);
    }
    method.parameterTypes(declared);
  }

  /** A field is not hidden by a local variable of an enclosing method: a local class declares its own members. */
  static boolean isField(Symbol variable) {
    return variable.owner().isTypeSymbol();
  }

  /** Usages are stored under the symbol's own id, which can differ from the fact key that resolved it. */
  static String usageId(Symbol symbol, String target) {
    if (symbol instanceof JSymbol jSymbol && !jSymbol.isUnknown() && !jSymbol.toString().equals(jSymbol.name())) {
      return jSymbol.toString();
    }
    return target;
  }

  static Type binaryType(String operator, Type left, Type right) {
    if (BOOLEAN_OPERATORS.contains(operator)) {
      return left instanceof JType j ? j.sema.getClassType(BOOLEAN) : Type.UNKNOWN;
    }
    if ("+".equals(operator) && (left.is(JAVA_LANG_STRING) || right.is(JAVA_LANG_STRING))) {
      return left instanceof JType j ? j.sema.getClassType(JAVA_LANG_STRING) : Type.UNKNOWN;
    }
    Type promoted = promotedBinaryType(operator, unboxed(left), unboxed(right));
    if (promoted != null) {
      return promoted;
    }
    if (left.isUnknown()) {
      return right;
    }
    return left;
  }

  static Type unboxed(Type type) {
    return type.isPrimitiveWrapper() ? type.primitiveType() : type;
  }

  /** The type JLS 5.6 gives an arithmetic, bitwise or shift operation, or null when an operand is not a primitive. */
  @Nullable
  private static Type promotedBinaryType(String operator, Type left, Type right) {
    if (!(left instanceof JType j) || !left.isPrimitive() || !right.isPrimitive()) {
      return null;
    }
    boolean bitwise = "&".equals(operator) || "|".equals(operator) || "^".equals(operator);
    if (left.is(BOOLEAN) || right.is(BOOLEAN)) {
      return bitwise && left.is(BOOLEAN) && right.is(BOOLEAN) ? left : null;
    }
    if ("<<".equals(operator) || ">>".equals(operator) || ">>>".equals(operator)) {
      return j.sema.getClassType(left.is("long") ? "long" : "int");
    }
    return j.sema.getClassType(widestNumericName(left, right));
  }

  private static String widestNumericName(Type left, Type right) {
    for (String name : List.of("double", "float", "long")) {
      if (left.is(name) || right.is(name)) {
        return name;
      }
    }
    return "int";
  }

  static Tree.Kind binaryKind(String operator) {
    return switch (operator) {
      case "+" -> Tree.Kind.PLUS;
      case "-" -> Tree.Kind.MINUS;
      case "*" -> Tree.Kind.MULTIPLY;
      case "/" -> Tree.Kind.DIVIDE;
      case "%" -> Tree.Kind.REMAINDER;
      case "<<" -> Tree.Kind.LEFT_SHIFT;
      case ">>" -> Tree.Kind.RIGHT_SHIFT;
      case ">>>" -> Tree.Kind.UNSIGNED_RIGHT_SHIFT;
      case "<" -> Tree.Kind.LESS_THAN;
      case ">" -> Tree.Kind.GREATER_THAN;
      case "<=" -> Tree.Kind.LESS_THAN_OR_EQUAL_TO;
      case ">=" -> Tree.Kind.GREATER_THAN_OR_EQUAL_TO;
      case "==" -> Tree.Kind.EQUAL_TO;
      case "!=" -> Tree.Kind.NOT_EQUAL_TO;
      case "&" -> Tree.Kind.AND;
      case "|" -> Tree.Kind.OR;
      case "^" -> Tree.Kind.XOR;
      case "&&" -> Tree.Kind.CONDITIONAL_AND;
      case "||" -> Tree.Kind.CONDITIONAL_OR;
      default -> Tree.Kind.PLUS;
    };
  }

  static Tree.Kind assignmentKind(String operator) {
    return switch (operator) {
      case "+=" -> Tree.Kind.PLUS_ASSIGNMENT;
      case "-=" -> Tree.Kind.MINUS_ASSIGNMENT;
      case "*=" -> Tree.Kind.MULTIPLY_ASSIGNMENT;
      case "/=" -> Tree.Kind.DIVIDE_ASSIGNMENT;
      case "%=" -> Tree.Kind.REMAINDER_ASSIGNMENT;
      case "&=" -> Tree.Kind.AND_ASSIGNMENT;
      case "|=" -> Tree.Kind.OR_ASSIGNMENT;
      case "^=" -> Tree.Kind.XOR_ASSIGNMENT;
      case "<<=" -> Tree.Kind.LEFT_SHIFT_ASSIGNMENT;
      case ">>=" -> Tree.Kind.RIGHT_SHIFT_ASSIGNMENT;
      case ">>>=" -> Tree.Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT;
      default -> Tree.Kind.ASSIGNMENT;
    };
  }

  static Tree.Kind unaryKind(String operator, boolean prefix) {
    return switch (operator) {
      case "++" -> prefix ? Tree.Kind.PREFIX_INCREMENT : Tree.Kind.POSTFIX_INCREMENT;
      case "--" -> prefix ? Tree.Kind.PREFIX_DECREMENT : Tree.Kind.POSTFIX_DECREMENT;
      case "-" -> Tree.Kind.UNARY_MINUS;
      case "+" -> Tree.Kind.UNARY_PLUS;
      case "~" -> Tree.Kind.BITWISE_COMPLEMENT;
      default -> Tree.Kind.LOGICAL_COMPLEMENT;
    };
  }

  static Type identifierType(Symbol symbol) {
    return symbol instanceof Symbol.MethodSymbol method && !method.isUnknown() ? method.returnType().type() : symbol.type();
  }

  /** A resolved type, or an unresolved one that still carries its written name. */
  static boolean isKnownOrRecovered(Type type) {
    return !type.isUnknown() || !"!Unknown!".equals(type.fullyQualifiedName());
  }

  static Type variableType(VariableTreeImpl variable) {
    return variable.symbol().type().isUnknown() ? variable.type().symbolType() : variable.symbol().type();
  }

  static void ascribe(Tree tree, @Nullable Type type) {
    if (tree instanceof AbstractTypedTree typed && type != null) {
      typed.setSymbolType(type);
    }
  }

  static void bind(IdentifierTreeImpl identifier, @Nullable Symbol symbol) {
    identifier.setSymbol(symbol == null ? Symbol.UNKNOWN_SYMBOL : symbol);
  }

  static void bindMethod(MethodInvocationTreeImpl invocation, @Nullable Symbol.MethodSymbol method) {
    invocation.setMethodSymbol(method == null ? Symbol.MethodSymbol.UNKNOWN_METHOD : method);
  }
}
