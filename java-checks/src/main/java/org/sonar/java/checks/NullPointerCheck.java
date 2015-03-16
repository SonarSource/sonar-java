/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.google.common.base.Preconditions;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.resolve.AnnotationInstance;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.resolve.Symbol;
import org.sonar.java.resolve.Symbol.MethodSymbol;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ArrayAccessExpressionTree;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IfStatementTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Rule(
  key = "S2259",
  name = "Null pointers should not be dereferenced",
  tags = {"bug", "cert", "cwe", "owasp-a1", "owasp-a2", "owasp-a6", "security"},
  priority = Priority.BLOCKER)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.ERRORS)
@SqaleConstantRemediation("20min")
public class NullPointerCheck extends BaseTreeVisitor implements JavaFileScanner {

  public static final String KEY = "S2259";
  private static final RuleKey RULE_KEY = RuleKey.of(CheckList.REPOSITORY_KEY, KEY);

  public enum AbstractValue {
    // value is known to be not null.
    NOTNULL,
    // value is known to be null.
    NULL,
    // value is unknown (could be null or not null).
    UNKNOWN
  }

  private class ConditionVisitor extends BaseTreeVisitor {
    private State trueState;
    @Nullable
    private State falseState;

    void visitCondition(ExpressionTree tree, State trueState, @Nullable State falseState) {
      this.trueState = trueState;
      this.falseState = falseState;
      tree.accept(this);
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      Symbol identifierSymbol;
      if (tree.leftOperand().is(Tree.Kind.NULL_LITERAL) && tree.rightOperand().is(Tree.Kind.IDENTIFIER)) {
        identifierSymbol = semanticModel.getReference((IdentifierTreeImpl) tree.rightOperand());
      } else if (tree.leftOperand().is(Tree.Kind.IDENTIFIER) && tree.rightOperand().is(Tree.Kind.NULL_LITERAL)) {
        identifierSymbol = semanticModel.getReference((IdentifierTreeImpl) tree.leftOperand());
      } else {
        return;
      }
      if (tree.is(Tree.Kind.EQUAL_TO)) {
        trueState.setVariableValue(identifierSymbol, AbstractValue.NULL);
        if (falseState != null) {
          falseState.setVariableValue(identifierSymbol, AbstractValue.NOTNULL);
        }
      } else {
        Preconditions.checkState(tree.is(Tree.Kind.NOT_EQUAL_TO));
        trueState.setVariableValue(identifierSymbol, AbstractValue.NOTNULL);
        if (falseState != null) {
          falseState.setVariableValue(identifierSymbol, AbstractValue.NULL);
        }
      }
    }
  }

  public static class State {
    @Nullable
    public final State parentState;
    Map<Symbol, AbstractValue> variables;

    public State(@Nullable State parentState) {
      this.parentState = parentState;
      this.variables = new HashMap<>();
    }

    // returns the value of the variable in the current state.
    public AbstractValue getVariableValue(Symbol variable) {
      for (State state = this; state != null; state = state.parentState) {
        AbstractValue result = variables.get(variable);
        if (result != null) {
          return result;
        }
      }
      return AbstractValue.UNKNOWN;
    }

    // sets the value of the variable in the current state.
    public void setVariableValue(Symbol variable, AbstractValue value) {
      variables.put(variable, value);
    }
  }

  private final ConditionVisitor conditionVisitor = new ConditionVisitor();
  private JavaFileScannerContext context;
  private State currentState;
  private SemanticModel semanticModel;

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    semanticModel = (SemanticModel) context.getSemanticModel();
    if (semanticModel != null) {
      context.getTree().accept(this);
    }
  }

  @Override
  public void visitArrayAccessExpression(ArrayAccessExpressionTree tree) {
    checkForIssue(tree.expression());
    super.visitArrayAccessExpression(tree);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    // currently we only handle assignment of the form: identifier =
    // TODO(merciesa): array[expr] = and mse.identifier = requires advanced tracking and are left outside of the scope
    if (tree.variable().is(Tree.Kind.IDENTIFIER)) {
      Symbol identifierSymbol = semanticModel.getReference((IdentifierTreeImpl) tree.variable());
      AbstractValue value = tree.expression().is(Tree.Kind.NULL_LITERAL) ? AbstractValue.NULL : AbstractValue.UNKNOWN;
      currentState.setVariableValue(identifierSymbol, value);
    }
    super.visitAssignmentExpression(tree);
  }

  @Override
  public void visitClass(ClassTree tree) {
    // state required for assignments in class body (e.g. static initializers, and int a, b, c = b = 0;)
    State oldState = currentState;
    currentState = new State(null);
    super.visitClass(tree);
    currentState = oldState;
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    // state required for annotations (e.g. @Annotation(name = value))
    State oldState = currentState;
    currentState = new State(null);
    super.visitCompilationUnit(tree);
    currentState = oldState;
  }

  @Override
  public void visitIfStatement(IfStatementTree tree) {
    State trueState = new State(currentState);
    State falseState = tree.elseStatement() != null ? new State(currentState) : null;
    conditionVisitor.visitCondition(tree.condition(), trueState, falseState);
    currentState = trueState;
    tree.thenStatement().accept(this);
    if (tree.elseStatement() != null) {
      currentState = falseState;
      tree.elseStatement().accept(this);
    }
    currentState = currentState.parentState;
  }

  @Override
  public void visitMemberSelectExpression(MemberSelectExpressionTree tree) {
    checkForIssue(tree.expression());
    super.visitMemberSelectExpression(tree);
  }

  @Override
  public void visitMethod(MethodTree tree) {
    State oldState = currentState;
    currentState = new State(null);
    super.visitMethod(tree);
    currentState = oldState;
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    Symbol symbol = ((MethodInvocationTreeImpl) tree).getSymbol();
    if (symbol.isMethodSymbol()) {
      MethodSymbol methodSymbol = (MethodSymbol) symbol;
      List<Symbol> parameters = methodSymbol.getParameters().scopeSymbols();
      // FIXME(merciesa): in some cases (method overloading with parameterized methods) a method symbol without parameter can be called with arguments.
      if (parameters.size() != 0) {
        for (int i = 0; i < tree.arguments().size(); i += 1) {
          // in case of varargs, there could be more arguments than parameters. in that case, pick the last parameter.
          if (isNonnullParameter(parameters.get(i < parameters.size() ? i : parameters.size() - 1))) {
            this.checkForIssue(tree.arguments().get(i));
          }
        }
      }
    }
    super.visitMethodInvocation(tree);
  }

  // returns true if the passed method parameter cannot be null.
  private boolean isNonnullParameter(Symbol symbol) {
    // FIXME(merciesa): it should also use annotation on package and class
    for (AnnotationInstance annotation : symbol.metadata().annotations()) {
      if (annotation.isTyped("javax.annotation.Nonnull")) {
        return true;
      }
    }
    return false;
  }

  // returns true if the symbol can be null.
  private boolean isNullReturnValue(Symbol symbol) {
    for (AnnotationInstance annotation : symbol.metadata().annotations()) {
      // FIXME(merciesa): it should also use annotation on package and class
      if (annotation.isTyped("javax.annotation.CheckForNull") || annotation.isTyped("javax.annotation.Nullable")) {
        return true;
      }
    }
    return false;
  }

  // raises an issue if the passed tree is a method invocation that can return null.
  private void checkForIssue(Tree tree) {
    if (tree.is(Tree.Kind.IDENTIFIER)) {
      Symbol symbol = semanticModel.getReference((IdentifierTreeImpl) tree);
      if (currentState != null && currentState.getVariableValue(symbol) == AbstractValue.NULL) {
        context.addIssue(tree, RULE_KEY, String.format("%s can be null.", symbol.getName()));
      }
    } else if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      Symbol symbol = ((MethodInvocationTreeImpl) tree).getSymbol();
      if (isNullReturnValue(symbol)) {
        context.addIssue(tree, RULE_KEY, String.format("Value returned by method '%s' can be null.", symbol.getName()));
      }
    } else if (tree.is(Tree.Kind.NULL_LITERAL)) {
      context.addIssue(tree, RULE_KEY, "null is dereferenced or passed as argument.");
    }
  }

}
