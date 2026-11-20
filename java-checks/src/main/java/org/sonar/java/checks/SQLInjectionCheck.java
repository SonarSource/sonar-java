/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BinaryExpressionTree;
import org.sonar.plugins.java.api.tree.ConditionalExpressionTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionStatementTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.ReassignmentFinder.getInitializerOrExpression;
import static org.sonar.java.checks.helpers.ReassignmentFinder.getReassignments;
import static org.sonar.plugins.java.api.semantic.MethodMatchers.CONSTRUCTOR;

@Rule(key = "S2077")
public class SQLInjectionCheck extends IssuableSubscriptionVisitor {

  private static final String JAVA_SQL_STATEMENT = "java.sql.Statement";
  private static final String JAVA_SQL_CONNECTION = "java.sql.Connection";
  private static final String SPRING_JDBC_OPERATIONS = "org.springframework.jdbc.core.JdbcOperations";

  private static final MethodMatchers SQL_INJECTION_SUSPECTS = MethodMatchers.or(
    MethodMatchers.create()
      .ofSubTypes("org.hibernate.Session")
      .names("createQuery", "createSQLQuery")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofSubTypes(JAVA_SQL_STATEMENT)
      .names("executeQuery", "execute", "executeUpdate", "executeLargeUpdate", "addBatch")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofSubTypes(JAVA_SQL_CONNECTION)
      .names("prepareStatement", "prepareCall", "nativeSQL")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("javax.persistence.EntityManager", "jakarta.persistence.EntityManager")
      .names("createNativeQuery", "createQuery")
      .withAnyParameters()
      .build(),
    MethodMatchers.create().ofSubTypes(SPRING_JDBC_OPERATIONS, "org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate")
      .names("batchUpdate", "execute", "query", "queryForList", "queryForMap", "queryForObject",
        "queryForRowSet", "queryForInt", "queryForLong", "update", "queryForStream")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("org.springframework.jdbc.core.PreparedStatementCreatorFactory")
      .names(CONSTRUCTOR, "newPreparedStatementCreator")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofSubTypes("javax.jdo.PersistenceManager")
      .names("newQuery")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofSubTypes("javax.jdo.Query")
      .names("setFilter", "setGrouping")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofSubTypes("org.springframework.security.core.userdetails.jdbc.JdbcDaoImpl")
      .names("setAuthoritiesByUsernameQuery", "setGroupAuthoritiesByUsernameQuery", "setUsersByUsernameQuery")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofSubTypes("org.springframework.security.provisioning.JdbcUserDetailsManager")
      .names("setChangePasswordSql", "setCreateAuthoritySql", "setCreateUserSql", "setDeleteGroupAuthoritiesSql",
        "setDeleteGroupAuthoritySql", "setDeleteGroupMemberSql", "setDeleteGroupMembersSql", "setDeleteGroupSql",
        "setDeleteUserAuthoritiesSql", "setDeleteUserSql", "setFindAllGroupsSql", "setFindGroupIdSql", "setFindUsersInGroupSql",
        "setGroupAuthoritiesSql", "setInsertGroupAuthoritySql", "setInsertGroupMemberSql", "setInsertGroupSql", "setRenameGroupSql",
        "setUpdateUserSql", "setUserExistsSql")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofSubTypes("org.springframework.jdbc.core.simple.JdbcClient")
      .names("sql")
      .withAnyParameters()
      .build(),
    MethodMatchers.create()
      .ofTypes("org.springframework.data.r2dbc.repository.query.StringBasedR2dbcQuery")
      .names(CONSTRUCTOR)
      .withAnyParameters()
      .build());

  private static final String JAVA_LANG_STRING = "java.lang.String";

  private static final MethodMatchers FORMAT_METHODS = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("format", "formatted")
    .withAnyParameters()
    .build();

  private static final String JAVA_UTIL_STREAM = "java.util.stream.Stream";
  private static final String STRING_BUILDER = "java.lang.StringBuilder";
  private static final String STRING_BUFFER = "java.lang.StringBuffer";

  private static final MethodMatchers STRING_JOIN = MethodMatchers.create()
    .ofTypes(JAVA_LANG_STRING)
    .names("join")
    .withAnyParameters()
    .build();

  private static final MethodMatchers N_COPIES = MethodMatchers.create()
    .ofTypes("java.util.Collections")
    .names("nCopies")
    .withAnyParameters()
    .build();

  private static final MethodMatchers STREAM_MAP = MethodMatchers.create()
    .ofSubTypes(JAVA_UTIL_STREAM)
    .names("map")
    .withAnyParameters()
    .build();

  private static final MethodMatchers STREAM_COLLECT = MethodMatchers.create()
    .ofSubTypes(JAVA_UTIL_STREAM)
    .names("collect")
    .withAnyParameters()
    .build();

  private static final MethodMatchers COLLECTORS_JOINING = MethodMatchers.create()
    .ofTypes("java.util.stream.Collectors")
    .names("joining")
    .withAnyParameters()
    .build();

  private static final MethodMatchers BUILDER_APPEND = MethodMatchers.create()
    .ofTypes(STRING_BUILDER, STRING_BUFFER)
    .names("append")
    .withAnyParameters()
    .build();

  private static final MethodMatchers BUILDER_TO_STRING = MethodMatchers.create()
    .ofTypes(STRING_BUILDER, STRING_BUFFER)
    .names("toString")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers CLASS_NAME_METHODS = MethodMatchers.create()
    .ofTypes("java.lang.Class")
    .names("getName", "getSimpleName", "getCanonicalName", "getTypeName")
    .addWithoutParametersMatcher()
    .build();

  private static final MethodMatchers SAFE_CONCATENATION_METHODS = MethodMatchers.or(
    CLASS_NAME_METHODS,
    MethodMatchers.create()
      .ofSubTypes(JAVA_SQL_STATEMENT)
      .names("enquoteIdentifier")
      .addParametersMatcher(JAVA_LANG_STRING, "boolean")
      .build());

  /**
   * Bounds the recursive analysis of helper methods and StringBuilder usages: inside a helper body other helpers are not
   * followed, and inside the arguments appended to a builder neither helpers nor other builders are followed.
   */
  private enum Scope {
    FULL(true, true),
    IN_HELPER(false, true),
    IN_BUILDER(false, false);

    private final boolean followsHelpers;
    private final boolean followsBuilders;

    Scope(boolean followsHelpers, boolean followsBuilders) {
      this.followsHelpers = followsHelpers;
      this.followsBuilders = followsBuilders;
    }
  }

  private static final String MAIN_MESSAGE = "Make sure using a dynamically formatted SQL query is safe here.";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (anyMatch(tree)) {
      Optional<ExpressionTree> sqlStringArg = arguments(tree)
        .filter(arg -> arg.symbolType().is(JAVA_LANG_STRING))
        .findFirst();

      if (sqlStringArg.isPresent()) {
        ExpressionTree sqlArg = sqlStringArg.get();
        if (isDynamicString(sqlArg)) {
          reportIssue(sqlArg, MAIN_MESSAGE);
        } else if (sqlArg.is(Tree.Kind.IDENTIFIER)) {
          IdentifierTree identifierTree = (IdentifierTree) sqlArg;
          Symbol symbol = identifierTree.symbol();
          ExpressionTree initializerOrExpression = getInitializerOrExpression(symbol.declaration());
          List<AssignmentExpressionTree> reassignments = getReassignments(symbol.owner().declaration(), symbol.usages());

          if ((initializerOrExpression != null && isDynamicString(initializerOrExpression)) ||
            reassignments.stream().anyMatch(SQLInjectionCheck::isDynamicPlusAssignment)) {
            reportIssue(sqlArg, MAIN_MESSAGE, secondaryLocations(initializerOrExpression, reassignments, identifierTree.name()), null);
          }
        }
      }
    }
  }

  private static List<JavaFileScannerContext.Location> secondaryLocations(@Nullable ExpressionTree initializerOrExpression,
    List<AssignmentExpressionTree> reassignments,
    String identifierName) {
    List<JavaFileScannerContext.Location> secondaryLocations = reassignments.stream()
      .map(assignment -> new JavaFileScannerContext.Location("SQL Query is assigned to '" + getVariableName(assignment) + "'",
        assignment.expression()))
      .collect(Collectors.toCollection(ArrayList::new));

    if (initializerOrExpression != null) {
      secondaryLocations.add(new JavaFileScannerContext.Location("SQL Query is dynamically formatted and assigned to '" + identifierName + "'",
        initializerOrExpression));
    }
    return secondaryLocations;
  }

  private static String getVariableName(AssignmentExpressionTree assignment) {
    ExpressionTree variable = assignment.variable();
    return ((IdentifierTree) variable).name();
  }

  private static Stream<ExpressionTree> arguments(Tree methodTree) {
    if (methodTree.is(Tree.Kind.METHOD_INVOCATION)) {
      return ((MethodInvocationTree) methodTree).arguments().stream();
    }
    if (methodTree.is(Tree.Kind.NEW_CLASS)) {
      return ((NewClassTree) methodTree).arguments().stream();
    }
    return Stream.empty();
  }

  private static boolean anyMatch(Tree tree) {
    if (!hasArguments(tree)) {
      return false;
    }
    if (tree.is(Tree.Kind.NEW_CLASS)) {
      return SQL_INJECTION_SUSPECTS.matches((NewClassTree) tree);
    }
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      return SQL_INJECTION_SUSPECTS.matches((MethodInvocationTree) tree);
    }
    return false;
  }

  private static boolean hasArguments(Tree tree) {
    return arguments(tree).findAny().isPresent();
  }

  private static boolean isDynamicPlusAssignment(ExpressionTree arg) {
    return arg.is(Tree.Kind.PLUS_ASSIGNMENT) && hasDynamicConcatenationOperand(((AssignmentExpressionTree) arg).expression());
  }

  private static boolean isDynamicString(ExpressionTree arg) {
    return isDynamicConcatenation(arg) || isDynamicFormat(arg);
  }

  private static boolean isDynamicConcatenation(ExpressionTree arg) {
    return arg.is(Tree.Kind.PLUS) && hasDynamicConcatenationOperand(arg);
  }

  private static boolean hasDynamicConcatenationOperand(ExpressionTree arg) {
    return hasDynamicConcatenationOperand(arg, Scope.FULL);
  }

  private static boolean hasDynamicConcatenationOperand(ExpressionTree arg, Scope scope) {
    ExpressionTree expression = ExpressionUtils.skipParentheses(arg);
    if (expression.asConstant().isPresent() || isPrimitiveOrWrapper(expression.symbolType())
      || isSafeExpression(expression, SAFE_CONCATENATION_METHODS, new HashSet<>(), scope)) {
      return false;
    }
    if (expression instanceof ConditionalExpressionTree conditional) {
      return hasDynamicConcatenationOperand(conditional.trueExpression(), scope)
        || hasDynamicConcatenationOperand(conditional.falseExpression(), scope);
    }
    if (expression instanceof BinaryExpressionTree binaryExpression && expression.is(Tree.Kind.PLUS)) {
      return hasDynamicConcatenationOperand(binaryExpression.leftOperand(), scope)
        || hasDynamicConcatenationOperand(binaryExpression.rightOperand(), scope);
    }
    return true;
  }

  private static boolean isPrimitiveOrWrapper(Type type) {
    return type.isPrimitive() || type.isPrimitiveWrapper();
  }

  private static boolean isSafeExpression(ExpressionTree arg, MethodMatchers safeMethods, Set<Symbol> visitedSymbols, Scope scope) {
    ExpressionTree expression = ExpressionUtils.skipParentheses(arg);
    if (expression instanceof MethodInvocationTree methodInvocation) {
      return safeMethods.matches(methodInvocation) || hasFixedContent(methodInvocation, scope);
    }
    if (expression instanceof IdentifierTree identifier) {
      Symbol symbol = identifier.symbol();
      if (symbol.isLocalVariable() && !symbol.isParameter() && visitedSymbols.add(symbol)) {
        if (isBuilder(symbol.type())) {
          return scope.followsBuilders && isFixedContentBuilder(symbol, scope);
        }
        ExpressionTree singleWrite = ExpressionsHelper.getSingleWriteUsage(symbol);
        return singleWrite != null && isSafeExpression(singleWrite, safeMethods, visitedSymbols, scope);
      }
    }
    return false;
  }

  private static boolean hasFixedContent(MethodInvocationTree mit, Scope scope) {
    return isJoinOfRepeatedFixedElement(mit, scope)
      || isJoinOfMappedFixedElement(mit, scope)
      || isFormatOfFixedContent(mit, scope)
      || (scope.followsBuilders && isFixedContentBuilderToString(mit, scope))
      || (scope.followsHelpers && isFixedContentHelperCall(mit));
  }

  private static boolean isFixed(ExpressionTree expression, Scope scope) {
    return !hasDynamicConcatenationOperand(expression, scope);
  }

  private static boolean isJoinOfRepeatedFixedElement(MethodInvocationTree mit, Scope scope) {
    if (!STRING_JOIN.matches(mit) || mit.arguments().size() != 2) {
      return false;
    }
    ExpressionTree delimiter = mit.arguments().get(0);
    return ExpressionUtils.skipParentheses(mit.arguments().get(1)) instanceof MethodInvocationTree copies
      && N_COPIES.matches(copies)
      && copies.arguments().size() == 2
      && isFixed(delimiter, scope)
      && isFixed(copies.arguments().get(1), scope);
  }

  private static boolean isJoinOfMappedFixedElement(MethodInvocationTree collect, Scope scope) {
    if (!STREAM_COLLECT.matches(collect) || collect.arguments().size() != 1
      || !(ExpressionUtils.skipParentheses(collect.arguments().get(0)) instanceof MethodInvocationTree joining)
      || !COLLECTORS_JOINING.matches(joining)
      || !joining.arguments().stream().allMatch(argument -> isFixed(argument, scope))) {
      return false;
    }
    return collect.methodSelect() instanceof MemberSelectExpressionTree select
      && ExpressionUtils.skipParentheses(select.expression()) instanceof MethodInvocationTree map
      && STREAM_MAP.matches(map)
      && map.arguments().size() == 1
      && map.arguments().get(0) instanceof LambdaExpressionTree lambda
      && lambda.body() instanceof ExpressionTree body
      && isFixed(body, scope);
  }

  private static boolean isFormatOfFixedContent(MethodInvocationTree mit, Scope scope) {
    if (!FORMAT_METHODS.matches(mit)) {
      return false;
    }
    boolean isStatic = mit.methodSymbol().isStatic();
    boolean fixedReceiver = isStatic || (mit.methodSelect() instanceof MemberSelectExpressionTree select && isFixed(select.expression(), scope));
    List<ExpressionTree> arguments = mit.arguments();
    for (int i = 0; i < arguments.size(); i++) {
      boolean isLeadingLocale = isStatic && i == 0 && arguments.get(i).symbolType().is("java.util.Locale");
      if (!isLeadingLocale && !isFixed(arguments.get(i), scope)) {
        return false;
      }
    }
    return fixedReceiver;
  }

  private static boolean isFixedContentBuilderToString(MethodInvocationTree mit, Scope scope) {
    if (!BUILDER_TO_STRING.matches(mit) || !(mit.methodSelect() instanceof MemberSelectExpressionTree select)) {
      return false;
    }
    ExpressionTree root = ExpressionUtils.skipParentheses(select.expression());
    while (root instanceof MethodInvocationTree call && call.methodSelect() instanceof MemberSelectExpressionTree callSelect && BUILDER_APPEND.matches(call)) {
      root = ExpressionUtils.skipParentheses(callSelect.expression());
    }
    return root instanceof IdentifierTree identifier && isBuilder(identifier.symbol().type()) && isFixedContentBuilder(identifier.symbol(), scope);
  }

  private static boolean isBuilder(Type type) {
    return type.is(STRING_BUILDER) || type.is(STRING_BUFFER);
  }

  /**
   * A local StringBuilder/StringBuffer is fixed-content when it is created with fixed arguments (or returned by a fixed
   * helper) and every usage is either an append of fixed content (possibly chained), a toString call, or a read-only
   * use (return, expression statement, operand of a concatenation, argument of format). Any other usage (passed as
   * argument, aliased, reassigned) makes its content unknown.
   */
  private static boolean isFixedContentBuilder(Symbol symbol, Scope scope) {
    return symbol.isLocalVariable() && !symbol.isParameter()
      && symbol.declaration() instanceof VariableTree variable
      && isFixedBuilderInitializer(variable.initializer(), scope)
      && symbol.usages().stream().allMatch(usage -> isFixedContentBuilderUsage(usage, variable));
  }

  private static boolean isFixedBuilderInitializer(@Nullable ExpressionTree initializer, Scope scope) {
    if (initializer instanceof NewClassTree creation) {
      return creation.arguments().stream().allMatch(argument -> isFixed(argument, Scope.IN_BUILDER));
    }
    return scope.followsHelpers && initializer instanceof MethodInvocationTree call && isFixedContentHelperCall(call);
  }

  private static boolean isFixedContentBuilderUsage(IdentifierTree usage, VariableTree declaration) {
    Tree current = usage;
    while (current.parent() instanceof MemberSelectExpressionTree select && select.expression() == current
      && select.parent() instanceof MethodInvocationTree call && call.methodSelect() == select) {
      if (BUILDER_TO_STRING.matches(call)) {
        return true;
      }
      if (!BUILDER_APPEND.matches(call) || !call.arguments().stream().allMatch(argument -> isFixed(argument, Scope.IN_BUILDER))) {
        return false;
      }
      current = call;
    }
    Tree parent = current.parent();
    return (parent instanceof ReturnStatementTree && enclosingScope(parent) == enclosingScope(declaration))
      || parent instanceof ExpressionStatementTree
      || parent.is(Tree.Kind.PLUS)
      || (parent instanceof Arguments arguments && arguments.parent() instanceof MethodInvocationTree call && FORMAT_METHODS.matches(call));
  }

  @Nullable
  private static Tree enclosingScope(Tree tree) {
    Tree current = tree.parent();
    while (current != null && !(current instanceof MethodTree || current instanceof LambdaExpressionTree || current instanceof ClassTree)) {
      current = current.parent();
    }
    return current;
  }

  /**
   * A call to a non-overridable method declared in the same file is fixed-content when it has at least one return
   * statement and all of them return fixed content. Other helpers called from inside the helper are not followed.
   */
  private static boolean isFixedContentHelperCall(MethodInvocationTree mit) {
    Symbol.MethodSymbol method = mit.methodSymbol();
    MethodTree declaration = method.declaration();
    if (declaration == null || declaration.block() == null || !(method.isPrivate() || method.isStatic() || method.isFinal())) {
      return false;
    }
    Type returnType = method.returnType().type();
    if (!(returnType.is(JAVA_LANG_STRING) || returnType.is(STRING_BUILDER) || returnType.is(STRING_BUFFER))) {
      return false;
    }
    ReturnCollector collector = new ReturnCollector();
    declaration.block().accept(collector);
    return !collector.returns.isEmpty()
      && collector.returns.stream().allMatch(returnTree -> returnTree.expression() != null && isFixed(returnTree.expression(), Scope.IN_HELPER));
  }

  private static class ReturnCollector extends BaseTreeVisitor {
    private final List<ReturnStatementTree> returns = new ArrayList<>();

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      returns.add(tree);
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // intentionally empty: do not collect return statements from nested lambdas
    }

    @Override
    public void visitClass(ClassTree tree) {
      // intentionally empty: do not collect return statements from nested classes
    }
  }

  private static boolean isDynamicFormat(Tree tree) {
    return tree instanceof MethodInvocationTree mit
      && FORMAT_METHODS.matches(mit)
      && hasDynamicStringParameters(mit);
  }

  /**
   * Checks if parameters to format/formatted are dynamic variables susceptible to SQL injection.
   */
  private static boolean hasDynamicStringParameters(MethodInvocationTree mit) {
    boolean firstArg = true;
    for (ExpressionTree arg: mit.arguments()) {
      Type type = arg.symbolType();
      // `format` has a variant with Locale as the first argument - we do not need to check that parameter.
      boolean isFirstLocaleArgument = firstArg && type.is("java.util.Locale");
      // Primitives will not lead to SQL injection, so the code is compliant.
      if (!isFirstLocaleArgument && !type.isUnknown() && !isPrimitiveOrWrapper(type)
        && arg.asConstant().isEmpty() && !isSafeExpression(arg, CLASS_NAME_METHODS, new HashSet<>(), Scope.FULL)) {
        return true;
      }
      firstArg = false;
    }
    return false;
  }
}
