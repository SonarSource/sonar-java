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
package org.sonar.java.se.checks;

import com.google.common.collect.ImmutableList;

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "S3655")
public class OptionalGetBeforeIsPresentCheck extends SECheck {

  private static final String JAVA_UTIL_OPTIONAL = "java.util.Optional";
  private static final MethodMatcher OPTIONAL_GET = MethodMatcher.create().typeDefinition(JAVA_UTIL_OPTIONAL).name("get").withNoParameterConstraint();
  private static final MethodMatcher OPTIONAL_IS_PRESENT = MethodMatcher.create().typeDefinition(JAVA_UTIL_OPTIONAL).name("isPresent").withNoParameterConstraint();

  private enum Status {
    PRESENT, NOT_PRESENT
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    final PreStatementVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  private static class OptionalSymbolicValue extends SymbolicValue {

    private final SymbolicValue optionalSV;

    public OptionalSymbolicValue(int id, SymbolicValue sv) {
      super(id);
      this.optionalSV = sv;
    }

    /**
     * Will be called only after calling optional.isPresent()
     */
    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      ObjectConstraint optionalConstraint = (ObjectConstraint) programState.getConstraint(optionalSV);
      if (optionalConstraint == null) {
        return ImmutableList.of();
      }
      boolean isknownAsNotPresent = optionalConstraint.hasStatus(Status.NOT_PRESENT);
      boolean isKnownAsPresent = optionalConstraint.hasStatus(Status.PRESENT);
      if (isknownAsNotPresent || isKnownAsPresent) {
        if (isImpossibleState(booleanConstraint, isknownAsNotPresent, isKnownAsPresent)) {
          return ImmutableList.of();
        }
        return ImmutableList.of(programState);
      }
      ObjectConstraint newConstraint = booleanConstraint.isTrue() ? optionalConstraint.withStatus(Status.PRESENT) : optionalConstraint.withStatus(Status.NOT_PRESENT);
      return ImmutableList.of(programState.addConstraint(optionalSV, newConstraint));
    }

    private static boolean isImpossibleState(BooleanConstraint booleanConstraint, boolean isknownAsNotPresent, boolean isKnownAsPresent) {
      return (isKnownAsPresent && booleanConstraint.isFalse()) || (isknownAsNotPresent && booleanConstraint.isTrue());
    }
  }

  private class PreStatementVisitor extends CheckerTreeNodeVisitor {

    private final CheckerContext context;
    private final ConstraintManager constraintManager;

    protected PreStatementVisitor(CheckerContext context) {
      super(context.getState());
      this.context = context;
      this.constraintManager = context.getConstraintManager();
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (OPTIONAL_IS_PRESENT.matches(tree)) {
        constraintManager.setValueFactory((id, node) -> new OptionalSymbolicValue(id, programState.peekValue()));
      } else if (OPTIONAL_GET.matches(tree)) {
        SymbolicValue sv = programState.peekValue();
        if (!isPresent(sv)) {
          reportIssue(tree);
          programState = null;
        }
      }
    }

    private boolean isPresent(SymbolicValue sv) {
      return programState.getConstraintWithStatus(sv, Status.PRESENT) != null;
    }

    private void reportIssue(MethodInvocationTree tree) {
      String identifierPart = "";
      ExpressionTree methodSelect = tree.methodSelect();
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mset = (MemberSelectExpressionTree) methodSelect;
        ExpressionTree expression = mset.expression();
        if (expression.is(Tree.Kind.IDENTIFIER)) {
          identifierPart = ((IdentifierTree) expression).name() + ".";
        }
      }
      context.reportIssue(tree, OptionalGetBeforeIsPresentCheck.this, "call \"" + identifierPart + "isPresent()\" before accessing the value.");
    }

  }

}
