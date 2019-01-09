/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.matcher.MethodMatcherFactory;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.Flow;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;

@Rule(key = "S3546")
public class CustomUnclosedResourcesCheck extends SECheck {

  //see SONARJAVA-1624 class cannot be static, different classes are needed for every instance of this template rule
  public class CustomResourceConstraint implements Constraint {
    private final String valueAsString;

    CustomResourceConstraint(String valueAsString) {
      this.valueAsString = valueAsString;
    }

    @Override
    public String valueAsString() {
      return valueAsString;
    }
  }

  //see SONARJAVA-1624 fields cannot be static, different instances are needed for every instance of this template rule
  private final CustomResourceConstraint OPENED = new CustomResourceConstraint("open");
  private final CustomResourceConstraint CLOSED = new CustomResourceConstraint("close");

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
    if (context.getState().exitingOnRuntimeException()) {
      return;
    }
    ExplodedGraph.Node node = context.getNode();
    context.getState().getValuesWithConstraints(OPENED).forEach(sv -> processUnclosedSymbolicValue(node, sv));
  }

  private void processUnclosedSymbolicValue(ExplodedGraph.Node node, SymbolicValue sv) {
    List<Class<? extends Constraint>> domains = Lists.newArrayList(CustomResourceConstraint.class);
    FlowComputation.flowWithoutExceptions(node, sv, OPENED::equals, domains).stream()
      .flatMap(Flow::firstFlowLocation)
      .filter(location -> location.syntaxNode.is(Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION))
      .forEach(this::reportIssue);
  }

  private void reportIssue(JavaFileScannerContext.Location location) {
    String message = "Close this \"" + name(location.syntaxNode) + "\".";
    reportIssue(location.syntaxNode, message);
  }

  private static String name(Tree tree) {
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      return ((NewClassTree) tree).symbolType().name();
    }
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (mit.symbolType().isVoid()) {
      return mit.symbol().owner().name();
    }
    return mit.symbolType().name();
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
        CustomResourceConstraint oConstraint = programState.getConstraint(target, CustomResourceConstraint.class);
        if (oConstraint != null) {
          programState = programState.addConstraintTransitively(target.wrappedValue(), CLOSED);
        }
      }
    }

    protected void openResource(SymbolicValue sv) {
      programState = programState.addConstraintTransitively(sv, OPENED);
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
        openResource(getTargetSV(mit));
      } else if (isClosingResource(mit)) {
        closeResource(getTargetSV(mit));
      } else {
        programState.peekValues(mit.arguments().size()).forEach(this::closeResource);
      }
    }

    private SymbolicValue getTargetSV(MethodInvocationTree mit) {
      return programState.peekValue(mit.arguments().size());
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
        openResource(programState.peekValue());
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree mit) {
      if (isCreatingResource(mit)) {
        openResource(programState.peekValue());
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
