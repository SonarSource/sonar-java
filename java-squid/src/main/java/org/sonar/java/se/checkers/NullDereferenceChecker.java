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
package org.sonar.java.se.checkers;

import org.sonar.java.model.JavaTree;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ConstraintManager;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicValue;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.PrintStream;

public class NullDereferenceChecker extends SEChecker {

  public NullDereferenceChecker(PrintStream out) {
    super(out);
  }

  @Override
  public void checkPreStatement(CheckerContext context, Tree syntaxNode) {
    ProgramState programState = setNullConstraint(context, syntaxNode);
    SymbolicValue val = null;
    if (syntaxNode.is(Tree.Kind.MEMBER_SELECT)) {
      val = context.getVal(((MemberSelectExpressionTree) syntaxNode).expression());
    } else if (syntaxNode.is(Tree.Kind.SWITCH_STATEMENT)) {
      val = context.getVal(((SwitchStatementTree) syntaxNode).expression());
    }
    if (val != null && context.isNull(val)) {
      out.println("Null pointer dereference at line " + ((JavaTree) syntaxNode).getLine());
      context.createSink();
      return;
    }
    // TODO : improve next state with assumption on not null value as we can safely assume that if we did not sink, value is not null.
    context.addTransition(programState);
  }

  private ProgramState setNullConstraint(CheckerContext context, Tree syntaxNode) {
    SymbolicValue val = context.getVal(syntaxNode);
    switch (syntaxNode.kind()) {
      case NULL_LITERAL:
        return context.setConstraint(val, ConstraintManager.NullConstraint.NULL);
      case METHOD_INVOCATION:
        ProgramState ps = context.getState();
        if (((MethodInvocationTree) syntaxNode).symbol().metadata().isAnnotatedWith("javax.annotation.CheckForNull")) {
          ps = context.setConstraint(val, ConstraintManager.NullConstraint.NULL);
        }
        return ps;
    }
    return context.getState();
  }
}
