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

import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ConstraintsByDomain;
import org.sonar.java.se.symbolicvalues.SymbolicValue;

@Rule(key = "S6376")
public class DenialOfServiceXMLCheck extends SECheck {

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
      context.reportIssue(xxeSV.init,
        this,
        "Enable XML parsing limitations to prevent Denial of Service attacks."); // TODO: Flows
    }

  }

  private static boolean isUnSecuredByProperty(@Nullable ConstraintsByDomain constraintsByDomain) {
    if (constraintsByDomain == null) {
      // Not vulnerable unless some properties are explicitly set.
      return false;
    }
    return constraintsByDomain.hasConstraint(XxeProperty.FeatureSecureProcessing.UNSECURED)
      && !constraintsByDomain.hasConstraint(XxeProperty.FeatureDisallowDoctypeDecl.SECURED);
  }
}
