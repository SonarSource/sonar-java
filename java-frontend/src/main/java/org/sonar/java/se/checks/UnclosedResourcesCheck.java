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
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.resolve.Flags;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicValueFactory;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.VariableTree;

import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Rule(key = "S2095")
public class UnclosedResourcesCheck extends SECheck {

  private enum Status {
    OPENED, CLOSED
  }

  private static final String JAVA_IO_AUTO_CLOSEABLE = "java.lang.AutoCloseable";
  private static final String JAVA_IO_CLOSEABLE = "java.io.Closeable";
  private static final String JAVA_SQL_STATEMENT = "java.sql.Statement";
  private static final String[] JDBC_RESOURCE_CREATIONS = {"java.sql.Connection", JAVA_SQL_STATEMENT};
  private static final String STREAM_TOP_HIERARCHY = "java.util.stream.BaseStream";
  private static final String[] IGNORED_CLOSEABLE_SUBTYPES = {
    "java.io.ByteArrayOutputStream",
    "java.io.ByteArrayInputStream",
    "java.io.CharArrayReader",
    "java.io.CharArrayWriter",
    "java.io.StringReader",
    "java.io.StringWriter",
    "com.sun.org.apache.xml.internal.security.utils.UnsyncByteArrayOutputStream"
  };
  private static final MethodMatcher[] CLOSEABLE_EXCEPTIONS = new MethodMatcher[] {
    MethodMatcher.create().typeDefinition("java.nio.file.FileSystems").name("getDefault").withoutParameter()
  };

  private List<Symbol> closeableMethodParameters;

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    this.closeableMethodParameters = methodTree.parameters().stream()
      .map(VariableTree::symbol)
      .filter(s -> needsClosing(s.type()))
      .collect(Collectors.toList());
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    ProgramState programState = context.getState();
    List<SymbolicValue> closeableParameters = closeableMethodParameters.stream()
      .map(programState::getValue)
      .filter(Objects::nonNull)
      .collect(Collectors.toList());

    final PreStatementVisitor visitor = new PreStatementVisitor(context, closeableParameters);
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

  private static boolean needsClosing(Type type) {
    if (type.isSubtypeOf(STREAM_TOP_HIERARCHY)) {
      return false;
    }
    for (String ignoredTypes : IGNORED_CLOSEABLE_SUBTYPES) {
      if (type.isSubtypeOf(ignoredTypes)) {
        return false;
      }
    }
    return isCloseable(type);
  }

  private static boolean isCloseable(Type type) {
    return type.isSubtypeOf(JAVA_IO_AUTO_CLOSEABLE) || type.isSubtypeOf(JAVA_IO_CLOSEABLE);
  }

  private static boolean isOpeningResource(NewClassTree syntaxNode) {
    if (isWithinTryHeader(syntaxNode)) {
      return false;
    }
    return needsClosing(syntaxNode.symbolType());
  }

  private static boolean isWithinTryHeader(Tree syntaxNode) {
    final Tree parent = syntaxNode.parent();
    if (parent.is(Tree.Kind.VARIABLE)) {
      return isTryStatementResource((VariableTree) parent);
    }
    return false;
  }

  private static boolean isTryStatementResource(VariableTree variable) {
    final TryStatementTree tryStatement = getEnclosingTryStatement(variable);
    return tryStatement != null && tryStatement.resources().contains(variable);
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

  private static class ResourceWrapperSymbolicValue extends SymbolicValue {

    private final SymbolicValue dependent;

    ResourceWrapperSymbolicValue(int id, SymbolicValue dependent) {
      super(id);
      this.dependent = dependent;
    }

    @Override
    public SymbolicValue wrappedValue() {
      return dependent.wrappedValue();
    }

  }

  private static class WrappedValueFactory implements SymbolicValueFactory {

    private final SymbolicValue value;

    WrappedValueFactory(SymbolicValue value) {
      this.value = value;
    }

    @Override
    public SymbolicValue createSymbolicValue(int counter, Tree syntaxNode) {
      return new ResourceWrapperSymbolicValue(counter, value);
    }

  }

  private static class PreStatementVisitor extends CheckerTreeNodeVisitor {
    // closing methods
    private static final String CLOSE = "close";
    private static final String GET_MORE_RESULTS = "getMoreResults";
    // opening resources method
    private static final String GET_RESULT_SET = "getResultSet";

    private final CheckerContext context;
    private final ConstraintManager constraintManager;
    private final List<SymbolicValue> closeableParameters;

    PreStatementVisitor(CheckerContext context, List<SymbolicValue> closeableParameters) {
      super(context.getState());
      this.context = context;
      this.closeableParameters = closeableParameters;
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
          closeResource(currentVal);
        }
      }
    }

    @Override
    public void visitAssignmentExpression(AssignmentExpressionTree syntaxNode) {
      final ExpressionTree variable = syntaxNode.variable();
      if (isNonLocalStorage(variable)) {
        SymbolicValue value;
        if (ExpressionUtils.isSimpleAssignment(syntaxNode)) {
          value = programState.peekValue();
        } else {
          value = programState.peekValues(2).get(0);
        }

        closeResource(value);
      }
    }

    private static boolean isNonLocalStorage(ExpressionTree variable) {
      if (variable.is(Tree.Kind.IDENTIFIER)) {
        Symbol owner = ((IdentifierTree) variable).symbol().owner();
        return !owner.isMethodSymbol();
      }
      return true;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree syntaxNode) {
      Symbol symbol = syntaxNode.symbol();
      if (symbol.isMethodSymbol() && syntaxNode.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        String methodName = symbol.name();
        SymbolicValue value = getTargetValue(syntaxNode);
        if (CLOSE.equals(methodName)) {
          closeResource(value);
        } else if (GET_MORE_RESULTS.equals(methodName)) {
          closeResultSetsRelatedTo(value);
        } else if (GET_RESULT_SET.equals(methodName)) {
          constraintManager.setValueFactory(new WrappedValueFactory(value));
        }
      }

      if (methodCanNotBeOverriden(symbol)) {
        // we rely on X-Procedural analysis
        return;
      }

      // close any resource used as argument, even for unknown methods
      closeArguments(syntaxNode.arguments(), 1);
    }

    private static boolean methodCanNotBeOverriden(Symbol symbol) {
      if (!symbol.isMethodSymbol()) {
        return false;
      }
      JavaSymbol.MethodJavaSymbol methodSymbol = (JavaSymbol.MethodJavaSymbol) symbol;
      if (methodSymbol.declaration() == null) {
        // method is external to the file
        return false;
      }
      if (((methodSymbol.flags() & Flags.NATIVE) != 0) || methodSymbol.isAbstract() || methodSymbol.isVarArgs()) {
        return false;
      }
      return methodSymbol.isPrivate() || methodSymbol.isFinal() || methodSymbol.isStatic() || methodSymbol.owner().isFinal();
    }

    private SymbolicValue getTargetValue(MethodInvocationTree syntaxNode) {
      ExpressionTree targetExpression = ((MemberSelectExpressionTree) syntaxNode.methodSelect()).expression();
      SymbolicValue value;
      if (targetExpression.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree identifier = (IdentifierTree) targetExpression;
        value = programState.getValue(identifier.symbol());
      } else {
        value = programState.peekValue();
      }
      return value;
    }

    private void closeResultSetsRelatedTo(SymbolicValue value) {
      for (Map.Entry<SymbolicValue, ObjectConstraint> constrainedValue : programState.getValuesWithConstraints(Status.OPENED).entrySet()) {
        if (constrainedValue.getKey() instanceof ResourceWrapperSymbolicValue) {
          ResourceWrapperSymbolicValue rValue = (ResourceWrapperSymbolicValue) constrainedValue.getKey();
          if (value.equals(rValue.dependent)) {
            programState = programState.addConstraint(rValue, constrainedValue.getValue().withStatus(Status.CLOSED));
          }
        }
      }
    }

    private void closeArguments(final Arguments arguments, int stackOffset) {
      final List<SymbolicValue> values = programState.peekValues(arguments.size() + stackOffset);
      final List<SymbolicValue> argumentValues = values.subList(stackOffset, values.size());
      for (SymbolicValue target : argumentValues) {
        closeResource(target);
      }
    }

    private void closeResource(@Nullable final SymbolicValue target) {
      ObjectConstraint openConstraint = null;
      if (target != null) {
        openConstraint = programState.getConstraintWithStatus(target, Status.OPENED);
        if (openConstraint != null) {
          programState = programState.addConstraint(target.wrappedValue(), openConstraint.withStatus(Status.CLOSED));
        }
      }
      if (openConstraint == null) {
        Optional<SymbolicValue> closeableParameter = closeableParameters.stream().filter(sv -> sv == target).findAny();
        if (!closeableParameter.isPresent()) {
          // not a closeable, do nothing
          return;
        }
        Constraint constraint = programState.getConstraint(target);
        if (constraint instanceof ObjectConstraint) {
          programState = programState.addConstraint(target, ((ObjectConstraint) constraint).withStatus(Status.CLOSED));
        } else if (constraint == null) {
          // no known constraint on the closable which need to be closed, but we do not invalidate the fact that it still can be null
          ProgramState nullProgramState = programState.addConstraint(target, ObjectConstraint.nullConstraint());
          context.addTransition(nullProgramState);

          programState = programState.addConstraint(target, new ObjectConstraint(false, false, null, Status.CLOSED));
        }
      }
    }
  }

  private static class PostStatementVisitor extends CheckerTreeNodeVisitor {

    PostStatementVisitor(CheckerContext context) {
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
      for (MethodMatcher matcher : CLOSEABLE_EXCEPTIONS) {
        if (matcher.matches(syntaxNode)) {
          return;
        }
      }
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

}
