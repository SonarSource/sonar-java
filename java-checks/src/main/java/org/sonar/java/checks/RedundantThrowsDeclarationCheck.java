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
package org.sonar.java.checks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.java.RspecKey;
import org.sonar.java.checks.helpers.Javadoc;
import org.sonar.java.checks.serialization.SerializableContract;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.resolve.JavaSymbol;
import org.sonar.java.resolve.JavaType;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.BlockTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LambdaExpressionTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.ThrowStatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TryStatementTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

@Rule(key = "RedundantThrowsDeclarationCheck")
@RspecKey("S1130")
public class RedundantThrowsDeclarationCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return Arrays.asList(Tree.Kind.METHOD, Tree.Kind.CONSTRUCTOR);
  }

  @Override
  public void visitNode(Tree tree) {
    ListTree<TypeTree> thrownList = ((MethodTree) tree).throwsClauses();
    if (!hasSemantic() || thrownList.isEmpty()) {
      return;
    }
    checkMethodThrownList((MethodTree) tree, thrownList);
  }

  private void checkMethodThrownList(MethodTree methodTree, ListTree<TypeTree> thrownList) {
    Set<Type> thrownExceptions = thrownExceptionsFromBody(methodTree);
    boolean isOverridableMethod = ((JavaSymbol.MethodJavaSymbol) methodTree.symbol()).isOverridable();
    List<String> undocumentedExceptionNames = new Javadoc(methodTree).undocumentedThrownExceptions();
    Set<String> reported = new HashSet<>();

    for (TypeTree typeTree : thrownList) {
      Type exceptionType = typeTree.symbolType();
      if (exceptionType.isUnknown()) {
        continue;
      }
      String fullyQualifiedName = exceptionType.fullyQualifiedName();
      if (!reported.contains(fullyQualifiedName)) {
        String superTypeName = isSubclassOfAny(exceptionType, thrownList);
        if (superTypeName != null) {
          reportIssue(typeTree, String.format("Remove the declaration of thrown exception '%s' which is a subclass of '%s'.", fullyQualifiedName, superTypeName));
        } else if (exceptionType.isSubtypeOf("java.lang.RuntimeException")) {
          reportIssue(typeTree, String.format("Remove the declaration of thrown exception '%s' which is a runtime exception.", fullyQualifiedName));
        } else if (declaredMoreThanOnce(fullyQualifiedName, thrownList)) {
          reportIssue(typeTree, String.format("Remove the redundant '%s' thrown exception declaration(s).", fullyQualifiedName));
        } else if (canNotBeThrown(methodTree, exceptionType, thrownExceptions) && (!isOverridableMethod || undocumentedExceptionNames.contains(exceptionType.name()))) {
          reportIssue(typeTree, String.format("Remove the declaration of thrown exception '%s', as it cannot be thrown from %s's body.", fullyQualifiedName,
            methodTreeType(methodTree)));
        }
        reported.add(fullyQualifiedName);
      }
    }
  }

  private static String methodTreeType(MethodTree tree) {
    return tree.is(Tree.Kind.CONSTRUCTOR) ? "constructor" : "method";
  }

  private static boolean canNotBeThrown(MethodTree methodTree, Type exceptionType, @Nullable Set<Type> thrownExceptions) {
    if (isOverridingOrDesignedForExtension(methodTree)
      || !exceptionType.isSubtypeOf("java.lang.Exception")
      || exceptionType.isSubtypeOf("java.lang.RuntimeException")
      || thrownExceptions == null) {
      return false;
    }

    if (thrownExceptions.stream().anyMatch(t -> ((JavaType) t).isTagged(JavaType.TYPEVAR))) {
      // should be handled by SONARJAVA-1778 - type substitution not applied on thrown type when parameterized on parametric methods
      return false;
    }

    return thrownExceptions.stream().noneMatch(t -> t.isSubtypeOf(exceptionType));
  }

  private static boolean isOverridingOrDesignedForExtension(MethodTree methodTree) {
    // we need to be sure that it's not an override
    return !Boolean.FALSE.equals(methodTree.isOverriding())
      || SerializableContract.SERIALIZABLE_CONTRACT_METHODS.contains(methodTree.simpleName().name())
      || isDesignedForExtension(methodTree);
  }

  private static boolean isDesignedForExtension(MethodTree methodTree) {
    ModifiersTree modifiers = methodTree.modifiers();
    if (ModifiersUtils.hasModifier(modifiers, Modifier.PRIVATE)) {
      return false;
    }
    return ModifiersUtils.hasModifier(modifiers, Modifier.DEFAULT)
      || emptyBody(methodTree)
      || onlyReturnLiteralsOrThrowException(methodTree);
  }

  private static boolean onlyReturnLiteralsOrThrowException(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    if (block == null) {
      return false;
    }
    List<StatementTree> body = block.body();
    if (body.size() != 1) {
      return false;
    }
    StatementTree singleStatement = body.get(0);
    return singleStatement.is(Tree.Kind.THROW_STATEMENT) || returnStatementWithLiteral(singleStatement);
  }

  private static boolean returnStatementWithLiteral(StatementTree statement) {
    if (statement.is(Tree.Kind.RETURN_STATEMENT)) {
      ExpressionTree expression = ((ReturnStatementTree) statement).expression();
      return expression == null || ExpressionUtils.skipParentheses(expression).is(
        Tree.Kind.NULL_LITERAL,
        Tree.Kind.STRING_LITERAL,
        Tree.Kind.BOOLEAN_LITERAL,
        Tree.Kind.CHAR_LITERAL,
        Tree.Kind.DOUBLE_LITERAL,
        Tree.Kind.FLOAT_LITERAL,
        Tree.Kind.LONG_LITERAL,
        Tree.Kind.INT_LITERAL);
    }
    return false;
  }

  private static boolean emptyBody(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    return block != null && block.body().isEmpty();
  }

  @Nullable
  private static Set<Type> thrownExceptionsFromBody(MethodTree methodTree) {
    BlockTree block = methodTree.block();
    if (block != null) {
      ThrownExceptionVisitor visitor = new ThrownExceptionVisitor(methodTree);
      block.accept(visitor);
      return visitor.thrownExceptions();
    }
    return null;
  }

  private static class ThrownExceptionVisitor extends BaseTreeVisitor {
    private Set<Type> thrownExceptions = new HashSet<>();
    private boolean visitedUnknown = false;
    private boolean visitedOtherConstructor = false;
    private final MethodTree methodTree;
    private static final String CONSTRUCTOR_NAME = "<init>";

    ThrownExceptionVisitor(MethodTree methodTree) {
      this.methodTree = methodTree;
    }

    @Nullable
    public Set<Type> thrownExceptions() {
      if (visitedUnknown || thrownExceptions.stream().anyMatch(Type::isUnknown)) {
        // as soon as there is an unknown type, we discard any attempt to find an issue
        return null;
      }
      if (methodTree.is(Tree.Kind.CONSTRUCTOR) && !visitedOtherConstructor) {
        getImplicitlyCalledConstructor(methodTree)
          .map(Symbol.MethodSymbol::thrownTypes)
          .ifPresent(thrownExceptions::addAll);
      }
      return thrownExceptions;
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
      if (CONSTRUCTOR_NAME.equals(tree.symbol().name())) {
        visitedOtherConstructor = true;
      }
      addThrownTypes(tree.symbol());
      super.visitMethodInvocation(tree);
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
      addThrownTypes(tree.constructorSymbol());
      super.visitNewClass(tree);
    }

    private void addThrownTypes(Symbol methodSymbol) {
      if (!visitedUnknown) {
        if (methodSymbol.isUnknown() || !methodSymbol.isMethodSymbol()) {
          visitedUnknown = true;
        } else {
          thrownExceptions.addAll(((Symbol.MethodSymbol) methodSymbol).thrownTypes());
        }
      }
    }

    @Override
    public void visitThrowStatement(ThrowStatementTree tree) {
      Type exceptionType = tree.expression().symbolType();
      thrownExceptions.add(exceptionType);
      super.visitThrowStatement(tree);
    }

    @Override
    public void visitTryStatement(TryStatementTree tree) {
      for (Tree resource : tree.resourceList()) {
        Type resourceType = resourceType(resource);
        List<Type> thrownTypes = closeMethodThrownTypes(resourceType);
        if (thrownTypes == null) {
          visitedUnknown = true;
        } else {
          thrownExceptions.addAll(thrownTypes);
        }
      }
      super.visitTryStatement(tree);
    }

    private static Type resourceType(Tree resource) {
      if (resource.is(Tree.Kind.VARIABLE)) {
        return ((VariableTree) resource).type().symbolType();
      }
      return ((TypeTree) resource).symbolType();
    }

    @Override
    public void visitClass(ClassTree tree) {
      // skip anonymous classes
    }

    @Override
    public void visitLambdaExpression(LambdaExpressionTree lambdaExpressionTree) {
      // skip lambdas
    }

    @CheckForNull
    private static List<Type> closeMethodThrownTypes(Type classType) {
      return classType.symbol().lookupSymbols("close").stream()
        .filter(Symbol::isMethodSymbol)
        .map(Symbol.MethodSymbol.class::cast)
        .filter(method -> method.parameterTypes().isEmpty())
        .map(Symbol.MethodSymbol::thrownTypes)
        .findFirst()
        .orElseGet(() -> directSuperTypeStream(classType).map(ThrownExceptionVisitor::closeMethodThrownTypes)
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null));
    }

    private static Stream<Type> directSuperTypeStream(Type classType) {
      Symbol.TypeSymbol symbol = classType.symbol();
      Stream<Type> interfaceStream = symbol.interfaces().stream();
      Type superClass = symbol.superClass();
      return superClass != null ? Stream.concat(Stream.of(superClass), interfaceStream) : interfaceStream;
    }

    private static Optional<Symbol.MethodSymbol> getImplicitlyCalledConstructor(MethodTree methodTree) {
      Type superType = ((Symbol.TypeSymbol)methodTree.symbol().owner()).superClass();
      // superClass() returns null only for java.lang.Object; it is not possible.
      return Objects.requireNonNull(superType).symbol().memberSymbols().stream()
        .filter(ThrownExceptionVisitor::isDefaultConstructor)
        .map(Symbol.MethodSymbol.class::cast)
        .findFirst();
    }

    private static boolean isDefaultConstructor(Symbol symbol) {
      if (symbol.isMethodSymbol()) {
        Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) symbol;
        if (CONSTRUCTOR_NAME.equals(methodSymbol.name())) {
          if (methodSymbol.declaration() != null) {
            // Constructor is inside this file, in case of nested class, parameterTypes() will include an extra implicit
            // parameter type. We hopefully have access to the declaration that does not include implicit parameter.
            return methodSymbol.declaration().parameters().isEmpty();
          } else {
            // The declaration is in another class, we can use parameterTypes() safely since it can not be nested.
            return  methodSymbol.parameterTypes().isEmpty();
          }
        }
      }
      return false;
    }
  }

  private static boolean declaredMoreThanOnce(String fullyQualifiedName, ListTree<TypeTree> thrown) {
    boolean firstOccurrenceFound = false;
    for (TypeTree typeTree : thrown) {
      if (typeTree.symbolType().is(fullyQualifiedName)) {
        if (firstOccurrenceFound) {
          return true;
        } else {
          firstOccurrenceFound = true;
        }
      }
    }
    return false;
  }

  private static String isSubclassOfAny(Type type, ListTree<TypeTree> thrownList) {
    for (TypeTree thrown : thrownList) {
      String name = thrown.symbolType().fullyQualifiedName();
      if (!type.is(name) && type.isSubtypeOf(name)) {
        return name;
      }
    }
    return null;
  }
}
