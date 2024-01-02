/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
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
package org.sonar.java.se.checks;

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

public abstract class AbstractXMLProcessing extends SECheck {

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    PreStatementVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  private class PreStatementVisitor extends CheckerTreeNodeVisitor {

    private final CheckerContext context;

    private PreStatementVisitor(CheckerContext context) {
      super(context.getState());
      this.context = context;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (getParsingMethods().matches(mit)) {
        SymbolicValue peek = programState.peekValue(mit.arguments().size());

        if (peek instanceof XxeProcessingCheck.XxeSymbolicValue) {
          XxeProcessingCheck.XxeSymbolicValue xxeSymbolicValue = (XxeProcessingCheck.XxeSymbolicValue) peek;
          reportIfNotSecured(context, xxeSymbolicValue, programState.getConstraints(xxeSymbolicValue));
        }
      }
    }
  }

  protected abstract MethodMatchers getParsingMethods();

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    ProgramState endState = context.getState();
    if (endState.exitingOnRuntimeException()) {
      return;
    }

    // We want to report only when the unsecured factory is returned, if it is the case, it will be on the top of the stack.
    SymbolicValue peek = endState.peekValue();
    if (peek instanceof XxeProcessingCheck.XxeSymbolicValue) {
      XxeProcessingCheck.XxeSymbolicValue xxeSV = (XxeProcessingCheck.XxeSymbolicValue) peek;
      reportIfNotSecured(context, xxeSV, endState.getConstraints(xxeSV));
    }
  }

  private void reportIfNotSecured(CheckerContext context, XxeProcessingCheck.XxeSymbolicValue xxeSV, @Nullable ConstraintsByDomain constraintsByDomain) {
    if (!xxeSV.isField && isUnSecuredByProperty(constraintsByDomain)) {
      context.reportIssue(getIssueLocation(context, xxeSV),
        this,
        getMessage());
    }
  }

  protected abstract boolean isUnSecuredByProperty(@Nullable ConstraintsByDomain constraintsByDomain);

  protected abstract String getMessage();

  protected abstract boolean shouldTrackConstraint(Constraint constraint);

  protected abstract List<Class<? extends Constraint>> getDomains();

  private Tree getIssueLocation(CheckerContext context, XxeProcessingCheck.XxeSymbolicValue xxeSV) {
    return FlowComputation.flowWithoutExceptions(context.getNode(), xxeSV, this::shouldTrackConstraint,
      getDomains(), FlowComputation.FIRST_FLOW)
      .stream()
      .findFirst()
      .flatMap(f -> f.elements().stream().findFirst())
      .map(e -> e.syntaxNode)
      // Last step should never occurs, we add it for defensive programming
      .orElse(xxeSV.init);
  }

}
