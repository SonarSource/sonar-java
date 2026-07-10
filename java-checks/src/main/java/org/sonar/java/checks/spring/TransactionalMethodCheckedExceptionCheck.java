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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.check.Rule;
import org.sonar.java.checks.helpers.QuickFixHelper;
import org.sonar.java.checks.helpers.SpringUtils;
import org.sonar.java.reporting.JavaQuickFix;
import org.sonar.java.reporting.JavaTextEdit;
import org.sonar.plugins.java.api.DependencyVersionAware;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.Version;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

@Rule(key = "S8989")
public class TransactionalMethodCheckedExceptionCheck extends IssuableSubscriptionVisitor implements DependencyVersionAware {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Collections.singletonList(Tree.Kind.METHOD);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodTree method = (MethodTree) tree;

    List<TypeTree> throwsClauses = method.throwsClauses();
    if (throwsClauses.isEmpty()) {
      return;
    }

    List<Type> checkedExceptions = throwsClauses.stream()
      .map(TypeTree::symbolType)
      .filter(this::isCheckedException)
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

    QuickFixHelper.newIssue(context)
      .forRule(this)
      .onTree(transactionalAnnotation)
      .withMessage("Specify rollback behavior for checked exceptions using \"rollbackFor\" or \"noRollbackFor\" attributes.")
      .withQuickFixes(() -> computeQuickFixes(transactionalAnnotation, checkedExceptions))
      .report();
  }

  private static AnnotationTree getTransactionalAnnotation(MethodTree method) {
    // Check method-level annotation first
    for (AnnotationTree annotation : method.modifiers().annotations()) {
      if (annotation.symbolType().is(SpringUtils.TRANSACTIONAL_ANNOTATION)) {
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
        if (annotation.symbolType().is(SpringUtils.TRANSACTIONAL_ANNOTATION)) {
          return annotation;
        }
      }
    }

    return null;
  }

  private List<JavaQuickFix> computeQuickFixes(AnnotationTree annotation, List<Type> checkedExceptions) {
    List<JavaQuickFix> quickFixes = new ArrayList<>();

    // Quick fix 1: Add rollbackFor with all checked exceptions
    String exceptionsList = checkedExceptions.stream()
      .map(Type::name)
      .map(name -> name + ".class")
      .collect(Collectors.joining(", "));

    String rollbackForAttribute = checkedExceptions.size() == 1
      ? "rollbackFor = " + exceptionsList
      : "rollbackFor = {" + exceptionsList + "}";

    quickFixes.add(createQuickFix(annotation, rollbackForAttribute, "Add rollbackFor attribute"));

    // Quick fix 2: Add rollbackFor = Exception.class (covers all checked exceptions)
    quickFixes.add(createQuickFix(annotation, "rollbackFor = Exception.class", "Add rollbackFor = Exception.class"));

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

  private boolean isCheckedException(Type type) {
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
          var assignment = (org.sonar.plugins.java.api.tree.AssignmentExpressionTree) arg;
          String name = ((org.sonar.plugins.java.api.tree.IdentifierTree) assignment.variable()).name();
          return "rollbackFor".equals(name)
            || "rollbackForClassName".equals(name)
            || "noRollbackFor".equals(name)
            || "noRollbackForClassName".equals(name);
        }
        return false;
      });
  }

  private boolean hasRollbackConfiguration(SymbolMetadata metadata) {
    List<SymbolMetadata.AnnotationValue> values = metadata.valuesForAnnotation(SpringUtils.TRANSACTIONAL_ANNOTATION);
    if (values == null) {
      return false;
    }

    return values.stream()
      .anyMatch(av -> "rollbackFor".equals(av.name())
        || "rollbackForClassName".equals(av.name())
        || "noRollbackFor".equals(av.name())
        || "noRollbackForClassName".equals(av.name()));
  }

  @Override
  public boolean isCompatibleWithDependencies(Function<String, Optional<Version>> dependencyFinder) {
    Optional<Version> springContextVersion = dependencyFinder.apply("spring-context");
    Optional<Version> springTxVersion = dependencyFinder.apply("spring-tx");
    return springTxVersion.isPresent() || springContextVersion.isPresent();
  }
}
