/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
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
package org.sonar.java.se.constraint;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.sonar.java.se.Pair;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicValueFactory;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue;
import org.sonar.java.se.symbolicvalues.RelationalSymbolicValue.Kind;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import javax.annotation.Nullable;

import java.util.List;

public class ConstraintManager {

  private SymbolicValueFactory symbolicValueFactory;

  public void setValueFactory(SymbolicValueFactory valueFactory) {
    Preconditions.checkState(symbolicValueFactory == null, "The symbolic value factory has already been defined by another checker!");
    symbolicValueFactory = valueFactory;
  }

  public SymbolicValue createSymbolicValue(Tree syntaxNode) {
    SymbolicValue result;
    switch (syntaxNode.kind()) {
      case LOGICAL_COMPLEMENT:
        result = new SymbolicValue.NotSymbolicValue();
        break;
      case INSTANCE_OF:
        result = new SymbolicValue.InstanceOfSymbolicValue();
        break;
      case MEMBER_SELECT:
        result = createIdentifierSymbolicValue(((MemberSelectExpressionTree) syntaxNode).identifier());
        break;
      case IDENTIFIER:
        result = createIdentifierSymbolicValue((IdentifierTree) syntaxNode);
        break;
      default:
        result = createDefaultSymbolicValue();
    }
    return result;
  }

  public SymbolicValue createBinarySymbolicValue(Tree syntaxNode, List<ProgramState.SymbolicValueSymbol> computedFrom) {
    SymbolicValue result;
    switch (syntaxNode.kind()) {
      case EQUAL_TO:
        result = createRelationalSymbolicValue(Kind.EQUAL, computedFrom);
        break;
      case NOT_EQUAL_TO:
        result = createRelationalSymbolicValue(Kind.NOT_EQUAL, computedFrom);
        break;
      case LESS_THAN:
        result = createRelationalSymbolicValue(Kind.LESS_THAN, computedFrom);
        break;
      case LESS_THAN_OR_EQUAL_TO:
        result = createRelationalSymbolicValue(Kind.GREATER_THAN_OR_EQUAL, Lists.reverse(computedFrom));
        break;
      case GREATER_THAN:
        result = createRelationalSymbolicValue(Kind.LESS_THAN, Lists.reverse(computedFrom));
        break;
      case GREATER_THAN_OR_EQUAL_TO:
        result = createRelationalSymbolicValue(Kind.GREATER_THAN_OR_EQUAL, computedFrom);
        break;
      case AND:
      case AND_ASSIGNMENT:
        result = new SymbolicValue.AndSymbolicValue();
        result.computedFrom(computedFrom);
        break;
      case OR:
      case OR_ASSIGNMENT:
        result = new SymbolicValue.OrSymbolicValue();
        result.computedFrom(computedFrom);
        break;
      case XOR:
      case XOR_ASSIGNMENT:
        result = new SymbolicValue.XorSymbolicValue();
        result.computedFrom(computedFrom);
        break;
      default:
        result = createDefaultSymbolicValue();
        result.computedFrom(computedFrom);
    }
    return result;
  }

  private static RelationalSymbolicValue createRelationalSymbolicValue(Kind kind, List<ProgramState.SymbolicValueSymbol> computedFrom) {
    RelationalSymbolicValue result = new RelationalSymbolicValue(kind);
    result.computedFrom(computedFrom);
    return result;
  }

  public SymbolicValue.ExceptionalSymbolicValue createExceptionalSymbolicValue(@Nullable Type exceptionType) {
    return new SymbolicValue.ExceptionalSymbolicValue(exceptionType);
  }

  public SymbolicValue createMethodSymbolicValue(MethodInvocationTree syntaxNode, List<ProgramState.SymbolicValueSymbol> values) {
    SymbolicValue result;
    if (isEqualsMethod(syntaxNode) || isObjectsEqualsMethod(syntaxNode.symbol())) {
      result = new RelationalSymbolicValue(RelationalSymbolicValue.Kind.METHOD_EQUALS);
      ProgramState.SymbolicValueSymbol leftOp = values.get(1);
      ProgramState.SymbolicValueSymbol rightOp = values.get(0);
      result.computedFrom(ImmutableList.of(rightOp, leftOp));
    } else {
      result = createDefaultSymbolicValue();
    }
    return result;
  }

  private static boolean isObjectsEqualsMethod(Symbol symbol) {
    return symbol.isMethodSymbol() && symbol.owner().type().is("java.util.Objects") && "equals".equals(symbol.name());
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
    return createDefaultSymbolicValue();
  }

  private SymbolicValue createDefaultSymbolicValue() {
    SymbolicValue result;
    result = symbolicValueFactory == null ? new SymbolicValue() : symbolicValueFactory.createSymbolicValue();
    symbolicValueFactory = null;
    return result;
  }

  public boolean isNull(ProgramState ps, SymbolicValue val) {
    ObjectConstraint constraint = ps.getConstraint(val, ObjectConstraint.class);
    return constraint!= null && constraint.isNull();
  }

  public Pair<List<ProgramState>, List<ProgramState>> assumeDual(ProgramState programState) {

    ProgramState.Pop unstack = programState.unstackValue(1);
    SymbolicValue sv = unstack.values.get(0);
    List<ProgramState> falseConstraint = sv.setConstraint(unstack.state, BooleanConstraint.FALSE);
    List<ProgramState> trueConstraint = sv.setConstraint(unstack.state, BooleanConstraint.TRUE);
    return new Pair<>(falseConstraint, trueConstraint);
  }
  
}
