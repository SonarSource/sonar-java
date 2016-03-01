/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.se.constraint;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.sonar.java.se.Pair;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicValueFactory;
import org.sonar.java.se.symbolicvalues.NullCheckSymbolicValue;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

public class ConstraintManager {

  private int counter = ProgramState.EMPTY_STATE.constraintsSize();
  private SymbolicValueFactory symbolicValueFactory;

  public void setValueFactory(SymbolicValueFactory valueFactory) {
    Preconditions.checkState(symbolicValueFactory == null, "The symbolic value factory has already been defined by another checker!");
    symbolicValueFactory = valueFactory;
  }

  public SymbolicValue createSymbolicValue(Tree syntaxNode) {
    SymbolicValue result;
    switch (syntaxNode.kind()) {
      case EQUAL_TO:
        result = new RelationalSymbolicValue(counter, RelationalSymbolicValue.Kind.EQUAL);
        break;
      case NOT_EQUAL_TO:
        result = new RelationalSymbolicValue(counter, RelationalSymbolicValue.Kind.NOT_EQUAL);
        break;
      case LESS_THAN:
        result = new RelationalSymbolicValue(counter, RelationalSymbolicValue.Kind.LESS_THAN);
        break;
      case LESS_THAN_OR_EQUAL_TO:
        result = new RelationalSymbolicValue(counter, RelationalSymbolicValue.Kind.LESS_THAN_OR_EQUAL);
        break;
      case GREATER_THAN:
        result = new RelationalSymbolicValue(counter, RelationalSymbolicValue.Kind.GREATER_THAN);
        break;
      case GREATER_THAN_OR_EQUAL_TO:
        result = new RelationalSymbolicValue(counter, RelationalSymbolicValue.Kind.GREATER_THAN_OR_EQUAL);
        break;
      case LOGICAL_COMPLEMENT:
        result = new SymbolicValue.NotSymbolicValue(counter);
        break;
      case AND:
      case AND_ASSIGNMENT:
        result = new SymbolicValue.AndSymbolicValue(counter);
        break;
      case OR:
      case OR_ASSIGNMENT:
        result = new SymbolicValue.OrSymbolicValue(counter);
        break;
      case XOR:
      case XOR_ASSIGNMENT:
        result = new SymbolicValue.XorSymbolicValue(counter);
        break;
      case INSTANCE_OF:
        result = new SymbolicValue.InstanceOfSymbolicValue(counter);
        break;
      case MEMBER_SELECT:
        result = createIdentifierSymbolicValue(((MemberSelectExpressionTree) syntaxNode).identifier());
        break;
      case IDENTIFIER:
        result = createIdentifierSymbolicValue((IdentifierTree) syntaxNode);
        break;
      default:
        result = createDefaultSymbolicValue(syntaxNode);
    }
    counter++;
    return result;
  }

  public SymbolicValue createMethodSymbolicValue(MethodInvocationTree syntaxNode, List<SymbolicValue> values) {
    SymbolicValue result;
    if (isEqualsMethod(syntaxNode)) {
      result = new RelationalSymbolicValue(counter, RelationalSymbolicValue.Kind.METHOD_EQUALS);
      SymbolicValue leftOp = values.get(0);
      SymbolicValue rightOp = values.get(1);
      result.computedFrom(ImmutableList.of(rightOp, leftOp));
    } else if (isObjectsMethod(syntaxNode.symbol(), "isNull")) {
      result = new NullCheckSymbolicValue(counter, true);
      SymbolicValue operand = values.get(1);
      result.computedFrom(ImmutableList.of(operand));
    } else if (isObjectsMethod(syntaxNode.symbol(), "nonNull")) {
      result = new NullCheckSymbolicValue(counter, false);
      SymbolicValue operand = values.get(1);
      result.computedFrom(ImmutableList.of(operand));
    } else {
      result = createDefaultSymbolicValue(syntaxNode);
    }
    counter++;
    return result;
  }

  private static boolean isObjectsMethod(Symbol symbol, String methodName) {
    return symbol.owner().type().is("java.util.Objects") && methodName.equals(symbol.name());
  }

  private static boolean isEqualsMethod(MethodInvocationTree syntaxNode) {
    if (syntaxNode.arguments().size() == 1) {
      ExpressionTree methodSelect = syntaxNode.methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree expression = (MemberSelectExpressionTree) methodSelect;
        if ("equals".equals(expression.identifier().name()) && syntaxNode.symbol().isMethodSymbol()) {
          Symbol.MethodSymbol symbol = (Symbol.MethodSymbol) syntaxNode.symbol();
          return symbol.parameterTypes().get(0).is("java.lang.Object");
        }
      }
    }
    return false;
  }

  private SymbolicValue createIdentifierSymbolicValue(IdentifierTree identifier) {
    final Type type = identifier.symbol().type();
    if (type != null && type.is("java.lang.Boolean")) {
      if ("TRUE".equals(identifier.name())) {
        return SymbolicValue.TRUE_LITERAL;
      } else if ("FALSE".equals(identifier.name())) {
        return SymbolicValue.FALSE_LITERAL;
      }
    }
    return createDefaultSymbolicValue(identifier);
  }

  private SymbolicValue createDefaultSymbolicValue(Tree syntaxNode) {
    SymbolicValue result;
    result = symbolicValueFactory == null ? new SymbolicValue(counter) : symbolicValueFactory.createSymbolicValue(counter, syntaxNode);
    symbolicValueFactory = null;
    return result;
  }

  public SymbolicValue evalLiteral(LiteralTree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.NULL_LITERAL)) {
      return SymbolicValue.NULL_LITERAL;
    } else if (syntaxNode.is(Tree.Kind.BOOLEAN_LITERAL)) {
      boolean value = Boolean.parseBoolean(syntaxNode.value());
      if (value) {
        return SymbolicValue.TRUE_LITERAL;
      }
      return SymbolicValue.FALSE_LITERAL;
    }
    return createSymbolicValue(syntaxNode);
  }

  public boolean isNull(ProgramState ps, SymbolicValue val) {
    Object constraint = ps.getConstraint(val);
    return constraint instanceof ObjectConstraint && ((ObjectConstraint) constraint).isNull();
  }

  public Pair<List<ProgramState>, List<ProgramState>> assumeDual(ProgramState programState) {

    ProgramState.Pop unstack = programState.unstackValue(1);
    SymbolicValue sv = unstack.values.get(0);
    List<ProgramState> falseConstraint = sv.setConstraint(unstack.state, BooleanConstraint.FALSE);
    List<ProgramState> trueConstraint = sv.setConstraint(unstack.state, BooleanConstraint.TRUE);
    return new Pair<>(falseConstraint, trueConstraint);
  }

}
