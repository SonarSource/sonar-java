/*
 * SonarQube Java
 * Copyright (C) 2012-2025 SonarSource SA
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
package org.sonar.java.checks.security;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.checks.methods.AbstractMethodDetection;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.plugins.java.api.semantic.MethodMatchers;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AssignmentExpressionTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;

import static org.sonar.java.checks.helpers.ReassignmentFinder.getReassignments;

@Rule(key = "S3329")
public class CipherBlockChainingCheck extends AbstractMethodDetection {
  private final SecureByteArrayGeneratorDetector secureByteArrayGeneratorDetector = new SecureByteArrayGeneratorDetector();

  private static final MethodMatchers SECURE_RANDOM_GENERATE_SEED = MethodMatchers.create()
    .ofTypes("java.security.SecureRandom")
    .names("generateSeed")
    .withAnyParameters()
    .build();

  @Override
  protected MethodMatchers getMethodInvocationMatchers() {
    return MethodMatchers.create().ofTypes("javax.crypto.spec.IvParameterSpec").constructor()
      .addParametersMatcher(types -> !types.isEmpty())
      .build();
  }

  @Override
  protected void onConstructorFound(NewClassTree newClassTree) {
    if (newClassTree.arguments().isEmpty() || secureByteArrayGeneratorDetector.isDynamicallyGenerated(newClassTree.arguments().get(0))) {
      return;
    }

    var mTree = ExpressionUtils.getEnclosingMethod(newClassTree);
    if (mTree == null) {
      return;
    }

    var hasBeenSecurelyInitialized = SecureInitializationFinder
      .forIvParameterSpecConstruction(newClassTree)
      .appliesSecureInitialization(mTree);

    if (!hasBeenSecurelyInitialized) {
      reportIssue(newClassTree, "Use a dynamically-generated, random IV.");
    }
  }

  private static class SecureInitializationFinder extends BaseTreeVisitor {

    private boolean hasBeenSecurelyInitialized = false;
    private final ExpressionTree ivBytesExpression;
    private final @Nullable NewClassTree ivParameterSpecInstantiation;
    // to be used in case of assignment to a variable
    private final @Nullable Symbol ivParameterSymbol;

    private static final MethodMatchers SECURE_RANDOM_NEXT_BYTES = MethodMatchers.create()
      .ofTypes("java.security.SecureRandom")
      .names("nextBytes")
      .withAnyParameters()
      .build();
    private static final MethodMatchers CIPHER_INIT = MethodMatchers.create()
      .ofTypes("javax.crypto.Cipher")
      .names("init")
      .withAnyParameters()
      .build();

    private static final MethodMatchers BYTEBUFFER_GET = MethodMatchers.create()
      .ofTypes("java.nio.ByteBuffer")
      .names("get")
      .withAnyParameters()
      .build();

    // value of javax.crypto.Cipher.DECRYPT_MODE
    private static final int CIPHER_INIT_DECRYPT_MODE = 2;

    private SecureInitializationFinder(ExpressionTree ivBytesExpression, @Nullable NewClassTree ivParameterSpecTree) {
      this.ivBytesExpression = ivBytesExpression;
      this.ivParameterSpecInstantiation = ivParameterSpecTree;
      this.ivParameterSymbol = ivParameterSpecTree != null ? ivSymbol(ivParameterSpecTree) : null;
    }

    public static SecureInitializationFinder forIvParameterSpecConstruction(NewClassTree ivParameterSpecInstantiation) {
      var bytesExpression = ivParameterSpecInstantiation.arguments().get(0);

      return new SecureInitializationFinder(bytesExpression, ivParameterSpecInstantiation);
    }

    public static SecureInitializationFinder forIvBytesExpression(ExpressionTree ivBytesExpression) {
      return new SecureInitializationFinder(ivBytesExpression, null);
    }

    public boolean appliesSecureInitialization(Tree tree) {
      tree.accept(this);
      return hasBeenSecurelyInitialized;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree methodInvocation) {
      if (SECURE_RANDOM_NEXT_BYTES.matches(methodInvocation)) {
        Symbol initVector = symbol(ivBytesExpression);
        if (!initVector.isUnknown() && initVector.equals(symbol(methodInvocation.arguments().get(0)))) {
          hasBeenSecurelyInitialized = true;
        }
      }
      // make sure it is not used for decryption - in such case you need to reuse one
      if (CIPHER_INIT.matches(methodInvocation) && methodInvocation.arguments().size() > 2) {
        int opMode = methodInvocation.arguments().get(0).asConstant(Integer.class).orElse(-1);
        if (CIPHER_INIT_DECRYPT_MODE == opMode && isPartOfArguments(methodInvocation)) {
          hasBeenSecurelyInitialized = true;
        }
      }
      if (isInitVectorCopiedFromByteBuffer(methodInvocation)) {
        hasBeenSecurelyInitialized = true;
      }
      if (methodInvocation.methodSymbol().isUnknown()) {
        hasBeenSecurelyInitialized = true;
      }

      super.visitMethodInvocation(methodInvocation);
    }

    private boolean isInitVectorCopiedFromByteBuffer(MethodInvocationTree methodInvocation) {
      if (!BYTEBUFFER_GET.matches(methodInvocation)) {
        return false;
      }
      Symbol initVector = symbol(ivBytesExpression);
      return methodInvocation.arguments().stream()
        .map(SecureInitializationFinder::symbol)
        .filter(argument -> argument.type().is("byte[]"))
        .anyMatch(initVector::equals);
    }

    private boolean isPartOfArguments(MethodInvocationTree methodInvocation) {
      if (ivParameterSpecInstantiation == null || ivParameterSymbol == null) {
        return false;
      }

      return isPartOfArguments(methodInvocation, ivParameterSpecInstantiation)
        || (!ivParameterSymbol.isUnknown() && isPartOfArguments(methodInvocation, ivParameterSymbol));
    }

    private static boolean isPartOfArguments(MethodInvocationTree methodInvocation, ExpressionTree ivParameter) {
      return methodInvocation.arguments()
        .stream()
        .map(ExpressionUtils::skipParentheses)
        .anyMatch(ivParameter::equals);
    }

    private static boolean isPartOfArguments(MethodInvocationTree methodInvocation, Symbol ivParameterSymbol) {
      return methodInvocation.arguments()
        .stream()
        .map(ExpressionUtils::skipParentheses)
        .map(SecureInitializationFinder::symbol)
        .anyMatch(ivParameterSymbol::equals);
    }

    private static Symbol symbol(ExpressionTree expression) {
      if (expression.is(Tree.Kind.IDENTIFIER)) {
        return ((IdentifierTree) expression).symbol();
      }
      if (expression.is(Tree.Kind.MEMBER_SELECT)) {
        return ((MemberSelectExpressionTree) expression).identifier().symbol();
      }
      return Symbol.UNKNOWN_SYMBOL;
    }

    private static Symbol ivSymbol(NewClassTree newClassTree) {
      Tree parent = newClassTree.parent();
      if (parent.is(Tree.Kind.VARIABLE)) {
        return ((VariableTree) parent).symbol();
      }
      if (parent.is(Tree.Kind.ASSIGNMENT)) {
        return symbol(((AssignmentExpressionTree) parent).variable());
      }
      return Symbol.UNKNOWN_SYMBOL;
    }
  }

  private static class SecureByteArrayGeneratorDetector {
    public boolean isDynamicallyGenerated(ExpressionTree tree) {
      if (tree instanceof IdentifierTree identifierTree) {
        Symbol symbol = identifierTree.symbol();
        if (symbol.isParameter()) {
          return true;
        }
      }

      return findConstructingMethods(tree)
        .anyMatch(SECURE_RANDOM_GENERATE_SEED::matches);
    }

    private static Stream<MethodInvocationTree> findConstructingMethods(ExpressionTree expressionTree) {
      if (expressionTree instanceof MethodInvocationTree methodInvocationTree) {
        return Stream.of(methodInvocationTree);
      }

      if (!(expressionTree instanceof IdentifierTree identifierTree) || !(identifierTree.symbol() instanceof Symbol.VariableSymbol variableSymbol)) {
        return Stream.empty();
      }

      var declaration = variableSymbol.declaration();
      if (declaration == null) {
        return Stream.empty();
      }

      var initializerStream = Stream.<MethodInvocationTree>of();
      if (declaration.initializer() instanceof MethodInvocationTree methodInvocationTree) {
        initializerStream = Stream.of(methodInvocationTree);
      }

      var reassignments = getReassignments(declaration, variableSymbol.usages())
        .stream()
        .map(assignmentExpressionTree -> assignmentExpressionTree.expression() instanceof MethodInvocationTree methodInvocationTree ? methodInvocationTree : null)
        .filter(Objects::nonNull);

      return Stream.concat(initializerStream, reassignments);
    }
  }

  /**
   * Collects all methods that construct IV byte arrays in a secure way.
   * After applying this visitor to a class tree, {@link SecureByteArrayFactoryFinder#producesSecureBytesArray(MethodInvocationTree)} can
   * be used to check whether the invocation of a method belonging to that tree will return such a secure array.
   */
  private static class SecureByteArrayFactoryFinder extends BaseTreeVisitor {
    private @Nullable MethodTree currentMethodTree = null;
    private final Set<String> secureByteArrayFactories = new HashSet<>();
    private final SecureByteArrayGeneratorDetector secureByteArrayGeneratorDetector = new SecureByteArrayGeneratorDetector();

    public boolean producesSecureBytesArray(MethodInvocationTree methodInvocation) {
      return secureByteArrayFactories.contains(methodInvocation.methodSymbol().signature());
    }

    public void clear() {
      secureByteArrayFactories.clear();
    }

    @Override
    public void visitMethod(MethodTree tree) {
      // There is no need to explore methods that do not even produce IV byte arrays
      if (doesNotReturnByteArray(tree)
        // We do not track nested methods
        || currentMethodTree != null) {
        return;
      }

      currentMethodTree = tree;
      super.visitMethod(tree);
      currentMethodTree = null;
    }

    @Override
    public void visitReturnStatement(ReturnStatementTree tree) {
      super.visitReturnStatement(tree);
      if (currentMethodTree == null) {
        return;
      }

      var returnedExpression = tree.expression();
      if (returnedExpression == null) {
        return;
      }

      if (secureByteArrayGeneratorDetector.isDynamicallyGenerated(returnedExpression)) {
        markAsSecureFactory();
        return;
      }

      var isSecureBytesArray = SecureInitializationFinder
        .forIvBytesExpression(returnedExpression)
        .appliesSecureInitialization(currentMethodTree);

      if (isSecureBytesArray) {
        markAsSecureFactory();
      }
    }

    private void markAsSecureFactory() {
      Objects.requireNonNull(currentMethodTree);
      secureByteArrayFactories.add(currentMethodTree.symbol().signature());
    }

    private static boolean doesNotReturnByteArray(MethodTree methodTree) {
      var returnType = methodTree.returnType();
      if (returnType == null) {
        return false;
      }

      return !"byte[]".equals(returnType.symbolType().fullyQualifiedName());
    }
  }
}
