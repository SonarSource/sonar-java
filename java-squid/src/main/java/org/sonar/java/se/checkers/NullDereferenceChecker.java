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

import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ConstraintManager;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicValue;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.SwitchStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

public class NullDereferenceChecker extends SEChecker {

  private static final String RULE_KEY = "S2259";

  @Override
  public void checkPreStatement(CheckerContext context, Tree syntaxNode) {
    ProgramState programState = setNullConstraint(context, syntaxNode);
    SymbolicValue val = null;
    ExpressionTree expressionTree = null;
    String name = "";
    if (syntaxNode.is(Tree.Kind.MEMBER_SELECT)) {
      expressionTree = ((MemberSelectExpressionTree) syntaxNode).expression();
    } else if (syntaxNode.is(Tree.Kind.SWITCH_STATEMENT)) {
      expressionTree = ((SwitchStatementTree) syntaxNode).expression();
    }
    if(expressionTree != null) {
      val = context.getVal(expressionTree);
      if(expressionTree.is(Tree.Kind.IDENTIFIER)) {
        name = ((IdentifierTree) expressionTree).name();
      } else if(expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
        name = ((MethodInvocationTree) expressionTree).symbol().name();
      }
    }
    if (val != null) {
      if (context.isNull(val)) {
        context.addIssue(syntaxNode, RULE_KEY, "NullPointerException might be thrown as '" + name + "' is nullable here");
        context.createSink();
        return;
      } else {
        //we dereferenced the symbolic value so we can assume it is not null
        programState = context.setConstraint(val, ConstraintManager.NullConstraint.NOT_NULL);
      }
    }
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
