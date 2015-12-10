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

import com.google.common.collect.Multimap;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.model.DefaultJavaFileScannerContext;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ConstraintManager;
import org.sonar.java.se.ObjectConstraint;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.ProgramState.ConstrainedValue;
import org.sonar.java.se.SymbolicValue;
import org.sonar.java.se.SymbolicValueFactory;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Rule(
  key = "S2095",
  name = "Resources should be closed",
  priority = Priority.BLOCKER,
  tags = {"bug", "cert", "cwe", "denial-of-service", "leak", "security"})
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.LOGIC_RELIABILITY)
@SqaleConstantRemediation("5min")
public class UnclosedResourcesCheck extends SECheck implements JavaFileScanner {

  private enum Status {
    OPENED, CLOSED;
  }

  private static final String JAVA_IO_AUTO_CLOSEABLE = "java.lang.AutoCloseable";
  private static final String JAVA_IO_CLOSEABLE = "java.io.Closeable";
  private static final String[] IGNORED_CLOSEABLE_SUBTYPES = {
    "java.io.ByteArrayOutputStream",
    "java.io.ByteArrayInputStream",
    "java.io.CharArrayReader",
    "java.io.CharArrayWriter",
    "java.io.StringReader",
    "java.io.StringWriter",
    "com.sun.org.apache.xml.internal.security.utils.UnsyncByteArrayOutputStream"
  };
  private static final String JAVA_SQL_STATEMENT = "java.sql.Statement";
  private static final String[] JDBC_RESOURCE_CREATIONS = {"java.sql.Connection", JAVA_SQL_STATEMENT};

  static boolean needsClosing(Type type) {
    for (String ignoredTypes : IGNORED_CLOSEABLE_SUBTYPES) {
      if (type.is(ignoredTypes)) {
        return false;
      }
    }
    return isCloseable(type);
  }

  static boolean isCloseable(Type type) {
    return type.isSubtypeOf(JAVA_IO_AUTO_CLOSEABLE) || type.isSubtypeOf(JAVA_IO_CLOSEABLE);
  }

  static boolean isOpeningResource(NewClassTree syntaxNode) {
    if (isWithinTryHeader(syntaxNode)) {
      return false;
    }
    final Type type = syntaxNode.symbolType();
    return needsClosing(type);
  }

  static boolean isWithinTryHeader(Tree syntaxNode) {
    final Tree parent = syntaxNode.parent();
    if (parent.is(Tree.Kind.VARIABLE)) {
      return isTryStatementResource((VariableTree) parent);
    }
    return false;
  }

  static boolean isTryStatementResource(VariableTree variable) {
    final TryStatementTree tryStatement = getEnclosingTryStatement(variable);
    if (tryStatement != null) {
      return tryStatement.resources().contains(variable);
    }
    return false;
  }

  private static TryStatementTree getEnclosingTryStatement(Tree syntaxNode) {
    Tree parent = syntaxNode.parent();
    while (parent != null) {
      if (parent.is(Tree.Kind.TRY_STATEMENT)) {
        return (TryStatementTree) parent;
      }
      parent = parent.parent();
    }
    return null;
  }

  private static class PostStatementVisitor extends CheckerTreeNodeVisitor {

    public PostStatementVisitor(CheckerContext context) {
      super(context.getState());
    }

    @Override
    public void visitNewClass(NewClassTree syntaxNode) {
      if (isOpeningResource(syntaxNode)) {
        final SymbolicValue instanceValue = programState.peekValue();
        if (!(instanceValue instanceof ResourceWrapperSymbolicValue)) {
          programState = programState.addConstraint(instanceValue, new ObjectConstraint(false, false, syntaxNode, Status.OPENED));
        }
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree syntaxNode) {
      if (syntaxNode.methodSelect().is(Tree.Kind.MEMBER_SELECT) && needsClosing(syntaxNode.symbolType())) {
        final ExpressionTree targetExpression = ((MemberSelectExpressionTree) syntaxNode.methodSelect()).expression();
        if (targetExpression.is(Tree.Kind.IDENTIFIER) && !isWithinTryHeader(syntaxNode)
          && (syntaxNode.symbol().isStatic() || isJdbcResourceCreation(targetExpression))) {
          programState = programState.addConstraint(programState.peekValue(), new ObjectConstraint(false, false, syntaxNode, Status.OPENED));
        }
      }
    }

    private static boolean isJdbcResourceCreation(ExpressionTree targetExpression) {
      for (String creator : JDBC_RESOURCE_CREATIONS) {
        if (targetExpression.symbolType().is(creator)) {
          return true;
        }
      }
      return false;
    }
  }

  public static class ResourceWrapperSymbolicValue extends SymbolicValue {

    private final SymbolicValue dependent;

    public ResourceWrapperSymbolicValue(int id, SymbolicValue dependent) {
      super(id);
      this.dependent = dependent;
    }

    @Override
    public SymbolicValue wrappedValue() {
      return dependent.wrappedValue();
    }
  }

  static class WrappedValueFactory implements SymbolicValueFactory {

    private final SymbolicValue value;

    WrappedValueFactory(SymbolicValue value) {
      this.value = value;
    }

    @Override
    public SymbolicValue createSymbolicValue(int counter, Tree syntaxNode) {
      return new ResourceWrapperSymbolicValue(counter, value);
    }
  }

  static class PreStatementVisitor extends CheckerTreeNodeVisitor {

    private static final String CLOSE_METHOD_NAME = "close";
    private static final String GET_MORE_RESULTS_METHOD_NAME = "getMoreResults";
    private static final String GET_RESULT_SET = "getResultSet";

    private final ConstraintManager constraintManager;

    PreStatementVisitor(CheckerContext context) {
      super(context.getState());
      constraintManager = context.getConstraintManager();
    }

    @Override
    public void visitNewClass(NewClassTree syntaxNode) {
      final List<SymbolicValue> arguments = programState.peekValues(syntaxNode.arguments().size());
      if (isOpeningResource(syntaxNode)) {
        Iterator<SymbolicValue> iterator = arguments.iterator();
        for (ExpressionTree argument : syntaxNode.arguments()) {
          if (!iterator.hasNext()) {
            throw new IllegalStateException("Mismatch between declared constructor arguments and argument values!");
          }
          final Type type = argument.symbolType();
          final SymbolicValue value = iterator.next();
          if (isCloseable(type)) {
            constraintManager.setValueFactory(new WrappedValueFactory(value));
            break;
          }
        }
      } else {
        closeArguments(syntaxNode.arguments(), 0);
      }
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree syntaxNode) {
      SymbolicValue currentVal = programState.peekValue();
      if (currentVal != null) {
        final ExpressionTree expression = syntaxNode.expression();
        if (expression != null) {
          if (expression.is(Tree.Kind.IDENTIFIER)) {
            final IdentifierTree identifier = (IdentifierTree) expression;
            currentVal = programState.getValue(identifier.symbol());
          } else {
            currentVal = programState.peekValue();
          }
          programState = closeResource(programState, currentVal);
        }
      }
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree syntaxNode) {
      final ExpressionTree variable = syntaxNode.variable();
      if (variable.is(Tree.Kind.ARRAY_ACCESS_EXPRESSION)) {
        List<SymbolicValue> stackedValues = programState.peekValues(2);
        SymbolicValue value = stackedValues.get(1);
        programState = closeResource(programState, value);
      }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree syntaxNode) {
      final ExpressionTree methodSelect = syntaxNode.methodSelect();
      if (isClosingResource(syntaxNode.symbol())) {
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          final ExpressionTree targetExpression = ((MemberSelectExpressionTree) methodSelect).expression();
          if (targetExpression.is(Tree.Kind.IDENTIFIER)) {
            final IdentifierTree identifier = (IdentifierTree) targetExpression;
            programState = closeResource(programState, programState.getValue(identifier.symbol()));
          } else {
            programState = closeResource(programState, programState.peekValue());
          }
        }
      } else if (syntaxNode.methodSelect().is(Tree.Kind.MEMBER_SELECT) && isOpeningResultSet(syntaxNode.symbol())) {
        final SymbolicValue value = getTargetValue(syntaxNode);
        constraintManager.setValueFactory(new WrappedValueFactory(value));
      } else if (isClosingResultSets(syntaxNode.symbol())) {
        if (methodSelect.is(Tree.Kind.MEMBER_SELECT)) {
          final SymbolicValue value = getTargetValue(syntaxNode);
          closeResultSetsRelatedTo(value);
        }
      } else {
        closeArguments(syntaxNode.arguments(), 1);
      }
    }

    private SymbolicValue getTargetValue(MethodInvocationTree syntaxNode) {
      final ExpressionTree targetExpression = ((MemberSelectExpressionTree) syntaxNode.methodSelect()).expression();
      final SymbolicValue value;
      if (targetExpression.is(Tree.Kind.IDENTIFIER)) {
        final IdentifierTree identifier = (IdentifierTree) targetExpression;
        value = programState.getValue(identifier.symbol());
      } else {
        value = programState.peekValue();
      }
      return value;
    }

    private void closeResultSetsRelatedTo(SymbolicValue value) {
      final List<ConstrainedValue> constrainedValues = programState.getValuesWithConstraints(Status.OPENED);
      for (ConstrainedValue constrainedValue : constrainedValues) {
        if (constrainedValue.value instanceof ResourceWrapperSymbolicValue) {
          ResourceWrapperSymbolicValue rValue = (ResourceWrapperSymbolicValue) constrainedValue.value;
          if (value.equals(rValue.dependent)) {
            programState = programState.addConstraint(rValue, constrainedValue.constraint.withStatus(Status.CLOSED));
          }
        }
      }
    }

    private void closeArguments(final Arguments arguments, int stackOffset) {
      final List<SymbolicValue> values = programState.peekValues(arguments.size() + stackOffset);
      final List<SymbolicValue> argumentValues = values.subList(stackOffset, values.size());
      for (SymbolicValue target : argumentValues) {
        programState = closeResource(programState, target);
      }
    }

    private static ProgramState closeResource(ProgramState programState, @Nullable final SymbolicValue target) {
      if (target != null) {
        ObjectConstraint oConstraint = openedConstraint(programState, target);
        if (oConstraint != null) {
          return programState.addConstraint(target.wrappedValue(), oConstraint.withStatus(Status.CLOSED));
        }
      }
      return programState;
    }

    private static ObjectConstraint openedConstraint(ProgramState programState, SymbolicValue value) {
      final Object constraint = programState.getConstraint(value.wrappedValue());
      if (constraint instanceof ObjectConstraint) {
        ObjectConstraint oConstraint = (ObjectConstraint) constraint;
        if (oConstraint.hasStatus(Status.OPENED)) {
          return oConstraint;
        }
      }
      return null;
    }

    private static boolean isClosingResource(Symbol methodInvocationSymbol) {
      return methodInvocationSymbol.isMethodSymbol() && CLOSE_METHOD_NAME.equals(methodInvocationSymbol.name());
    }

    private static boolean isClosingResultSets(Symbol methodInvocationSymbol) {
      return methodInvocationSymbol.isMethodSymbol() && GET_MORE_RESULTS_METHOD_NAME.equals(methodInvocationSymbol.name());
    }

    private static boolean isOpeningResultSet(Symbol methodInvocationSymbol) {
      return methodInvocationSymbol.isMethodSymbol() && GET_RESULT_SET.equals(methodInvocationSymbol.name());
    }
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    Multimap<Tree, String> issues = ((DefaultJavaFileScannerContext) context).getSEIssues(UnclosedResourcesCheck.class);
    for (Map.Entry<Tree, String> issue : issues.entries()) {
      context.reportIssue(this, issue.getKey(), issue.getValue());
    }
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    final PreStatementVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  @Override
  public ProgramState checkPostStatement(CheckerContext context, Tree syntaxNode) {
    final PostStatementVisitor visitor = new PostStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  @Override
  public void checkEndOfExecutionPath(CheckerContext context, ConstraintManager constraintManager) {
    final List<ObjectConstraint> constraints = context.getState().getFieldConstraints(Status.OPENED);
    for (ObjectConstraint constraint : constraints) {
      Tree syntaxNode = constraint.syntaxNode();
      if (syntaxNode.is(Tree.Kind.NEW_CLASS)) {
        context.reportIssue(syntaxNode, this, "Close this \"" + ((NewClassTree) syntaxNode).identifier() + "\".");
      } else if (syntaxNode.is(Tree.Kind.METHOD_INVOCATION)) {
        context.reportIssue(syntaxNode, this, "Close this \"" + toString((MethodInvocationTree) syntaxNode) + "\".");
      }
    }
  }

  private static String toString(MethodInvocationTree syntaxNode) {
    return syntaxNode.symbolType().name();
  }
}
