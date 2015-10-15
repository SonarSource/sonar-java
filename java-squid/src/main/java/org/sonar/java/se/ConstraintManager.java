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

import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import java.util.List;

public class ConstraintManager {

  private int counter = ProgramState.EMPTY_STATE.constraints.size();

  public SymbolicValue createSymbolicValue(Tree syntaxNode) {
    SymbolicValue result;
    switch (syntaxNode.kind()) {
      case EQUAL_TO:
        result = new SymbolicValue.EqualToSymbolicValue(counter++);
        break;
      case NOT_EQUAL_TO:
        result = new SymbolicValue.NotEqualToSymbolicValue(counter++);
        break;
      case LOGICAL_COMPLEMENT:
        result = new SymbolicValue.NotSymbolicValue(counter++);
        break;
      case INSTANCE_OF:
        result = new SymbolicValue.InstanceOfSymbolicValue(counter++);
        break;
      default:
        result = new SymbolicValue.ObjectSymbolicValue(counter++);
    }
    return result;
  }

  public SymbolicValue supersedeSymbolicValue(VariableTree variable) {
    return createSymbolicValue(variable);
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
    return NullConstraint.NULL.equals(ps.constraints.get(val));
  }

  public Pair<ProgramState, ProgramState> assumeDual(ProgramState programState) {
    Pair<ProgramState, List<SymbolicValue>> unstack = ProgramState.unstack(programState, 1);
    SymbolicValue sv = unstack.b.get(0);
    return new Pair<>(sv.setConstraint(unstack.a, BooleanConstraint.FALSE), sv.setConstraint(unstack.a, BooleanConstraint.TRUE));
  }

  public enum NullConstraint {
    NULL,
    NOT_NULL;
    NullConstraint inverse() {
      if (NULL == this) {
        return NOT_NULL;
      }
      return NULL;
    }
  }

  public enum BooleanConstraint {
    TRUE,
    FALSE;
    BooleanConstraint inverse() {
      if (TRUE == this) {
        return FALSE;
      }
      return TRUE;
    }
  }
}
