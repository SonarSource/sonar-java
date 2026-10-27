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
package org.sonar.java.checks.spring;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.java.utils.SpringUtils;
import org.sonar.plugins.java.api.DependencyVersionAware;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.Version;
import org.sonar.plugins.java.api.cfg.ControlFlowGraph;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "S8989")
public class TransactionalMethodCheckedExceptionCheck extends IssuableSubscriptionVisitor implements DependencyVersionAware {

  private static final List<String> TRANSACTIONAL_PREFIXES = List.of(
    "save", "delete", "update", "persist", "merge", "flush", "insert");

  private static final List<String> READ_PREFIXES = List.of(
    "get", "find", "read", "exists", "count", "is", "has", "query", "select", "load", "create", "contains");

  private static final List<String> PERSISTENCE_TYPES = List.of(
    "javax.persistence.EntityManager",
    "jakarta.persistence.EntityManager",
    "javax.persistence.Query",
    "jakarta.persistence.Query",
    "org.hibernate.SharedSessionContract",
    "org.springframework.jdbc.core.JdbcOperations",
    "org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations",
    "org.springframework.data.repository.Repository",
    "java.sql.Statement");

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;

    // @Transactional has no effect on non-public methods (Spring proxy-based AOP only intercepts public methods)
    if (isNonPublicMethod(method)) {
      return;
    }

    List<TypeTree> throwsClauses = method.throwsClauses();
    if (throwsClauses.isEmpty()) {
      return;
    }

    List<Type> checkedExceptions = throwsClauses.stream()
      .map(TypeTree::symbolType)
      .filter(TransactionalMethodCheckedExceptionCheck::isCheckedException)
      .toList();

    if (checkedExceptions.isEmpty()) {
      return;
    }

    AnnotationTree transactionalAnnotation = getTransactionalAnnotation(method);
    if (transactionalAnnotation == null) {
      return;
    }

    // Check if the annotation tree itself has rollback configuration
    if (hasRollbackConfiguration(transactionalAnnotation)) {
      return;
    }

    // No transaction is created with NOT_SUPPORTED or NEVER propagation, so rollback configuration is inapplicable
    if (hasNonTransactionalPropagation(transactionalAnnotation)) {
      return;
    }

    // Read-only transactions perform no write operations, making rollback policies irrelevant
    if (hasReadOnly(transactionalAnnotation)) {
      return;
    }

    boolean isClassLevel = isClassLevelAnnotation(method, transactionalAnnotation);

    if (isClassLevel && !looksTransactional(method)) {
      return;
    }

    if (allCheckedExceptionsThrownBeforeWrites(method)) {
      return;
    }

    var issueBuilder = QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(method.simpleName());

    if (isClassLevel) {
      issueBuilder
        .withMessage("Specify rollback behavior for checked exceptions using \"rollbackFor\" or \"noRollbackFor\" attributes on the class-level @Transactional.")
        .withSecondaries(new JavaFileScannerContext.Location("Class-level @Transactional annotation", transactionalAnnotation))
        .withQuickFixes(() -> computeQuickFixes(transactionalAnnotation, checkedExceptions));
    } else {
      issueBuilder
        .withMessage("Specify rollback behavior for checked exceptions using \"rollbackFor\" or \"noRollbackFor\" attributes.")
        .withSecondaries(new JavaFileScannerContext.Location("@Transactional annotation", transactionalAnnotation))
        .withQuickFixes(() -> computeQuickFixes(transactionalAnnotation, checkedExceptions));
    }

    issueBuilder.report();
  }

  private static AnnotationTree getTransactionalAnnotation(MethodTree method) {
    // Check method-level annotation first (including meta-annotations)
    for (AnnotationTree annotation : method.modifiers().annotations()) {
      if (isTransactionalAnnotation(annotation)) {
        return annotation;
      }
    }

    // Check class-level annotation
    Tree parent = method.parent();
    while (parent != null && !parent.is(Tree.Kind.CLASS, Tree.Kind.INTERFACE, Tree.Kind.ENUM, Tree.Kind.RECORD)) {
      parent = parent.parent();
    }

    if (parent instanceof ClassTree classTree) {
      for (AnnotationTree annotation : classTree.modifiers().annotations()) {
        if (isTransactionalAnnotation(annotation)) {
          return annotation;
        }
      }
    }

    return null;
  }

  private static boolean isClassLevelAnnotation(MethodTree method, AnnotationTree annotation) {
    // Check if the annotation is on the method itself
    for (AnnotationTree methodAnnotation : method.modifiers().annotations()) {
      if (methodAnnotation == annotation) {
        return false;
      }
    }
    // If not on the method, it must be class-level
    return true;
  }

  private static boolean isTransactionalAnnotation(AnnotationTree annotation) {
    // Check if the annotation itself is @Transactional
    if (annotation.symbolType().is(SpringUtils.TRANSACTIONAL_ANNOTATION)) {
      return true;
    }
    // Check if the annotation is meta-annotated with @Transactional (composed annotation)
    return annotation.symbolType().symbol().metadata().isAnnotatedWith(SpringUtils.TRANSACTIONAL_ANNOTATION);
  }

  private List<JavaQuickFix> computeQuickFixes(AnnotationTree annotation, List<Type> checkedExceptions) {
    List<JavaQuickFix> quickFixes = new ArrayList<>();

    // Quick fix 1: Add rollbackFor with all checked exceptions (using fully qualified names)
    String exceptionsList = checkedExceptions.stream()
      .map(Type::fullyQualifiedName)
      .map(name -> name + ".class")
      .collect(Collectors.joining(", "));

    String rollbackForAttribute = (checkedExceptions.size() == 1)
      ? ("rollbackFor = " + exceptionsList)
      : ("rollbackFor = {" + exceptionsList + "}");

    // Only add the specific exceptions quick fix if it's different from Exception.class
    boolean isAlreadyExceptionClass = checkedExceptions.size() == 1
      && "java.lang.Exception".equals(checkedExceptions.get(0).fullyQualifiedName());

    if (!isAlreadyExceptionClass) {
      quickFixes.add(createQuickFix(annotation, rollbackForAttribute, "Add rollbackFor attribute"));
    }

    // Quick fix 2: Add rollbackFor = Exception.class (covers all checked exceptions)
    quickFixes.add(createQuickFix(annotation, "rollbackFor = java.lang.Exception.class", "Add rollbackFor = Exception.class"));

    return quickFixes;
  }

  private JavaQuickFix createQuickFix(AnnotationTree annotation, String attribute, String description) {
    Arguments arguments = annotation.arguments();

    if (arguments.isEmpty()) {
      // @Transactional -> @Transactional(rollbackFor = ...)
      String annotationTypeName = QuickFixHelper.contentForTree(annotation.annotationType(), context);
      String replacement = annotationTypeName + "(" + attribute + ")";
      return JavaQuickFix.newQuickFix(description)
        .addTextEdit(JavaTextEdit.replaceTree(annotation, "@" + replacement))
        .build();
    } else {
      // @Transactional(timeout = 30) -> @Transactional(timeout = 30, rollbackFor = ...)
      SyntaxToken closeParenToken = arguments.closeParenToken();
      return JavaQuickFix.newQuickFix(description)
        .addTextEdit(JavaTextEdit.insertBeforeTree(closeParenToken, ", " + attribute))
        .build();
    }
  }

  private static boolean hasNonTransactionalPropagation(AnnotationTree annotation) {
    return annotation.arguments().stream()
      .anyMatch(arg -> {
        if (arg.is(Tree.Kind.ASSIGNMENT)) {
          var assignment = (AssignmentExpressionTree) arg;
          String name = ((IdentifierTree) assignment.variable()).name();
          if ("propagation".equals(name)) {
            String enumName = resolveEnumConstantName(assignment.expression());
            return "NOT_SUPPORTED".equals(enumName) || "NEVER".equals(enumName);
          }
        }
        return false;
      });
  }

  private static boolean hasReadOnly(AnnotationTree annotation) {
    return annotation.arguments().stream()
      .anyMatch(arg -> {
        if (arg.is(Tree.Kind.ASSIGNMENT)) {
          var assignment = (AssignmentExpressionTree) arg;
          String name = ((IdentifierTree) assignment.variable()).name();
          if ("readOnly".equals(name)) {
            ExpressionTree expression = assignment.expression();
            return expression.is(Tree.Kind.BOOLEAN_LITERAL) && "true".equals(((LiteralTree) expression).value());
          }
        }
        return false;
      });
  }

  private static String resolveEnumConstantName(ExpressionTree expression) {
    if (expression.is(Tree.Kind.MEMBER_SELECT)) {
      return ((MemberSelectExpressionTree) expression).identifier().name();
    } else if (expression.is(Tree.Kind.IDENTIFIER)) {
      return ((IdentifierTree) expression).name();
    }
    return "";
  }

  private static boolean isCheckedException(Type type) {
    if (type.isUnknown()) {
      return false;
    }

    return type.isSubtypeOf("java.lang.Exception")
      && !type.isSubtypeOf("java.lang.RuntimeException")
      && !type.isSubtypeOf("java.lang.Error");
  }

  private static boolean hasRollbackConfiguration(AnnotationTree annotation) {
    return annotation.arguments().stream()
      .anyMatch(arg -> {
        if (arg.is(Tree.Kind.ASSIGNMENT)) {
          var assignment = (AssignmentExpressionTree) arg;
          String name = ((IdentifierTree) assignment.variable()).name();
          return "rollbackFor".equals(name)
            || "rollbackForClassName".equals(name)
            || "noRollbackFor".equals(name)
            || "noRollbackForClassName".equals(name);
        }
        return false;
      });
  }

  private static boolean looksTransactional(MethodTree method) {
    if (hasTransactionalPrefix(method.simpleName().name())) {
      return true;
    }
    var visitor = new MethodInvocationVisitor(TransactionalMethodCheckedExceptionCheck::hasTransactionalName);
    method.accept(visitor);
    return visitor.foundTransactionalCall;
  }

  /**
   * Uses the control flow graph of the method to determine whether every point that may throw a checked exception
   * is executed before any write operation. In that case, no write can have happened when a checked exception is
   * thrown, so the rollback behavior is irrelevant.
   * Returns false when the method has no body, no write operation or no point throwing a checked exception.
   */
  private static boolean allCheckedExceptionsThrownBeforeWrites(MethodTree method) {
    ControlFlowGraph cfg = method.cfg();
    if (cfg == null) {
      return false;
    }
    boolean hasWrite = false;
    boolean hasThrowPoint = false;
    for (ControlFlowGraph.Block block : cfg.blocks()) {
      List<Tree> items = blockItems(block);
      for (int i = 0; i < items.size(); i++) {
        hasThrowPoint |= isCheckedThrowPoint(items.get(i));
        if (isWriteOperation(items.get(i))) {
          hasWrite = true;
          if (items.subList(i, items.size()).stream().anyMatch(TransactionalMethodCheckedExceptionCheck::isCheckedThrowPoint)
            || hasReachableThrowPoint(block)
            || isInTryWithResourcesClosingWithCheckedException(items.get(i))) {
            return false;
          }
        }
      }
    }
    return hasWrite && hasThrowPoint;
  }

  private static List<Tree> blockItems(ControlFlowGraph.Block block) {
    Tree terminator = block.terminator();
    if (terminator == null) {
      return block.elements();
    }
    List<Tree> items = new ArrayList<>(block.elements());
    items.add(terminator);
    return items;
  }

  private static boolean hasReachableThrowPoint(ControlFlowGraph.Block start) {
    Set<ControlFlowGraph.Block> visited = new HashSet<>();
    Deque<ControlFlowGraph.Block> toVisit = new ArrayDeque<>();
    addSuccessors(start, toVisit);
    while (!toVisit.isEmpty()) {
      ControlFlowGraph.Block block = toVisit.pop();
      if (visited.add(block)) {
        if (blockItems(block).stream().anyMatch(TransactionalMethodCheckedExceptionCheck::isCheckedThrowPoint)) {
          return true;
        }
        addSuccessors(block, toVisit);
      }
    }
    return false;
  }

  private static void addSuccessors(ControlFlowGraph.Block block, Deque<ControlFlowGraph.Block> toVisit) {
    toVisit.addAll(block.successors());
    toVisit.addAll(block.exceptions());
  }

  private static boolean isWriteOperation(Tree tree) {
    if (!(tree instanceof MethodInvocationTree) && !(tree instanceof NewClassTree)) {
      return false;
    }
    var visitor = new MethodInvocationVisitor(TransactionalMethodCheckedExceptionCheck::isPotentialWrite);
    tree.accept(visitor);
    return visitor.foundTransactionalCall;
  }

  private static boolean hasTransactionalName(MethodInvocationTree mit) {
    return hasTransactionalPrefix(ExpressionUtils.methodName(mit).name());
  }

  /**
   * A method invocation is considered as a potential write when its name starts with a transactional prefix,
   * or when it is invoked on a persistence API (EntityManager, JdbcTemplate, repository, ...) and its name
   * does not look like a read operation.
   */
  private static boolean isPotentialWrite(MethodInvocationTree mit) {
    if (hasTransactionalName(mit)) {
      return true;
    }
    Symbol.MethodSymbol symbol = mit.methodSymbol();
    if (symbol.isUnknown()) {
      return false;
    }
    Type ownerType = symbol.owner().type();
    return ownerType != null
      && PERSISTENCE_TYPES.stream().anyMatch(ownerType::isSubtypeOf)
      && !hasPrefix(ExpressionUtils.methodName(mit).name(), READ_PREFIXES);
  }

  /**
   * The control flow graph does not model the implicit close() calls of try-with-resources, which are executed
   * after the body and may throw checked exceptions.
   */
  private static boolean isInTryWithResourcesClosingWithCheckedException(Tree tree) {
    Tree parent = tree.parent();
    while (parent != null && !(parent instanceof MethodTree)) {
      if (parent instanceof TryStatementTree tryStatement
        && tryStatement.resourceList().stream().anyMatch(TransactionalMethodCheckedExceptionCheck::closeMayThrowCheckedException)) {
        return true;
      }
      parent = parent.parent();
    }
    return false;
  }

  private static boolean closeMayThrowCheckedException(Tree resource) {
    Type type = resource instanceof VariableTree variable ? variable.symbol().type() : ((ExpressionTree) resource).symbolType();
    return findCloseMethod(type).map(TransactionalMethodCheckedExceptionCheck::mayThrowCheckedException).orElse(true);
  }

  private static Optional<Symbol.MethodSymbol> findCloseMethod(@Nullable Type type) {
    if (type == null || type.isUnknown()) {
      return Optional.empty();
    }
    Symbol.TypeSymbol typeSymbol = type.symbol();
    Optional<Symbol.MethodSymbol> close = typeSymbol.lookupSymbols("close").stream()
      .filter(Symbol::isMethodSymbol)
      .map(Symbol.MethodSymbol.class::cast)
      .filter(m -> m.parameterTypes().isEmpty())
      .findFirst();
    if (close.isPresent()) {
      return close;
    }
    return Stream.concat(Stream.of(typeSymbol.superClass()), typeSymbol.interfaces().stream())
      .map(TransactionalMethodCheckedExceptionCheck::findCloseMethod)
      .flatMap(Optional::stream)
      .findFirst();
  }

  private static boolean isCheckedThrowPoint(Tree tree) {
    if (tree instanceof ThrowStatementTree throwStatement) {
      Type type = throwStatement.expression().symbolType();
      return type.isUnknown() || isCheckedException(type);
    }
    if (tree instanceof MethodInvocationTree mit) {
      return mayThrowCheckedException(mit.methodSymbol());
    }
    if (tree instanceof NewClassTree newClass) {
      return mayThrowCheckedException(newClass.methodSymbol());
    }
    return false;
  }

  private static boolean mayThrowCheckedException(Symbol.MethodSymbol symbol) {
    return symbol.isUnknown() || symbol.thrownTypes().stream().anyMatch(type -> type.isUnknown() || isCheckedException(type));
  }

  private static boolean hasTransactionalPrefix(String name) {
    return hasPrefix(name, TRANSACTIONAL_PREFIXES);
  }

  private static boolean hasPrefix(String name, List<String> prefixes) {
    String lowerName = name.toLowerCase(Locale.ROOT);
    return prefixes.stream().anyMatch(prefix -> {
      if (!lowerName.startsWith(prefix)) {
        return false;
      }
      if (name.length() == prefix.length()) {
        return true;
      }
      char next = name.charAt(prefix.length());
      return Character.isUpperCase(next) || Character.isDigit(next) || next == '_';
    });
  }

  private static class MethodInvocationVisitor extends BaseTreeVisitor {
    private final Predicate<MethodInvocationTree> isTransactionalCall;
    boolean foundTransactionalCall = false;

    MethodInvocationVisitor(Predicate<MethodInvocationTree> isTransactionalCall) {
      this.isTransactionalCall = isTransactionalCall;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (!foundTransactionalCall && isTransactionalCall.test(tree)) {
        foundTransactionalCall = true;
        return;
      }
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree tree) {
      if (isMethodArgument(tree)) {
        super.visitLambdaExpression(tree);
      }
    }

    @Override
    public void visitClass(ClassTree tree) {
      // Do not traverse into anonymous or local class declarations
    }

    private static boolean isMethodArgument(LambdaExpressionTree lambda) {
      Tree parent = lambda.parent();
      while (parent != null) {
        if (parent instanceof MethodInvocationTree || parent instanceof NewClassTree) {
          return true;
        }
        if (!(parent instanceof Arguments)) {
          return false;
        }
        parent = parent.parent();
      }
      return false;
    }
  }

  private static boolean isNonPublicMethod(MethodTree method) {
    if (ModifiersUtils.hasModifier(method.modifiers(), Modifier.PRIVATE)
      || ModifiersUtils.hasModifier(method.modifiers(), Modifier.PROTECTED)) {
      return true;
    }
    // Methods without an explicit access modifier are package-private in classes but implicitly public in interfaces
    if (!ModifiersUtils.hasModifier(method.modifiers(), Modifier.PUBLIC)) {
      Tree parent = method.parent();
      return parent == null || !parent.is(Tree.Kind.INTERFACE);
    }
    return false;
  }

  @Override
  public boolean isCompatibleWithDependencies(Function<String, Optional<Version>> dependencyFinder) {
    Optional<Version> springContextVersion = dependencyFinder.apply("spring-context");
    Optional<Version> springTxVersion = dependencyFinder.apply("spring-tx");
    return springTxVersion.isPresent() || springContextVersion.isPresent();
  }
}
