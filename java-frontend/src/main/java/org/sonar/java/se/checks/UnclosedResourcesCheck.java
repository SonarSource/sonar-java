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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.cfg.CFG;
import org.sonar.java.matcher.MethodMatcher;
import org.sonar.java.matcher.MethodMatcherCollection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.se.CheckerContext;
import org.sonar.java.se.ExplodedGraph;
import org.sonar.java.se.Flow;
import org.sonar.java.se.FlowComputation;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.SymbolicValueFactory;
import org.sonar.java.se.constraint.Constraint;
import org.sonar.java.se.constraint.ConstraintManager;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;

import static org.sonar.java.se.checks.UnclosedResourcesCheck.ResourceConstraint.CLOSED;
import static org.sonar.java.se.checks.UnclosedResourcesCheck.ResourceConstraint.OPEN;


@Rule(key = "S2095")
public class UnclosedResourcesCheck extends SECheck {

  private static final List<Class<? extends Constraint>> RESOURCE_CONSTRAINT_DOMAIN = Collections.singletonList(ResourceConstraint.class);

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

  private Type visitedMethodOwnerType;

  private static final String JAVA_IO_AUTO_CLOSEABLE = "java.lang.AutoCloseable";
  private static final String JAVA_IO_CLOSEABLE = "java.io.Closeable";
  private static final String JAVA_SQL_STATEMENT = "java.sql.Statement";
  private static final String JAVA_SQL_CONNECTION = "java.sql.Connection";
  private static final String JAVA_NIO_FILE_FILES = "java.nio.file.Files";

  private static final MethodMatcherCollection JDBC_RESOURCE_CREATIONS = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition(JAVA_SQL_CONNECTION).name("createStatement").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_SQL_CONNECTION).name("prepareStatement").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_SQL_CONNECTION).name("prepareCall").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_SQL_STATEMENT).name("executeQuery").addParameter("java.lang.String"),
    MethodMatcher.create().typeDefinition(JAVA_SQL_STATEMENT).name("getResultSet").withoutParameter(),
    MethodMatcher.create().typeDefinition(JAVA_SQL_STATEMENT).name("getGeneratedKeys").withoutParameter(),
    MethodMatcher.create().typeDefinition("java.sql.PreparedStatement").name("executeQuery").withoutParameter(),
    MethodMatcher.create().typeDefinition("javax.sql.DataSource").name("getConnection").withAnyParameters(),
    MethodMatcher.create().typeDefinition("java.sql.DriverManager").name("getConnection").withAnyParameters()
  );

  private static final MethodMatcherCollection STREAMS_BACKED_BY_RESOURCE = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition(JAVA_NIO_FILE_FILES).name("lines").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_NIO_FILE_FILES).name("newDirectoryStream").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_NIO_FILE_FILES).name("list").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_NIO_FILE_FILES).name("find").withAnyParameters(),
    MethodMatcher.create().typeDefinition(JAVA_NIO_FILE_FILES).name("walk").withAnyParameters()
  );

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

  private static final MethodMatcherCollection CLOSEABLE_EXCEPTIONS = MethodMatcherCollection.create(
    MethodMatcher.create().typeDefinition("java.nio.file.FileSystems").name("getDefault").withoutParameter()
  );

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    this.visitedMethodOwnerType = methodTree.symbol().owner().type();
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
    if (context.getState().exitingOnRuntimeException()) {
      return;
    }
    ExplodedGraph.Node node = context.getNode();
    Set<SymbolicValue> svToReport = symbolicValuesToReport(context);
    svToReport.forEach(sv -> processUnclosedSymbolicValue(node, sv));
  }

  private static Set<SymbolicValue> symbolicValuesToReport(CheckerContext context) {
    List<SymbolicValue> openSymbolicValues = context.getState().getValuesWithConstraints(OPEN);
    Set<SymbolicValue> svToReport = new HashSet<>(openSymbolicValues);
    // report only outermost OPEN symbolic value
    for (SymbolicValue openSymbolicValue : openSymbolicValues) {
      if (openSymbolicValue instanceof ResourceWrapperSymbolicValue) {
        svToReport.remove(openSymbolicValue.wrappedValue());
      }
    }
    return svToReport;
  }

  private void processUnclosedSymbolicValue(ExplodedGraph.Node node, SymbolicValue sv) {
    FlowComputation.flowWithoutExceptions(node, sv, OPEN::equals, RESOURCE_CONSTRAINT_DOMAIN).stream()
      .flatMap(Flow::firstFlowLocation)
      .filter(location -> location.syntaxNode.is(Tree.Kind.NEW_CLASS, Tree.Kind.METHOD_INVOCATION))
      .forEach(this::reportIssue);
  }

  private void reportIssue(JavaFileScannerContext.Location location) {
    String message = "Use try-with-resources or close this \"" + name(location.syntaxNode) + "\" in a \"finally\" clause.";
    reportIssue(location.syntaxNode, message);
  }

  private static String name(Tree tree) {
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      return ((NewClassTree) tree).symbolType().name();
    }
    return ((MethodInvocationTree) tree).symbolType().name();
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

  private boolean excludedByRuleOption(Type type) {
    for (String excludedType : loadExcludedTypesList()) {
      if (type.is(excludedType)) {
        return true;
      }
    }
    return false;
  }

  private List<String> loadExcludedTypesList() {
    if ( excludedTypesList.isEmpty() && !StringUtils.isBlank(excludedTypes)) {
      for (String excludedType : excludedTypes.split(",")) {
        excludedTypesList.add(excludedType.trim());
      }
    }
    return excludedTypesList;
  }

  private static boolean isCloseable(ExpressionTree expr) {
    return isCloseable(expr.symbolType());
  }

  private static boolean isCloseable(Type type) {
    return type.isSubtypeOf(JAVA_IO_AUTO_CLOSEABLE) || type.isSubtypeOf(JAVA_IO_CLOSEABLE);
  }

  private boolean isOpeningResource(NewClassTree syntaxNode) {
    if (isWithinTryHeader(syntaxNode) || excludedByRuleOption(syntaxNode.symbolType())) {
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
      return isTryStatementResource(parent);
    }
    return false;
  }

  private static boolean isTryStatementResource(Tree tree) {
    final TryStatementTree tryStatement = getEnclosingTryStatement(tree);
    return tryStatement != null && tryStatement.resourceList().contains(tree);
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

    ResourceWrapperSymbolicValue(SymbolicValue dependent) {
      this.dependent = dependent;
    }

    @Override
    public SymbolicValue wrappedValue() {
      return dependent;
    }

  }

  private static class WrappedValueFactory implements SymbolicValueFactory {

    private final SymbolicValue value;

    WrappedValueFactory(SymbolicValue value) {
      this.value = value;
    }

    @Override
    public SymbolicValue createSymbolicValue() {
      return new ResourceWrapperSymbolicValue(value);
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
      if (isOpeningResource(syntaxNode)) {
        List<ProgramState.SymbolicValueSymbol> arguments = Lists.reverse(programState.peekValuesAndSymbols(syntaxNode.arguments().size()));
        Iterator<ProgramState.SymbolicValueSymbol> iterator = arguments.iterator();
        for (ExpressionTree argumentTree : syntaxNode.arguments()) {
          if (!iterator.hasNext()) {
            throw new IllegalStateException("Mismatch between declared constructor arguments and argument values!");
          }
          ProgramState.SymbolicValueSymbol argument = iterator.next();
          if (shouldWrapArgument(argument, argumentTree)) {
            constraintManager.setValueFactory(new WrappedValueFactory(argument.symbolicValue()));
            break;
          }
          if (shouldCloseArgument(argument)){
            closeResource(argument.symbolicValue());
          }
        }
      } else {
        closeArguments(syntaxNode.arguments());
      }
    }

    private boolean shouldWrapArgument(ProgramState.SymbolicValueSymbol argument, ExpressionTree argumentTree) {
      ResourceConstraint argConstraint = programState.getConstraint(argument.symbolicValue(), ResourceConstraint.class);
      return (argConstraint == OPEN && argument.symbol() != null)
        || (argConstraint == null && isCloseable(argumentTree));
    }

    private boolean shouldCloseArgument(ProgramState.SymbolicValueSymbol argument) {
      ResourceConstraint argConstraint = programState.getConstraint(argument.symbolicValue(), ResourceConstraint.class);
      return argConstraint == OPEN;
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
            programState = programState.addConstraintTransitively(rValue, CLOSED);
          }
        }
      }
    }

    private void closeArguments(final Arguments arguments) {
      programState.peekValues(arguments.size()).forEach(this::closeResource);
    }

    private void closeResource(@Nullable final SymbolicValue target) {
      if (target instanceof ResourceWrapperSymbolicValue) {
        closeResource(target.wrappedValue());
      }
      if (target != null) {
        ResourceConstraint oConstraint = programState.getConstraint(target, ResourceConstraint.class);
        if (oConstraint != null) {
          programState = programState.addConstraintTransitively(target, CLOSED);
        }
      }
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      // close resource as soon as it is encountered in the resource declaration
      if (isTryStatementResource(tree)) {
        Symbol symbol = tree.symbol();
        closeResource(programState.getValue(symbol));
      }
    }
  }

  private class PostStatementVisitor extends CheckerTreeNodeVisitor {

    PostStatementVisitor(CheckerContext context) {
      super(context.getState());
    }

    @Override
    public void visitNewClass(NewClassTree syntaxNode) {
      final SymbolicValue instanceValue = programState.peekValue();
      if (isOpeningResource(syntaxNode) && !passedCloseableParameter(instanceValue)) {
        programState = programState.addConstraintTransitively(instanceValue, OPEN);
      }
    }

    private boolean passedCloseableParameter(SymbolicValue resource) {
      return resource instanceof ResourceWrapperSymbolicValue
        && programState.getConstraint(((ResourceWrapperSymbolicValue) resource).dependent, ResourceConstraint.class) == null;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree syntaxNode) {
      if (methodOpeningResource(syntaxNode)) {
        SymbolicValue peekedValue = programState.peekValue();
        programState = programState.addConstraintTransitively(peekedValue, OPEN);
        // returned resource is not null
        programState = programState.addConstraint(peekedValue, ObjectConstraint.NOT_NULL);
      }
    }

    private boolean methodOpeningResource(MethodInvocationTree mit) {
      return !isWithinTryHeader(mit)
        && !excludedByRuleOption(mit.symbolType())
        && !handledByFramework(mit)
        && (JDBC_RESOURCE_CREATIONS.anyMatch(mit)
          || STREAMS_BACKED_BY_RESOURCE.anyMatch(mit)
          || (needsClosing(mit.symbolType()) && !CLOSEABLE_EXCEPTIONS.anyMatch(mit) && mitHeuristics(mit)));
    }

    private boolean handledByFramework(MethodInvocationTree mit) {
      // according to spring documentation, no leak is expected:
      // "Implementations do not need to concern themselves with SQLExceptions that may be thrown from operations
      // they attempt. The JdbcTemplate class will catch and handle SQLExceptions appropriately."
      return JDBC_RESOURCE_CREATIONS.anyMatch(mit)
        && (visitedMethodOwnerType.isSubtypeOf("org.springframework.jdbc.core.PreparedStatementCreator")
          || visitedMethodOwnerType.isSubtypeOf("org.springframework.jdbc.core.CallableStatementCreator"));
    }

    private boolean mitHeuristics(MethodInvocationTree mit) {
      Symbol methodSymbol = mit.symbol();
      return !methodSymbol.isUnknown()
        && invocationOfMethodFromOtherClass(mit)
        && methodSymbol.name().matches("new.*|create.*|open.*");
    }

    private boolean invocationOfMethodFromOtherClass(MethodInvocationTree mit) {
      Symbol methodClass = mit.symbol().owner();
      Tree enclosingType = mit;
      while (!enclosingType.is(Tree.Kind.CLASS, Tree.Kind.ENUM, Tree.Kind.INTERFACE)) {
        enclosingType = enclosingType.parent();
      }
      return ((ClassTree) enclosingType).symbol() != methodClass;
    }
  }
}
