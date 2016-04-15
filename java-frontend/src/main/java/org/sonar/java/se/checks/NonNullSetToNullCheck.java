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
import org.sonar.java.cfg.CFG;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.Scope;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.CheckForNull;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Rule(key = "S2637")
public class NonNullSetToNullCheck extends SECheck {

  private static final String[] ANNOTATIONS = {"javax.annotation.Nonnull", "javax.validation.constraints.NotNull",
    "edu.umd.cs.findbugs.annotations.NonNull", "org.jetbrains.annotations.NotNull", "lombok.NonNull",
    "android.support.annotation.NonNull"};

  private MethodTree methodTree;

  @Override
  public void init(MethodTree tree, CFG cfg) {
    methodTree = tree;
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    AbstractStatementVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    AbstractStatementVisitor visitor = new PostStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    if (methodTree.is(Tree.Kind.CONSTRUCTOR)) {
      ClassTree classTree = (ClassTree) methodTree.parent();
      for (Tree member : classTree.members()) {
        if (member.is(Tree.Kind.VARIABLE)) {
          checkVariable(context, methodTree, ((VariableTree) member).symbol());
        }
      }
    }
  }

  private void checkVariable(CheckerContext context, MethodTree tree, final Symbol symbol) {
    String nonNullAnnotation = nonNullAnnotation(symbol);
    if (nonNullAnnotation != null && isUndefinedOrNull(context, symbol)) {
      context.reportIssue(tree, this,
        MessageFormat.format("\"{0}\" is marked \"{1}\" but is not initialized in this constructor.", symbol.name(), nonNullAnnotation));
    }
  }

  private static boolean isUndefinedOrNull(CheckerContext context, Symbol symbol) {
    ProgramState programState = context.getState();
    SymbolicValue value = programState.getValue(symbol);
    return value == null;
  }

  @CheckForNull
  private static String nonNullAnnotation(Symbol javaSymbol) {
    for (String annotation : ANNOTATIONS) {
      if (javaSymbol.metadata().isAnnotatedWith(annotation)) {
        return annotation;
      }
    }
    return null;
  }

  private abstract class AbstractStatementVisitor extends CheckerTreeNodeVisitor {

    private final CheckerContext context;

    protected AbstractStatementVisitor(CheckerContext context) {
      super(context.getState());
      this.context = context;
    }

    protected void reportIssue(Tree tree, String message, Object... parameters) {
      context.reportIssue(tree, NonNullSetToNullCheck.this, MessageFormat.format(message, parameters));
    }
  }

  private class PreStatementVisitor extends AbstractStatementVisitor {

    protected PreStatementVisitor(CheckerContext context) {
      super(context);
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree tree) {
      if (tree.variable().is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree variable = (IdentifierTree) tree.variable();
        Symbol symbol = variable.symbol();
        String nonNullAnnotation = nonNullAnnotation(symbol);
        if (nonNullAnnotation != null) {
          List<SymbolicValue> values = programState.peekValues(2);
          SymbolicValue assignedValue = values.get(1);
          Constraint constraint = programState.getConstraint(assignedValue);
          if (constraint != null && constraint.isNull()) {
            reportIssue(tree, "\"{0}\" is marked \"{1}\" but is set to null.", symbol.name(), nonNullAnnotation);
          }
        }
      }
    }

    @Override
    public void visitNewClass(NewClassTree syntaxTree) {
      Symbol symbol = syntaxTree.constructorSymbol();
      if (symbol.isMethodSymbol()) {
        List<SymbolicValue> argumentValues = programState.peekValues(syntaxTree.arguments().size());
        JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) symbol;
        checkNullArguments(syntaxTree, methodSymbol.getParameters(),
          argumentValues, "Parameter {0} to this constructor is marked \"{1}\" but null is passed.");
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree syntaxTree) {
      Symbol symbol = syntaxTree.symbol();
      if (symbol.isMethodSymbol()) {
        Arguments arguments = syntaxTree.arguments();
        List<SymbolicValue> argumentValues = new ArrayList<>(programState.peekValues(arguments.size() + 1));
        argumentValues.remove(0);
        // Arguments of method invocation are in reverse order on the stack, unlike constructors
        Collections.reverse(argumentValues);
        JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) symbol;
        checkNullArguments(syntaxTree, methodSymbol.getParameters(),
          argumentValues, "Parameter {0} to this call is marked \"{1}\" but null is passed.");
      }
    }

    protected void checkNullArguments(Tree syntaxTree, Scope parameters, List<SymbolicValue> argumentValues, String message) {
      if (parameters != null) {
        List<JavaSymbol> scopeSymbols = parameters.scopeSymbols();
        int parametersToTest = argumentValues.size();
        if (scopeSymbols.size() < parametersToTest) {
          // The last parameter is a variable length argument: the non-null condition does not apply to its values
          parametersToTest = scopeSymbols.size() - 1;
        }
        for (int i = 0; i < parametersToTest; i++) {
          checkNullArgument(syntaxTree, message, scopeSymbols, argumentValues, i);
        }
      }
    }

    protected void checkNullArgument(Tree syntaxTree, String message, List<JavaSymbol> scopeSymbols, List<SymbolicValue> argumentValues, int i) {
      String nonNullAnnotation = nonNullAnnotation(scopeSymbols.get(i));
      if (nonNullAnnotation != null) {
        Constraint constraint = programState.getConstraint(argumentValues.get(i));
        if (constraint != null && constraint.isNull()) {
          reportIssue(syntaxTree, message, Integer.valueOf(i + 1), nonNullAnnotation);
        }
      }
    }
  }

  private class PostStatementVisitor extends AbstractStatementVisitor {

    protected PostStatementVisitor(CheckerContext context) {
      super(context);
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      Tree parent = tree.parent();
      while (!parent.is(Tree.Kind.METHOD)) {
        parent = parent.parent();
        if (parent == null) {
          // This occurs when the return statement is within a constructor
          return;
        }
      }
      MethodTree mTree = (MethodTree) parent;
      String nonNullAnnotation = nonNullAnnotation(mTree.symbol());
      if (nonNullAnnotation != null && isLocalExpression(tree.expression())) {
        checkReturnedValue(tree, nonNullAnnotation);
      }
    }

    private boolean isLocalExpression(ExpressionTree expression) {
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        final Symbol symbol = ((IdentifierTree) expression).symbol().owner();
        return symbol.isMethodSymbol();
      }
      return true;
    }

    private void checkReturnedValue(ReturnStatementTree tree, String nonNullAnnotation) {
      SymbolicValue returnedValue = programState.peekValue();
      Constraint constraint = programState.getConstraint(returnedValue);
      if (constraint != null && constraint.isNull()) {
        reportIssue(tree, "This method''s return value is marked \"{0}\" but null is returned.", nonNullAnnotation);
      }
    }
  }

}
