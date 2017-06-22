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

import com.google.common.base.Preconditions;

import org.sonar.check.Rule;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.List;

@Rule(key = "S3655")
public class OptionalGetBeforeIsPresentCheck extends SECheck {

  private static final ExceptionalYieldChecker EXCEPTIONAL_YIELD_CHECKER = new ExceptionalYieldChecker(
    "\"NoSuchElementException\" will be thrown when invoking method \"%s()\" without verifying Optional parameter.");
  private static final String JAVA_UTIL_OPTIONAL = "java.util.Optional";
  private static final MethodMatcher OPTIONAL_GET = MethodMatcher.create().typeDefinition(JAVA_UTIL_OPTIONAL).name("get").withoutParameter();
  private static final MethodMatcher OPTIONAL_IS_PRESENT = MethodMatcher.create().typeDefinition(JAVA_UTIL_OPTIONAL).name("isPresent").withoutParameter();
  private static final MethodMatcher OPTIONAL_EMPTY = MethodMatcher.create().typeDefinition(JAVA_UTIL_OPTIONAL).name("empty").withoutParameter();
  private static final MethodMatcher OPTIONAL_OF = MethodMatcher.create().typeDefinition(JAVA_UTIL_OPTIONAL).name("of").withAnyParameters();
  private static final MethodMatcher OPTIONAL_OF_NULLABLE = MethodMatcher.create().typeDefinition(JAVA_UTIL_OPTIONAL).name("ofNullable").withAnyParameters();

  private enum OptionalConstraint implements Constraint {
    PRESENT, NOT_PRESENT;

    @Override
    public boolean hasPreciseValue() {
      return this == NOT_PRESENT;
    }
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    PreStatementVisitor visitor = new PreStatementVisitor(this, context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    List<ProgramState> programStates = setOptionalConstraint(context, syntaxNode);
    Preconditions.checkState(programStates.size() == 1);
    return programStates.get(0);
  }

  private static List<ProgramState> setOptionalConstraint(CheckerContext context, Tree syntaxNode) {
    ProgramState programState = context.getState();
    if (!syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
      return Collections.singletonList(programState);
    }
    MethodInvocationTree mit = (MethodInvocationTree) syntaxNode;
    SymbolicValue peekValue = programState.peekValue();
    Preconditions.checkNotNull(peekValue);
    if (OPTIONAL_EMPTY.matches(mit)) {
      return peekValue.setConstraint(programState, OptionalConstraint.NOT_PRESENT);
    }
    if (OPTIONAL_OF.matches(mit)) {
      return peekValue.setConstraint(programState, OptionalConstraint.PRESENT);
    }
    if (OPTIONAL_OF_NULLABLE.matches(mit)) {
      ProgramState psPriorMethodInvocation = context.getNode().programState;
      SymbolicValue paramSV = psPriorMethodInvocation.peekValue(0);
      ObjectConstraint paramConstraint = psPriorMethodInvocation.getConstraint(paramSV, ObjectConstraint.class);
      if (paramConstraint != null) {
        // Optional.ofNullable(null) returns an empty Optional
        return peekValue.setConstraint(programState, paramConstraint == ObjectConstraint.NULL ? OptionalConstraint.NOT_PRESENT : OptionalConstraint.PRESENT);
      }
    }
    return Collections.singletonList(programState);
  }

  private static class OptionalSymbolicValue extends SymbolicValue {

    private final SymbolicValue optionalSV;

    public OptionalSymbolicValue(SymbolicValue sv) {
      this.optionalSV = sv;
    }

    /**
     * Will be called only after calling Optional.isPresent()
     */
    @Override
    public List<ProgramState> setConstraint(ProgramState programState, BooleanConstraint booleanConstraint) {
      OptionalConstraint optionalConstraint =  programState.getConstraint(optionalSV, OptionalConstraint.class);
      if (isImpossibleState(booleanConstraint, optionalConstraint)) {
        return Collections.emptyList();
      }
      if (optionalConstraint == OptionalConstraint.NOT_PRESENT || optionalConstraint == OptionalConstraint.PRESENT) {
        return Collections.singletonList(programState);
      }
      OptionalConstraint newConstraint = booleanConstraint.isTrue() ? OptionalConstraint.PRESENT : OptionalConstraint.NOT_PRESENT;
      return Collections.singletonList(programState.addConstraint(optionalSV, newConstraint));
    }

    private static boolean isImpossibleState(BooleanConstraint booleanConstraint, OptionalConstraint optionalConstraint) {
      return (optionalConstraint == OptionalConstraint.PRESENT && booleanConstraint.isFalse())
        || (optionalConstraint == OptionalConstraint.NOT_PRESENT && booleanConstraint.isTrue());
    }
  }

  private static class PreStatementVisitor extends CheckerTreeNodeVisitor {

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
      SymbolicValue peek = programState.peekValue();
      if (OPTIONAL_IS_PRESENT.matches(tree)) {
        constraintManager.setValueFactory(() -> new OptionalSymbolicValue(peek));
      } else if (OPTIONAL_GET.matches(tree) && presenceHasNotBeenChecked(peek)) {
        context.addExceptionalYield(peek, programState, "java.util.NoSuchElementException", check);
        reportIssue(tree);
        programState = null;
      }
    }

    private void reportIssue(MethodInvocationTree mit) {
      String identifier = getIdentifierPart(mit.methodSelect());
      String issueMsg = identifier.isEmpty() ? "Optional#" : (identifier + ".");
      Tree reportTree = mit.methodSelect().is(Tree.Kind.MEMBER_SELECT) ? ((MemberSelectExpressionTree) mit.methodSelect()).expression() : mit;
      context.reportIssue(reportTree, check, "Call \""+ issueMsg + "isPresent()\" before accessing the value.");
    }

    private boolean presenceHasNotBeenChecked(SymbolicValue sv) {
      return programState.getConstraint(sv, OptionalConstraint.class) != OptionalConstraint.PRESENT;
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

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    EXCEPTIONAL_YIELD_CHECKER.reportOnExceptionalYield(context.getNode(), this);
  }
}
