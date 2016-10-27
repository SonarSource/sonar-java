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

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.MethodMatcherFactory;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.RuleTemplate;

import javax.annotation.Nullable;
import java.util.List;

@Rule(key = "S3546")
@RuleTemplate
public class CustomUnclosedResourcesCheck extends SECheck {

  class Status {
    Object OPENED = new Object();
    Object CLOSED = new Object();
  }

  private final Status status = new Status();


  @RuleProperty(
    key = "constructor",
    description = "the fully-qualified name of a constructor that creates an open resource."
      + " An optional signature may be specified after the class name."
      + " E.G. \"org.assoc.res.MyResource\" or \"org.assoc.res.MySpecialResource(java.lang.String, int)\"")
  public String constructor = "";

  @RuleProperty(
    key = "factoryMethod",
    description = "the fully-qualified name of a factory method that returns an open resource, with or without a parameter list."
      + " E.G. \"org.assoc.res.ResourceFactory$Innerclass#create\" or \"org.assoc.res.SpecialResourceFactory#create(java.lang.String, int)\"")
  public String factoryMethod = "";

  @RuleProperty(
    key = "openingMethod",
    description = "the fully-qualified name of a method that opens an existing resource, with or without a parameter list."
      + " E.G. \"org.assoc.res.ResourceFactory#create\" or \"org.assoc.res.SpecialResourceFactory #create(java.lang.String, int)\"")
  public String openingMethod = "";

  @RuleProperty(
    key = "closingMethod",
    description = "the fully-qualified name of the method which closes the open resource, with or without a parameter list."
      + " E.G. \"org.assoc.res.MyResource#closeMe\" or \"org.assoc.res.MySpecialResource#closeMe(java.lang.String, int)\"")
  public String closingMethod = "";

  private MethodMatcherCollection classConstructor;

  private MethodMatcherCollection factoryList;
  private MethodMatcherCollection openingList;
  private MethodMatcherCollection closingList;

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    AbstractStatementVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    PostStatementVisitor visitor = new PostStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    List<ObjectConstraint> constraints = context.getState().getFieldConstraints(status.OPENED);
    for (ObjectConstraint constraint : constraints) {
      Tree syntaxNode = constraint.syntaxNode();
      String name = null;
      if (syntaxNode.is(Tree.Kind.NEW_CLASS)) {
        name = ((NewClassTree) syntaxNode).identifier().symbolType().name();
      } else if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
        name = ((MethodInvocationTree) syntaxNode).symbolType().name();
      }
      if (name != null) {
        context.reportIssue(syntaxNode, this, "Close this \"" + name + "\".");
      }
    }
  }

  private static MethodMatcherCollection createMethodMatchers(String rule) {
    if (rule.length() > 0) {
      return MethodMatcherCollection.create(MethodMatcherFactory.methodMatcher(rule));
    } else {
      return MethodMatcherCollection.create();
    }
  }

  private abstract class AbstractStatementVisitor extends CheckerTreeNodeVisitor {

    protected AbstractStatementVisitor(ProgramState programState) {
      super(programState);
    }

    protected void closeResource(@Nullable SymbolicValue target) {
      if (target != null) {
        ObjectConstraint oConstraint = programState.getConstraintWithStatus(target, status.OPENED);
        if (oConstraint != null) {
          programState = programState.addConstraint(target.wrappedValue(), oConstraint.withStatus(status.CLOSED));
        }
      }
    }

    protected void openResource(@Nullable SymbolicValue sv, Tree syntaxNode) {
      if(sv == null) {
        sv = programState.peekValue();
      }
      programState = programState.addConstraint(sv, new ObjectConstraint(false, false, syntaxNode, status.OPENED));
    }

    protected boolean isClosingResource(MethodInvocationTree mit) {
      return closingMethods().anyMatch(mit);
    }

    private MethodMatcherCollection closingMethods() {
      if (closingList == null) {
        closingList = createMethodMatchers(closingMethod);
      }
      return closingList;
    }
  }
  private class PreStatementVisitor extends AbstractStatementVisitor {

    protected PreStatementVisitor(CheckerContext context) {
      super(context.getState());
    }

    @Override
    public void visitNewClass(NewClassTree syntaxNode) {
      programState.peekValues(syntaxNode.arguments().size()).forEach(this::closeResource);
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (isOpeningResource(mit)) {
        openResource(getTargetSV(mit), mit);
      } else if (isClosingResource(mit)) {
        closeResource(getTargetSV(mit));
      } else {
        programState.peekValues(mit.arguments().size()).forEach(this::closeResource);
      }
    }

    private SymbolicValue getTargetSV(MethodInvocationTree mit) {
      List<SymbolicValue> values = programState.peekValues(mit.arguments().size() + 1);
      return values.get(values.size() -1);
    }

    private boolean isOpeningResource(MethodInvocationTree syntaxNode) {
      return openingMethods().anyMatch(syntaxNode);
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree syntaxNode) {
      ExpressionTree expression = syntaxNode.expression();
      if (expression != null) {
        closeResource(programState.peekValue());
      }
    }

    private MethodMatcherCollection openingMethods() {
      if (openingList == null) {
        openingList = createMethodMatchers(openingMethod);
      }
      return openingList;
    }
  }
  private class PostStatementVisitor extends AbstractStatementVisitor {

    protected PostStatementVisitor(CheckerContext context) {
      super(context.getState());
    }

    @Override
    public void visitNewClass(NewClassTree newClassTree) {
      if (isCreatingResource(newClassTree)) {
        openResource(null, newClassTree);
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (isCreatingResource(mit)) {
        openResource(null, mit);
      }
    }

    private boolean isCreatingResource(NewClassTree newClassTree) {
      return constructorClasses().anyMatch(newClassTree);
    }

    private MethodMatcherCollection constructorClasses() {
      if (classConstructor == null) {
        classConstructor = MethodMatcherCollection.create();
        if (constructor.length() > 0) {
          classConstructor.add(MethodMatcherFactory.constructorMatcher(constructor));
        }
      }
      return classConstructor;
    }

    private boolean isCreatingResource(MethodInvocationTree mit) {
      return factoryMethods().anyMatch(mit);
    }

    private MethodMatcherCollection factoryMethods() {
      if (factoryList == null) {
        factoryList = createMethodMatchers(factoryMethod);
      }
      return factoryList;
    }

  }

}
