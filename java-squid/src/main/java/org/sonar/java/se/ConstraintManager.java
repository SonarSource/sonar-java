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
import org.sonar.plugins.java.api.tree.ParenthesizedTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
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
      result = new SymbolicValue.ObjectSymbolicValue(map.size()+1);
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
    switch (condition.kind()) {
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
      case CONDITIONAL_OR:
      case CONDITIONAL_AND:
        // this is the case for branches such as "if (lhs && lhs)" and "if (lhs || rhs)"
        // we already made an assumption on lhs, because CFG contains branch for it, so now let's make an assumption on rhs
        BinaryExpressionTree binaryExpressionTree = (BinaryExpressionTree) condition;
        return assumeDual(programState, binaryExpressionTree.rightOperand());
      case IDENTIFIER:
        IdentifierTree id = (IdentifierTree) condition;
        SymbolicValue eval = eval(programState, id);
        //TODO associate boolean constraint to that identifier.
//        return new Pair<>(ExplodedGraphWalker.put(programState, id.symbol(), eval));
        break;
    }
    return new Pair<>(programState, programState);
  }

  @CheckForNull
  static ProgramState setConstraint(ProgramState programState, SymbolicValue sv, NullConstraint nullConstraint) {
    Object data = programState.constraints.get(sv);
    // update program state only for a different constraint
    //For now, we only store null constraints so casting is ok.
    NullConstraint nc = (NullConstraint) data;
    if((NullConstraint.NULL.equals(nullConstraint) && NullConstraint.NOT_NULL.equals(nc)) ||
        (NullConstraint.NULL.equals(nc) && NullConstraint.NOT_NULL.equals(nullConstraint))) {
      //setting null where value is known to be non null or the contrary
      return null;
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
    UNKNOWN
  }
}
