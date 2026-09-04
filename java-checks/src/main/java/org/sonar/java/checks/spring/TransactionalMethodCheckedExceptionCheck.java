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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.DependencyVersionAware;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.Version;
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
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S8989")
public class TransactionalMethodCheckedExceptionCheck extends IssuableSubscriptionVisitor implements DependencyVersionAware {

  private static final List<String> TRANSACTIONAL_PREFIXES = List.of(
    "save", "delete", "update", "persist", "merge", "flush", "insert");

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
    var visitor = new MethodInvocationVisitor();
    method.accept(visitor);
    return visitor.foundTransactionalCall;
  }

  private static boolean hasTransactionalPrefix(String name) {
    String lowerName = name.toLowerCase(Locale.ROOT);
    return TRANSACTIONAL_PREFIXES.stream().anyMatch(prefix -> {
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
    boolean foundTransactionalCall = false;

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (!foundTransactionalCall) {
        String calledMethodName = ExpressionUtils.methodName(tree).name();
        if (hasTransactionalPrefix(calledMethodName)) {
          foundTransactionalCall = true;
          return;
        }
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
