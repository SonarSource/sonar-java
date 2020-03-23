/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3655")
public class OptionalGetBeforeIsPresentCheck extends SECheck {

  private static final MethodMatchers.NameBuilder JAVA_UTIL_OPTIONAL = MethodMatchers.create().ofTypes("java.util.Optional");
  private static final ExceptionalYieldChecker EXCEPTIONAL_YIELD_CHECKER = new ExceptionalYieldChecker(
    "\"NoSuchElementException\" will be thrown when invoking method \"%s()\" without verifying Optional parameter.");
  private static final MethodMatchers OPTIONAL_GET = JAVA_UTIL_OPTIONAL.names("get").addWithoutParametersMatcher().build();
  private static final MethodMatchers OPTIONAL_ORELSE = JAVA_UTIL_OPTIONAL.names("orElse").withAnyParameters().build();
  private static final MethodMatchers OPTIONAL_TEST_METHODS = JAVA_UTIL_OPTIONAL.names("isPresent", "isEmpty").addWithoutParametersMatcher().build();
  private static final MethodMatchers OPTIONAL_EMPTY = JAVA_UTIL_OPTIONAL.names("empty").addWithoutParametersMatcher().build();
  private static final MethodMatchers OPTIONAL_OF = JAVA_UTIL_OPTIONAL.names("of").withAnyParameters().build();
  private static final MethodMatchers OPTIONAL_OF_NULLABLE = JAVA_UTIL_OPTIONAL.names("ofNullable").withAnyParameters().build();
  private static final MethodMatchers OPTIONAL_FILTER = JAVA_UTIL_OPTIONAL.names("filter").withAnyParameters().build();

  private enum OptionalConstraint implements Constraint {
    PRESENT, NOT_PRESENT;

    @Override
    public boolean isValidWith(@Nullable Constraint constraint) {
      return constraint == null || this == constraint;
    }

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
    Objects.requireNonNull(peekValue);
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
    protected final SymbolicValue wrappedValue;

    private OptionalSymbolicValue(SymbolicValue wrappedValue) {
      this.wrappedValue = wrappedValue;
    }

    @Override
    public boolean references(SymbolicValue other) {
      return wrappedValue.equals(other) || wrappedValue.references(other);
    }
  }

  private static class FilteredOptionalSymbolicValue extends OptionalSymbolicValue {
    private FilteredOptionalSymbolicValue(SymbolicValue wrappedValue) {
      super(wrappedValue);
    }

    @Override
    public List<ProgramState> setConstraint(ProgramState programState, Constraint constraint) {
      ProgramState ps = programState;
      if (constraint == OptionalConstraint.PRESENT) {
        List<ProgramState> programStates = wrappedValue.setConstraint(ps, constraint);
        // programStates should always have size 1 here as a FilteredOptionalSymbolicValue is only created on top of SV having
        // either a PRESENT or no constraint. SV having already NOT_PRESENT constraints do not create a new FilteredOptionalSymbolicValue
        // since the filtering operation will have no effect.
        Preconditions.checkState(programStates.size() == 1);
        ps = programStates.get(0);
      }
      return super.setConstraint(ps, constraint);
    }

  }

  /**
   * Used to wrap symbolic value resulting from invocation of Optional test methods:
   * - isPresent() (jdk 8)
   * - isEmpty() (jdk 11)
   */
  private static class OptionalTestMethodSymbolicValue extends SymbolicValue {

    private final SymbolicValue optionalSV;
    private final boolean isIsEmpty;

    public OptionalTestMethodSymbolicValue(SymbolicValue sv, Symbol testMethod) {
      this.optionalSV = sv;
      this.isIsEmpty = "isEmpty".equals(testMethod.name());
    }

    /**
     * Will be called only after calling Optional.isPresent() or Optional.isEmpty()
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

      return optionalSV.setConstraint(programState, expectedOptionalConstraint(booleanConstraint));
    }

    private boolean isImpossibleState(BooleanConstraint booleanConstraint, @Nullable OptionalConstraint optionalConstraint) {
      return optionalConstraint == expectedOptionalConstraint(booleanConstraint.isTrue() ? BooleanConstraint.FALSE : BooleanConstraint.TRUE);
    }

    private OptionalConstraint expectedOptionalConstraint(BooleanConstraint booleanConstraint) {
      if (booleanConstraint.isTrue()) {
        return isIsEmpty ? OptionalConstraint.NOT_PRESENT : OptionalConstraint.PRESENT;
      }
      return isIsEmpty ? OptionalConstraint.PRESENT : OptionalConstraint.NOT_PRESENT;
    }

    @Override
    public boolean references(SymbolicValue other) {
      return optionalSV.equals(other) || optionalSV.references(other);
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
      if (OPTIONAL_TEST_METHODS.matches(tree)) {
        constraintManager.setValueFactory(() -> new OptionalTestMethodSymbolicValue(peek, tree.symbol()));
      } else if (OPTIONAL_GET.matches(tree) && presenceHasNotBeenChecked(peek)) {
        context.addExceptionalYield(peek, programState, "java.util.NoSuchElementException", check);
        reportIssue(tree);
        // continue exploration after reporting, assuming the optional is now present (killing any noise after the initial issue)
        programState = programState.addConstraint(peek, OptionalConstraint.PRESENT);
      } else if (OPTIONAL_FILTER.matches(tree)) {
        // filter has one parameter, so optional is next item on stack
        SymbolicValue optionalSV = programState.peekValue(1);

        if (programState.getConstraint(optionalSV, OptionalConstraint.class) == OptionalConstraint.NOT_PRESENT) {
          // reuse the same optional - filtering a non-present optional is a no-op
          constraintManager.setValueFactory(() -> optionalSV);
        } else {
          constraintManager.setValueFactory(() -> new FilteredOptionalSymbolicValue(optionalSV));
        }
      } else if (OPTIONAL_ORELSE.matches(tree)) {
        ProgramState.Pop pop = programState.unstackValue(2);
        SymbolicValue orElseValue = pop.values.get(0);
        SymbolicValue optional = pop.values.get(1);
        List<ProgramState> psEmpty = optional.setConstraint(pop.state.stackValue(orElseValue), OptionalConstraint.NOT_PRESENT);
        SymbolicValue symbolicValue;
        if(optional instanceof OptionalSymbolicValue) {
          symbolicValue = ((OptionalSymbolicValue) optional).wrappedValue;
        } else {
          symbolicValue = constraintManager.createSymbolicValue(tree);
        }
        List<ProgramState> psPresent = optional.setConstraint(pop.state.stackValue(symbolicValue), OptionalConstraint.PRESENT);
        psEmpty.forEach(context::addTransition);
        psPresent.forEach(context::addTransition);
        programState = null;
      } else if (OPTIONAL_OF.matches(tree) || OPTIONAL_OF_NULLABLE.matches(tree)) {
        constraintManager.setValueFactory(() -> new OptionalSymbolicValue(peek));
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
