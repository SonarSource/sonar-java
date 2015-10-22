/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.model;

import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
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
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.SynchronizedStatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

public abstract class SyntaxTreeDebug {

  public static String toString(Tree syntaxNode) {
    switch (syntaxNode.kind()) {
      case ARGUMENTS:
        return SyntaxTreeDebug.argumentsString((Arguments) syntaxNode);
      case VARIABLE:
        return SyntaxTreeDebug.variableString((VariableTree) syntaxNode);
      case IDENTIFIER:
        return SyntaxTreeDebug.identifierString((IdentifierTree) syntaxNode);
      case METHOD_INVOCATION:
        return SyntaxTreeDebug.methodInvocationString((MethodInvocationTree) syntaxNode);
      case MEMBER_SELECT:
        return SyntaxTreeDebug.memberSelectString((MemberSelectExpressionTree) syntaxNode);
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
        return SyntaxTreeDebug.binaryExpressionString((BinaryExpressionTree) syntaxNode);
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
        return SyntaxTreeDebug.assignmentString((AssignmentExpressionTree) syntaxNode);
      case NULL_LITERAL:
        return "null";
      case STRING_LITERAL:
      case CHAR_LITERAL:
      case BOOLEAN_LITERAL:
      case INT_LITERAL:
      case LONG_LITERAL:
      case FLOAT_LITERAL:
      case DOUBLE_LITERAL:
        return SyntaxTreeDebug.litteralString((LiteralTree) syntaxNode);
      case IF_STATEMENT:
        return SyntaxTreeDebug.ifStatementString((IfStatementTree) syntaxNode);
      case FOR_EACH_STATEMENT:
        return SyntaxTreeDebug.forEachStatementString((ForEachStatement) syntaxNode);
      case FOR_STATEMENT:
        return SyntaxTreeDebug.forStatementString((ForStatementTree) syntaxNode);
      case NEW_CLASS:
        return SyntaxTreeDebug.newInstanceString((NewClassTree) syntaxNode);
      case LIST:
        return SyntaxTreeDebug.listString((ListTree<?>) syntaxNode);
      case INSTANCE_OF:
        return SyntaxTreeDebug.instanceOfString((InstanceOfTree) syntaxNode);
      case RETURN_STATEMENT:
        return SyntaxTreeDebug.returnString((ReturnStatementTree) syntaxNode);
      case CONDITIONAL_EXPRESSION:
        return SyntaxTreeDebug.conditionalExpressionString((ConditionalExpressionTree) syntaxNode);
      case EMPTY_STATEMENT:
      case TRY_STATEMENT:
      case DO_STATEMENT:
        return "";
      case SYNCHRONIZED_STATEMENT:
        return SyntaxTreeDebug.synchronizedStatementString((SynchronizedStatementTree) syntaxNode);
      case PREFIX_DECREMENT:
      case PREFIX_INCREMENT:
      case LOGICAL_COMPLEMENT:
      case BITWISE_COMPLEMENT:
      case UNARY_MINUS:
      case UNARY_PLUS:
        return SyntaxTreeDebug.prefixExpressionString((UnaryExpressionTree) syntaxNode);
      case POSTFIX_DECREMENT:
      case POSTFIX_INCREMENT:
        return SyntaxTreeDebug.postfixExpressionString((UnaryExpressionTree) syntaxNode);
      case TYPE_CAST:
        return SyntaxTreeDebug.typeCastString((TypeCastTree) syntaxNode);
      case PARENTHESIZED_EXPRESSION:
        return SyntaxTreeDebug.parenthetizedTreeString((ParenthesizedTree) syntaxNode);
      case WHILE_STATEMENT:
        return SyntaxTreeDebug.whileStatementString((WhileStatementTree) syntaxNode);
      case SWITCH_STATEMENT:
        return SyntaxTreeDebug.switchStatementString((SwitchStatementTree) syntaxNode);
      case BREAK_STATEMENT:
        return "break";
      case CONTINUE_STATEMENT:
        return "continue";
      case ARRAY_ACCESS_EXPRESSION:
        return SyntaxTreeDebug.arrayAccessString((ArrayAccessExpressionTree) syntaxNode);
      case THROW_STATEMENT:
        return SyntaxTreeDebug.throwStatementString((ThrowStatementTree) syntaxNode);
      case EXPRESSION_STATEMENT:
        return SyntaxTreeDebug.expressionStatementString((ExpressionStatementTree) syntaxNode);
      case METHOD:
        return SyntaxTreeDebug.methodString((MethodTree) syntaxNode);
      default:
        return syntaxNode.toString();
    }
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
    StringBuilder buffer = new StringBuilder("synchronized(");
    buffer.append(toString(syntaxNode.expression()));
    buffer.append(')');
    return buffer.toString();
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
    return SyntaxTreeDebug.toString(syntaxNode.expression());
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

  private static String litteralString(LiteralTree litteral) {
    return litteral.token().text();
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
        buffer.append(litteralString((LiteralTree) target));
        break;
      default:
        break;
    }
    buffer.append('.');
    buffer.append(identifierString(expression.identifier()));
    return buffer.toString();
  }

  private static String prefixExpressionString(UnaryExpressionTree syntaxNode) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(syntaxNode.operatorToken().text());
    buffer.append(SyntaxTreeDebug.toString(syntaxNode.expression()));
    return buffer.toString();
  }

  private static String postfixExpressionString(UnaryExpressionTree syntaxNode) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(SyntaxTreeDebug.toString(syntaxNode.expression()));
    buffer.append(syntaxNode.operatorToken().text());
    return buffer.toString();
  }

  private static String typeCastString(TypeCastTree syntaxNode) {
    StringBuilder buffer = new StringBuilder();
    buffer.append('(');
    buffer.append(syntaxNode.type().toString());
    buffer.append(") ");
    return buffer.toString();
  }

  private static String assignmentString(AssignmentExpressionTree syntaxNode) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(SyntaxTreeDebug.toString(syntaxNode.variable()));
    buffer.append(syntaxNode.operatorToken().text());
    buffer.append(SyntaxTreeDebug.toString(syntaxNode.expression()));
    return buffer.toString();
  }

  private static String ifStatementString(IfStatementTree syntaxNode) {
    StringBuilder buffer = new StringBuilder("if (");
    buffer.append(SyntaxTreeDebug.toString(syntaxNode.condition()));
    buffer.append(')');
    return buffer.toString();
  }

  private static String forStatementString(ForStatementTree syntaxNode) {
    StringBuilder buffer = new StringBuilder("for {");
    if (syntaxNode.initializer() != null) {
      buffer.append(SyntaxTreeDebug.toString(syntaxNode.initializer()));
    }
    buffer.append(';');
    if (syntaxNode.condition() != null) {
      buffer.append(SyntaxTreeDebug.toString(syntaxNode.condition()));
    }
    buffer.append(';');
    if (syntaxNode.update() != null) {
      buffer.append(SyntaxTreeDebug.toString(syntaxNode.update()));
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
    StringBuilder buffer = new StringBuilder();
    buffer.append(toString(syntaxNode.leftOperand()));
    buffer.append(' ');
    buffer.append(syntaxNode.operatorToken().text());
    buffer.append(' ');
    buffer.append(toString(syntaxNode.rightOperand()));
    return buffer.toString();
  }

  private static String conditionalExpressionString(ConditionalExpressionTree syntaxNode) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(toString(syntaxNode.condition()));
    buffer.append(" ? ");
    buffer.append(toString(syntaxNode.trueExpression()));
    buffer.append(" : ");
    buffer.append(toString(syntaxNode.falseExpression()));
    return buffer.toString();
  }

  private static String instanceOfString(InstanceOfTree syntaxNode) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(toString(syntaxNode.expression()));
    buffer.append(' ');
    buffer.append(syntaxNode.instanceofKeyword().text());
    buffer.append(' ');
    buffer.append(syntaxNode.type().toString());
    return buffer.toString();
  }

  private static String returnString(ReturnStatementTree syntaxNode) {
    StringBuilder buffer = new StringBuilder("return");
    if (syntaxNode.expression() != null) {
      buffer.append(' ');
      buffer.append(toString(syntaxNode.expression()));
    }
    return buffer.toString();
  }

  private static String parenthetizedTreeString(ParenthesizedTree syntaxNode) {
    StringBuilder buffer = new StringBuilder();
    buffer.append('(');
    buffer.append(toString(syntaxNode.expression()));
    buffer.append(')');
    return buffer.toString();
  }

  private static String whileStatementString(WhileStatementTree syntaxNode) {
    StringBuilder buffer = new StringBuilder("while (");
    buffer.append(toString(syntaxNode.condition()));
    buffer.append(')');
    return buffer.toString();
  }

  private static String switchStatementString(SwitchStatementTree syntaxNode) {
    StringBuilder buffer = new StringBuilder("switch (");
    buffer.append(toString(syntaxNode.expression()));
    buffer.append(')');
    return buffer.toString();
  }

  private static String forEachStatementString(ForEachStatement syntaxNode) {
    StringBuilder buffer = new StringBuilder("for {");
    buffer.append(variableString(syntaxNode.variable()));
    buffer.append(" : ");
    buffer.append(toString(syntaxNode.expression()));
    buffer.append('}');
    return buffer.toString();
  }

  private static String arrayAccessString(ArrayAccessExpressionTree syntaxNode) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(toString(syntaxNode.expression()));
    buffer.append('[');
    buffer.append(toString(syntaxNode.dimension().expression()));
    buffer.append(']');
    return buffer.toString();
  }

  private static String throwStatementString(ThrowStatementTree syntaxNode) {
    StringBuilder buffer = new StringBuilder("throw ");
    buffer.append(toString(syntaxNode.expression()));
    return buffer.toString();
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

  private static String symbolString(Symbol symbol) {
    return symbol.name();
  }
}
