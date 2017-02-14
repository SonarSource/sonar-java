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

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicValueFactory;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
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

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.sonar.java.se.checks.UnclosedResourcesCheck.ResourceConstraint.CLOSED;
import static org.sonar.java.se.checks.UnclosedResourcesCheck.ResourceConstraint.OPEN;


@Rule(key = "S2095")
public class UnclosedResourcesCheck extends SECheck {

  public enum ResourceConstraint implements Constraint {
    OPEN, CLOSED;

    @Override
    public String valueAsString() {
      if (this == OPEN) {
        return "open";
      }
      return "closed";
    }
  }

  @RuleProperty(
    key = "excludedResourceTypes",
    description = "Comma separated list of the excluded resource types, using fully qualified names (example: \"org.apache.hadoop.fs.FileSystem\")",
    defaultValue = "")
  public String excludedTypes = "";
  private final List<String> excludedTypesList = new ArrayList<>();
  
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
    "com.sun.org.apache.xml.internal.security.utils.UnsyncByteArrayOutputStream",
    "org.springframework.context.ConfigurableApplicationContext"
  };
  private static final MethodMatcher[] CLOSEABLE_EXCEPTIONS = new MethodMatcher[] {
    MethodMatcher.create().typeDefinition("java.nio.file.FileSystems").name("getDefault").withoutParameter()
  };

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
    ExplodedGraph.Node node = context.getNode();
    context.getState().getValuesWithConstraints(OPEN).forEach(sv -> processUnclosedSymbolicValue(node, sv));
  }

  private void processUnclosedSymbolicValue(ExplodedGraph.Node node, SymbolicValue sv) {
    ArrayList<Class<? extends Constraint>> domains = Lists.newArrayList(ResourceConstraint.class);
    FlowComputation.flow(node, sv, OPEN::equals, domains).stream().flatMap(Collection::stream)
      .filter(location -> location.syntaxNode.is(Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION))
      .forEach(this::reportIssue);
  }

  private void reportIssue(JavaFileScannerContext.Location location) {
    String message = "Close this \"" + name(location.syntaxNode) + "\".";
    String flowMessage = name(location.syntaxNode) +  " is never closed.";
    Set<List<JavaFileScannerContext.Location>> flows = FlowComputation.singleton(flowMessage, location.syntaxNode);
    reportIssue(location.syntaxNode, message, flows);
  }

  private static String name(Tree tree) {
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      return ((NewClassTree) tree).symbolType().name();
    }
    return ((MethodInvocationTree) tree).symbolType().name();
  }

  private boolean needsClosing(Type type) {
    if (type.isSubtypeOf(STREAM_TOP_HIERARCHY)) {
      return false;
    }
    for (String ignoredTypes : IGNORED_CLOSEABLE_SUBTYPES) {
      if (type.isSubtypeOf(ignoredTypes)) {
        return false;
      }
    }
    for (String excludedType : loadExcludedTypesList()) {
      if (type.is(excludedType)) {
        return false;
      }
    }
    return isCloseable(type);
  }
  
  private List<String> loadExcludedTypesList() {
    if ( excludedTypesList.isEmpty() && !StringUtils.isBlank(excludedTypes)) {
      for (String excludedType : excludedTypes.split(",")) {
        excludedTypesList.add(excludedType.trim());
      }
    }
    return excludedTypesList;
  }

  private static boolean isCloseable(Type type) {
    return type.isSubtypeOf(JAVA_IO_AUTO_CLOSEABLE) || type.isSubtypeOf(JAVA_IO_CLOSEABLE);
  }

  private boolean isOpeningResource(NewClassTree syntaxNode) {
    if (isWithinTryHeader(syntaxNode)) {
      return false;
    }
    return needsClosing(syntaxNode.symbolType());
  }

  private static boolean isWithinTryHeader(Tree syntaxNode) {
    Tree parent = syntaxNode;
    while (parent != null && !parent.is(Tree.Kind.VARIABLE) ) {
      parent = parent.parent();
    }
    if (parent != null && parent.is(Tree.Kind.VARIABLE)) {
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
    public SymbolicValue createSymbolicValue(int counter) {
      return new ResourceWrapperSymbolicValue(counter, value);
    }

  }

  private class PreStatementVisitor extends CheckerTreeNodeVisitor {
    // closing methods
    private static final String CLOSE = "close";
    private static final String GET_MORE_RESULTS = "getMoreResults";
    // opening resources method
    private static final String GET_RESULT_SET = "getResultSet";

    private final ConstraintManager constraintManager;

    PreStatementVisitor(CheckerContext context) {
      super(context.getState());
      constraintManager = context.getConstraintManager();
    }

    @Override
    public void visitNewClass(NewClassTree syntaxNode) {
      List<SymbolicValue> arguments = new ArrayList<>(programState.peekValues(syntaxNode.arguments().size()));
      Collections.reverse(arguments);
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
        closeArguments(syntaxNode.arguments());
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

    private boolean isNonLocalStorage(ExpressionTree variable) {
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
      // close any resource used as argument, even for unknown methods
      closeArguments(syntaxNode.arguments());
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
      for (SymbolicValue constrainedValue : programState.getValuesWithConstraints(OPEN)) {
        if (constrainedValue instanceof ResourceWrapperSymbolicValue) {
          ResourceWrapperSymbolicValue rValue = (ResourceWrapperSymbolicValue) constrainedValue;
          if (value.equals(rValue.dependent)) {
            programState = programState.addConstraint(rValue, CLOSED);
          }
        }
      }
    }

    private void closeArguments(final Arguments arguments) {
      programState.peekValues(arguments.size()).forEach(this::closeResource);
    }

    private void closeResource(@Nullable final SymbolicValue target) {
      SymbolicValue toClose = target;
      if(toClose instanceof ResourceWrapperSymbolicValue) {
        toClose = target.wrappedValue();
      }
      if (toClose != null) {
        ResourceConstraint oConstraint = programState.getConstraint(toClose, ResourceConstraint.class);
        if (oConstraint != null) {
          programState = programState.addConstraint(toClose, CLOSED);
        }
      }
    }
  }

  private class PostStatementVisitor extends CheckerTreeNodeVisitor {

    PostStatementVisitor(CheckerContext context) {
      super(context.getState());
    }

    @Override
    public void visitNewClass(NewClassTree syntaxNode) {
      if (isOpeningResource(syntaxNode)) {
        final SymbolicValue instanceValue = programState.peekValue();
        if (!(instanceValue instanceof ResourceWrapperSymbolicValue)) {
          programState = programState.addConstraint(instanceValue, OPEN);
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
          programState = programState.addConstraint(programState.peekValue(), OPEN);
        }
      }
    }

    private boolean isJdbcResourceCreation(ExpressionTree targetExpression) {
      for (String creator : JDBC_RESOURCE_CREATIONS) {
        if (targetExpression.symbolType().is(creator)) {
          return true;
        }
      }
      return false;
    }
  }

}
