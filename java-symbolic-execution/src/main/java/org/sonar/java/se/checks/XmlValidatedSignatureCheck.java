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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S6377")
public class XmlValidatedSignatureCheck extends SECheck {

  private static final String MESSAGE = "Set the 'org.jcp.xml.dsig.secureValidation' property to \"true\" on the 'DOMValidateContext' object "
    + "to validate this XML signature securely.";
  private static final String JAVAX_XML_CRYPTO_VALIDATE_CONTEXT = "javax.xml.crypto.dsig.XMLValidateContext";

  private static final MethodMatchers DOM_VALIDATE_CONTEXT_CONSTRUCTOR = MethodMatchers.create()
    .ofSubTypes(JAVAX_XML_CRYPTO_VALIDATE_CONTEXT)
    .constructor()
    .withAnyParameters()
    .build();

  private static final MethodMatchers SET_PROPERTY = MethodMatchers.create()
    .ofSubTypes(JAVAX_XML_CRYPTO_VALIDATE_CONTEXT)
    .names("setProperty")
    .addParametersMatcher("java.lang.String", "java.lang.Object")
    .build();

  private static final MethodMatchers XML_SIGNATURE_VALIDATE = MethodMatchers.create()
    .ofAnyType()
    .names("validate")
    .addParametersMatcher(JAVAX_XML_CRYPTO_VALIDATE_CONTEXT)
    .build();

  private enum DomSecureValidation implements Constraint {
    DISABLED, EXPLICITLY_DISABLED;

    private static final Predicate<Constraint> IS_EXPLICITLY_DISABLED = c -> c == EXPLICITLY_DISABLED;
  }

  private static final List<Class<? extends Constraint>> DOMAINS = Collections.singletonList(DomSecureValidation.class);

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    PostStatementVisitor visitor = new PostStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  private static class PostStatementVisitor extends CheckerTreeNodeVisitor {

    private PostStatementVisitor(CheckerContext context) {
      super(context.getState());
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      if (DOM_VALIDATE_CONTEXT_CONSTRUCTOR.matches(tree)) {
        programState = programState.addConstraint(programState.peekValue(0), DomSecureValidation.DISABLED);
      }
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      ProgramState.SymbolicValueSymbol peek = programState.peekValueSymbol();
      Symbol symbol = peek.symbol();
      SymbolicValue sv = peek.symbolicValue();
      if (symbol != null && sv instanceof DomValidateContextSymbolicValue domValidateContextSymbolicValue) {
        domValidateContextSymbolicValue.setField(ProgramState.isField(symbol));
      }
    }
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    PreStatementVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  private class PreStatementVisitor extends CheckerTreeNodeVisitor {

    private static final String SECURE_VALIDATION_PROPERTY = "org.jcp.xml.dsig.secureValidation";
    private final CheckerContext context;

    private PreStatementVisitor(CheckerContext context) {
      super(context.getState());
      this.context = context;
    }

    @Override
    public void visitNewClass(NewClassTree newClass) {
      if (DOM_VALIDATE_CONTEXT_CONSTRUCTOR.matches(newClass)) {
        context.getConstraintManager().setValueFactory(() -> new DomValidateContextSymbolicValue(newClass.identifier()));
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (SET_PROPERTY.matches(mit)) {
        Arguments args = mit.arguments();
        if (args.get(0).asConstant(String.class).filter(SECURE_VALIDATION_PROPERTY::equalsIgnoreCase).isEmpty()) {
          // if we do not resolve correctly the property name, this can lead to FPs as we would not discard the constraint
          return;
        }
        SymbolicValue domSv = programState.peekValue(args.size());
        if (programState.getConstraint(programState.peekValue(0), BooleanConstraint.class) == BooleanConstraint.FALSE) {
          programState = programState.addConstraint(domSv, DomSecureValidation.EXPLICITLY_DISABLED);
        } else {
          // directly set to true, or no idea what the value could be, better say nothing
          programState = programState.removeConstraintsOnDomain(domSv, DomSecureValidation.class);
        }
      } else if (XML_SIGNATURE_VALIDATE.matches(mit)) {
        reportIfNotSecured(context, programState, programState.peekValue(0));
      }
    }
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    ProgramState endState = context.getState();
    if (endState.exitingOnRuntimeException()) {
      return;
    }

    // We want to report only when the unsecured DOMValidateContext is returned, if it is the case, it will be on the top of the stack.
    reportIfNotSecured(context, endState, endState.peekValue());
  }

  private void reportIfNotSecured(CheckerContext context, ProgramState ps, @Nullable SymbolicValue sv) {
    if (!(sv instanceof DomValidateContextSymbolicValue)) {
      return;
    }
    DomValidateContextSymbolicValue domSv = (DomValidateContextSymbolicValue) sv;
    if (domSv.isField) {
      return;
    }
    Optional.ofNullable(ps.getConstraint(domSv, DomSecureValidation.class))
      .ifPresent(constraint -> report(context, domSv, constraint));
  }

  private void report(CheckerContext context, DomValidateContextSymbolicValue sv, DomSecureValidation constraint) {
    Tree reportTree = sv.init;
    String message = MESSAGE;
    if (constraint != DomSecureValidation.DISABLED) {
      // has been explicitly set to something else than false, loop on the exploded graph
      reportTree = FlowComputation.flowWithoutExceptions(context.getNode(), sv, DomSecureValidation.IS_EXPLICITLY_DISABLED, DOMAINS, FlowComputation.FIRST_FLOW)
        .stream()
        .findFirst()
        .flatMap(f -> f.elements().stream().findFirst())
        .map(e -> e.syntaxNode)
        // Last step should never occurs, we add it for defensive programming
        .orElse(sv.init);
    }

    if (reportTree.is(Tree.Kind.METHOD_INVOCATION)) {
      // takes the argument matching the setProperty method
      reportTree = ((MethodInvocationTree) reportTree).arguments().get(1);
      message = "Change this to \"true\" to validate this XML signature securely.";
    }
    context.reportIssue(reportTree, this, message);
  }

  @VisibleForTesting
  static class DomValidateContextSymbolicValue extends SymbolicValue {
    private final Tree init;
    private boolean isField;

    DomValidateContextSymbolicValue(Tree init) {
      this.init = init;
      this.isField = false;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      DomValidateContextSymbolicValue that = (DomValidateContextSymbolicValue) o;
      return isField == that.isField && init.equals(that.init);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), init, isField);
    }

    public void setField(boolean isField) {
      this.isField = isField;
    }
  }

}
