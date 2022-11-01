/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.cfg.CFG;
import org.sonarsource.analyzer.commons.collections.ListUtils;
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
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ListTree;
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

  private final Set<TryStatementTree> visitedTryWithResourcesTrees = new HashSet<>();
  private final Set<Tree> knownResources = new HashSet<>();
  private Type visitedMethodOwnerType;

  private static final Pattern METHOD_NAMES_OPENING_RESOURCES = Pattern.compile("(new|create|open).*");

  private static final String JAVA_IO_AUTO_CLOSEABLE = "java.lang.AutoCloseable";
  private static final String JAVA_IO_CLOSEABLE = "java.io.Closeable";
  private static final String JAVA_SQL_CONNECTION = "java.sql.Connection";
  private static final String JAVA_NIO_FILE_FILES = "java.nio.file.Files";

  private static final MethodMatchers JDBC_RESOURCE_CREATIONS = MethodMatchers.or(
    MethodMatchers.create().ofTypes(JAVA_SQL_CONNECTION).names("createStatement", "prepareStatement", "prepareCall").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("javax.sql.DataSource").names("getConnection").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("java.sql.DriverManager").names("getConnection").withAnyParameters().build()
  );

  private static final MethodMatchers STREAMS_BACKED_BY_RESOURCE = MethodMatchers.or(
    MethodMatchers.create().ofTypes(JAVA_NIO_FILE_FILES)
      .names("lines", "newDirectoryStream", "list", "find", "walk")
      .withAnyParameters()
      .build()
  );

  private static final MethodMatchers KNOWN_METHODS_KEEPING_ARGUMENTS_OPEN =
    MethodMatchers.or(
      MethodMatchers.create()
        .ofTypes("java.util.Properties")
        // Note: "loadFromXML" is the only "Properties"'s method that close its argument.
        .names("load", "store", "storeToXML", "save", "list")
        .withAnyParameters()
        .build(),
      MethodMatchers.create()
        .ofTypes("org.apache.commons.io.IOUtils")
        .name(name -> name.startsWith("read") || name.startsWith("copy") || name.startsWith("contentEquals")
          || name.startsWith("skip") || "consume".equals(name))
        .withAnyParameters()
        .build()
    );

  private static final String STREAM_TOP_HIERARCHY = "java.util.stream.BaseStream";
  private static final String[] IGNORED_CLOSEABLE_SUBTYPES = {
    "java.io.ByteArrayOutputStream",
    "java.io.ByteArrayInputStream",
    "java.io.CharArrayReader",
    "java.io.CharArrayWriter",
    "java.io.StringReader",
    "java.io.StringWriter",
    "org.apache.commons.io.output.ByteArrayOutputStream",
    "org.apache.commons.io.output.UnsynchronizedByteArrayOutputStream",
    "com.sun.org.apache.xml.internal.security.utils.UnsyncByteArrayOutputStream",
    "org.springframework.context.ConfigurableApplicationContext"
  };

  private static final MethodMatchers CLOSEABLE_EXCEPTIONS = MethodMatchers.or(
    MethodMatchers.create()
      .ofTypes("java.nio.file.FileSystems")
      .names("getDefault")
      .addWithoutParametersMatcher()
      .build(),
    MethodMatchers.create()
      .ofTypes("javax.jms.Connection")
      .names("createSession")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("javax.jms.Session")
      .names("createProducer", "createConsumer", "createDurableConsumer", "createSharedConsumer", "createSharedDurableConsumer")
      .withAnyParameters()
      .build()
  );

  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.visitedTryWithResourcesTrees.clear();
    this.knownResources.clear();
    super.scanFile(context);
  }

  @Override
  public void init(MethodTree methodTree, CFG cfg) {
    this.visitedMethodOwnerType = methodTree.symbol().owner().type();
  }

  @Override
  public ProgramState checkPreStatement(CheckerContext context, Tree syntaxNode) {
    collectTryWithResources(syntaxNode);
    final PreStatementVisitor visitor = new PreStatementVisitor(context);
    syntaxNode.accept(visitor);
    return visitor.programState;
  }

  private void collectTryWithResources(Tree syntaxNode) {
    if (syntaxNode.is(Tree.Kind.TRY_STATEMENT)) {
      TryStatementTree tryStatementTree = (TryStatementTree) syntaxNode;
      ListTree<Tree> resourceList = tryStatementTree.resourceList();
      if (!resourceList.isEmpty() && visitedTryWithResourcesTrees.add(tryStatementTree)) {
        knownResources.addAll(ResourcesCollector.collect(resourceList));
      }
    }
  }

  private static class ResourcesCollector extends BaseTreeVisitor {

    private final Set<Tree> resources = new HashSet<>();

    static Set<Tree> collect(ListTree<Tree> resources) {
      ResourcesCollector collector = new ResourcesCollector();
      resources.accept(collector);
      return collector.resources;
    }

    @Override
    public void visitIdentifier(IdentifierTree tree) {
      resources.add(tree);
      // almost a leaf, no need to call the super implementation
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      resources.add(tree);
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      resources.add(tree);
      super.visitNewClass(tree);
    }
  }

  private boolean isWithinTryHeader(Tree syntaxNode) {
    return knownResources.contains(syntaxNode);
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
    FlowComputation.flowWithoutExceptions(node, sv, OPEN::equals, RESOURCE_CONSTRAINT_DOMAIN, FlowComputation.MAX_LOOKUP_FLOWS).stream()
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
    for (String ignoredType : IGNORED_CLOSEABLE_SUBTYPES) {
      if (type.isSubtypeOf(ignoredType)) {
        return false;
      }
    }
    return isCloseable(type);
  }

  private boolean excludedByRuleOption(Type type) {
    return loadExcludedTypesList().stream().anyMatch(type::is);
  }

  private List<String> loadExcludedTypesList() {
    if (excludedTypesList.isEmpty() && !StringUtils.isBlank(excludedTypes)) {
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

    private final ConstraintManager constraintManager;

    PreStatementVisitor(CheckerContext context) {
      super(context.getState());
      constraintManager = context.getConstraintManager();
    }

    @Override
    public void visitNewClass(NewClassTree syntaxNode) {
      if (isOpeningResource(syntaxNode)) {
        List<ProgramState.SymbolicValueSymbol> arguments = ListUtils.reverse(programState.peekValuesAndSymbols(syntaxNode.arguments().size()));
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
        if (value != null && CLOSE.equals(methodName)) {
          closeResource(value);
        }
      }
      if (!KNOWN_METHODS_KEEPING_ARGUMENTS_OPEN.matches(syntaxNode)) {
        // close any resource used as argument, even for unknown methods
        closeArguments(syntaxNode.arguments());
      }
    }

    @CheckForNull
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
      if (isWithinTryHeader(tree)) {
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
      SymbolicValue instanceValue = Objects.requireNonNull(programState.peekValue());
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
        SymbolicValue peekedValue = Objects.requireNonNull(programState.peekValue());
        programState = programState.addConstraintTransitively(peekedValue, OPEN);
        // returned resource is not null
        programState = programState.addConstraint(peekedValue, ObjectConstraint.NOT_NULL);
      }
    }

    private boolean methodOpeningResource(MethodInvocationTree mit) {
      return !isWithinTryHeader(mit)
        && !excludedByRuleOption(mit.symbolType())
        && !handledByFramework(mit)
        && (JDBC_RESOURCE_CREATIONS.matches(mit)
          || STREAMS_BACKED_BY_RESOURCE.matches(mit)
          || (needsClosing(mit.symbolType()) && !CLOSEABLE_EXCEPTIONS.matches(mit) && mitHeuristics(mit)));
    }

    private boolean handledByFramework(MethodInvocationTree mit) {
      // according to spring documentation, no leak is expected:
      // "Implementations do not need to concern themselves with SQLExceptions that may be thrown from operations
      // they attempt. The JdbcTemplate class will catch and handle SQLExceptions appropriately."
      return JDBC_RESOURCE_CREATIONS.matches(mit)
        && (visitedMethodOwnerType.isSubtypeOf("org.springframework.jdbc.core.PreparedStatementCreator")
          || visitedMethodOwnerType.isSubtypeOf("org.springframework.jdbc.core.CallableStatementCreator"));
    }

    private boolean mitHeuristics(MethodInvocationTree mit) {
      Symbol methodSymbol = mit.symbol();
      return !methodSymbol.isUnknown()
        && invocationOfMethodFromOtherClass(methodSymbol)
        && METHOD_NAMES_OPENING_RESOURCES.matcher(methodSymbol.name()).matches();
    }

    private boolean invocationOfMethodFromOtherClass(Symbol methodSymbol) {
      return !visitedMethodOwnerType.symbol().equals(methodSymbol.owner());
    }
  }
}
