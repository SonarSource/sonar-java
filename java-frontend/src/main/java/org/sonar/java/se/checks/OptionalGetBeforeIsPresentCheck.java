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
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;
import java.util.Set;

@Rule(key = "S3655")
public class OptionalGetBeforeIsPresentCheck extends SECheck {

  private enum Status implements ObjectConstraint.Status {
    PRESENT, NOT_PRESENT
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    PreStatementVisitor visitor = new PreStatementVisitor(this, context);
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
      ObjectConstraint<Status> optionalConstraint = (ObjectConstraint<Status>) programState.getConstraint(optionalSV);
      if(optionalConstraint == null) {
        // Constraint on the optional SV might have been disposed. But is is necessarily non null because NPE check is ran before.
        optionalConstraint = ObjectConstraint.notNull();
      }
      if (isImpossibleState(booleanConstraint, optionalConstraint)) {
        return ImmutableList.of();
      }
      if (optionalConstraint.hasStatus(Status.NOT_PRESENT) || optionalConstraint.hasStatus(Status.PRESENT)) {
        return ImmutableList.of(programState);
      }
      ObjectConstraint newConstraint = booleanConstraint.isTrue() ? optionalConstraint.withStatus(Status.PRESENT) : optionalConstraint.withStatus(Status.NOT_PRESENT);
      return ImmutableList.of(programState.addConstraint(optionalSV, newConstraint));
    }

    private static boolean isImpossibleState(BooleanConstraint booleanConstraint, ObjectConstraint optionalConstraint) {
      return (optionalConstraint.hasStatus(Status.PRESENT) && booleanConstraint.isFalse())
        || (optionalConstraint.hasStatus(Status.NOT_PRESENT) && booleanConstraint.isTrue());
    }
  }

  private static class PreStatementVisitor extends CheckerTreeNodeVisitor {

    private static final String JAVA_UTIL_OPTIONAL = "java.util.Optional";
    private static final MethodMatcher OPTIONAL_GET = MethodMatcher.create().typeDefinition(JAVA_UTIL_OPTIONAL).name("get").withoutParameter();
    private static final MethodMatcher OPTIONAL_IS_PRESENT = MethodMatcher.create().typeDefinition(JAVA_UTIL_OPTIONAL).name("isPresent").withoutParameter();

    private final CheckerContext context;
    private final ConstraintManager constraintManager;
    private final SECheck check;

    private PreStatementVisitor(SECheck check, CheckerContext context) {
      super(context.getState());
      this.context = context;
      this.constraintManager = context.getConstraintManager();
      this.check = check;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (OPTIONAL_IS_PRESENT.matches(tree)) {
        constraintManager.setValueFactory(id -> new OptionalSymbolicValue(id, programState.peekValue()));
      } else if (OPTIONAL_GET.matches(tree) && presenceHasNotBeenChecked(programState.peekValue())) {
        String identifier = getIdentifierPart(tree.methodSelect());
        String issueMsg;
        String flowMsg;
        if (identifier.isEmpty()) {
          issueMsg = "Optional#";
          flowMsg = "";
        } else {
          issueMsg = identifier + ".";
          flowMsg = identifier + " ";
        }
        Set<List<JavaFileScannerContext.Location>> flow = FlowComputation.singleton("Optional " + flowMsg + "is accessed", tree.methodSelect());
        context.reportIssue(tree, check, "Call \""+ issueMsg + "isPresent()\" before accessing the value.", flow);
        programState = null;
      }
    }

    private boolean presenceHasNotBeenChecked(SymbolicValue sv) {
      return programState.getConstraintWithStatus(sv, Status.PRESENT) == null;
    }

    private static String getIdentifierPart(ExpressionTree methodSelect) {
      if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
        ExpressionTree expression = ((MemberSelectExpressionTree) methodSelect).expression();
        if (expression.is(Tree.Kind.IDENTIFIER)) {
          return ((IdentifierTree) expression).name();
        }
      }
      return "";
    }

  }

}
