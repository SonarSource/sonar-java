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
package org.sonar.java.se;

import com.google.common.collect.Maps;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.UnaryExpressionTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import java.util.HashMap;
import java.util.Map;

public class ConstraintManager {

  private final Map<Tree, SymbolicValue> map = new HashMap<>();
  /**
   * Map to handle lookup of fields.
   * */
  private final Map<Symbol, SymbolicValue> symbolMap = new HashMap<>();



  private SymbolicValue createSymbolicValue(Tree syntaxNode) {
    SymbolicValue result = map.get(syntaxNode);
    if (result == null && syntaxNode.is(Tree.Kind.IDENTIFIER)) {
      result = symbolMap.get(((IdentifierTree) syntaxNode).symbol());
    } else if (result == null && syntaxNode.is(Tree.Kind.VARIABLE)) {
      result = symbolMap.get(((VariableTree) syntaxNode).symbol());
    }
    if (result == null) {
      result = new SymbolicValue.ObjectSymbolicValue(map.size()+ProgramState.EMPTY_STATE.constraints.size());
      map.put(syntaxNode, result);
      if(syntaxNode.is(Tree.Kind.IDENTIFIER)) {
        symbolMap.put(((IdentifierTree) syntaxNode).symbol(), result);
      } else if(syntaxNode.is(Tree.Kind.VARIABLE)) {
        symbolMap.put(((VariableTree) syntaxNode).symbol(), result);
      }
    }
    return result;
  }


  public SymbolicValue eval(ProgramState programState, Tree syntaxNode) {
    syntaxNode = skipTrivial(syntaxNode);
    switch (syntaxNode.kind()) {
      case NULL_LITERAL: {
        return SymbolicValue.NULL_LITERAL;
      }
      case BOOLEAN_LITERAL: {
        boolean value = Boolean.parseBoolean(((LiteralTree) syntaxNode).value());
        if(value) {
          return SymbolicValue.TRUE_LITERAL;
        }
        return SymbolicValue.FALSE_LITERAL;
      }
      case VARIABLE: {
        Symbol symbol = ((VariableTree) syntaxNode).symbol();
        SymbolicValue result = programState.values.get(symbol);
        if (result != null) {
          // symbolic value associated with local variable
          return result;
        }
        break;
      }
      case IDENTIFIER: {
        Symbol symbol = ((IdentifierTree) syntaxNode).symbol();
        SymbolicValue result = programState.values.get(symbol);
        if (result != null) {
          // symbolic value associated with local variable
          return result;
        }
        break;
      }
      case LOGICAL_COMPLEMENT : {
        UnaryExpressionTree unaryExpressionTree = (UnaryExpressionTree) syntaxNode;
        SymbolicValue val = eval(programState, unaryExpressionTree.expression());
        if(SymbolicValue.FALSE_LITERAL.equals(val)) {
          return SymbolicValue.TRUE_LITERAL;
        } else if(val.equals(SymbolicValue.TRUE_LITERAL)) {
          return SymbolicValue.FALSE_LITERAL;
        }
        //if not tied to a concrete value, create symbolic value with no constraint for now.
        //TODO : create constraint between expression and created symbolic value
      }
    }
    return createSymbolicValue(syntaxNode);
  }

  /**
   * Remove parenthesis and type cast.
   */
  private static Tree skipTrivial(Tree expression) {
    do {
      switch (expression.kind()) {
        case PARENTHESIZED_EXPRESSION:
          expression = ((ParenthesizedTree) expression).expression();
          break;
        case TYPE_CAST:
          expression = ((TypeCastTree) expression).expression();
          break;
        default:
          return expression;
      }
    } while (true);

  }

  public boolean isNull(ProgramState ps, SymbolicValue val){
    return NullConstraint.NULL.equals(ps.constraints.get(val));
  }

  public Pair<ProgramState, ProgramState> assumeDual(ProgramState programState, Tree condition) {
    //FIXME condition value should be evaluated to determine if it is worth exploring this branch. This should probably be done in a dedicated checker.
    condition = skipTrivial(condition);
    switch (condition.kind()) {
      case INSTANCE_OF: {
        InstanceOfTree instanceOfTree = (InstanceOfTree) condition;
        SymbolicValue exprValue = eval(programState, instanceOfTree.expression());
        if(isNull(programState, exprValue)) {
          return new Pair<>(programState, null);
        }
        //if instanceof is true then we know for sure that expression is not null.
        return new Pair<>(programState, setConstraint(programState, exprValue, NullConstraint.NOT_NULL));
      }
      case EQUAL_TO: {
        BinaryExpressionTree equalTo = (BinaryExpressionTree) condition;
        SymbolicValue lhs = eval(programState, equalTo.leftOperand());
        SymbolicValue rhs = eval(programState, equalTo.rightOperand());
        if (isNull(programState, lhs)) {
          ProgramState stateNull = setConstraint(programState, rhs, NullConstraint.NULL);
          ProgramState stateNotNull = setConstraint(programState, rhs, NullConstraint.NOT_NULL);
          return new Pair<>(stateNotNull, stateNull);
        } else if (isNull(programState, rhs)) {
          ProgramState stateNull = setConstraint(programState, lhs, NullConstraint.NULL);
          ProgramState stateNotNull = setConstraint(programState, lhs, NullConstraint.NOT_NULL);
          return new Pair<>(stateNotNull, stateNull);
        }
        break;
      }
      case NOT_EQUAL_TO: {
        BinaryExpressionTree notEqualTo = (BinaryExpressionTree) condition;
        SymbolicValue lhs = eval(programState, notEqualTo.leftOperand());
        SymbolicValue rhs = eval(programState, notEqualTo.rightOperand());
        if (isNull(programState, lhs)) {
          ProgramState stateNull = setConstraint(programState, rhs, NullConstraint.NULL);
          ProgramState stateNotNull = setConstraint(programState, rhs, NullConstraint.NOT_NULL);
          return new Pair<>(stateNull, stateNotNull);
        } else if (isNull(programState, rhs)) {
          ProgramState stateNull = setConstraint(programState, lhs, NullConstraint.NULL);
          ProgramState stateNotNull = setConstraint(programState, lhs, NullConstraint.NOT_NULL);
          return new Pair<>(stateNull, stateNotNull);
        }
        break;
      }
      case LOGICAL_COMPLEMENT:
        return assumeDual(programState, ((UnaryExpressionTree) condition).expression()).invert();
      case CONDITIONAL_OR:
      case CONDITIONAL_AND:
        // this is the case for branches such as "if (lhs && rhs)" and "if (lhs || rhs)"
        // we already made an assumption on lhs, because CFG contains branch for it, so now let's make an assumption on rhs
        BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) condition;
        return assumeDual(programState, binaryExpressionTree.rightOperand());
      case BOOLEAN_LITERAL:
        LiteralTree literalTree = ((LiteralTree) condition);
        if ("true".equals(literalTree.value())) {
          return new Pair<>(null, programState);
        }
        return new Pair<>(programState, null);
      case IDENTIFIER:
        IdentifierTree id = (IdentifierTree) condition;
        SymbolicValue eval = eval(programState, id);
        return new Pair<>(setConstraint(programState, eval, BooleanConstraint.FALSE), setConstraint(programState, eval, BooleanConstraint.TRUE));
    }
    return new Pair<>(programState, programState);
  }

  @CheckForNull
  static ProgramState setConstraint(ProgramState programState, SymbolicValue sv, BooleanConstraint booleanConstraint) {
    Object data = programState.constraints.get(sv);
    // update program state only for a different constraint
    if(data instanceof BooleanConstraint) {
      BooleanConstraint bc = (BooleanConstraint) data;
      if((BooleanConstraint.TRUE.equals(booleanConstraint) && BooleanConstraint.FALSE.equals(bc)) ||
          (BooleanConstraint.TRUE.equals(bc) && BooleanConstraint.FALSE.equals(booleanConstraint))) {
        //setting null where value is known to be non null or the contrary
        return null;
      }
    }
    if (data == null || !data.equals(booleanConstraint)) {
      Map<SymbolicValue, Object> temp = Maps.newHashMap(programState.constraints);
      temp.put(sv, booleanConstraint);
      return new ProgramState(programState.values, temp);
    }
    return programState;
  }

  @CheckForNull
  static ProgramState setConstraint(ProgramState programState, SymbolicValue sv, NullConstraint nullConstraint) {
    Object data = programState.constraints.get(sv);
    // update program state only for a different constraint
    if(data instanceof NullConstraint) {
      NullConstraint nc = (NullConstraint) data;
      if((NullConstraint.NULL.equals(nullConstraint) && NullConstraint.NOT_NULL.equals(nc)) ||
          (NullConstraint.NULL.equals(nc) && NullConstraint.NOT_NULL.equals(nullConstraint))) {
        //setting null where value is known to be non null or the contrary
        return null;
      }
    }
    if (data == null || !data.equals(nullConstraint)) {
      Map<SymbolicValue, Object> temp = Maps.newHashMap(programState.constraints);
      temp.put(sv, nullConstraint);
      return new ProgramState(programState.values, temp);
    }
    return programState;
  }

  public enum NullConstraint {
    NULL,
    NOT_NULL,
  }

  public enum BooleanConstraint {
    TRUE,
    FALSE,
  }
}
