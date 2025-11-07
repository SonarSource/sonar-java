/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.annotations.VisibleForTesting;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.LiteralUtils;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S2384")
public class MutableMembersUsageCheck extends BaseTreeVisitor implements JavaFileScanner {

  private static final List<String> MUTABLE_TYPES = Arrays.asList(
    "java.util.Collection",
    "java.util.Date",
    "java.util.Map");
  private static final List<String> IMMUTABLE_TYPES = Arrays.asList(
    "java.util.Collections.UnmodifiableCollection",
    "java.util.Collections.UnmodifiableMap",
    "com.google.common.collect.ImmutableCollection",
    "com.google.common.collect.ImmutableMap");

  private static final MethodMatchers UNMODIFIABLE_COLLECTION_CALL = MethodMatchers.or(
    MethodMatchers.create().ofType(type -> MutableMembersUsageCheck.containsImmutableLikeTerm(type.name())).anyName().withAnyParameters().build(),
    MethodMatchers.create().ofAnyType().name(MutableMembersUsageCheck::containsImmutableLikeTerm).withAnyParameters().build(),
    MethodMatchers.create().ofTypes("java.util.Collections")
      .name(name -> name.startsWith("singleton") || name.startsWith("empty"))
      .withAnyParameters().build(),
    MethodMatchers.create().ofTypes("java.util.Set", "java.util.List").names("of", "copyOf").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("com.google.common.collect.Sets").names("union", "intersection", "difference", "symmetricDifference").withAnyParameters().build(),
    MethodMatchers.create().ofTypes("com.google.common.collect.Lists").names("asList").withAnyParameters().build()
  );

  private static final MethodMatchers STREAM_COLLECT_CALL = MethodMatchers.create().
    ofTypes("java.util.stream.Stream")
    .names("collect")
    .addParametersMatcher("java.util.stream.Collector")
    .build();

  private static final MethodMatchers UNMODIFIABLE_COLLECTOR_CALL = MethodMatchers.create().
    ofTypes("java.util.stream.Collectors")
    .names("toUnmodifiableSet", "toUnmodifiableList", "toUnmodifiableMap")
    .withAnyParameters()
    .build();

  private final Deque<List<Symbol>> parametersStack = new LinkedList<>();

  private final Deque<String> methodSignatureStack = new ArrayDeque<>();

  /**
   * In the context of a method call, this means the argument at {@code argumentIndex}, is given by the
   * calling method parameter at {@code parameterIndex}. For instance, in {@code int f(int a, int b) { g(0, 1, a); }}, we would have
   * a mapping (0, 2): parameter 0 of `f`, i.e. a, is used as argument 2 of `g`.
   */
  @VisibleForTesting
  record ArgumentParameterMapping(int parameterIndex, int argumentIndex) {
  }

  /**
   * Maps index of arguments at the call site to parameters of the calling method.
   * For instance in `int f(int a, int b) { g(b, 2, 3); h(4, a);}`, we would have two call-sites:
   * `{g, (1,0)} and {h, (0, 1)}`
   */
  @VisibleForTesting
  record CallSite(String methodSignature, List<ArgumentParameterMapping> parameters) {

    /**
     * For debugging.
     */
    @Override
    public String toString() {
      return "{" + methodSignature + ", <" +
        parameters.stream()
          .map(entry -> entry.parameterIndex + "->" + entry.argumentIndex)
          .collect(Collectors.joining(", ")) + ">}";
    }
  }

  /**
   * Identifier of field in which a parameter of method is stored.
   * For instance, for method `void foo(int[] a) { myField = a; }`, would be denoted by `ParameterStore("myField", 0)`.
   */
  private record ParameterStore(IdentifierTree memberId, int parameterIndex) {
  }

  /**
   * Track how data passed as arguments are propagated through methods of the class. The goal is to detect if data passed to
   * a public method, ultimately ends-up being stored in a field of the class. This would be reported in
   * {@link #reportMutableStoreReachableByOutsideCall(JavaFileScannerContext, JavaFileScanner)}.
   */
  private static class MutableDataPropagationGraph {
    /**
     * Node of the graph are method signatures.
     * Edges are only present if the source node is a method that calls the target method with an argument which
     * is a mutable value directly coming from a parameter.
     */
    private final Map<String, List<CallSite>> callGraph = new HashMap<>();

    /**
     * Keys in this map are nodes of the graph that store a mutable parameter in a field.
     * Values are identifiers corresponding to such fields and the index of the corresponding parameter.
     */
    private final Map<String, List<ParameterStore>> mutableStoredByMethod = new HashMap<>();

    /**
     * Signatures of methods that are callable from outside the class.
     */
    private final Set<String> nonPrivateMethods = new HashSet<>();

    /**
     * Maps method that return the result of another method
     */
    private final Map<String, List<String>> passingThroughMethod = new HashMap<>();

    /**
     * Map methods that return private mutable values to the identifier of the mutable they are returning.
     */
    private final Map<String, List<IdentifierTree>> methodsReturningPrivateMutable = new HashMap<>();

    public void clear() {
      mutableStoredByMethod.clear();
      callGraph.clear();
      nonPrivateMethods.clear();
      passingThroughMethod.clear();
      methodsReturningPrivateMutable.clear();
    }

    private static final List<CallSite> EMPTY_CALL_SITE_LIST = new ArrayList<>();

    /**
     * Refers to the i-th parameter in the context of the method given by the signature.
     */
    private record MethodEntryPoint(String methodSignature, Integer paramIndex) {
    }

    /**
     * Look for entry points for the given method, for which there are call sites using the same parameter.
     */
    private List<MethodEntryPoint> findEntryPointsWithOutgoingEdges(String methodSignature) {
      return callGraph.getOrDefault(methodSignature, EMPTY_CALL_SITE_LIST).stream()
        .flatMap(callSite -> callSite.parameters().stream())
        .map(ArgumentParameterMapping::parameterIndex)
        .distinct()
        .map(parameter -> new MethodEntryPoint(methodSignature, parameter))
        .toList();
    }

    public void reportMutableStoreReachableByOutsideCall(JavaFileScannerContext context, JavaFileScanner check) {
      // Set of mutable store reachable by outside calls, that will need reporting.
      Set<ParameterStore> reachableMutableStore = new HashSet<>();

      // Add all parameters stored directly by non-private methods
      mutableStoredByMethod.entrySet().stream()
        .filter(entry -> nonPrivateMethods.contains(entry.getKey()))
        .forEach(entry -> reachableMutableStore.addAll(entry.getValue()));

      // An entry point in the queue means that the i-th argument of method m may have been a mutable value provided to some
      // method callable outside the class.
      Deque<MethodEntryPoint> toProcess = new ArrayDeque<>();

      for (String method : nonPrivateMethods) {
        toProcess.addAll(findEntryPointsWithOutgoingEdges(method));
      }

      // Keep track of already explored entry points to avoid infinite loops
      Set<MethodEntryPoint> explored = new HashSet<>();

      while (!toProcess.isEmpty()) {
        MethodEntryPoint current = toProcess.pop();
        if (explored.add(current)) {
          if (mutableStoredByMethod.containsKey(current.methodSignature())) {
            mutableStoredByMethod.get(current.methodSignature()).stream()
              .filter(parameterStore -> parameterStore.parameterIndex == current.paramIndex())
              .forEach(reachableMutableStore::add);
          }
          List<CallSite> callSites = callGraph.getOrDefault(current.methodSignature(), EMPTY_CALL_SITE_LIST);
          for (CallSite callSite : callSites) {
            callSite.parameters.stream()
              .filter(mapping -> mapping.parameterIndex == current.paramIndex())
              .map(mapping -> new MethodEntryPoint(callSite.methodSignature, mapping.argumentIndex))
              .forEach(toProcess::add);
          }
        }
      }

      for (ParameterStore parameterStore : reachableMutableStore) {
        context.reportIssue(check, parameterStore.memberId,
          "Store a copy of \"" + parameterStore.memberId.name() + "\".");
      }
    }

    private void reportMutableFieldReachingToOutside(JavaFileScannerContext context, JavaFileScanner check) {
      Set<IdentifierTree> mutableValuesToReport = new HashSet<>();
      ArrayDeque<String> queue = new ArrayDeque<>(nonPrivateMethods);
      Set<String> explored = new HashSet<>();

      while (!queue.isEmpty()) {
        String current = queue.pop();
        if (methodsReturningPrivateMutable.containsKey(current)) {
          mutableValuesToReport.addAll(methodsReturningPrivateMutable.get(current));
        }
        if (explored.add(current)) {
          queue.addAll(passingThroughMethod.getOrDefault(current, new ArrayList<>()));
        }
      }

      for (IdentifierTree identifierTree : mutableValuesToReport) {
        context.reportIssue(check, identifierTree, "Return a copy of \"" + identifierTree.name() + "\".");
      }
    }

    /**
     * Record whether the method is callable from outside the class.
     */
    public void addMethod(MethodTree tree) {
      if (!tree.symbol().isPrivate()) {
        nonPrivateMethods.add(tree.symbol().signature());
      }
    }

    /**
     * Add edges between nodes if a call propagates a mutable parameter.
     */
    public void addMethodInvocation(Symbol.MethodSymbol methodSymbol, Arguments arguments, String callerSignature, List<Symbol> callerParameters) {
      List<ArgumentParameterMapping> mutableParameters = findMutableParameters(arguments, callerParameters);
      callGraph.computeIfAbsent(callerSignature, s -> new ArrayList<>())
        .add(new CallSite(methodSymbol.signature(), mutableParameters));
    }

    /**
     * Returns the indexes of arguments to the indexes of mutable parameters that correspond.
     */
    private static List<ArgumentParameterMapping> findMutableParameters(Arguments arguments, List<Symbol> parameters) {
      List<ArgumentParameterMapping> result = new ArrayList<>();
      for (int i = 0; i < arguments.size(); ++i) {
        ExpressionTree arg = arguments.get(i);
        if (isMutableType(arg) && arg instanceof IdentifierTree id) {
          int correspondingParameterIndex = parameters.indexOf(id.symbol());
          if (correspondingParameterIndex != -1) {
            result.add(new ArgumentParameterMapping(correspondingParameterIndex, i));
          }
        }
      }
      return result;
    }

    /**
     * Record that the method is assigning a mutable parameter in the given member.
     */
    public void addStore(String methodSignature, IdentifierTree assignedMember, int parameterIndex) {
      mutableStoredByMethod.computeIfAbsent(methodSignature, k -> new ArrayList<>())
        .add(new ParameterStore(assignedMember, parameterIndex));
    }

    private void addPassingThroughMethod(String callingMethodSignature, String calledMethodSignature) {
      passingThroughMethod.computeIfAbsent(callingMethodSignature, key -> new ArrayList<>())
        .add(calledMethodSignature);
    }

    private void addMethodReturningPrivateMutable(String methodSignature, IdentifierTree mutableIdentifier) {
      methodsReturningPrivateMutable
        .computeIfAbsent(methodSignature, key -> new ArrayList<>())
        .add(mutableIdentifier);
    }
  }

  private final MutableDataPropagationGraph dataPropagationGraph = new MutableDataPropagationGraph();

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    scan(context.getTree());
    dataPropagationGraph.reportMutableStoreReachableByOutsideCall(context, this);
    dataPropagationGraph.reportMutableFieldReachingToOutside(context, this);
    dataPropagationGraph.clear();
  }

  @Override
  public void visitMethod(MethodTree tree) {
    if (tree.is(Tree.Kind.CONSTRUCTOR)) {
      Symbol.TypeSymbol enclosingClass = tree.symbol().enclosingClass();
      if (enclosingClass.isEnum()) {
        return;
      }
    }
    dataPropagationGraph.addMethod(tree);
    parametersStack.push(tree.parameters().stream()
      .map(VariableTree::symbol)
      .toList());
    methodSignatureStack.push(tree.symbol().signature());
    super.visitMethod(tree);
    methodSignatureStack.pop();
    parametersStack.pop();
  }

  @Override
  public void visitMethodInvocation(MethodInvocationTree tree) {
    if (!methodSignatureStack.isEmpty() && !parametersStack.isEmpty()) {
      dataPropagationGraph.addMethodInvocation(
        tree.methodSymbol(), tree.arguments(), methodSignatureStack.peek(), parametersStack.peek());
    }
    super.visitMethodInvocation(tree);
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    if (!methodSignatureStack.isEmpty() && !parametersStack.isEmpty()) {
      dataPropagationGraph.addMethodInvocation(
        tree.methodSymbol(), tree.arguments(), methodSignatureStack.peek(), parametersStack.peek());
    }

    super.visitNewClass(tree);
  }

  @Override
  public void visitAssignmentExpression(AssignmentExpressionTree tree) {
    super.visitAssignmentExpression(tree);
    if (!isMutableType(tree.expression())) {
      return;
    }
    ExpressionTree variable = tree.variable();
    Symbol leftSymbol = null;
    if (variable.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) variable;
      leftSymbol = identifierTree.symbol();
    } else if (variable.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mit = (MemberSelectExpressionTree) variable;
      leftSymbol = mit.identifier().symbol();
    }
    if (leftSymbol != null && leftSymbol.isPrivate()) {
      checkStore(tree.expression());
    }
  }

  /**
   * Check whether a mutable parameter is assigned to a private field
   */
  private void checkStore(ExpressionTree expression) {
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) expression;
      if (!methodSignatureStack.isEmpty() && !parametersStack.isEmpty()) {
        int parameterIndex = parametersStack.peek().indexOf(identifierTree.symbol());
        if (parameterIndex != -1) {
          dataPropagationGraph.addStore(methodSignatureStack.peek(), identifierTree, parameterIndex);
        }
      }
    }
  }

  @Override
  public void visitReturnStatement(ReturnStatementTree tree) {
    super.visitReturnStatement(tree);
    ExpressionTree expressionTree = tree.expression();
    if (expressionTree == null || !isMutableType(expressionTree)) {
      return;
    }
    checkReturnedExpression(expressionTree);
  }

  private void checkReturnedExpression(ExpressionTree expression) {
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expression;
      if (ExpressionUtils.isThis(mse.expression())) {
        checkReturnedExpression(mse.identifier());
      }
    }
    if (expression.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree identifierTree = (IdentifierTree) expression;
      if (identifierTree.symbol().isPrivate()
        && !isOnlyAssignedImmutableVariable((Symbol.VariableSymbol) identifierTree.symbol())
        && !methodSignatureStack.isEmpty()) {
        dataPropagationGraph.addMethodReturningPrivateMutable(methodSignatureStack.peek(), identifierTree);
      }
    }
    if (expression instanceof MethodInvocationTree methodInvocationTree && !methodSignatureStack.isEmpty()) {
      dataPropagationGraph.addPassingThroughMethod(methodSignatureStack.peek(), methodInvocationTree.methodSymbol().signature());
    }
  }

  private static boolean isOnlyAssignedImmutableVariable(Symbol.VariableSymbol symbol) {
    VariableTree declaration = symbol.declaration();
    if (declaration != null) {
      ExpressionTree initializer = declaration.initializer();
      if (initializer != null) {
        boolean isInitializerImmutable = !isMutableType(initializer) || isEmptyArray(initializer);
        if (symbol.isFinal() || !isInitializerImmutable) {
          // If the symbol is final or it is assigned something mutable, no need to look at re-assignment:
          // we already know if it is immutable or not.
          return isInitializerImmutable;
        }
      }
    }

    return !assignmentsOfMutableType(symbol.usages());
  }

  private static boolean isEmptyArray(ExpressionTree initializer) {
    return initializer.is(Tree.Kind.NEW_ARRAY) &&
      !((NewArrayTree) initializer).dimensions().isEmpty() &&
      ((NewArrayTree) initializer).dimensions().stream().allMatch(adt -> isZeroLiteralValue(adt.expression()));
  }

  private static boolean isZeroLiteralValue(@Nullable ExpressionTree expressionTree) {
    if (expressionTree == null) {
      return false;
    }
    Integer integer = LiteralUtils.intLiteralValue(expressionTree);
    return integer != null && integer == 0;
  }

  private static boolean assignmentsOfMutableType(List<IdentifierTree> usages) {
    for (IdentifierTree usage : usages) {
      Tree current = usage;
      Tree parent = usage.parent();
      do {
        if (parent.is(Tree.Kind.ASSIGNMENT)) {
          break;
        }
        current = parent;
        parent = current.parent();
      } while (parent != null);
      if (parent != null) {
        AssignmentExpressionTree assignment = (AssignmentExpressionTree) parent;
        if (assignment.variable().equals(current) && isMutableType(assignment.expression())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isMutableType(ExpressionTree expressionTree) {
    if (expressionTree.is(Tree.Kind.NULL_LITERAL)) {
      // In case of incomplete semantic, working with "nulltype" returns strange results, we can return early as the null will never be mutable anyway.
      return false;
    }
    if (expressionTree.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) expressionTree;
      if (UNMODIFIABLE_COLLECTION_CALL.matches(methodInvocationTree) || (isUnmodifiableCollector(methodInvocationTree))) {
        return false;
      }
    }
    return isMutableType(expressionTree.symbolType());
  }

  private static boolean isUnmodifiableCollector(MethodInvocationTree methodInvocationTree) {
    if (STREAM_COLLECT_CALL.matches(methodInvocationTree) && methodInvocationTree.arguments().get(0).is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree collector = (MethodInvocationTree) methodInvocationTree.arguments().get(0);
      return UNMODIFIABLE_COLLECTOR_CALL.matches(collector);
    }
    return false;
  }

  private static boolean isMutableType(Type type) {
    if (type.isArray()) {
      return true;
    }
    for (String mutableType : MUTABLE_TYPES) {
      if (type.isSubtypeOf(mutableType) && isNotImmutable(type)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isNotImmutable(Type type) {
    for (String immutableType : IMMUTABLE_TYPES) {
      if (type.isSubtypeOf(immutableType)) {
        return false;
      }
    }
    return true;
  }

  public static boolean containsImmutableLikeTerm(String methodName) {
    String lowerCaseName = methodName.toLowerCase(Locale.ROOT);
    return lowerCaseName.contains("unmodifiable") || lowerCaseName.contains("immutable");
  }

}
