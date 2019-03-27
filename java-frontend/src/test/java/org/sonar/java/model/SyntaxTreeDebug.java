/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.BreakStatementTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.ForEachStatement;
import org.sonar.plugins.java.api.tree.ForStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SwitchExpressionTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

public class SyntaxTreeDebug {

  private SyntaxTreeDebug() {
  }

  public static String toString(Tree syntaxNode) {
    switch (syntaxNode.kind()) {
      case ARGUMENTS:
        return argumentsString((Arguments) syntaxNode);
      case VARIABLE:
        return variableString((VariableTree) syntaxNode);
      case IDENTIFIER:
        return identifierString((IdentifierTree) syntaxNode);
      case METHOD_INVOCATION:
        return methodInvocationString((MethodInvocationTree) syntaxNode);
      case MEMBER_SELECT:
        return memberSelectString((MemberSelectExpressionTree) syntaxNode);
      case EQUAL_TO:
      case NOT_EQUAL_TO:
      case CONDITIONAL_AND:
      case CONDITIONAL_OR:
      case LESS_THAN:
      case LESS_THAN_OR_EQUAL_TO:
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUAL_TO:
      case AND:
      case OR:
      case XOR:
      case PLUS:
      case MINUS:
      case MULTIPLY:
      case DIVIDE:
      case REMAINDER:
      case LEFT_SHIFT:
      case RIGHT_SHIFT:
      case UNSIGNED_RIGHT_SHIFT:
        return binaryExpressionString((BinaryExpressionTree) syntaxNode);
      case ASSIGNMENT:
      case PLUS_ASSIGNMENT:
      case MINUS_ASSIGNMENT:
      case MULTIPLY_ASSIGNMENT:
      case DIVIDE_ASSIGNMENT:
      case REMAINDER_ASSIGNMENT:
      case AND_ASSIGNMENT:
      case LEFT_SHIFT_ASSIGNMENT:
      case RIGHT_SHIFT_ASSIGNMENT:
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
        return assignmentString((AssignmentExpressionTree) syntaxNode);
      case NULL_LITERAL:
        return "null";
      case STRING_LITERAL:
      case CHAR_LITERAL:
      case BOOLEAN_LITERAL:
      case INT_LITERAL:
      case LONG_LITERAL:
      case FLOAT_LITERAL:
      case DOUBLE_LITERAL:
        return literalString((LiteralTree) syntaxNode);
      case IF_STATEMENT:
        return ifStatementString((IfStatementTree) syntaxNode);
      case FOR_EACH_STATEMENT:
        return forEachStatementString((ForEachStatement) syntaxNode);
      case FOR_STATEMENT:
        return forStatementString((ForStatementTree) syntaxNode);
      case NEW_CLASS:
        return newInstanceString((NewClassTree) syntaxNode);
      case LIST:
        return listString((ListTree<?>) syntaxNode);
      case INSTANCE_OF:
        return instanceOfString((InstanceOfTree) syntaxNode);
      case RETURN_STATEMENT:
        return returnString((ReturnStatementTree) syntaxNode);
      case CONDITIONAL_EXPRESSION:
        return conditionalExpressionString((ConditionalExpressionTree) syntaxNode);
      case EMPTY_STATEMENT:
      case TRY_STATEMENT:
      case DO_STATEMENT:
        return "";
      case SYNCHRONIZED_STATEMENT:
        return synchronizedStatementString((SynchronizedStatementTree) syntaxNode);
      case PREFIX_DECREMENT:
      case PREFIX_INCREMENT:
      case LOGICAL_COMPLEMENT:
      case BITWISE_COMPLEMENT:
      case UNARY_MINUS:
      case UNARY_PLUS:
        return prefixExpressionString((UnaryExpressionTree) syntaxNode);
      case POSTFIX_DECREMENT:
      case POSTFIX_INCREMENT:
        return postfixExpressionString((UnaryExpressionTree) syntaxNode);
      case TYPE_CAST:
        return typeCastString((TypeCastTree) syntaxNode);
      case PARENTHESIZED_EXPRESSION:
        return parenthesizedTreeString((ParenthesizedTree) syntaxNode);
      case WHILE_STATEMENT:
        return whileStatementString((WhileStatementTree) syntaxNode);
      case SWITCH_STATEMENT:
        return switchStatementString((SwitchStatementTree) syntaxNode);
      case SWITCH_EXPRESSION:
        return switchExpressionString((SwitchExpressionTree) syntaxNode);
      case BREAK_STATEMENT:
        return breakStatementString((BreakStatementTree) syntaxNode);
      case CONTINUE_STATEMENT:
        return "continue";
      case ARRAY_ACCESS_EXPRESSION:
        return arrayAccessString((ArrayAccessExpressionTree) syntaxNode);
      case THROW_STATEMENT:
        return throwStatementString((ThrowStatementTree) syntaxNode);
      case EXPRESSION_STATEMENT:
        return expressionStatementString((ExpressionStatementTree) syntaxNode);
      case METHOD:
        return methodString((MethodTree) syntaxNode);
      case BLOCK:
        return blockString((BlockTree) syntaxNode);
      case NEW_ARRAY:
        return "new []";
      default:
        return syntaxNode.toString();
    }
  }

  private static String blockString(BlockTree syntaxNode) {
    StringBuilder buffer = new StringBuilder();
    boolean first = true;
    for (StatementTree tree : syntaxNode.body()) {
      if (first) {
        first = false;
      } else {
        buffer.append(", ");
      }
      buffer.append(tree);
    }
    return buffer.toString();
  }

  private static String methodString(MethodTree syntaxNode) {
    StringBuilder buffer = new StringBuilder();
    for (ModifierTree modifier : syntaxNode.modifiers()) {
      if (modifier.is(Tree.Kind.TOKEN)) {
        buffer.append(((SyntaxToken) modifier).text());
        buffer.append(' ');
      }
    }
    buffer.append(syntaxNode.returnType().symbolType().toString());
    buffer.append(' ');
    buffer.append(syntaxNode.simpleName().name());
    buffer.append('(');
    boolean first = true;
    for (VariableTree parameter : syntaxNode.parameters()) {
      if (first) {
        first = false;
      } else {
        buffer.append(", ");
      }
      buffer.append(parameter.type().symbolType().toString());
      buffer.append(' ');
      buffer.append(parameter.simpleName().toString());
    }
    buffer.append(')');
    return buffer.toString();
  }

  private static String synchronizedStatementString(SynchronizedStatementTree syntaxNode) {
    return "synchronized(" + toString(syntaxNode.expression()) + ')';
  }

  private static String argumentsString(Arguments syntaxNode) {
    StringBuilder buffer = new StringBuilder();
    boolean first = true;
    for (ExpressionTree expressionTree : syntaxNode) {
      if (first) {
        first = false;
      } else {
        buffer.append(',');
      }
      buffer.append(toString(expressionTree));
    }
    return buffer.toString();
  }

  private static String expressionStatementString(ExpressionStatementTree syntaxNode) {
    return toString(syntaxNode.expression());
  }

  private static String methodInvocationString(MethodInvocationTree method) {
    StringBuilder buffer = new StringBuilder();
    ExpressionTree methodSelect = method.methodSelect();
    if (methodSelect.is(Tree.Kind.IDENTIFIER)) {
      buffer.append(identifierString((IdentifierTree) methodSelect));
    } else if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
      buffer.append(memberSelectString((MemberSelectExpressionTree) methodSelect));
    }
    buffer.append('(');
    buffer.append(toString(method.arguments()));
    buffer.append(')');
    return buffer.toString();
  }

  private static String variableString(VariableTree variable) {
    return variable.simpleName().name();
  }

  private static String identifierString(IdentifierTree identifier) {
    return identifier.name();
  }

  private static String literalString(LiteralTree literal) {
    return literal.token().text();
  }

  private static String memberSelectString(MemberSelectExpressionTree expression) {
    StringBuilder buffer = new StringBuilder();
    ExpressionTree target = expression.expression();
    switch (target.kind()) {
      case IDENTIFIER:
        buffer.append(identifierString((IdentifierTree) target));
        break;
      case METHOD_INVOCATION:
        buffer.append(methodInvocationString((MethodInvocationTree) target));
        break;
      case VARIABLE:
        buffer.append(variableString((VariableTree) target));
        break;
      case INT_LITERAL:
        buffer.append(literalString((LiteralTree) target));
        break;
      default:
        break;
    }
    buffer.append('.');
    buffer.append(identifierString(expression.identifier()));
    return buffer.toString();
  }

  private static String prefixExpressionString(UnaryExpressionTree syntaxNode) {
    return syntaxNode.operatorToken().text() + toString(syntaxNode.expression());
  }

  private static String postfixExpressionString(UnaryExpressionTree syntaxNode) {
    return toString(syntaxNode.expression()) + syntaxNode.operatorToken().text();
  }

  private static String typeCastString(TypeCastTree syntaxNode) {
    return "(" + syntaxNode.type().toString() + ") ";
  }

  private static String assignmentString(AssignmentExpressionTree syntaxNode) {
    return toString(syntaxNode.variable()) + syntaxNode.operatorToken().text() + toString(syntaxNode.expression());
  }

  private static String ifStatementString(IfStatementTree syntaxNode) {
    return "if (" + toString(syntaxNode.condition()) + ')';
  }

  private static String forStatementString(ForStatementTree syntaxNode) {
    StringBuilder buffer = new StringBuilder("for {");
    if (syntaxNode.initializer() != null) {
      buffer.append(toString(syntaxNode.initializer()));
    }
    buffer.append(';');
    if (syntaxNode.condition() != null) {
      buffer.append(toString(syntaxNode.condition()));
    }
    buffer.append(';');
    if (syntaxNode.update() != null) {
      buffer.append(toString(syntaxNode.update()));
    }
    buffer.append('}');
    return buffer.toString();
  }

  private static String listString(ListTree<?> syntaxNode) {
    StringBuilder buffer = new StringBuilder();
    boolean first = true;
    for (Object object : syntaxNode) {
      if (first) {
        first = false;
      } else {
        buffer.append(',');
      }
      buffer.append(toString((Tree) object));
    }
    return buffer.toString();
  }

  private static String binaryExpressionString(BinaryExpressionTree syntaxNode) {
    return toString(syntaxNode.leftOperand()) + ' ' + syntaxNode.operatorToken().text() + ' ' + toString(syntaxNode.rightOperand());
  }

  private static String conditionalExpressionString(ConditionalExpressionTree syntaxNode) {
    return toString(syntaxNode.condition()) + " ? " + toString(syntaxNode.trueExpression()) + " : " + toString(syntaxNode.falseExpression());
  }

  private static String instanceOfString(InstanceOfTree syntaxNode) {
    return toString(syntaxNode.expression()) + ' ' + syntaxNode.instanceofKeyword().text() + ' ' + syntaxNode.type().toString();
  }

  private static String returnString(ReturnStatementTree syntaxNode) {
    StringBuilder buffer = new StringBuilder("return");
    if (syntaxNode.expression() != null) {
      buffer.append(' ');
      buffer.append(toString(syntaxNode.expression()));
    }
    return buffer.toString();
  }

  private static String parenthesizedTreeString(ParenthesizedTree syntaxNode) {
    return "(" + toString(syntaxNode.expression()) + ')';
  }

  private static String whileStatementString(WhileStatementTree syntaxNode) {
    return "while (" + toString(syntaxNode.condition()) + ')';
  }

  private static String switchStatementString(SwitchStatementTree syntaxNode) {
    return switchExpressionString(syntaxNode.asSwitchExpression());
  }

  private static String switchExpressionString(SwitchExpressionTree syntaxNode) {
    return "switch (" + toString(syntaxNode.expression()) + ')';
  }

  private static String breakStatementString(BreakStatementTree syntaxNode) {
    ExpressionTree value = syntaxNode.value();
    if (value == null) {
      return "break";
    }
    return "break " + toString(value);
  }

  private static String forEachStatementString(ForEachStatement syntaxNode) {
    return "for {" + variableString(syntaxNode.variable()) + " : " + toString(syntaxNode.expression()) + '}';
  }

  private static String arrayAccessString(ArrayAccessExpressionTree syntaxNode) {
    return toString(syntaxNode.expression()) + '[' + toString(syntaxNode.dimension().expression()) + ']';
  }

  private static String throwStatementString(ThrowStatementTree syntaxNode) {
    return "throw " + toString(syntaxNode.expression());
  }

  private static String newInstanceString(NewClassTree syntaxNode) {
    StringBuilder buffer = new StringBuilder("new ");
    buffer.append(syntaxNode.identifier().toString());
    buffer.append('(');
    boolean first = true;
    for (ExpressionTree argument : syntaxNode.arguments()) {
      if (first) {
        first = false;
      } else {
        buffer.append(',');
      }
      buffer.append(toString(argument));
    }
    buffer.append(')');
    return buffer.toString();
  }

}
