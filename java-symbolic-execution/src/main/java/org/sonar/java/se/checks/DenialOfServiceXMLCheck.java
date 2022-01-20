/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.Collections;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.XxeProcessingCheck.XxeSymbolicValue;
import org.sonar.java.se.checks.XxeProperty.FeatureSecureProcessing;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import static org.sonar.java.se.checks.XxeProcessingCheck.NEW_DOCUMENT_BUILDER;
import static org.sonar.java.se.checks.XxeProcessingCheck.PARSING_METHODS;

/**
 * This check uses the symbolic value and constraints set by XxeProcessingCheck.
 * It must therefore always be executed afterwards.
 *
 * @see org.sonar.java.se.checks.XxeProcessingCheck
 */
@Rule(key = "S6376")
public class DenialOfServiceXMLCheck extends SECheck {

  private static final MethodMatchers PARSING_METHODS_DOS = MethodMatchers.or(
    PARSING_METHODS,
    // When "newDocumentBuilder" is called on the factory, there is no way to secure the processing anymore
    NEW_DOCUMENT_BUILDER
  );

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
      if (PARSING_METHODS_DOS.matches(mit)) {
        SymbolicValue peek = programState.peekValue(mit.arguments().size());

        if (peek instanceof XxeSymbolicValue) {
          XxeSymbolicValue xxeSymbolicValue = (XxeSymbolicValue) peek;
          reportIfUnSecured(context, xxeSymbolicValue, programState.getConstraints(xxeSymbolicValue));
        }
      }
    }
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    ProgramState endState = context.getState();
    if (endState.exitingOnRuntimeException()) {
      return;
    }

    // We want to report only when the unsecured factory is returned, if it is the case, it will be on the top of the stack.
    SymbolicValue peek = endState.peekValue();
    if (peek instanceof XxeSymbolicValue) {
      XxeSymbolicValue xxeSV = (XxeSymbolicValue) peek;
      reportIfUnSecured(context, xxeSV, endState.getConstraints(xxeSV));
    }
  }

  private void reportIfUnSecured(CheckerContext context, XxeSymbolicValue xxeSV, @Nullable ConstraintsByDomain constraintsByDomain) {
    if (!xxeSV.isField && isUnSecuredByProperty(constraintsByDomain)) {
      context.reportIssue(getIssueLocation(context, xxeSV),
        this,
        "Enable XML parsing limitations to prevent Denial of Service attacks.");
    }
  }

  private static Tree getIssueLocation(CheckerContext context, XxeSymbolicValue xxeSV) {
    return FlowComputation.flowWithoutExceptions(context.getNode(), xxeSV, c -> c == FeatureSecureProcessing.UNSECURED,
      Collections.singletonList(FeatureSecureProcessing.class), FlowComputation.FIRST_FLOW)
      .stream()
      .findFirst()
      .flatMap(f -> f.elements().stream().findFirst())
      .map(e -> e.syntaxNode)
      // Last step should never occurs, we add it for defensive programming
      .orElse(xxeSV.init);
  }

  private static boolean isUnSecuredByProperty(@Nullable ConstraintsByDomain constraintsByDomain) {
    if (constraintsByDomain == null) {
      // Not vulnerable unless some properties are explicitly set.
      return false;
    }
    return constraintsByDomain.hasConstraint(FeatureSecureProcessing.UNSECURED)
      && !constraintsByDomain.hasConstraint(XxeProperty.FeatureDisallowDoctypeDecl.SECURED);
  }
}
